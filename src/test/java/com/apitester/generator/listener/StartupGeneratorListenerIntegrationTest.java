package com.apitester.generator.listener;

import com.apitester.generator.config.ApiTesterProperties;
import com.apitester.generator.example.TestMemberController;
import com.apitester.generator.generator.PostmanCollectionGenerator;
import com.apitester.generator.processor.DtoAnalyzer;
import com.apitester.generator.processor.EndpointProcessor;
import com.apitester.generator.scanner.ControllerScanner;
import com.apitester.generator.service.PostmanMergeService;
import com.apitester.generator.writer.PostmanCollectionWriter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StartupGeneratorListenerIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Startup lifecycle should generate cache on 1st run and preserve user modifications on 2nd run")
    void testEndToEndPreserveUserChangesLifecycle() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        File outputFile = tempDir.resolve("postman_collection.json").toFile();
        File cacheFile = tempDir.resolve(".apitester-cache.json").toFile();

        ApiTesterProperties properties = new ApiTesterProperties();
        properties.getCollection().setOutputPath(outputFile.getAbsolutePath());
        properties.getCollection().setCachePath(cacheFile.getAbsolutePath());
        properties.getCollection().setPreserveUserChanges(true);

        ControllerScanner scanner = mock(ControllerScanner.class);
        ControllerScanner.ControllerInfo memberInfo = new ControllerScanner.ControllerInfo(
                TestMemberController.class, "Member", new String[]{},
                Collections.emptyMap(), new Class<?>[]{});
        when(scanner.scan(any(ApplicationContext.class))).thenReturn(List.of(memberInfo));

        EndpointProcessor endpointProcessor = new EndpointProcessor(new DtoAnalyzer());
        PostmanCollectionGenerator generator = new PostmanCollectionGenerator(properties);
        PostmanCollectionWriter writer = new PostmanCollectionWriter();
        PostmanMergeService mergeService = new PostmanMergeService(mapper);

        StartupGeneratorListener listener = new StartupGeneratorListener(
                scanner, endpointProcessor, generator, writer, mergeService, properties);

        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);
        org.springframework.context.ConfigurableApplicationContext context = mock(org.springframework.context.ConfigurableApplicationContext.class);
        when(event.getApplicationContext()).thenReturn(context);

        // 1. First Startup Run: should create both postman_collection.json and .apitester-cache.json
        listener.onApplicationReady(event);

        assertTrue(outputFile.exists());
        assertTrue(cacheFile.exists());

        JsonNode initialCollection = mapper.readTree(outputFile);
        JsonNode initialCache = mapper.readTree(cacheFile);
        assertEquals(initialCollection.path("info").path("name").asText(), initialCache.path("info").path("name").asText());

        // 2. User modifies collection: customizes payload on a request
        JsonNode firstRequest = initialCollection.path("item").get(0).path("item").get(0);
        String originalRequestId = firstRequest.path("id").asText();
        assertFalse(originalRequestId.isEmpty());

        // User sets custom body payload
        ObjectNode bodyObj = mapper.createObjectNode();
        bodyObj.put("mode", "raw");
        bodyObj.put("raw", "{\"userTestKey\":\"customValue123\"}");
        ((ObjectNode) firstRequest.path("request")).set("body", bodyObj);

        // Save user edits to disk
        mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, initialCollection);

        // 3. Second Startup Run: 3-way merge should preserve user changes
        listener.onApplicationReady(event);

        assertTrue(outputFile.exists());
        JsonNode secondCollection = mapper.readTree(outputFile);
        JsonNode secondFirstRequest = secondCollection.path("item").get(0).path("item").get(0);

        // Deterministic ID must remain identical
        assertEquals(originalRequestId, secondFirstRequest.path("id").asText());

        // User's customized body must be preserved!
        assertEquals("raw", secondFirstRequest.path("request").path("body").path("mode").asText());
        String mergedRaw = secondFirstRequest.path("request").path("body").path("raw").asText();
        assertTrue(mergedRaw.contains("customValue123"), "Expected user customized payload to be preserved in merged collection");
    }
}
