package com.apitester.generator.config;

import com.apitester.generator.annotation.EnableApiTester;
import com.apitester.generator.generator.PostmanCollectionGenerator;
import com.apitester.generator.listener.StartupGeneratorListener;
import com.apitester.generator.model.PostmanMapItem;
import com.apitester.generator.processor.DtoAnalyzer;
import com.apitester.generator.processor.EndpointProcessor;
import com.apitester.generator.scanner.ControllerScanner;
import com.apitester.generator.writer.PostmanCollectionWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@EnableConfigurationProperties(ApiTesterProperties.class)
@ConditionalOnProperty(name = "apitester.enabled", havingValue = "true", matchIfMissing = true)
public class ApiTesterAutoConfiguration {

    @Autowired
    public void extractGlobalVariables(ApplicationContext ctx, ApiTesterProperties properties) {
        Map<String, Object> beans = ctx.getBeansWithAnnotation(EnableApiTester.class);
        for (Object bean : beans.values()) {
            Class<?> clazz = bean.getClass();
            if (clazz.getSimpleName().contains("$$")) {
                clazz = clazz.getSuperclass();
            }
            EnableApiTester ann = clazz.getAnnotation(EnableApiTester.class);
            if (ann == null) continue;
            for (EnableApiTester.Variable v : ann.globalVariables()) {
                properties.getGlobalVariables().add(PostmanMapItem.builder()
                        .key(v.key())
                        .value(v.value())
                        .build());
            }
        }
    }

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

    @Configuration(proxyBeanMethods = false)
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(annotation = EnableApiTester.class)
    public static class ApiTesterWebConfiguration implements org.springframework.web.servlet.config.annotation.WebMvcConfigurer {

        private final ApiTesterProperties properties;

        public ApiTesterWebConfiguration(ApiTesterProperties properties) {
            this.properties = properties;
        }

        @Override
        public void addViewControllers(org.springframework.web.servlet.config.annotation.ViewControllerRegistry registry) {
            String uiPath = properties.getResolvedUiPath();
            registry.addRedirectViewController(uiPath, uiPath + "/");
            registry.addViewController(uiPath + "/").setViewName("forward:" + uiPath + "/index.html");
            if (!"/apitester".equals(uiPath)) {
                registry.addRedirectViewController("/apitester", "/apitester/");
                registry.addViewController("/apitester/").setViewName("forward:/apitester/index.html");
            }
        }

        @Override
        public void addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
            String uiPath = properties.getResolvedUiPath();
            registry.addResourceHandler(uiPath + "/**")
                    .addResourceLocations("classpath:/META-INF/resources/apitester/", "classpath:/static/apitester/")
                    .resourceChain(true)
                    .addResolver(new org.springframework.web.servlet.resource.PathResourceResolver() {
                        @Override
                        protected org.springframework.core.io.Resource getResource(String resourcePath, org.springframework.core.io.Resource location) throws java.io.IOException {
                            org.springframework.core.io.Resource requestedResource = location.createRelative(resourcePath);
                            return (requestedResource.exists() && requestedResource.isReadable())
                                    ? requestedResource
                                    : location.createRelative("index.html");
                        }
                    });

            if (!"/apitester".equals(uiPath)) {
                registry.addResourceHandler("/apitester/**")
                        .addResourceLocations("classpath:/META-INF/resources/apitester/", "classpath:/static/apitester/")
                        .resourceChain(true)
                        .addResolver(new org.springframework.web.servlet.resource.PathResourceResolver() {
                            @Override
                            protected org.springframework.core.io.Resource getResource(String resourcePath, org.springframework.core.io.Resource location) throws java.io.IOException {
                                org.springframework.core.io.Resource requestedResource = location.createRelative(resourcePath);
                                return (requestedResource.exists() && requestedResource.isReadable())
                                        ? requestedResource
                                        : location.createRelative("index.html");
                            }
                        });
            }
        }

        @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(com.fasterxml.jackson.databind.ObjectMapper.class)
        public com.fasterxml.jackson.databind.ObjectMapper apiTesterObjectMapper() {
            return new com.fasterxml.jackson.databind.ObjectMapper();
        }

        @Bean
        public com.apitester.generator.service.ApiTesterService apiTesterService(
                ApiTesterProperties properties,
                com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
            return new com.apitester.generator.service.ApiTesterService(properties, objectMapper);
        }

        @Bean
        public com.apitester.generator.controller.ApiTesterCollectionController apiTesterCollectionController(
                com.apitester.generator.service.ApiTesterService apiTesterService) {
            return new com.apitester.generator.controller.ApiTesterCollectionController(apiTesterService);
        }

        @Bean
        public com.apitester.generator.controller.ApiTesterAuthController apiTesterAuthController(
                com.apitester.generator.service.ApiTesterService apiTesterService) {
            return new com.apitester.generator.controller.ApiTesterAuthController(apiTesterService);
        }

        @Bean
        public com.apitester.generator.controller.ApiTesterUiController apiTesterUiController() {
            return new com.apitester.generator.controller.ApiTesterUiController();
        }

        @Bean
        public org.springframework.web.filter.CorsFilter apiTesterCorsFilter() {
            org.springframework.web.cors.UrlBasedCorsConfigurationSource source =
                    new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
            org.springframework.web.cors.CorsConfiguration config =
                    new org.springframework.web.cors.CorsConfiguration();
            config.setAllowCredentials(true);
            config.addAllowedOriginPattern("*");
            config.addAllowedHeader("*");
            config.addAllowedMethod("*");
            source.registerCorsConfiguration("/api/v1/**", config);
            source.registerCorsConfiguration("/apitester/**", config);
            return new org.springframework.web.filter.CorsFilter(source);
        }
    }
}

