package com.apitester.generator.generator;

import com.apitester.generator.config.ApiTesterProperties;
import com.apitester.generator.example.TestAdminController;
import com.apitester.generator.example.TestMemberController;
import com.apitester.generator.model.PostmanCollection;
import com.apitester.generator.model.PostmanItem;
import com.apitester.generator.model.PostmanMapItem;
import com.apitester.generator.processor.DtoAnalyzer;
import com.apitester.generator.processor.EndpointProcessor;
import com.apitester.generator.scanner.ControllerScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PostmanCollectionGeneratorTest {

    private PostmanCollectionGenerator generator;
    private EndpointProcessor endpointProcessor;

    @BeforeEach
    void setUp() {
        ApiTesterProperties properties = new ApiTesterProperties();
        properties.getCollection().setName("Test Collection");
        properties.getCollection().setBaseUrl("http://test.com");
        generator = new PostmanCollectionGenerator(properties);
        endpointProcessor = new EndpointProcessor(new DtoAnalyzer());
    }

    @Test
    @DisplayName("Should create collection with correct info")
    void generateCollectionInfo() {
        ControllerScanner.ControllerInfo controllerInfo = createControllerInfo(
                TestMemberController.class, "Member", new String[]{}, new Class<?>[]{});

        PostmanCollection collection = generator.generate(List.of(controllerInfo), endpointProcessor);

        assertNotNull(collection);
        assertEquals("Test Collection", collection.getInfo().getName());
        assertNotNull(collection.getItem());
        assertFalse(collection.getItem().isEmpty());
    }

    @Test
    @DisplayName("Should nest controller folder under parent folders")
    void generateWithParentFolders() {
        ControllerScanner.ControllerInfo controllerInfo = createControllerInfo(
                TestAdminController.class, "Admin",
                new String[]{"Admin", "Dashboard"}, new Class<?>[]{});

        PostmanCollection collection = generator.generate(List.of(controllerInfo), endpointProcessor);

        assertEquals(1, collection.getItem().size());

        PostmanItem adminFolder = collection.getItem().get(0);
        assertEquals("Admin", adminFolder.getName());
        assertNotNull(adminFolder.getItem());
        assertEquals(1, adminFolder.getItem().size());

        PostmanItem dashboardFolder = adminFolder.getItem().get(0);
        assertEquals("Dashboard", dashboardFolder.getName());
        assertNotNull(dashboardFolder.getItem());
        assertEquals(1, dashboardFolder.getItem().size());

        PostmanItem controllerFolder = dashboardFolder.getItem().get(0);
        assertEquals("Admin", controllerFolder.getName());
    }

    @Test
    @DisplayName("Should place controller directly in root when no parent folders")
    void generateWithoutParentFolders() {
        ControllerScanner.ControllerInfo controllerInfo = createControllerInfo(
                TestMemberController.class, "Member", new String[]{}, new Class<?>[]{});

        PostmanCollection collection = generator.generate(List.of(controllerInfo), endpointProcessor);

        assertFalse(collection.getItem().isEmpty());
        assertEquals("Member", collection.getItem().get(0).getName());
    }

    @Test
    @DisplayName("Each request item should have UUID id")
    void requestItemsHaveIds() {
        ControllerScanner.ControllerInfo controllerInfo = createControllerInfo(
                TestMemberController.class, "Member", new String[]{}, new Class<?>[]{});

        PostmanCollection collection = generator.generate(List.of(controllerInfo), endpointProcessor);
        PostmanItem folder = collection.getItem().get(0);

        assertNotNull(folder.getItem());
        for (PostmanItem item : folder.getItem()) {
            assertNotNull(item.getId());
            assertFalse(item.getId().isEmpty());
            assertNotNull(item.getRequest());
        }
    }

    @Test
    @DisplayName("Should merge controllers under same parent folder path")
    void mergeControllersUnderSameParent() {
        ControllerScanner.ControllerInfo info1 = createControllerInfo(
                TestAdminController.class, "Ctrl1",
                new String[]{"Admin", "Dashboard"}, new Class<?>[]{});
        ControllerScanner.ControllerInfo info2 = createControllerInfo(
                TestMemberController.class, "Ctrl2",
                new String[]{"Admin", "Dashboard"}, new Class<?>[]{});

        PostmanCollection collection = generator.generate(List.of(info1, info2), endpointProcessor);

        assertEquals(1, collection.getItem().size());
        assertEquals("Admin", collection.getItem().get(0).getName());
        assertEquals(1, collection.getItem().get(0).getItem().size());
        assertEquals("Dashboard", collection.getItem().get(0).getItem().get(0).getName());
        assertEquals(2, collection.getItem().get(0).getItem().get(0).getItem().size());
    }

    @Test
    @DisplayName("Should include global variables from properties in collection")
    void includeGlobalVariables() {
        ApiTesterProperties properties = new ApiTesterProperties();
        properties.getCollection().setName("Test Collection");
        properties.getCollection().setBaseUrl("http://test.com");
        properties.getGlobalVariables().add(PostmanMapItem.builder()
                .key("token")
                .value("abc123")
                .build());
        properties.getGlobalVariables().add(PostmanMapItem.builder()
                .key("apiKey")
                .value("xyz789")
                .build());

        generator = new PostmanCollectionGenerator(properties);

        ControllerScanner.ControllerInfo controllerInfo = createControllerInfo(
                TestMemberController.class, "Member", new String[]{}, new Class<?>[]{});

        PostmanCollection collection = generator.generate(List.of(controllerInfo), endpointProcessor);

        assertNotNull(collection.getVariable());
        assertEquals(3, collection.getVariable().size());

        assertTrue(collection.getVariable().stream()
                .anyMatch(v -> "baseUrl".equals(v.getKey()) && "http://test.com".equals(v.getValue())));
        assertTrue(collection.getVariable().stream()
                .anyMatch(v -> "token".equals(v.getKey()) && "abc123".equals(v.getValue())));
        assertTrue(collection.getVariable().stream()
                .anyMatch(v -> "apiKey".equals(v.getKey()) && "xyz789".equals(v.getValue())));
    }

    private ControllerScanner.ControllerInfo createControllerInfo(
            Class<?> controllerClass, String folderName, String[] parentFolders, Class<?>[] ignoreParams) {
        return new ControllerScanner.ControllerInfo(controllerClass, folderName, parentFolders,
                Collections.emptyMap(), ignoreParams);
    }
}
