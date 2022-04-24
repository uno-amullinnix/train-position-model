package com.uprr.pac.convert;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.Valid;

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
    
    public FilesConverter(FindTrackNetworkDeviceStateResponse subdivDevices, List<SystemStationType> subdivisionStations, SubdivisionTrackRange trackRange) {
        this.subdivisionName = subdivDevices.getSubdivisionName();
        this.subdivisionId = Integer.valueOf(subdivDevices.getDeviceList().get(0).getSubdivisionId());
        createTrackMilepostMap(trackRange);
        
        subdivisionDevicesMap = subdivDevices.getDeviceList().stream()
                .filter(d -> (d.getWiuId() != null && d.getWiuStatusIndex() != null
                        && trackMilepostMap.containsKey(d.getTrackName().toUpperCase())
                        && d.getDeviceTypeCode() > 0 && d.getDeviceTypeCode() < 3)
                        && d.getDeviceMilepost() != null)
                .collect(Collectors.toMap(d -> Pair.of(d.getWiuId(), d.getWiuStatusIndex()), Function.identity(),
                        (existing, replacement) -> existing));
        systemStationsMap = subdivisionStations.stream()
                .collect(Collectors.toMap(SystemStationType::getSystemStationId, Function.identity(),
                        (existing, replacement) -> existing));
        System.out.println("Started");
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
    
    public PTCSubdivisionData convertPulseModel(TrainCacheObjects trainCache) {
        SubdivisionStateData inputState = trainCache.getData().get(0);
        if (!subdivisionId.equals(inputState.getSubdivisionId())) {
            throw new IllegalArgumentException("Subdivision Mismatch expected " + subdivisionId +
                    ", got " + inputState.getSubdivisionId());
        }
        PTCSubdivisionData ptcSubdivisionData = new PTCSubdivisionData()
                .subdivisionId(inputState.getSubdivisionId())
                .subdivisionName(subdivisionName)
                .lastTrainReporting(createLastTrainReporting(trainCache))
                .milepostSegmentStateList(createMilepostSegmentList(trainCache));
        
        return ptcSubdivisionData;
        
    }
    
    private TrainStateType createLastTrainReporting(TrainCacheObjects trainCache) {
        String leadLocomotive = trainCache.getMetadata().getLocomotiveId().trim();
        
        SubdivisionStateData subdivisionStateData = trainCache.getData().stream()
                .filter(d -> d.getSubdivisionId().equals(this.subdivisionId))
                .findAny().get();
        List<PTCTrainData> locoList = subdivisionStateData.getPollingLocomotiveList();
        PTCTrainData trainData = findLeadLocomotiveTrainData(locoList, leadLocomotive);
        assert (trainData != null);
        return new TrainStateType()
                .aotuTrainData(createAOTU(trainData.getAotuTrainData()))
                .emptyCarCount(trainData.getEmptyCarCount())
                .lastReportedPosition(createLastPosition(trainData.getLastReportedPosition()))
                .loadedCarCount(trainData.getLoadedCarCount())
                .locomotiveList(createLocomotiveList(trainData))
                .maximumTrainSpeed(trainData.getMaximumTrainSpeed())
                .positionHistoryList(createPositionHistory(trainData.getPositionHistoryList()))
                .ptcLeadLocomotiveId(leadLocomotive)
                .subdivisionList(createSubdivisionList(trainData.getSubdivisionList()))
                .trailingTonnage(trainData.getTrailingTonnage())
                .trainEstimatedPositionList(createTrainEstimatedPositionList(trainData))
                .trainId(createTrainId(trainData.getTrainId()))
                .trainLength(trainData.getTrainLength());
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
    
    private OffsetDateTime convertUTCTimestamp(UTCTimestamp positionTime) {
        if (positionTime == null) {
            return null;
        }
        ZoneId utc = ZoneId.of("UTC");
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(positionTime.getTimeInMillis()), utc);
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
                .speedMPH(computeSpeed(estimate.getSpeed(),estimate.getDirectionOfTravel()));
    }

    private List<MilepostSegmentStateType> createMilepostSegmentList(TrainCacheObjects trainCache) {
        Map<Pair<String, Integer>, MilepostSegmentStateType> segmentMap = new HashMap<>();
        trainCache.getData().get(0).getDeviceList().stream().forEach(device -> processDevice(segmentMap, device));
        //TODO - trains
        return segmentMap.values().stream()
            .sorted(Comparator.comparing(m -> Pair.of(m.getTrackName(), m.getMilepost().getMilepostNumber())))
            .collect(Collectors.toList());        
    }
    
    private void processDevice(Map<Pair<String, Integer>, MilepostSegmentStateType> segmentMap,
            com.uprr.psm.lsc.bindings.swagger.find.subdivision.state.v1_0.DeviceData device) {
        DeviceData thisDevice = subdivisionDevicesMap.get(Pair.of(device.getWiuId(), device.getWiuStatusIndex()));
        if (thisDevice == null) {
            return;
        }
        Pair<String, Integer> milepostKey = Pair.of(thisDevice.getTrackName(), thisDevice.getDeviceMilepost().getMilepost().intValue());
        MilepostSegmentStateType mpSegment = segmentMap.get(milepostKey);
        if (mpSegment == null) {
            mpSegment = new MilepostSegmentStateType();
            mpSegment.setMilepost(createMilepost(thisDevice.getDeviceMilepost()));
            mpSegment.setTrackName(thisDevice.getTrackName());
            segmentMap.put(milepostKey, mpSegment);
        }
        if (thisDevice.getDeviceTypeCode() == 1) {
            mpSegment.addSignalListItem(createSignalItem(thisDevice, device));
        } else if (thisDevice.getDeviceTypeCode() == 2) {
            mpSegment.addSwitchListItem(createSwitch(thisDevice, device));
        }
    }
    
    private MilepostType createMilepost(com.uprr.psm.lsc.bindings.swagger.find.track.network.device.state.v1_0.@Valid MilepostType deviceMilepost) {
        return new MilepostType().milepostNumber(Integer.valueOf(deviceMilepost.getMilepost().intValue()).floatValue());
    }

    private SwitchStateType createSwitch(DeviceData thisDevice, com.uprr.psm.lsc.bindings.swagger.find.subdivision.state.v1_0.DeviceData device) {
        return new SwitchStateType()
                .milepostLocation(createMP(thisDevice.getDeviceMilepost()))
                .switchState(convertSwitchState(thisDevice, device));
    }
    
    private Integer convertSwitchState(DeviceData thisDevice, com.uprr.psm.lsc.bindings.swagger.find.subdivision.state.v1_0.DeviceData device) {
        int state = 0;
        if (device.getRawDeviceStatusCode() < 3) {
            state = device.getRawDeviceStatusCode();
        }
        int facing = thisDevice.getFacingDirectionCode();
        if (facing == 2 || facing == 4) {
            state = state *-1;
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

    private MilepostType createMP(com.uprr.psm.lsc.bindings.swagger.find.track.network.device.state.v1_0.@Valid MilepostType deviceMilepost) {
        return new MilepostType().milepostNumber(deviceMilepost.getMilepost());
    }

    private SignalStateType createSignalItem(DeviceData thisDevice, com.uprr.psm.lsc.bindings.swagger.find.subdivision.state.v1_0.DeviceData device) {
        return new SignalStateType()
                .milepostLocation(createMP(thisDevice.getDeviceMilepost()))
                .signalState(convertSignalState(thisDevice,device));
    }

    private Integer convertSignalState(DeviceData thisDevice, com.uprr.psm.lsc.bindings.swagger.find.subdivision.state.v1_0.DeviceData device) {
        int state = device.getNormalizedDeviceStatusCode();
        if (state == 6) {
            state = 0;
        }
        if (state > 3) {
            state = state - 1;
        }
        state = state * facingMultiplier(thisDevice.getFacingDirectionCode());
        return state;
    }
    
}
