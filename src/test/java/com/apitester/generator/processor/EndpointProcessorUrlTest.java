package com.apitester.generator.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EndpointProcessorUrlTest {

    private EndpointProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new EndpointProcessor(new DtoAnalyzer());
    }

    @Test
    @DisplayName("Should build full path from class and method level mappings")
    void buildFullPath() {
        assertEquals("/api/v1/users", processor.buildFullPath("/api/v1", "/users"));
        assertEquals("/api/v1/users", processor.buildFullPath("/api/v1/", "/users"));
        assertEquals("/api/v1/users", processor.buildFullPath("/api/v1", "users"));
        assertEquals("/api/v1/users", processor.buildFullPath("/api/v1/", "/users/"));
        assertEquals("/", processor.buildFullPath("", ""));
        assertEquals("/health", processor.buildFullPath("", "/health"));
    }

    @Test
    @DisplayName("Should handle path with trailing and leading slashes")
    void buildFullPathEdgeCases() {
        assertEquals("/api", processor.buildFullPath("/api", ""));
        assertEquals("/api", processor.buildFullPath("/api/", ""));
        assertEquals("/api/users", processor.buildFullPath("/api/", "users/"));
    }
}
