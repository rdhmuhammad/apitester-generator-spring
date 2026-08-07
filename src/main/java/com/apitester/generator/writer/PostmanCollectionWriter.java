package com.apitester.generator.writer;

import com.apitester.generator.model.PostmanCollection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

@Slf4j
public class PostmanCollectionWriter {

    private final ObjectMapper objectMapper;

    public PostmanCollectionWriter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void write(PostmanCollection collection, String outputPath) {
        try {
            File outputFile = new File(outputPath);
            File parentDir = outputFile.getAbsoluteFile().getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            objectMapper.writeValue(outputFile, collection);
            log.info("Postman collection written to: {}", outputFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write Postman collection to {}: {}", outputPath, e.getMessage());
        }
    }
}
