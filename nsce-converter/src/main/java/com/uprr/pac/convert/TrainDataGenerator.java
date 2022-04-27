package com.uprr.pac.convert;

import java.io.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.openapitools.model.*;

import com.uprr.pac.handler.common.FilesUtils;
import com.uprr.psm.core.cache.vo.TrainCacheObjects;

public class TrainDataGenerator {

    private static final boolean SHOW_SIGNALS = true;
    private FilesConverter converter;

    public TrainDataGenerator(FilesConverter converter) {
        this.converter = converter;
    }

    public void generateFiles(String inputDirectoryName, String outputPath) throws IOException {
        List<File> files = FilesUtils.getFilesInDirectory(inputDirectoryName);
        assert files != null;
        files.sort((f1, f2) -> f1.compareTo(f2));
        Map<LocalDateTime, PTCSubdivisionData> trainMap = new HashMap<>();
        int i = 1;
        TrainCacheObjects trainCache = FilesUtils.loadNetworkStateEvent(files.get(0));
        String trainId = trainCache.getMetadata().getTrainId();
        String previousTrainId = trainId;
        while (i <= files.size()) {
            PTCSubdivisionData converted = converter.convertPulseModel(trainCache);
            trainMap.put(converted.getLastTrainReporting().getLastReportedPosition().getPositionTime().toLocalDateTime(), converted);
            while (trainId.equalsIgnoreCase(previousTrainId) && i < files.size()) {
                trainCache = FilesUtils.loadNetworkStateEvent(files.get(i));
                trainId = trainCache.getMetadata().getTrainId();
                if (!trainId.equalsIgnoreCase(previousTrainId)) {
                    break;
                }
                converted = converter.convertPulseModel(trainCache);
                trainMap.put(converted.getLastTrainReporting().getLastReportedPosition().getPositionTime().toLocalDateTime(), converted);
                i++;
            }
            List<PTCSubdivisionData> listWithActuals = computeActualTimes(trainMap);
            listWithActuals.stream().forEach(trainEvent -> {
                try {
                    FilesUtils.writeTrainFile(outputPath, trainEvent);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            trainMap = new HashMap<>();
            previousTrainId = trainId;
            i++;
        }
    }
    
    private List<PTCSubdivisionData> computeActualTimes(Map<LocalDateTime, PTCSubdivisionData> trainMap) {
        List<LocalDateTime> reportingTimes = trainMap.keySet().stream().sorted().collect(Collectors.toList());
        return trainMap.values().stream()
                .sorted((t1, t2) -> t1.getLastTrainReporting().getLastReportedPosition().getPositionTime().compareTo(t2.getLastTrainReporting().getLastReportedPosition().getPositionTime()))
                .map(trainData -> computeActuals(trainData, reportingTimes, trainMap))
                .collect(Collectors.toList());
    }

    private PTCSubdivisionData computeActuals(PTCSubdivisionData trainData, List<LocalDateTime> reportingTimes, Map<LocalDateTime, PTCSubdivisionData> trainMap) {
        LocalDateTime timeOfReporting = trainData.getLastTrainReporting().getLastReportedPosition().getPositionTime().toLocalDateTime();
        for (int t = 2; t < 120; t = t + 2) {
            LocalDateTime forecastTime = timeOfReporting.plus(t, ChronoUnit.MINUTES);
            int start = reportingTimes.indexOf(timeOfReporting);
            for (int i = start; i < reportingTimes.size() - 1; i++) {
                if (reportingTimes.get(i).isBefore(forecastTime) && reportingTimes.get(i+1).isAfter(forecastTime)) {
                    PTCSubdivisionData prevReporting = trainMap.get(reportingTimes.get(i));
                    PTCSubdivisionData nextReporting = trainMap.get(reportingTimes.get(i+1));
                    TrainPositionReport actual = computeLocation(forecastTime, prevReporting, nextReporting);
                    if (SHOW_SIGNALS) {
                        setSignals(prevReporting, nextReporting, actual); 
                    }
                    trainData.getLastTrainReporting().addTrainActualPositionListItem(actual);
                }
            }
        }
        return trainData;
    }

    private void setSignals(PTCSubdivisionData prevReporting, PTCSubdivisionData nextReporting, TrainPositionReport actual) {
        SignalStateType prevSignal = nextReporting.getLastTrainReporting().getLastReportedPosition().getPrecedingSignalState();
        SignalStateType nextSignal = prevReporting.getLastTrainReporting().getLastReportedPosition().getNextSignalState();
        if (actual.getSpeedMPH() > 0) {
            if (prevSignal == null || prevSignal.getMilepostLocation().getMilepostNumber() > actual.getMilepostLocation().getMilepost().getMilepostNumber()) {
                prevSignal = prevReporting.getLastTrainReporting().getLastReportedPosition().getPrecedingSignalState();
            }
            if (nextSignal == null || nextSignal.getMilepostLocation().getMilepostNumber() < actual.getMilepostLocation().getMilepost().getMilepostNumber()) {
                nextSignal = nextReporting.getLastTrainReporting().getLastReportedPosition().getNextSignalState();
            }
        } else {
            if (prevSignal == null || prevSignal.getMilepostLocation().getMilepostNumber() < actual.getMilepostLocation().getMilepost().getMilepostNumber()) {
                prevSignal = prevReporting.getLastTrainReporting().getLastReportedPosition().getPrecedingSignalState();
            }
            if (nextSignal == null || nextSignal.getMilepostLocation().getMilepostNumber() > actual.getMilepostLocation().getMilepost().getMilepostNumber()) {
                nextSignal = nextReporting.getLastTrainReporting().getLastReportedPosition().getNextSignalState();
            }
        }
        actual.setPrecedingSignalState(prevSignal);
        actual.setNextSignalState(nextSignal);
    }

    private TrainPositionReport computeLocation(LocalDateTime forecastTime, PTCSubdivisionData prevReporting, PTCSubdivisionData nextReporting) {
        TrainPositionReport tpr = new TrainPositionReport().positionTime(OffsetDateTime.of(forecastTime, ZoneOffset.UTC));
        TrainPositionReport prevLastReportedPosition = prevReporting.getLastTrainReporting().getLastReportedPosition();
        TrainPositionReport nextLastReportedPosition = nextReporting.getLastTrainReporting().getLastReportedPosition();
        LocalDateTime prevTime = prevLastReportedPosition.getPositionTime().toLocalDateTime();
        LocalDateTime nextTime = nextLastReportedPosition.getPositionTime().toLocalDateTime();
        MilepostLocationType closestMilepostLocation = prevReporting.getLastTrainReporting().getLastReportedPosition().getMilepostLocation();
        long secondsFromPrev = prevTime.until(forecastTime, ChronoUnit.SECONDS);
        long secondsToNext = forecastTime.until(nextTime, ChronoUnit.SECONDS);
        if (secondsFromPrev > secondsToNext){
            closestMilepostLocation = nextLastReportedPosition.getMilepostLocation();
            tpr.setReversing(nextLastReportedPosition.getReversing());
        } else {
            closestMilepostLocation = prevLastReportedPosition.getMilepostLocation();
            tpr.setReversing(prevLastReportedPosition.getReversing());
        }
        String trackName = prevLastReportedPosition.getMilepostLocation().getTrackName();
        if (!nextLastReportedPosition.getMilepostLocation().getTrackName().equals(trackName)) {
            trackName = trackName +"-"+nextLastReportedPosition.getMilepostLocation().getTrackName();
        }
        MilepostLocationType milepostLocation = new MilepostLocationType()
                .subdivisionId(closestMilepostLocation.getSubdivisionId())
                .trackName(trackName);  
        
        Float forecastMilepostNumber = computeForecastMilepostNumber(prevLastReportedPosition, nextLastReportedPosition, secondsFromPrev, secondsToNext);
        milepostLocation.setMilepost(new MilepostType().milepostNumber(forecastMilepostNumber));
        tpr.setSpeedMPH(forecastSpeed(prevLastReportedPosition.getSpeedMPH(), nextLastReportedPosition.getSpeedMPH(), secondsFromPrev, secondsToNext));
        tpr.setMilepostLocation(milepostLocation);
        return tpr;
    }

    private Integer forecastSpeed(Integer prevSpeedMPH, Integer nextSpeedMPH, long secondsFromPrev, long secondsToNext) {
        if (prevSpeedMPH == nextSpeedMPH) {
            return prevSpeedMPH;
        }
        float sum = secondsFromPrev+secondsToNext;
        float ratio = secondsFromPrev/sum; 
        int delta = Math.abs(prevSpeedMPH - nextSpeedMPH);
        Float changeAtForecastTime = delta * ratio;
        if (prevSpeedMPH > nextSpeedMPH) {
            return prevSpeedMPH - changeAtForecastTime.intValue();
        }
        return prevSpeedMPH + changeAtForecastTime.intValue();
    }

    /**
     * NOTE: This is inaccurate because it is missing MILEPOST EQUATIONS, but probably sufficient for now.
     * @param prevLastReportedPosition
     * @param nextLastReportedPosition
     * @param secondsFromPrev
     * @param secondsToNext
     * @return
     */
    private Float computeForecastMilepostNumber(TrainPositionReport prevLastReportedPosition, TrainPositionReport nextLastReportedPosition,
            long secondsFromPrev, long secondsToNext) {
        Float forecastMilepostNumber;

        Float prevMilepostNumber = prevLastReportedPosition.getMilepostLocation().getMilepost().getMilepostNumber();
        Float nextMilepostNumber = nextLastReportedPosition.getMilepostLocation().getMilepost().getMilepostNumber();
        float distanceMiles = Math.abs(prevMilepostNumber - nextMilepostNumber);
        
        if (distanceMiles < .01) {
            forecastMilepostNumber = prevMilepostNumber;
        } else {
           float sum = secondsFromPrev+secondsToNext;
           if(secondsFromPrev < secondsToNext) {
               float ratio = secondsFromPrev/sum; 
               float forecastDistance = distanceMiles * ratio;
               if (prevMilepostNumber < nextMilepostNumber) {  //work from closest position to minimize screwups ("good enough" for now) 
                   forecastMilepostNumber = prevMilepostNumber + forecastDistance;
               } else {
                   forecastMilepostNumber = prevMilepostNumber - forecastDistance;
               }
           } else {
               float ratio = secondsToNext/sum; 
               float forecastDistance = distanceMiles * ratio;
               if (prevMilepostNumber < nextMilepostNumber) {
                   forecastMilepostNumber = nextMilepostNumber - forecastDistance;
               } else {
                   forecastMilepostNumber = nextMilepostNumber + forecastDistance;
               }
           }
        }
        return forecastMilepostNumber;
    }
    
}
