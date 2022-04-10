package com.uprr.pac.handler.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.*;

import org.junit.jupiter.api.Test;
import org.openapitools.model.*;
import org.openapitools.model.MilepostType;

import com.uprr.netcontrol.shared.xml_bindings.jaxb2.location.find_system_station_2_2.SystemStationType;
import com.uprr.psm.core.cache.vo.*;
import com.uprr.psm.lsc.bindings.swagger.find.subdivision.state.v1_0.SubdivisionStateData;
import com.uprr.psm.lsc.bindings.swagger.find.track.network.device.state.v1_0.*;

class FilesUtilsTest {
    
    @Test
    void testLoadDevices() throws Exception {
        FindTrackNetworkDeviceStateResponse response = FilesUtils.loadDevices("/boone-devices.json");
        assertThat(response.getSubdivisionName()).isEqualTo("Boone");
        List<DeviceData> devices = response.getDeviceList();
        assertThat(devices).isNotEmpty();
    }
    
    @Test
    void loadNetworkStateEventTest() throws Exception {
        File f = new File("network-events/UP8105-1648749499000-{2}.json");
        TrainCacheObjects trainCache = FilesUtils.loadNetworkStateEvent(f);
        assertThat(trainCache.getMetadata().getTrainId()).isEqualTo("UPCWKBT926");
        CacheMetadata trainData = trainCache.getMetadata();
        Collection<SubdivisionStateData> events = trainCache.getData();
        assertThat(events).isNotEmpty();
    }
    @Test
    void loadNetworkStateEventTestZip() throws Exception {
        File f = new File("network-events/UP8105-1648749499000-{2}.json.gz");
        TrainCacheObjects trainCache = FilesUtils.loadNetworkStateEvent(f);
        assertThat(trainCache.getMetadata().getTrainId()).isEqualTo("UPCWKBT926");
        CacheMetadata trainData = trainCache.getMetadata();
        Collection<SubdivisionStateData> events = trainCache.getData();
        assertThat(events).isNotEmpty();
    }
    @Test
    void testWriteTrainFile() throws Exception {
        PTCSubdivisionData outputEvent = new PTCSubdivisionData()
                .lastTrainReporting(createTrainReporting())
                .subdivisionId(106)
                .subdivisionName("Boone");
        String filePath = FilesUtils.writeTrainFile("test-train-events", outputEvent);
        File f = new File(filePath);
        assertThat(f.exists());
        assertThat(f.delete()).isTrue();
    }
    
    private TrainStateType createTrainReporting() {
        TrainStateType trainReporting = new TrainStateType()
                .ptcLeadLocomotiveId("UP TEST")
                .lastReportedPosition(
                        new TrainPositionReport()
                        .positionTime(OffsetDateTime.now())
                        .milepostLocation(
                                new MilepostLocationType()
                                .milepost(new MilepostType().milepostNumber(11.0f))
                                .subdivisionId(106)
                                )
                        )
                .trainId(new TrainStateTypeTrainId()
                    .trainDate("2022-04-06")
                    .trainSection("2")
                    .trainSymbol("ZLAMN"));
        return trainReporting;
    }

    @Test
    void getFilesInPathTest() throws Exception {
        List<File> files = FilesUtils.getFilesInDirectory("network-events");
        assertThat(files).isNotEmpty();
        assertThat(files).anyMatch(f -> f.getName().equals("UP8105-1648749499000-{2}.json"));
        assertThat(files).anyMatch(f -> f.getName().equals("UP3015-1648747759000-{2}.json.gz"));
        
    }
    
    @Test
    void loadSystemStations() throws Exception {
        List<SystemStationType> stationsList = FilesUtils.loadSystemStations("/boone-stations.xml");
        assertThat(stationsList).isNotEmpty();
    }
    
}
