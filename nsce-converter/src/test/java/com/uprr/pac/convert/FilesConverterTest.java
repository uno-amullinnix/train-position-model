package com.uprr.pac.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openapitools.model.PTCSubdivisionData;

import com.uprr.pac.clients.track.state.SubdivisionTrackRange;
import com.uprr.pac.clients.track.state.impl.SubdivisionTrackRangeImpl;
import com.uprr.pac.handler.common.FilesUtils;
import com.uprr.psm.core.cache.vo.TrainCacheObjects;
import com.uprr.psm.lsc.bindings.swagger.get.subdivision.mileposts.v1_0.MilepostDataResponse;


@RunWith(MockitoJUnitRunner.class)
class FilesConverterTest {
    
    @Test
    void testConvertPulseModel() throws IOException {
        MilepostDataResponse milepostDataResponse = FilesUtils.loadTrackData("/boone-track-data.json");
        SubdivisionTrackRange trackRange = new SubdivisionTrackRangeImpl(milepostDataResponse);
        FilesConverter converter = new FilesConverter(FilesUtils.loadDevices("/boone-devices.json"),FilesUtils.loadSystemStations("/boone-stations.xml"), trackRange);
        List<File> files = FilesUtils.getFilesInDirectory("network-events-old");
        assertThat(files).isNotEmpty();
        for (File file : files) {
            TrainCacheObjects trainCache = FilesUtils.loadNetworkStateEvent(file);
            PTCSubdivisionData converted = converter.convertPulseModel(trainCache);
            System.out.println(converted.getLastTrainReporting().getTrainId().getTrainSymbol()+"-"+
                    converted.getLastTrainReporting().getTrainId().getTrainDate()+"_"+
                    converted.getLastTrainReporting().getLastReportedPosition().getPositionTime().format(DateTimeFormatter.ISO_DATE_TIME));
            FilesUtils.writeTrainFile("test-train-events", converted);
        }
    }
   
    
}
