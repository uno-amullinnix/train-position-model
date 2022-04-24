package com.uprr.pac.convert;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.uprr.pac.clients.track.state.SubdivisionTrackRange;
import com.uprr.pac.clients.track.state.impl.SubdivisionTrackRangeImpl;
import com.uprr.pac.handler.common.FilesUtils;
import com.uprr.psm.lsc.bindings.swagger.get.subdivision.mileposts.v1_0.MilepostDataResponse;

@RunWith(MockitoJUnitRunner.class)
class FilesConverterTest {
    
    @Test
    void testConvertPulseModel() throws IOException {
        MilepostDataResponse milepostDataResponse = FilesUtils.loadTrackData("/boone-track-data.json");
        SubdivisionTrackRange trackRange = new SubdivisionTrackRangeImpl(milepostDataResponse);
        FilesConverter converter = new FilesConverter(FilesUtils.loadDevices("/boone-devices.json"), FilesUtils.loadSystemStations("/boone-stations.xml"),
                trackRange);
        TrainDataGenerator tdGenerator = new TrainDataGenerator(converter);
        tdGenerator.generateFiles("test-network-events", "train-events");
    }
    
}
