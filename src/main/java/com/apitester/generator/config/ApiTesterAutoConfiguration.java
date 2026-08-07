package com.apitester.generator.config;

import com.apitester.generator.generator.PostmanCollectionGenerator;
import com.apitester.generator.listener.StartupGeneratorListener;
import com.apitester.generator.processor.DtoAnalyzer;
import com.apitester.generator.processor.EndpointProcessor;
import com.apitester.generator.scanner.ControllerScanner;
import com.apitester.generator.writer.PostmanCollectionWriter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ApiTesterProperties.class)
@ConditionalOnProperty(name = "apitester.enabled", havingValue = "true", matchIfMissing = true)
public class ApiTesterAutoConfiguration {

    @Bean
    public DtoAnalyzer dtoAnalyzer() {
        return new DtoAnalyzer();
    }

    @Bean
    public ControllerScanner controllerScanner() {
        return new ControllerScanner();
    }

    @Bean
    public EndpointProcessor endpointProcessor(DtoAnalyzer dtoAnalyzer) {
        return new EndpointProcessor(dtoAnalyzer);
    }

    @Bean
    public PostmanCollectionGenerator postmanCollectionGenerator(ApiTesterProperties properties) {
        return new PostmanCollectionGenerator(properties);
    }

    @Bean
    public PostmanCollectionWriter postmanCollectionWriter() {
        return new PostmanCollectionWriter();
    }

    @Bean
    public StartupGeneratorListener startupGeneratorListener(
            ControllerScanner scanner,
            EndpointProcessor endpointProcessor,
            PostmanCollectionGenerator generator,
            PostmanCollectionWriter writer,
            ApiTesterProperties properties) {
        return new StartupGeneratorListener(scanner, endpointProcessor, generator, writer, properties);
    }
}
