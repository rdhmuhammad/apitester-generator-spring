package com.apitester.generator.controller;

import com.apitester.generator.annotation.EnableApiTester;
import com.apitester.generator.config.ApiTesterProperties;
import com.apitester.generator.dto.DocsContent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = ApiTesterControllerIntegrationTest.TestApplication.class)
@AutoConfigureMockMvc
class ApiTesterControllerIntegrationTest {

    @SpringBootApplication(scanBasePackages = "com.apitester.generator.config")
    @EnableApiTester
    static class TestApplication {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiTesterProperties properties;

    @Autowired
    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    private File collectionFile;

    @BeforeEach
    void setUp() throws Exception {
        collectionFile = tempDir.resolve("test_collection.json").toFile();
        properties.getCollection().setOutputPath(collectionFile.getAbsolutePath());
        properties.getCollection().setName("Test App Collection");

        String sampleJson = "\uFEFF{\n" +
                "  \"info\": {\n" +
                "    \"_postman_id\": \"test-postman-uuid-123\",\n" +
                "    \"name\": \"Test App Collection\",\n" +
                "    \"schema\": \"https://schema.getpostman.com/json/collection/v2.1.0/collection.json\"\n" +
                "  },\n" +
                "  \"item\": [\n" +
                "    {\n" +
                "      \"name\": \"User Endpoint\",\n" +
                "      \"request\": {\n" +
                "        \"method\": \"GET\",\n" +
                "        \"header\": [{\"key\": \"Accept\", \"value\": \"application/json\"}],\n" +
                "        \"url\": {\n" +
                "          \"raw\": \"{{baseUrl}}/api/v1/users\",\n" +
                "          \"host\": [\"{{baseUrl}}\"],\n" +
                "          \"path\": [\"api\", \"v1\", \"users\"],\n" +
                "          \"query\": [{\"key\": \"page\", \"value\": \"1\"}]\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"variable\": [\n" +
                "    {\"key\": \"baseUrl\", \"value\": \"http://localhost:8080\"}\n" +
                "  ]\n" +
                "}";

        Files.write(collectionFile.toPath(), sampleJson.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("GET /apitester/api/v1/collection/list should return collection metadata")
    void testListCollections() throws Exception {
        mockMvc.perform(get("/apitester/api/v1/collection/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.messageTitle", is("Success")))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is("test-postman-uuid-123")))
                .andExpect(jsonPath("$.data[0].name", is("Test App Collection")))
                .andExpect(jsonPath("$.data[0].is_selected", is(true)))
                .andExpect(jsonPath("$.data[0].path", is(collectionFile.getAbsolutePath())));
    }

