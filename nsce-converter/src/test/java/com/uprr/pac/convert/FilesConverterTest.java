package com.uprr.pac.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.*;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openapitools.model.PTCSubdivisionData;

import com.uprr.pac.handler.common.FilesUtils;
import com.uprr.psm.core.cache.vo.TrainCacheObjects;


@RunWith(MockitoJUnitRunner.class)
class FilesConverterTest {
    
    @Test
    void testConvertPulseModel() throws IOException {
        FilesConverter converter = new FilesConverter(FilesUtils.loadDevices("/boone-devices.json"),FilesUtils.loadSystemStations("/boone-stations.xml"));
        List<File> files = FilesUtils.getFilesInDirectory("network-events");
        assertThat(files).isNotEmpty();
        for (File file : files) {
            TrainCacheObjects trainCache = FilesUtils.loadNetworkStateEvent(file);
            PTCSubdivisionData converted = converter.convertPulseModel(trainCache);
            System.out.println(converted.getLastTrainReporting().getTrainId());
            FilesUtils.writeTrainFile("test-train-events", converted);
        }
    }
   
    
}
