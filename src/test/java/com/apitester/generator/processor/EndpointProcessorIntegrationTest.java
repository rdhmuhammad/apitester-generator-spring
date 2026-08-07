package com.apitester.generator.processor;

import com.apitester.generator.example.TestAdminController;
import com.apitester.generator.example.TestMemberController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EndpointProcessorIntegrationTest {

    private EndpointProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new EndpointProcessor(new DtoAnalyzer());
    }

    @Test
    @DisplayName("Should process POST endpoint with @RequestBody")
    void processPostEndpoint() {
        List<EndpointProcessor.EndpointInfo> endpoints =
                processor.processController(TestMemberController.class, Set.of());

        assertTrue(endpoints.size() >= 4);

        EndpointProcessor.EndpointInfo postEndpoint = endpoints.stream()
                .filter(e -> e.getRequest().getMethod().equals("POST"))
                .findFirst().orElse(null);

        assertNotNull(postEndpoint);
        assertEquals("Online", postEndpoint.getName());
        assertNotNull(postEndpoint.getRequest().getBody());
        assertEquals("raw", postEndpoint.getRequest().getBody().getMode());
        assertNotNull(postEndpoint.getRequest().getBody().getRaw());
    }

    @Test
    @DisplayName("Should process GET endpoint with @PathVariable")
    void processGetWithPathVariable() {
        List<EndpointProcessor.EndpointInfo> endpoints =
                processor.processController(TestMemberController.class, Set.of());

        EndpointProcessor.EndpointInfo verifyEndpoint = endpoints.stream()
                .filter(e -> e.getRequest().getUrl().getRaw().contains("verify"))
                .findFirst().orElse(null);

        assertNotNull(verifyEndpoint);
        assertEquals("GET", verifyEndpoint.getRequest().getMethod());
        assertTrue(verifyEndpoint.getRequest().getUrl().getRaw().contains(":phone"));
    }

    @Test
    @DisplayName("Should process GET endpoint with @RequestParam")
    void processGetWithRequestParam() {
        List<EndpointProcessor.EndpointInfo> endpoints =
                processor.processController(TestMemberController.class, Set.of());

        EndpointProcessor.EndpointInfo searchEndpoint = endpoints.stream()
                .filter(e -> e.getRequest().getUrl().getRaw().contains("search"))
                .findFirst().orElse(null);

        assertNotNull(searchEndpoint);
        assertEquals("GET", searchEndpoint.getRequest().getMethod());
        assertNotNull(searchEndpoint.getRequest().getUrl().getQuery());
        assertEquals(2, searchEndpoint.getRequest().getUrl().getQuery().size());
    }

    @Test
    @DisplayName("Should process GET endpoint with DTO query params (SnakeCase)")
    void processGetWithDtoQueryParams() {
        List<EndpointProcessor.EndpointInfo> endpoints =
                processor.processController(TestMemberController.class, Set.of());

        EndpointProcessor.EndpointInfo filterEndpoint = endpoints.stream()
                .filter(e -> e.getRequest().getUrl().getRaw().contains("filter"))
                .findFirst().orElse(null);

        assertNotNull(filterEndpoint);
        assertNotNull(filterEndpoint.getRequest().getUrl().getQuery());
        assertEquals(2, filterEndpoint.getRequest().getUrl().getQuery().size());
        assertNull(filterEndpoint.getRequest().getBody());
    }

    @Test
    @DisplayName("Should process endpoint with @JsonProperty DTO query params")
    void processJsonPropertyDtoQueryParams() {
        List<EndpointProcessor.EndpointInfo> endpoints =
                processor.processController(TestAdminController.class, Set.of());

        EndpointProcessor.EndpointInfo listEndpoint = endpoints.stream()
                .filter(e -> e.getRequest().getUrl().getRaw().contains("list"))
                .findFirst().orElse(null);

        assertNotNull(listEndpoint);
        assertNotNull(listEndpoint.getRequest().getUrl().getQuery());
        assertTrue(listEndpoint.getRequest().getUrl().getQuery().stream()
                .anyMatch(q -> q.getKey().equals("custom_name")));
    }

    @Test
    @DisplayName("Should generate correct URL paths")
    void generateCorrectUrlPaths() {
        List<EndpointProcessor.EndpointInfo> endpoints =
                processor.processController(TestMemberController.class, Set.of());

        EndpointProcessor.EndpointInfo onlineEndpoint = endpoints.stream()
                .filter(e -> e.getRequest().getMethod().equals("POST"))
                .findFirst().orElse(null);

        assertNotNull(onlineEndpoint);
        assertTrue(onlineEndpoint.getRequest().getUrl().getRaw().startsWith("{{baseUrl}}"));
        assertTrue(onlineEndpoint.getRequest().getUrl().getPath().contains("api"));
        assertTrue(onlineEndpoint.getRequest().getUrl().getPath().contains("v1"));
        assertTrue(onlineEndpoint.getRequest().getUrl().getPath().contains("member"));
    }
}
