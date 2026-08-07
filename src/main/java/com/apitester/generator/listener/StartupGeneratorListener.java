package com.apitester.generator.listener;

import com.apitester.generator.config.ApiTesterProperties;
import com.apitester.generator.generator.PostmanCollectionGenerator;
import com.apitester.generator.model.PostmanCollection;
import com.apitester.generator.processor.EndpointProcessor;
import com.apitester.generator.scanner.ControllerScanner;
import com.apitester.generator.writer.PostmanCollectionWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class StartupGeneratorListener {

    private final ControllerScanner scanner;
    private final EndpointProcessor endpointProcessor;
    private final PostmanCollectionGenerator generator;
    private final PostmanCollectionWriter writer;
    private final ApiTesterProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("Starting Postman collection generation...");

        ApplicationContext context = event.getApplicationContext();
        List<ControllerScanner.ControllerInfo> controllers = scanner.scan(context);

        if (controllers.isEmpty()) {
            log.warn("No controllers annotated with @ApiTester found. Skipping generation.");
            return;
        }

        PostmanCollection collection = generator.generate(controllers, endpointProcessor);
        writer.write(collection, properties.getCollection().getOutputPath());

        log.info("Postman collection generation complete. {} controllers processed.", controllers.size());
    }
}
