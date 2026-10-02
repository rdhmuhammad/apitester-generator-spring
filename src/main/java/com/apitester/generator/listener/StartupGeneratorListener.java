package com.apitester.generator.listener;

import com.apitester.generator.config.ApiTesterProperties;
import com.apitester.generator.generator.PostmanCollectionGenerator;
import com.apitester.generator.model.PostmanCollection;
import com.apitester.generator.processor.EndpointProcessor;
import com.apitester.generator.scanner.ControllerScanner;
import com.apitester.generator.service.PostmanMergeService;
import com.apitester.generator.writer.PostmanCollectionWriter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class StartupGeneratorListener {

    private final ControllerScanner scanner;
    private final EndpointProcessor endpointProcessor;
    private final PostmanCollectionGenerator generator;
    private final PostmanCollectionWriter writer;
    private final PostmanMergeService mergeService;
    private final ApiTesterProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        if (!properties.getCollection().isGenerate()) {
            log.info("Postman collection generation is disabled (apitester.collection.generate=false).");
            return;
        }

        log.info("Starting Postman collection generation...");

        ApplicationContext context = event.getApplicationContext();
        List<ControllerScanner.ControllerInfo> controllers = scanner.scan(context);

        if (controllers.isEmpty()) {
            log.warn("No controllers annotated with @ApiTester found. Skipping generation.");
            return;
        }

        PostmanCollection freshlyGenerated = generator.generate(controllers, endpointProcessor);
        String outputPath = properties.getCollection().getOutputPath();
        String cachePath = properties.getCollection().getCachePath();

        if (!properties.getCollection().isPreserveUserChanges()) {
            writer.write(freshlyGenerated, outputPath);
            log.info("Postman collection generation complete (preserveUserChanges=false). {} controllers processed.", controllers.size());
            return;
        }

        // Three-Way Merge Workflow
        File targetFile = new File(outputPath);
        File cacheFile = new File(cachePath);

        try {
            JsonNode updateNode = objectMapper.valueToTree(freshlyGenerated);

            if (!targetFile.exists()) {
                // First generation ever: output collection and seed cache
                writer.write(freshlyGenerated, outputPath);
                writeJsonNodeToFile(updateNode, cacheFile);
                log.info("Initial Postman collection and cache created at {} and {}", outputPath, cachePath);
            } else if (!cacheFile.exists()) {
                // Target exists (e.g. pre-existing collection or user export), but no cache
                JsonNode targetNode = readJsonFile(targetFile);
                JsonNode merged = mergeService.executeTwoWayMerge(targetNode, updateNode);
                writeJsonNodeToFile(merged, targetFile);
                writeJsonNodeToFile(updateNode, cacheFile);
                log.info("Postman collection reconciled with existing file and cache initialized.");
            } else {
                // Standard Three-Way Merge
                JsonNode baseNode = readJsonFile(cacheFile);
                JsonNode targetNode = readJsonFile(targetFile);
                JsonNode merged = mergeService.executeThreeWayMerge(baseNode, targetNode, updateNode);
                writeJsonNodeToFile(merged, targetFile);
                writeJsonNodeToFile(updateNode, cacheFile);
                log.info("Three-way merge completed successfully: user changes preserved.");
            }
        } catch (Exception e) {
            log.error("Error during three-way collection merge, falling back to direct write: {}", e.getMessage(), e);
            writer.write(freshlyGenerated, outputPath);
        }

        log.info("Postman collection generation complete. {} controllers processed.", controllers.size());
    }

    private JsonNode readJsonFile(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        String str = new String(bytes, StandardCharsets.UTF_8);
        if (str.startsWith("\uFEFF")) {
            str = str.substring(1);
        }
        return objectMapper.readTree(str);
    }

    private void writeJsonNodeToFile(JsonNode node, File file) throws IOException {
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, node);
    }
}
