package com.uprr.pac.convert;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.tuple.Pair;
import org.openapitools.model.*;
import org.openapitools.model.AOTUActivity;
import org.openapitools.model.AOTURouteLocation;
import org.openapitools.model.AOTUTrainData;
import org.openapitools.model.LocomotiveStatusReport;
import org.openapitools.model.MilepostType;
import org.openapitools.model.SubdivisionData;

import com.uprr.enterprise.datetime.UTCTimestamp;
import com.uprr.netcontrol.shared.xml_bindings.jaxb2.location.find_system_station_2_2.SystemStationType;
import com.uprr.pac.clients.track.state.SubdivisionTrackRange;
import com.uprr.psm.core.cache.vo.TrainCacheObjects;
import com.uprr.psm.lsc.bindings.swagger.find.subdivision.state.v1_0.*;
import com.uprr.psm.lsc.bindings.swagger.find.track.network.device.state.v1_0.DeviceData;
import com.uprr.psm.lsc.bindings.swagger.find.track.network.device.state.v1_0.FindTrackNetworkDeviceStateResponse;

public class FilesConverter {
    private static final String DIRECTION_MP_DECREASING = "2";
    private Map<Pair<Long, Integer>, DeviceData> subdivisionDevicesMap;
    private Map<Integer, SystemStationType> systemStationsMap;
    private String subdivisionName;
    private Map<String, List<Integer>> trackMilepostMap;
    private Integer subdivisionId;
    private Float lowestMP = 9999.9999f;
    private Float highestMP = 0.0f;
    private Map<Pair<String, Integer>, MilepostSegmentStateType> trackMilepostSegmentStateMap = null;
    
    public FilesConverter(FindTrackNetworkDeviceStateResponse subdivDevices, List<SystemStationType> subdivisionStations, SubdivisionTrackRange trackRange) {
        this.subdivisionName = subdivDevices.getSubdivisionName();
        this.subdivisionId = Integer.valueOf(subdivDevices.getDeviceList().get(0).getSubdivisionId());
        createTrackMilepostMap(trackRange);
        
        subdivisionDevicesMap = subdivDevices.getDeviceList().stream()
                .filter(d -> (d.getWiuId() != null && d.getWiuStatusIndex() != null
                        && trackMilepostMap.containsKey(d.getTrackName().toUpperCase())
                        && d.getDeviceTypeCode() > 0 && d.getDeviceTypeCode() < 3)
                        && d.getDeviceMilepost() != null
                        && checkHighLow(d))
                .collect(Collectors.toMap(d -> Pair.of(d.getWiuId(), d.getWiuStatusIndex()), Function.identity(),
                        (existing, replacement) -> existing));
        systemStationsMap = subdivisionStations.stream()
                .collect(Collectors.toMap(SystemStationType::getSystemStationId, Function.identity(),
                        (existing, replacement) -> existing));
        System.out.println("Started");
    }
    
    private boolean checkHighLow(DeviceData d) {
        Float milepost = d.getDeviceMilepost().getMilepost();
        if (milepost < lowestMP) {
            lowestMP = milepost;
        }
        if (milepost > highestMP) {
            highestMP = milepost;
        }
        return true;
    }
    
    public PTCSubdivisionData convertPulseModel(TrainCacheObjects trainCache) {
        SubdivisionStateData inputState = trainCache.getData().get(0);
        if (!subdivisionId.equals(inputState.getSubdivisionId())) {
            throw new IllegalArgumentException("Subdivision Mismatch expected " + subdivisionId +
                    ", got " + inputState.getSubdivisionId());
        }
        PTCSubdivisionData ptcSubdivisionData = new PTCSubdivisionData()
                .subdivisionId(inputState.getSubdivisionId())
                .subdivisionName(subdivisionName)
                .milepostSegmentStateList(createMilepostSegmentList(trainCache))
                .lastTrainReporting(createLastTrainReporting(trainCache));
        
        return ptcSubdivisionData;
        
    }
    
