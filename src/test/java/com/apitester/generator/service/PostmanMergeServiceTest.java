package com.apitester.generator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PostmanMergeServiceTest {

    private PostmanMergeService mergeService;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mergeService = new PostmanMergeService(mapper);
    }

    @Test
    @DisplayName("Should preserve user-customized JSON body payload while injecting newly added Spring DTO fields")
    void testDeepMergeRequestBody() throws Exception {
        String baseJson = "{\n" +
                "  \"info\": { \"name\": \"API\", \"schema\": \"https://schema.getpostman.com/json/collection/v2.1.0/collection.json\" },\n" +
                "  \"item\": [{\n" +
                "    \"name\": \"Create User\",\n" +
                "    \"funIden\": \"UserController#createUser\",\n" +
                "    \"id\": \"user-create-uuid\",\n" +
                "    \"request\": {\n" +
                "      \"method\": \"POST\",\n" +
                "      \"funIden\": \"UserController#createUser\",\n" +
                "      \"url\": { \"raw\": \"{{baseUrl}}/api/v1/users\" },\n" +
                "      \"body\": {\n" +
                "        \"mode\": \"raw\",\n" +
                "        \"raw\": \"{\\n  \\\"name\\\": \\\"string\\\"\\n}\"\n" +
                "      }\n" +
                "    }\n" +
                "  }]\n" +
                "}";

        // Target: User customized the payload to realistic test data
        String targetJson = "{\n" +
                "  \"info\": { \"name\": \"API\", \"schema\": \"https://schema.getpostman.com/json/collection/v2.1.0/collection.json\" },\n" +
                "  \"item\": [{\n" +
                "    \"name\": \"Create User\",\n" +
                "    \"funIden\": \"UserController#createUser\",\n" +
                "    \"id\": \"user-create-uuid\",\n" +
                "    \"request\": {\n" +
                "      \"method\": \"POST\",\n" +
                "      \"funIden\": \"UserController#createUser\",\n" +
                "      \"url\": { \"raw\": \"{{baseUrl}}/api/v1/users\" },\n" +
                "      \"body\": {\n" +
                "        \"mode\": \"raw\",\n" +
                "        \"raw\": \"{\\n  \\\"name\\\": \\\"Alice In Wonderland\\\"\\n}\"\n" +
                "      }\n" +
                "    }\n" +
                "  }]\n" +
                "}";

        // Update: Spring model added "age" field with default 0
        String updateJson = "{\n" +
                "  \"info\": { \"name\": \"API\", \"schema\": \"https://schema.getpostman.com/json/collection/v2.1.0/collection.json\" },\n" +
                "  \"item\": [{\n" +
                "    \"name\": \"Create User\",\n" +
                "    \"funIden\": \"UserController#createUser\",\n" +
                "    \"id\": \"user-create-uuid\",\n" +
                "    \"request\": {\n" +
                "      \"method\": \"POST\",\n" +
                "      \"funIden\": \"UserController#createUser\",\n" +
                "      \"url\": { \"raw\": \"{{baseUrl}}/api/v1/users\" },\n" +
                "      \"body\": {\n" +
                "        \"mode\": \"raw\",\n" +
                "        \"raw\": \"{\\n  \\\"name\\\": \\\"string\\\",\\n  \\\"age\\\": 0\\n}\"\n" +
                "      }\n" +
                "    }\n" +
                "  }]\n" +
                "}";

        JsonNode baseNode = mapper.readTree(baseJson);
        JsonNode targetNode = mapper.readTree(targetJson);
        JsonNode updateNode = mapper.readTree(updateJson);

        JsonNode merged = mergeService.executeThreeWayMerge(baseNode, targetNode, updateNode);

        assertNotNull(merged);
        JsonNode reqBody = merged.path("item").get(0).path("request").path("body").path("raw");
        assertFalse(reqBody.asText().isEmpty());

        JsonNode parsedPayload = mapper.readTree(reqBody.asText());
        assertEquals("Alice In Wonderland", parsedPayload.path("name").asText());
        assertEquals(0, parsedPayload.path("age").asInt());
    }

    @Test
    @DisplayName("Should revert user method modification back to Spring controller method (generator ownership)")
    void testMethodOwnershipRevertsToGenerator() throws Exception {
        String baseJson = "{\n" +
                "  \"item\": [{\n" +
                "    \"name\": \"Get Users\",\n" +
                "    \"funIden\": \"UserController#getUsers\",\n" +
                "    \"id\": \"uuid-1\",\n" +
                "    \"request\": {\n" +
                "      \"method\": \"GET\",\n" +
                "      \"funIden\": \"UserController#getUsers\",\n" +
                "      \"url\": { \"raw\": \"{{baseUrl}}/api/v1/users\" }\n" +
                "    }\n" +
                "  }]\n" +
                "}";

        // User manually changed GET to POST in Postman
        String targetJson = "{\n" +
                "  \"item\": [{\n" +
                "    \"name\": \"Get Users\",\n" +
                "    \"funIden\": \"UserController#getUsers\",\n" +
                "    \"id\": \"uuid-1\",\n" +
                "    \"request\": {\n" +
                "      \"method\": \"POST\",\n" +
                "      \"funIden\": \"UserController#getUsers\",\n" +
                "      \"url\": { \"raw\": \"{{baseUrl}}/api/v1/users\" }\n" +
                "    }\n" +
                "  }]\n" +
                "}";

        // Spring controller still declares @GetMapping
        String updateJson = "{\n" +
                "  \"item\": [{\n" +
                "    \"name\": \"Get Users\",\n" +
                "    \"funIden\": \"UserController#getUsers\",\n" +
                "    \"id\": \"uuid-1\",\n" +
                "    \"request\": {\n" +
                "      \"method\": \"GET\",\n" +
                "      \"funIden\": \"UserController#getUsers\",\n" +
                "      \"url\": { \"raw\": \"{{baseUrl}}/api/v1/users\" }\n" +
                "    }\n" +
                "  }]\n" +
                "}";

        JsonNode baseNode = mapper.readTree(baseJson);
        JsonNode targetNode = mapper.readTree(targetJson);
        JsonNode updateNode = mapper.readTree(updateJson);

        JsonNode merged = mergeService.executeThreeWayMerge(baseNode, targetNode, updateNode);

        // Generator owns method: must be GET
        assertEquals("GET", merged.path("item").get(0).path("request").path("method").asText());
        assertEquals("uuid-1", merged.path("item").get(0).path("id").asText());
    }

    @Test
    @DisplayName("Should preserve user-added test scripts and custom authorization headers")
    void testPreserveScriptsAndAuthHeaders() throws Exception {
        String baseJson = "{\n" +
                "  \"item\": [{\n" +
                "    \"name\": \"Profile\",\n" +
                "    \"funIden\": \"UserController#getProfile\",\n" +
                "    \"id\": \"profile-id\",\n" +
                "    \"request\": {\n" +
                "      \"method\": \"GET\",\n" +
                "      \"funIden\": \"UserController#getProfile\",\n" +
                "      \"url\": { \"raw\": \"{{baseUrl}}/api/v1/profile\" },\n" +
                "      \"header\": []\n" +
                "    }\n" +
                "  }]\n" +
                "}";

        // Target: User added test script and Authorization header
        String targetJson = "{\n" +
                "  \"item\": [{\n" +
                "    \"name\": \"Profile\",\n" +
                "    \"funIden\": \"UserController#getProfile\",\n" +
                "    \"id\": \"profile-id\",\n" +
                "    \"request\": {\n" +
                "      \"method\": \"GET\",\n" +
                "      \"funIden\": \"UserController#getProfile\",\n" +
                "      \"url\": { \"raw\": \"{{baseUrl}}/api/v1/profile\" },\n" +
                "      \"header\": [{ \"key\": \"Authorization\", \"value\": \"Bearer token123\" }]\n" +
                "    },\n" +
                "    \"event\": [{\n" +
                "      \"listen\": \"test\",\n" +
                "      \"script\": { \"type\": \"text/javascript\", \"exec\": [\"pm.test('Status is 200', function() { pm.response.to.have.status(200); });\"] }\n" +
                "    }]\n" +
                "  }]\n" +
                "}";

        String updateJson = baseJson; // Generator ran again with unchanged controller

        JsonNode baseNode = mapper.readTree(baseJson);
        JsonNode targetNode = mapper.readTree(targetJson);
        JsonNode updateNode = mapper.readTree(updateJson);

        JsonNode merged = mergeService.executeThreeWayMerge(baseNode, targetNode, updateNode);

        JsonNode item = merged.path("item").get(0);
        // Script preserved
        assertTrue(item.has("event"));
        assertEquals(1, item.path("event").size());
        assertEquals("test", item.path("event").get(0).path("listen").asText());

        // Header preserved
        assertTrue(item.path("request").has("header"));
        assertEquals(1, item.path("request").path("header").size());
        assertEquals("Authorization", item.path("request").path("header").get(0).path("key").asText());
        assertEquals("Bearer token123", item.path("request").path("header").get(0).path("value").asText());
    }

    @Test
    @DisplayName("Should remove deleted controller endpoints from collection (Option A)")
    void testRemovedControllerEndpointDeleted() throws Exception {
        String baseJson = "{\n" +
                "  \"item\": [\n" +
                "    { \"name\": \"Endpoint 1\", \"funIden\": \"Ctrl#ep1\", \"id\": \"id-1\", \"request\": { \"method\": \"GET\", \"funIden\": \"Ctrl#ep1\" } },\n" +
                "    { \"name\": \"Endpoint 2\", \"funIden\": \"Ctrl#ep2\", \"id\": \"id-2\", \"request\": { \"method\": \"GET\", \"funIden\": \"Ctrl#ep2\" } }\n" +
                "  ]\n" +
                "}";

        String targetJson = baseJson;

        // Controller for ep2 was deleted in Spring code
        String updateJson = "{\n" +
                "  \"item\": [\n" +
                "    { \"name\": \"Endpoint 1\", \"funIden\": \"Ctrl#ep1\", \"id\": \"id-1\", \"request\": { \"method\": \"GET\", \"funIden\": \"Ctrl#ep1\" } }\n" +
                "  ]\n" +
                "}";

        JsonNode baseNode = mapper.readTree(baseJson);
        JsonNode targetNode = mapper.readTree(targetJson);
        JsonNode updateNode = mapper.readTree(updateJson);

        JsonNode merged = mergeService.executeThreeWayMerge(baseNode, targetNode, updateNode);

        assertEquals(1, merged.path("item").size());
        assertEquals("Endpoint 1", merged.path("item").get(0).path("name").asText());
    }

    @Test
    @DisplayName("Should add newly introduced controller endpoints into collection")
    void testNewControllerEndpointAdded() throws Exception {
        String baseJson = "{\n" +
                "  \"item\": [\n" +
                "    { \"name\": \"Endpoint 1\", \"funIden\": \"Ctrl#ep1\", \"id\": \"id-1\", \"request\": { \"method\": \"GET\", \"funIden\": \"Ctrl#ep1\" } }\n" +
                "  ]\n" +
                "}";

        String targetJson = baseJson;

        // Controller added ep2
        String updateJson = "{\n" +
                "  \"item\": [\n" +
                "    { \"name\": \"Endpoint 1\", \"funIden\": \"Ctrl#ep1\", \"id\": \"id-1\", \"request\": { \"method\": \"GET\", \"funIden\": \"Ctrl#ep1\" } },\n" +
                "    { \"name\": \"Endpoint 2\", \"funIden\": \"Ctrl#ep2\", \"id\": \"id-2\", \"request\": { \"method\": \"POST\", \"funIden\": \"Ctrl#ep2\" } }\n" +
                "  ]\n" +
                "}";

        JsonNode baseNode = mapper.readTree(baseJson);
        JsonNode targetNode = mapper.readTree(targetJson);
        JsonNode updateNode = mapper.readTree(updateJson);

        JsonNode merged = mergeService.executeThreeWayMerge(baseNode, targetNode, updateNode);

        assertEquals(2, merged.path("item").size());
        assertEquals("Endpoint 1", merged.path("item").get(0).path("name").asText());
        assertEquals("Endpoint 2", merged.path("item").get(1).path("name").asText());
    }

    @Test
    @DisplayName("Should preserve user-customized collection variables")
    void testPreserveCollectionVariables() throws Exception {
        String baseJson = "{\n" +
                "  \"item\": [],\n" +
                "  \"variable\": [\n" +
                "    { \"key\": \"baseUrl\", \"value\": \"http://localhost:8080\" }\n" +
                "  ]\n" +
                "}";

        // User changed baseUrl to staging and added an apiKey variable
        String targetJson = "{\n" +
                "  \"item\": [],\n" +
                "  \"variable\": [\n" +
                "    { \"key\": \"baseUrl\", \"value\": \"https://staging.example.com\" },\n" +
                "    { \"key\": \"apiKey\", \"value\": \"secret-key-123\" }\n" +
                "  ]\n" +
                "}";

        String updateJson = baseJson;

        JsonNode baseNode = mapper.readTree(baseJson);
        JsonNode targetNode = mapper.readTree(targetJson);
        JsonNode updateNode = mapper.readTree(updateJson);

        JsonNode merged = mergeService.executeThreeWayMerge(baseNode, targetNode, updateNode);

        assertEquals(2, merged.path("variable").size());
        assertEquals("baseUrl", merged.path("variable").get(0).path("key").asText());
        assertEquals("https://staging.example.com", merged.path("variable").get(0).path("value").asText());
        assertEquals("apiKey", merged.path("variable").get(1).path("key").asText());
        assertEquals("secret-key-123", merged.path("variable").get(1).path("value").asText());
    }
}
