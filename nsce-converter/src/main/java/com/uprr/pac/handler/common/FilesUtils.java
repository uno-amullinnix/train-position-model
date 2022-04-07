package com.uprr.pac.handler.common;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.openapitools.model.PTCSubdivisionData;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.uprr.psm.core.cache.vo.TrainCacheObjects;
import com.uprr.psm.lsc.bindings.swagger.find.subdivision.state.v1_0.SubdivisionStateData;
import com.uprr.psm.lsc.bindings.swagger.find.track.network.device.state.v1_0.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FilesUtils {
    private static final DateTimeFormatter FILEDATE_PATTERN = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public static List<File> getFilesInDirectory(String directoryName) throws IOException {
        List<File> fileNames = Files.walk(Paths.get(directoryName))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .collect(Collectors.toList());
        return fileNames;
    }
    
    public static TrainCacheObjects loadNetworkStateEvent(File fileName) throws IOException, JsonParseException, JsonMappingException {
        TrainCacheObjects[] response = createObjectMapper().readValue(fileName, TrainCacheObjects[].class);
        assert (response.length == 1);
        return response[0];
    }

    private static ObjectMapper createObjectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
    }
    
    public static FindTrackNetworkDeviceStateResponse loadDevices(String fileName) throws IOException, JsonParseException, JsonMappingException {
        InputStream inputStream = getFileStream(fileName);
        return createObjectMapper().readValue(inputStream, FindTrackNetworkDeviceStateResponse.class);
    }
    
    public static String writeTrainFile(String path, PTCSubdivisionData outputEvent) throws IOException {
        if (outputEvent == null) {
            throw new IllegalArgumentException("outputEvent required");
        }
        String fileName = createFilename(outputEvent);
        log.info("Path= {}, Filename={} ", path, fileName);
        
        String filePath = new StringBuilder(String.valueOf(path))
                .append(System.getProperty("file.separator"))
                .append(fileName).toString();
        
        // Safety check to see if the path exists!
        File f = new File(filePath);
        if (!f.exists()) {
            f.getParentFile().mkdirs();
        }
        
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            log.info("Writing the objects of Cache to the dump file");
            log.info(outputEvent.toString());
            writer.printf(createObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(outputEvent));
            writer.println();
            return filePath;
        } catch (IOException ioe) {
            log.error("Error generating state dump file: ", ioe);
            throw ioe;
        }
        
    }
    
    private static String createFilename(PTCSubdivisionData outputEvent) {
        String locomotive = outputEvent.getLastTrainReporting().getPtcLeadLocomotiveId();
        String trainSymbol = outputEvent.getLastTrainReporting().getTrainId().getTrainSymbol().trim();
        if (StringUtils.isNotBlank(outputEvent.getLastTrainReporting().getTrainId().getTrainSection())) {
            trainSymbol = trainSymbol + "_" + outputEvent.getLastTrainReporting().getTrainId().getTrainSection().trim();
        }
        String trainDate = outputEvent.getLastTrainReporting().getTrainId().getTrainDate();
        String timestamp = outputEvent.getLastTrainReporting().getLastReportedPosition().getPositionTime().format(FILEDATE_PATTERN);
        String filename = format("{0}-{1}-{2}-{3}.json",
                locomotive.replace(" ", "_"),
                trainSymbol,
                trainDate,
                timestamp);
        return filename;
    }
    
    private static InputStream getFileStream(String fileName) {
        Class<?> clazz = FilesUtils.class;
        InputStream inputStream = clazz.getResourceAsStream(fileName);
        return inputStream;
    }
    
    public static String format(String message, String... replaceValues) {
        return new MessageFormat(toNonNullString(message)).format(replaceValues);
    }
    
    public static String toNonNullString(String s) {
        return StringUtils.isBlank(s) ? "" : s.trim();
    }
    
}