    private void createTrackMilepostMap(SubdivisionTrackRange trackRange) {
        trackMilepostMap = new HashMap<>();
        trackRange.getAllTrackNames().stream()
                .forEach(trackName -> trackRange.findMilepostRangesForTrack(trackName).stream()
                        .forEach(range -> range.getMilepostSpanList().stream()
                                .forEach(span -> addMileposts(trackName, span.getLowestMilepost(), span.getHighestMilepost()))));
    }
    
    private void addMileposts(String trackName, double start, double end) {
        List<Integer> milepostList = trackMilepostMap.get(trackName);
        if (milepostList == null) {
            milepostList = new ArrayList<>();
            trackMilepostMap.put(trackName.toUpperCase(), milepostList);
        }
        for (int i = Double.valueOf(start).intValue(); i < Double.valueOf(end).intValue(); i++) {
            milepostList.add(i);
        }
    }
    
    private TrainStateType createLastTrainReporting(TrainCacheObjects trainCache) {
        String leadLocomotive = trainCache.getMetadata().getLocomotiveId().trim();
        SubdivisionStateData subdivisionStateData = trainCache.getData().stream()
                .filter(d -> d.getSubdivisionId().equals(this.subdivisionId))
                .findAny().get();
        List<PTCTrainData> locoList = subdivisionStateData.getPollingLocomotiveList();
        PTCTrainData trainData = findLeadLocomotiveTrainData(locoList, leadLocomotive);
        assert (trainData != null);
        TrainStateType trainStateType = createTrainStateType(trainData);
        trainStateType.getLastReportedPosition().setPrecedingSignalState(
                findPrecedingSignal(trackMilepostSegmentStateMap, trainStateType.getLastReportedPosition()));
        trainStateType.getLastReportedPosition().setNextSignalState(
                findNextSignal(trackMilepostSegmentStateMap, trainStateType.getLastReportedPosition()));
        
        return trainStateType;
    }
    
    private TrainStateTypeTrainId createTrainId(TrainId trainId) {
        return new TrainStateTypeTrainId()
                .trainDate(trainId.getTrainDate())
                .trainSCAC(trainId.getTrainSCAC())
                .trainSection(trainId.getTrainSection())
                .trainSymbol(trainId.getTrainSymbol());
    }
    
    private List<SubdivisionData> createSubdivisionList(
            @Valid List<com.uprr.psm.lsc.bindings.swagger.find.subdivision.state.v1_0.SubdivisionData> subdivisionList) {
        return subdivisionList.stream()
                .map(s -> new SubdivisionData().subdivisionId(s.getSubdivisionId()))
                .collect(Collectors.toList());
    }
    
    private List<TrainPositionReport> createPositionHistory(@Valid List<LocomotivePositionReport> positionHistoryList) {
        return positionHistoryList.stream()
                .map(p -> new TrainPositionReport()
                        .milepostLocation(createMilepostLocationType(p.getHeadEndLocation()))
                        .positionTime(convertUTCTimestamp(p.getAuditData().getEventCreatedDateTime()))
                        .speedMPH(computeSpeed(p.getSpeed(), p.getDirectionOfTravel())))
                .collect(Collectors.toList());
    }
    
    private Integer computeSpeed(Integer speed, String directionOfTravel) {
        return DIRECTION_MP_DECREASING.equals(directionOfTravel.trim()) ? speed * -1 : speed;
    }
    
