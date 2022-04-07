package com.uprr.pac.convert;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.openapitools.model.*;
import org.openapitools.model.AOTUActivity;
import org.openapitools.model.AOTURouteLocation;
import org.openapitools.model.AOTUTrainData;
import org.openapitools.model.LocomotiveStatusReport;
import org.openapitools.model.MilepostType;
import org.openapitools.model.SubdivisionData;

import com.uprr.enterprise.datetime.UTCTimestamp;
import com.uprr.psm.core.cache.vo.TrainCacheObjects;
import com.uprr.psm.lsc.bindings.swagger.find.subdivision.state.v1_0.*;
import com.uprr.psm.lsc.bindings.swagger.find.track.network.device.state.v1_0.DeviceData;
import com.uprr.psm.lsc.bindings.swagger.find.track.network.device.state.v1_0.FindTrackNetworkDeviceStateResponse;

public class FilesConverter {
    private static final String DIRECTION_MP_DECREASING = "2";
    private Map<Integer, DeviceData> subdivisionDevices;
    private String subdivisionName;
    private Integer subdivisionId;
    
    public FilesConverter(FindTrackNetworkDeviceStateResponse subdivDevices) {
        this.subdivisionName = subdivDevices.getSubdivisionName();
        this.subdivisionId = Integer.valueOf(subdivDevices.getDeviceList().get(0).getSubdivisionId());
        subdivDevices.getDeviceList().stream()
            .filter(d -> StringUtils.isNotBlank(d.getDataSourceId()))
            .map( d -> subdivisionDevices.put(Integer.getInteger(d.getDataSourceId()), d));
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
                        .positionTime(convertUTCTimestamp(p.getPositionTime()))
                        .speedMPH(computeSpeed(p.getSpeed(), p.getDirectionOfTravel()))
                        )
                .collect(Collectors.toList());
    }
    
    private Integer computeSpeed(Integer speed, String directionOfTravel) {
        return DIRECTION_MP_DECREASING.equals(directionOfTravel.trim())? speed * -1:speed;
    }

    private OffsetDateTime convertUTCTimestamp(UTCTimestamp positionTime) {
        if (positionTime == null) {
            return null;
        }
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(positionTime.getTimeInMillis()), ZoneOffset.UTC);
    }

    private MilepostLocationType createMilepostLocationType(@Valid PTCLocationData headEndLocation) {
        return new MilepostLocationType()
                .subdivisionId(headEndLocation.getSubdivisionId())
                .trackName(headEndLocation.getTrackName())
                .milepost(createMilepostType(headEndLocation.getLocationMilepost()));
    }

    private MilepostType createMilepostType(com.uprr.psm.lsc.bindings.swagger.find.subdivision.state.v1_0.@Valid MilepostType locationMilepost) {
        return new MilepostType()
                .milepostNumber(locationMilepost.getMilepost().floatValue())
                .milepostPrefix(locationMilepost.getMilepostPrefix())
                .milepostSuffix(locationMilepost.getMilepostSuffix());
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
                        .positionOnTrain(l.getPositionOnTrain())
                        )
                .collect(Collectors.toList());
    }
    
    private TrainPositionReport createLastPosition(LocomotivePositionReport lastReportedPosition) {
        return new TrainPositionReport()
                .milepostLocation(createMilepostLocationType(lastReportedPosition.getHeadEndLocation()))
                .positionTime(convertUTCTimestamp(lastReportedPosition.getPositionTime()))
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
                        .milepost(lookupMilepost(l.getSystemStationId()))
                        .routeSequence(l.getRouteSequence())
                        .stationType(l.getStationType())
                        )
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
    
    private List<MilepostSegmentStateType> createMilepostSegmentList(TrainCacheObjects trainCache) {
        // TODO Auto-generated method stub
        return null;
    }
    
    private MilepostType lookupMilepost(Integer systemStationId) {
        // TODO Auto-generated method stub
        return new MilepostType().milepostNumber(999.99f);
    }
    
    private List<TrainPositionReport> createTrainEstimatedPositionList(PTCTrainData trainData) {
        // TODO Auto-generated method stub
        return null;
    }
    
}
