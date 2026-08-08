package com.apitester.generator.processor;

import com.apitester.generator.example.TestAdminController;
import com.apitester.generator.example.TestMemberController;
import com.apitester.generator.example.TestUploadController;
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
        assertEquals("Create Online", postEndpoint.getName());
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

    @Test
    @DisplayName("Should process multipart form-data endpoint")
    void processMultipartFormData() {
        List<EndpointProcessor.EndpointInfo> endpoints =
                processor.processController(TestUploadController.class, Set.of());

        assertEquals(4, endpoints.size());

        EndpointProcessor.EndpointInfo uploadEndpoint = endpoints.stream()
                .filter(e -> e.getRequest().getUrl().getRaw().contains("file"))
                .findFirst().orElse(null);

        assertNotNull(uploadEndpoint);
        assertEquals("POST", uploadEndpoint.getRequest().getMethod());
        assertNotNull(uploadEndpoint.getRequest().getBody());
        assertEquals("formdata", uploadEndpoint.getRequest().getBody().getMode());
        assertNotNull(uploadEndpoint.getRequest().getBody().getFormdata());
        assertEquals(2, uploadEndpoint.getRequest().getBody().getFormdata().size());

        assertTrue(uploadEndpoint.getRequest().getBody().getFormdata().stream()
                .anyMatch(f -> f.getKey().equals("file") && "file".equals(f.getType())));
        assertTrue(uploadEndpoint.getRequest().getBody().getFormdata().stream()
                .anyMatch(f -> f.getKey().equals("name") && "text".equals(f.getType())));
    }

    @Test
    @DisplayName("Should process multipart form-data with multiple files and params")
    void processMultipartMultipleFiles() {
        List<EndpointProcessor.EndpointInfo> endpoints =
                processor.processController(TestUploadController.class, Set.of());

        EndpointProcessor.EndpointInfo multiEndpoint = endpoints.stream()
                .filter(e -> e.getRequest().getUrl().getRaw().contains("multi"))
                .findFirst().orElse(null);

        assertNotNull(multiEndpoint);
        assertEquals("formdata", multiEndpoint.getRequest().getBody().getMode());
        assertNotNull(multiEndpoint.getRequest().getBody().getFormdata());
        assertEquals(3, multiEndpoint.getRequest().getBody().getFormdata().size());

        assertTrue(multiEndpoint.getRequest().getBody().getFormdata().stream()
                .anyMatch(f -> f.getKey().equals("files") && "file".equals(f.getType())));
        assertTrue(multiEndpoint.getRequest().getBody().getFormdata().stream()
                .anyMatch(f -> f.getKey().equals("folder") && "text".equals(f.getType())));
        assertTrue(multiEndpoint.getRequest().getBody().getFormdata().stream()
                .anyMatch(f -> f.getKey().equals("overwrite") && "text".equals(f.getType())));

        assertNull(multiEndpoint.getRequest().getUrl().getQuery());
    }

    @Test
    @DisplayName("Should process @ModelAttribute as formdata")
    void processModelAttributeFormData() {
        List<EndpointProcessor.EndpointInfo> endpoints =
                processor.processController(TestUploadController.class, Set.of());

        EndpointProcessor.EndpointInfo profileEndpoint = endpoints.stream()
                .filter(e -> e.getRequest().getUrl().getRaw().contains("profile"))
                .findFirst().orElse(null);

        assertNotNull(profileEndpoint);
        assertEquals("POST", profileEndpoint.getRequest().getMethod());
        assertNotNull(profileEndpoint.getRequest().getBody());
        assertEquals("formdata", profileEndpoint.getRequest().getBody().getMode());
        assertNotNull(profileEndpoint.getRequest().getBody().getFormdata());
        assertEquals(3, profileEndpoint.getRequest().getBody().getFormdata().size());

        assertTrue(profileEndpoint.getRequest().getBody().getFormdata().stream()
                .allMatch(f -> "text".equals(f.getType())));

        assertTrue(profileEndpoint.getRequest().getBody().getFormdata().stream()
                .anyMatch(f -> f.getKey().equals("username")));
        assertTrue(profileEndpoint.getRequest().getBody().getFormdata().stream()
                .anyMatch(f -> f.getKey().equals("email")));
        assertTrue(profileEndpoint.getRequest().getBody().getFormdata().stream()
                .anyMatch(f -> f.getKey().equals("age")));

        assertNull(profileEndpoint.getRequest().getUrl().getQuery());
    }

    @Test
    @DisplayName("Should process @ModelAttribute mixed with @RequestPart")
    void processModelAttributeWithMultipart() {
        List<EndpointProcessor.EndpointInfo> endpoints =
                processor.processController(TestUploadController.class, Set.of());

        EndpointProcessor.EndpointInfo docEndpoint = endpoints.stream()
                .filter(e -> e.getRequest().getUrl().getRaw().contains("document"))
                .findFirst().orElse(null);

        assertNotNull(docEndpoint);
        assertEquals("formdata", docEndpoint.getRequest().getBody().getMode());
        assertNotNull(docEndpoint.getRequest().getBody().getFormdata());
        assertEquals(4, docEndpoint.getRequest().getBody().getFormdata().size());

        assertTrue(docEndpoint.getRequest().getBody().getFormdata().stream()
                .anyMatch(f -> f.getKey().equals("attachment") && "file".equals(f.getType())));

        assertTrue(docEndpoint.getRequest().getBody().getFormdata().stream()
                .anyMatch(f -> f.getKey().equals("username") && "text".equals(f.getType())));
        assertTrue(docEndpoint.getRequest().getBody().getFormdata().stream()
                .anyMatch(f -> f.getKey().equals("email") && "text".equals(f.getType())));
        assertTrue(docEndpoint.getRequest().getBody().getFormdata().stream()
                .anyMatch(f -> f.getKey().equals("age") && "text".equals(f.getType())));

        assertNull(docEndpoint.getRequest().getUrl().getQuery());
    }
}