    private OffsetDateTime convertUTCTimestamp(UTCTimestamp utcTime) {
        if (utcTime == null) {
            return null;
        }
        ZoneId utc = ZoneId.of("UTC");
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(utcTime.getTimeInMillis()), utc);
    }
    
    private MilepostLocationType createMilepostLocationType(@Valid PTCLocationData headEndLocation) {
        return new MilepostLocationType()
                .subdivisionId(headEndLocation.getSubdivisionId())
                .trackName(headEndLocation.getTrackName())
                .milepost(createMilepostType(headEndLocation.getLocationMilepost()));
    }
    
    private MilepostType createMilepostType(com.uprr.psm.lsc.bindings.swagger.find.subdivision.state.v1_0.@Valid MilepostType locationMilepost) {
        return new MilepostType()
                .milepostNumber(locationMilepost.getMilepost().floatValue());
    }
    
    private List<LocomotiveStatusReport> createLocomotiveList(PTCTrainData trainData) {
        List<com.uprr.psm.lsc.bindings.swagger.find.subdivision.state.v1_0.LocomotiveStatusReport> locoList = trainData.getLocomotiveList();
        return locoList.stream()
                .map(l -> new LocomotiveStatusReport()
                        .locomotiveHorsepower(l.getLocomotiveHorsepower())
                        .locomotiveId(l.getLocomotiveId())
                        .locomotiveLength(l.getLocomotiveLength())
                        .locomotiveRunStatus(Integer.getInteger(l.getLocomotiveRunStatus()))
                        .locomotiveWeight(l.getLocomotiveWeight())
                        .positionOnTrain(l.getPositionOnTrain()))
                .collect(Collectors.toList());
    }
    
    private TrainPositionReport createLastPosition(LocomotivePositionReport lastReportedPosition) {
        return new TrainPositionReport()
                .milepostLocation(createMilepostLocationType(lastReportedPosition.getHeadEndLocation()))
                .positionTime(convertUTCTimestamp(lastReportedPosition.getAuditData().getEventCreatedDateTime()))
                .speedMPH(computeSpeed(lastReportedPosition.getSpeed(), lastReportedPosition.getDirectionOfTravel()));
    }
    
    private AOTUTrainData createAOTU(com.uprr.psm.lsc.bindings.swagger.find.subdivision.state.v1_0.AOTUTrainData aotu) {
        return new AOTUTrainData()
                .actualTonsPerAxle(aotu.getActualTonsPerAxle())
                .authorityRangeCode(aotu.getAuthorityRangeCode())
                .categoryCode(aotu.getCategoryCode())
                .eventCode(aotu.getEventCode())
                .heavy143TonCarCount(aotu.getHeavy143TonCarCount())
                .heavy158TonCarCount(aotu.getHeavy158TonCarCount())
                .highWideCarCount(aotu.getHighWideCarCount())
                .over158TonCarCount(aotu.getOver158TonCarCount())
                .ownershipCode(aotu.getOwnershipCode())
                .priorityCode(aotu.getPriorityCode())
                .routeLocationList(createLocationList(aotu.getRouteLocationList()))
                .tenantCarrier(aotu.getTenantCarrier())
                .totalCarLength(aotu.getTotalCarLength())
                .totalCarWeight(aotu.getTotalCarWeight())
                .totalLocomotiveLength(aotu.getTotalLocomotiveLength())
                .totalLocomotiveWeight(aotu.getTotalLocomotiveWeight());
    }
    
    private List<AOTURouteLocation> createLocationList(
            @Valid List<com.uprr.psm.lsc.bindings.swagger.find.subdivision.state.v1_0.AOTURouteLocation> routeLocationList) {
        return routeLocationList.stream()
                .map(l -> new AOTURouteLocation()
                        .activityList(createActivities(l.getActivityList()))
                        .isEventLocation(l.getIsEventLocation())
                        .locationId(l.getLocationId())
                        .subdivisionId(this.subdivisionId) // we will eliminate any others
                        .milepost(lookupMilepost(l.getSystemStationId()))
                        .routeSequence(l.getRouteSequence())
                        .stationType(l.getStationType()))
                .filter(location -> location.getMilepost() != null)
                .collect(Collectors.toList());
    }
    
    private List<AOTUActivity> createActivities(List<com.uprr.psm.lsc.bindings.swagger.find.subdivision.state.v1_0.AOTUActivity> activityList) {
        return activityList.stream()
                .map(a -> new AOTUActivity()
                        .activityCode(a.getActivityCode())
                        .qualifier(a.getQualifier())
                        .statusCode(a.getStatusCode()))
                .collect(Collectors.toList());
    }
    
    private PTCTrainData findLeadLocomotiveTrainData(List<PTCTrainData> locoList, String locoId) {
        return locoList.stream()
                .filter(trainData -> locoId.equalsIgnoreCase(trainData.getPtcLocomotiveId().trim()))
                .findAny()
                .orElse(null);
    }
    
    private MilepostType lookupMilepost(Integer systemStationId) {
        if (systemStationId == null) {
            return null;
        }
        SystemStationType station = systemStationsMap.get(systemStationId);
        if (station == null) {
            return null;
        }
        return new MilepostType().milepostNumber(station.getMilepost().floatValue());
    }
    
    private List<TrainPositionReport> createTrainEstimatedPositionList(PTCTrainData trainData) {
        return trainData.getTrainEstimatedPositionList().stream()
                .map(estimate -> processEstimate(estimate))
                .collect(Collectors.toList());
        
    }
    
    private TrainPositionReport processEstimate(LocomotivePositionReport estimate) {
        if (estimate.getHeadEndLocation() == null) {
            return null;
        }
        return new TrainPositionReport()
                .milepostLocation(new MilepostLocationType()
                        .milepost(createMilepostType(estimate.getHeadEndLocation().getLocationMilepost()))
                        .subdivisionId(estimate.getHeadEndLocation().getSubdivisionId())
                        .trackName(estimate.getHeadEndLocation().getTrackName()))
                .positionTime(convertUTCTimestamp(estimate.getPositionTime()))
                .speedMPH(computeSpeed(estimate.getSpeed(), estimate.getDirectionOfTravel()));
    }
    
    private List<MilepostSegmentStateType> createMilepostSegmentList(TrainCacheObjects trainCache) {
        trackMilepostSegmentStateMap = new HashMap<>();
        trainCache.getData().get(0).getDeviceList().stream().forEach(device -> processDevice(trackMilepostSegmentStateMap, device));
        trainCache.getData().get(0).getPollingLocomotiveList().stream()
                .filter(loco -> loco.getLocomotiveSystemState().getLocomotiveStateSummary().equals("1")
                        && trainCache.getMetadata().getCurrentSubdivisionId().equals(loco.getLastReportedPosition().getHeadEndLocation().getSubdivisionId())
                        && !trainCache.getMetadata().getLocomotiveId().equalsIgnoreCase(loco.getPtcLocomotiveId())) // only OTHER  trains
                .forEach(loco -> processLocomotive(trackMilepostSegmentStateMap, loco));
        return trackMilepostSegmentStateMap.values().stream()
                .sorted(Comparator.comparing(m -> Pair.of(m.getTrackName(), m.getMilepost().getMilepostNumber())))
                .collect(Collectors.toList());
    }
    
    private void processLocomotive(Map<Pair<String, Integer>, MilepostSegmentStateType> trackMilepostMap, PTCTrainData loco) {
        PTCLocationData headEndLocation = loco.getLastReportedPosition().getHeadEndLocation();
        String trackName = headEndLocation.getTrackName();
        Integer intMilepost = headEndLocation.getLocationMilepost().getMilepost().intValue();
        Pair<String, Integer> trackMilepostKey = Pair.of(trackName, intMilepost);
        MilepostSegmentStateType mpSegment = trackMilepostMap.get(trackMilepostKey);
        if (mpSegment == null) {
            mpSegment = new MilepostSegmentStateType();
            mpSegment.setMilepost(createMilepostType(headEndLocation.getLocationMilepost()));
            mpSegment.setTrackName(headEndLocation.getTrackName());
            trackMilepostMap.put(trackMilepostKey, mpSegment);
        }
        TrainStateType summaryTrainStateType = createSummaryTrainStateType(loco);
        summaryTrainStateType.getLastReportedPosition().setPrecedingSignalState(
                findPrecedingSignal(trackMilepostMap, summaryTrainStateType.getLastReportedPosition()));
        summaryTrainStateType.getLastReportedPosition().setNextSignalState(
                findNextSignal(trackMilepostMap, summaryTrainStateType.getLastReportedPosition()));
        mpSegment.addTrainListItem(summaryTrainStateType);
        
    }
    
    private SignalStateType findPrecedingSignal(Map<Pair<String, Integer>, MilepostSegmentStateType> trackMilepostMap,
            TrainPositionReport lastReportedPosition) {
        
        Float thisMP = lastReportedPosition.getMilepostLocation().getMilepost().getMilepostNumber();
        int currentMP = thisMP.intValue();
        int direction = 1;
        if (lastReportedPosition.getSpeedMPH() < 0) {
            direction = -1;
        }
        MilepostLocationType milepostLocation = lastReportedPosition.getMilepostLocation();
        if (milepostLocation != null) {
            String track = milepostLocation.getTrackName();
            while (currentMP >= lowestMP.intValue() && currentMP <= highestMP.intValue()) {
                MilepostSegmentStateType milepostSegmentStateType = trackMilepostMap.get(Pair.of(track, currentMP));
                if (milepostSegmentStateType != null) {
                    List<SignalStateType> signals = milepostSegmentStateType.getSignalList();
                    if (signals != null) {
                        for (SignalStateType signal : signals) {
                            if (direction > 0) {
                                if (signal.getMilepostLocation().getMilepostNumber() < thisMP && (signal.getSignalState() > 0)) {
                                    return signal;
                                }
                            } else {
                                if (signal.getMilepostLocation().getMilepostNumber() > thisMP && (signal.getSignalState() < 0)) {
                                    return signal;
                                }
                            }
                        }
                    }
                }
                currentMP -= direction;
            }
        }
        return null;
    }
    
    private SignalStateType findNextSignal(Map<Pair<String, Integer>, MilepostSegmentStateType> trackMilepostMap,
            TrainPositionReport lastReportedPosition) {
        Float thisMP = lastReportedPosition.getMilepostLocation().getMilepost().getMilepostNumber();
        int currentMP = thisMP.intValue();
        int direction = 1;
        if (lastReportedPosition.getSpeedMPH() < 0) {
            direction = -1;
        }
        String track = lastReportedPosition.getMilepostLocation().getTrackName();
        while (currentMP > lowestMP && currentMP < highestMP) {
            MilepostSegmentStateType milepostSegmentStateType = trackMilepostMap.get(Pair.of(track, currentMP));
            if (milepostSegmentStateType != null) {
                List<SignalStateType> signals = milepostSegmentStateType.getSignalList();
                if (signals != null) {
                    for (SignalStateType signal : signals) {
                        if (direction < 0) {
                            if (signal.getMilepostLocation().getMilepostNumber() < thisMP && (signal.getSignalState() < 0)) {
                                return signal;
                            }
                        } else {
                            if (signal.getMilepostLocation().getMilepostNumber() > thisMP && (signal.getSignalState() > 0)) {
                                return signal;
                            }
                        }
                    }
                }
            }
            currentMP += direction;
        }
        return null;
    }
    
    private TrainStateType createSummaryTrainStateType(PTCTrainData trainData) {
        return new TrainStateType()
                .emptyCarCount(trainData.getEmptyCarCount())
                .lastReportedPosition(createLastPosition(trainData.getLastReportedPosition()))
                .loadedCarCount(trainData.getLoadedCarCount())
                .ptcLeadLocomotiveId(trainData.getPtcLocomotiveId())
                .trailingTonnage(trainData.getTrailingTonnage())
                .trainId(createTrainId(trainData.getTrainId()))
                .trainLength(trainData.getTrainLength());
    }
    
    private TrainStateType createTrainStateType(PTCTrainData trainData) {
        return new TrainStateType()
                .aotuTrainData(createAOTU(trainData.getAotuTrainData()))
                .emptyCarCount(trainData.getEmptyCarCount())
                .lastReportedPosition(createLastPosition(trainData.getLastReportedPosition()))
                .loadedCarCount(trainData.getLoadedCarCount())
                .locomotiveList(createLocomotiveList(trainData))
                .maximumTrainSpeed(trainData.getMaximumTrainSpeed())
                .positionHistoryList(createPositionHistory(trainData.getPositionHistoryList()))
                .ptcLeadLocomotiveId(trainData.getPtcLocomotiveId())
                .subdivisionList(createSubdivisionList(trainData.getSubdivisionList()))
                .trailingTonnage(trainData.getTrailingTonnage())
                .trainEstimatedPositionList(createTrainEstimatedPositionList(trainData))
                .trainId(createTrainId(trainData.getTrainId()))
                .trainLength(trainData.getTrainLength());
    }
    
    private void processDevice(Map<Pair<String, Integer>, MilepostSegmentStateType> trackMilepostMap,
            com.uprr.psm.lsc.bindings.swagger.find.subdivision.state.v1_0.DeviceData device) {
        DeviceData thisDevice = subdivisionDevicesMap.get(Pair.of(device.getWiuId(), device.getWiuStatusIndex()));
        if (thisDevice == null) {
            return;
        }
        Pair<String, Integer> trackMilepostKey = Pair.of(thisDevice.getTrackName(), thisDevice.getDeviceMilepost().getMilepost().intValue());
        MilepostSegmentStateType mpSegment = trackMilepostMap.get(trackMilepostKey);
        if (mpSegment == null) {
            mpSegment = new MilepostSegmentStateType();
            mpSegment.setMilepost(new MilepostType().milepostNumber((float) thisDevice.getDeviceMilepost().getMilepost().intValue()));
            mpSegment.setTrackName(thisDevice.getTrackName());
            trackMilepostMap.put(trackMilepostKey, mpSegment);
        }
        if (thisDevice.getDeviceTypeCode() == 1) {
            mpSegment.addSignalListItem(createSignalItem(thisDevice, device));
        } else if (thisDevice.getDeviceTypeCode() == 2) {
            mpSegment.addSwitchListItem(createSwitch(thisDevice, device));
        }
    }
    
    private MilepostType createMilepostType(com.uprr.psm.lsc.bindings.swagger.find.track.network.device.state.v1_0.@Valid MilepostType deviceMilepost) {
        return new MilepostType().milepostNumber(deviceMilepost.getMilepost());
    }
    
    private SwitchStateType createSwitch(DeviceData thisDevice, com.uprr.psm.lsc.bindings.swagger.find.subdivision.state.v1_0.DeviceData device) {
        return new SwitchStateType()
                .milepostLocation(createMilepostType(thisDevice.getDeviceMilepost()))
                .switchState(convertSwitchState(thisDevice, device));
    }
    
    private Integer convertSwitchState(DeviceData thisDevice, com.uprr.psm.lsc.bindings.swagger.find.subdivision.state.v1_0.DeviceData device) {
        int state = 9;
        if (device.getRawDeviceStatusCode() < 3 && device.getRawDeviceStatusCode() > 0) {
            state = device.getRawDeviceStatusCode();
        }
        state = state * facingMultiplier(thisDevice.getFacingDirectionCode());
        return state;
    }
    
    private int facingMultiplier(int facing) {
        if (facing == 2 || facing == 4) {
            return -1;
        }
        return 1;
    }
    
    private SignalStateType createSignalItem(DeviceData subdivDevice, com.uprr.psm.lsc.bindings.swagger.find.subdivision.state.v1_0.DeviceData device) {
        return new SignalStateType()
                .milepostLocation(createMilepostType(subdivDevice.getDeviceMilepost()))
                .signalState(convertSignalState(subdivDevice, device.getNormalizedDeviceStatusCode()))
                .previousSignalState(convertSignalState(subdivDevice, device.getPreviousNormalizedDeviceStatusCode()))
                .currentStateTime(convertUTCTimestamp(device.getTransitionTime()));
    }
    
    private Integer convertSignalState(DeviceData subdivDevice, int deviceStatusCode) {
        if (deviceStatusCode == -1) {
            return null;
        }
        if (deviceStatusCode == 6) {
            deviceStatusCode = 9;
        }
        if (deviceStatusCode > 3 && deviceStatusCode < 9) {
            deviceStatusCode = deviceStatusCode - 1;
        }
        deviceStatusCode = deviceStatusCode * facingMultiplier(subdivDevice.getFacingDirectionCode());
        return deviceStatusCode;
    }
    
}