    @Test
    @DisplayName("GET /apitester/api/v1/collection/read/{id} should strip BOM, categorize BASE_URL, and inject UUIDs")
    void testReadCollection() throws Exception {
        mockMvc.perform(get("/apitester/api/v1/collection/read/{id}", "test-postman-uuid-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.changed", is(false)))
                .andExpect(jsonPath("$.data.content.variable[0].category", is("BASE_URL")))
                .andExpect(jsonPath("$.data.content.variable[0].id", not(emptyOrNullString())))
                .andExpect(jsonPath("$.data.content.item[0].id", not(emptyOrNullString())))
                .andExpect(jsonPath("$.data.content.item[0].request.header[0].id", not(emptyOrNullString())))
                .andExpect(jsonPath("$.data.content.item[0].request.url.query[0].id", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("GET /apitester/api/v1/collection/read/{id} with invalid id should return 400")
    void testReadCollectionNotFound() throws Exception {
        mockMvc.perform(get("/apitester/api/v1/collection/read/{id}", "unknown-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.messageTitle", is("Invalid data.")))
                .andExpect(jsonPath("$.message", is("Collection not found")));
    }

    @Test
    @DisplayName("PUT /apitester/api/v1/collection/select/{id} should mark collection selected")
    void testSelectCollection() throws Exception {
        mockMvc.perform(put("/apitester/api/v1/collection/select/{id}", "test-postman-uuid-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Collection selected")))
                .andExpect(jsonPath("$.data.id", is("test-postman-uuid-123")))
                .andExpect(jsonPath("$.data.is_selected", is(true)));
    }

    @Test
    @DisplayName("GET /apitester/api/v1/collection/get-active and /read-selected should return active collection")
    void testGetActiveAndReadSelected() throws Exception {
        mockMvc.perform(get("/apitester/api/v1/collection/get-active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is("test-postman-uuid-123")));

        mockMvc.perform(get("/apitester/api/v1/collection/read-selected"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content.info.name", is("Test App Collection")));
    }

    @Test
    @DisplayName("PUT /apitester/api/v1/collection/write/{id} should update collection file on disk")
    void testWriteCollection() throws Exception {
        String updatedJson = "{\"info\":{\"name\":\"Updated Collection\"}}";

        mockMvc.perform(put("/apitester/api/v1/collection/write/{id}", "test-postman-uuid-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Collection written successfully")));

        String onDisk = Files.readString(collectionFile.toPath());
        assertTrue(onDisk.contains("Updated Collection"));
    }

    @Test
    @DisplayName("PUT /apitester/api/v1/collection/write/{id} with structured DocsContent DTO should persist and be readable as DocsContent")
    void testWriteAndReadStructuredDocsContent() throws Exception {
        DocsContent content = DocsContent.builder()
                .info(DocsContent.CollectionInfo.builder()
                        .name("Structured Test Collection")
                        .description("Test description")
                        .build())
                .item(java.util.List.of(
                        DocsContent.CollectionItem.builder()
                                .name("Get Item")
                                .request(DocsContent.Request.builder()
                                        .method("GET")
                                        .url(DocsContent.RequestUrl.builder()
                                                .raw("{{baseUrl}}/api/v1/items")
                                                .host(java.util.List.of("{{baseUrl}}"))
                                                .path(java.util.List.of("api", "v1", "items"))
                                                .query(java.util.List.of(
                                                        new DocsContent.Property("filter", "active")
                                                ))
                                                .build())
                                        .header(java.util.List.of(
                                                new DocsContent.Header("Accept", "application/json")
                                        ))
                                        .build())
                                .build()
                ))
                .auth(DocsContent.CollectionAuth.builder()
                        .type("bearer")
                        .bearer(java.util.List.of(
                                new DocsContent.Property("token", "secret-token")
                        ))
                        .build())
                .variable(java.util.List.of(
                        DocsContent.CollectionVar.builder()
                                .key("baseUrl")
                                .value("https://api.example.com")
                                .build()
                ))
                .build();

        String dtoJson = objectMapper.writeValueAsString(content);

        mockMvc.perform(put("/apitester/api/v1/collection/write/{id}", "test-postman-uuid-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dtoJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Collection written successfully")));

        mockMvc.perform(get("/apitester/api/v1/collection/read/{id}", "test-postman-uuid-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content.info.name", is("Structured Test Collection")))
                .andExpect(jsonPath("$.data.content.info._postman_id", is("test-postman-uuid-123")))
                .andExpect(jsonPath("$.data.content.item[0].name", is("Get Item")))
                .andExpect(jsonPath("$.data.content.item[0].id", not(emptyOrNullString())))
                .andExpect(jsonPath("$.data.content.item[0].request.method", is("GET")))
                .andExpect(jsonPath("$.data.content.item[0].request.header[0].key", is("Accept")))
                .andExpect(jsonPath("$.data.content.item[0].request.header[0].id", not(emptyOrNullString())))
                .andExpect(jsonPath("$.data.content.item[0].request.url.query[0].key", is("filter")))
                .andExpect(jsonPath("$.data.content.item[0].request.url.query[0].id", not(emptyOrNullString())))
                .andExpect(jsonPath("$.data.content.auth.type", is("bearer")))
                .andExpect(jsonPath("$.data.content.variable[0].key", is("baseUrl")))
                .andExpect(jsonPath("$.data.content.variable[0].category", is("BASE_URL")))
                .andExpect(jsonPath("$.data.content.variable[0].id", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("PUT /apitester/api/v1/collection/write-selected should accept DocsContent DTO")
    void testWriteSelectedCollectionDto() throws Exception {
        DocsContent content = DocsContent.builder()
                .info(DocsContent.CollectionInfo.builder()
                        .name("Active Selected Collection")
                        .build())
                .build();

        mockMvc.perform(put("/apitester/api/v1/collection/write-selected")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(content)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Collection written successfully")));

        mockMvc.perform(get("/apitester/api/v1/collection/read-selected"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content.info.name", is("Active Selected Collection")));
    }

    @Test
    @DisplayName("GET & PUT /apitester/api/v1/collection/{id}/environments should manage environment JSON")
    void testEnvironments() throws Exception {
        mockMvc.perform(get("/apitester/api/v1/collection/{id}/environments", "test-postman-uuid-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.environments", hasSize(0)));

        String envJson = "{\n" +
                "  \"environments\": [\n" +
                "    {\n" +
                "      \"name\": \"development\",\n" +
                "      \"variables\": {\n" +
                "        \"baseUrl\": \"http://localhost:3000\",\n" +
                "        \"apiKey\": \"dev-key\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        mockMvc.perform(put("/apitester/api/v1/collection/{id}/environments", "test-postman-uuid-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Environments written successfully")));

        mockMvc.perform(get("/apitester/api/v1/collection/{id}/environments", "test-postman-uuid-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.environments", hasSize(1)))
                .andExpect(jsonPath("$.data.environments[0].name", is("development")))
                .andExpect(jsonPath("$.data.environments[0].variables.baseUrl", is("http://localhost:3000")))
                .andExpect(jsonPath("$.data.environments[0].variables.apiKey", is("dev-key")));
    }

    @Test
    @DisplayName("POST /apitester/api/v1/auth/login and GET /apitester/api/v1/auth/me should succeed and set cookie")
    void testAuthEndpoints() throws Exception {
        String loginJson = "{\"username\":\"admin\",\"password\":\"secret\",\"rememberMe\":true}";

        mockMvc.perform(post("/apitester/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("token"))
                .andExpect(cookie().httpOnly("token", true))
                .andExpect(cookie().path("token", "/"))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Login successful")))
                .andExpect(jsonPath("$.data.username", is("admin")))
                .andExpect(jsonPath("$.data.token", not(emptyOrNullString())));

        mockMvc.perform(get("/apitester/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is("admin")))
                .andExpect(jsonPath("$.data.authenticated", is(true)));
    }

    @Test
    @DisplayName("Endpoints should NOT be mapped under unprefixed /api/v1 to avoid colliding with target service")
    void testNoCollisionWithTargetService() throws Exception {
        mockMvc.perform(get("/api/v1/collection/list"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"secret\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /apitester should redirect to /apitester/ and serve SPA index.html")
    void testUiEndpoints() throws Exception {
        mockMvc.perform(get("/apitester"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/apitester/"));

        mockMvc.perform(get("/apitester/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<title>Apitester</title>")));

        mockMvc.perform(get("/apitester/collection/any-client-route"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<title>Apitester</title>")));
    }

    @Test
    @DisplayName("GET /.env and /apitester/.env should return env text with VITE_API_URL")
    void testEnvEndpoints() throws Exception {
        mockMvc.perform(get("/.env"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(containsString("VITE_API_URL=/apitester/api/v1")));

        mockMvc.perform(get("/apitester/.env"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(containsString("VITE_API_URL=/apitester/api/v1")));
    }

    @Test
    @DisplayName("GET /apitester/api/v1/collection/read should work with active collection alias")
    void testCollectionReadAlias() throws Exception {
        mockMvc.perform(get("/apitester/api/v1/collection/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }
}
