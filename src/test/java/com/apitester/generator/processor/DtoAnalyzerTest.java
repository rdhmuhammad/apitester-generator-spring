package com.apitester.generator.processor;

import com.apitester.generator.TestDtoClasses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DtoAnalyzerTest {

    private DtoAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new DtoAnalyzer();
    }

    @Test
    @DisplayName("Should resolve simple field names as-is")
    void resolveSimpleQueryDto() {
        Map<String, Class<?>> params = analyzer.resolveQueryParams(TestDtoClasses.SimpleQueryDto.class);

        assertEquals(3, params.size());
        assertTrue(params.containsKey("name"));
        assertTrue(params.containsKey("email"));
        assertTrue(params.containsKey("age"));
        assertEquals(String.class, params.get("name"));
        assertEquals(int.class, params.get("age"));
    }

    @Test
    @DisplayName("Should apply @JsonNaming SnakeCaseStrategy")
    void resolveSnakeCaseDto() {
        com.fasterxml.jackson.databind.annotation.JsonNaming ann =
                TestDtoClasses.SnakeCaseDto.class.getAnnotation(
                        com.fasterxml.jackson.databind.annotation.JsonNaming.class);
        System.out.println("Annotation found: " + (ann != null));
        if (ann != null) {
            System.out.println("Annotation value: " + ann.value());
            System.out.println("Annotation value simple name: " + ann.value().getSimpleName());
        }

        Map<String, Class<?>> params = analyzer.resolveQueryParams(TestDtoClasses.SnakeCaseDto.class);
        System.out.println("SnakeCase params: " + params.keySet());

        assertEquals(2, params.size());
        assertTrue(params.containsKey("user_id"), "Keys: " + params.keySet());
        assertTrue(params.containsKey("first_name"), "Keys: " + params.keySet());
    }

    @Test
    @DisplayName("Should detect custom setter naming (setUser_id -> user_id)")
    void resolveCustomSetterDto() {
        Map<String, Class<?>> params = analyzer.resolveQueryParams(TestDtoClasses.CustomSetterDto.class);

        assertEquals(1, params.size());
        assertTrue(params.containsKey("user_id"));
        assertEquals(String.class, params.get("user_id"));
    }

    @Test
    @DisplayName("Should use @JsonProperty name over field name")
    void resolveJsonPropertyDto() {
        Map<String, Class<?>> params = analyzer.resolveQueryParams(TestDtoClasses.JsonPropertyDto.class);

        assertEquals(1, params.size());
        assertTrue(params.containsKey("custom_name"));
        assertFalse(params.containsKey("fieldName"));
    }

    @Test
    @DisplayName("Should not resolve mixed DTO with @JsonProperty and regular fields")
    void resolveMixedDto() {
        Map<String, Class<?>> params = analyzer.resolveQueryParams(TestDtoClasses.MixedDto.class);

        assertEquals(3, params.size());
        assertTrue(params.containsKey("user_id"));
        assertTrue(params.containsKey("userId"));
        assertTrue(params.containsKey("active"));
    }

    @Test
    @DisplayName("Should return empty map for class with no fields")
    void resolveEmptyClass() {
        Map<String, Class<?>> params = analyzer.resolveQueryParams(Object.class);
        assertEquals(0, params.size());
    }

    @Test
    @DisplayName("Should resolve @JsonNaming KebabCaseStrategy")
    void resolveKebabCaseNaming() {
        @com.fasterxml.jackson.databind.annotation.JsonNaming(
                com.fasterxml.jackson.databind.PropertyNamingStrategies.KebabCaseStrategy.class)
        class KebabDto {
            private String requestId;
            public String getRequestId() { return requestId; }
            public void setRequestId(String requestId) { this.requestId = requestId; }
        }

        Map<String, Class<?>> params = analyzer.resolveQueryParams(KebabDto.class);
        assertEquals(1, params.size());
        assertTrue(params.containsKey("request-id"));
    }

    @Test
    @DisplayName("Should generate example values based on type")
    void generateExampleValues() {
        assertEquals("", analyzer.generateExampleValue(String.class));
        assertEquals(0, analyzer.generateExampleValue(int.class));
        assertEquals(0L, analyzer.generateExampleValue(Long.class));
        assertEquals(false, analyzer.generateExampleValue(boolean.class));
        assertEquals(0.0, analyzer.generateExampleValue(Double.class));
    }
}
