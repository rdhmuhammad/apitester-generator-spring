package com.apitester.generator.generator;

import com.apitester.generator.config.ApiTesterProperties;
import com.apitester.generator.example.TestAdminController;
import com.apitester.generator.example.TestMemberController;
import com.apitester.generator.model.PostmanCollection;
import com.apitester.generator.model.PostmanItem;
import com.apitester.generator.processor.DtoAnalyzer;
import com.apitester.generator.processor.EndpointProcessor;
import com.apitester.generator.scanner.ControllerScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicUuidTest {

    private PostmanCollectionGenerator generator;
    private EndpointProcessor endpointProcessor;

    @BeforeEach
    void setUp() {
        ApiTesterProperties properties = new ApiTesterProperties();
        properties.getCollection().setName("Deterministic Test Collection");
        properties.getCollection().setBaseUrl("http://localhost:8080");
        generator = new PostmanCollectionGenerator(properties);
        endpointProcessor = new EndpointProcessor(new DtoAnalyzer());
    }

    @Test
    @DisplayName("Multiple generations should produce identical deterministic UUIDs")
    void testDeterministicUuidsAcrossGenerations() {
        ControllerScanner.ControllerInfo adminInfo = new ControllerScanner.ControllerInfo(
                TestAdminController.class, "Admin", new String[]{"Core"},
                Collections.emptyMap(), new Class<?>[]{});

        ControllerScanner.ControllerInfo memberInfo = new ControllerScanner.ControllerInfo(
                TestMemberController.class, "Member", new String[]{"Core"},
                Collections.emptyMap(), new Class<?>[]{});

        List<ControllerScanner.ControllerInfo> controllers = List.of(adminInfo, memberInfo);

        PostmanCollection run1 = generator.generate(controllers, endpointProcessor);
        PostmanCollection run2 = generator.generate(controllers, endpointProcessor);

        // 1. Collection ID must match
        assertEquals(run1.getInfo().get_postman_id(), run2.getInfo().get_postman_id());
        assertFalse(run1.getInfo().get_postman_id().isEmpty());

        // 2. Folder IDs must match
        assertEquals(run1.getItem().size(), run2.getItem().size());
        PostmanItem coreFolder1 = run1.getItem().get(0);
        PostmanItem coreFolder2 = run2.getItem().get(0);
        assertEquals(coreFolder1.getId(), coreFolder2.getId());

        // 3. Child request items must have identical IDs and funIden
        assertEquals(coreFolder1.getItem().size(), coreFolder2.getItem().size());
        for (int i = 0; i < coreFolder1.getItem().size(); i++) {
            PostmanItem ctrl1 = coreFolder1.getItem().get(i);
            PostmanItem ctrl2 = coreFolder2.getItem().get(i);
            assertEquals(ctrl1.getId(), ctrl2.getId());
            assertEquals(ctrl1.getFunIden(), ctrl2.getFunIden());

            for (int j = 0; j < ctrl1.getItem().size(); j++) {
                PostmanItem req1 = ctrl1.getItem().get(j);
                PostmanItem req2 = ctrl2.getItem().get(j);
                assertEquals(req1.getId(), req2.getId());
                assertEquals(req1.getFunIden(), req2.getFunIden());
                assertNotNull(req1.getId());
                assertFalse(req1.getId().isEmpty());

                // Response IDs must match
                if (req1.getResponse() != null && !req1.getResponse().isEmpty()) {
                    assertEquals(req1.getResponse().get(0).getId(), req2.getResponse().get(0).getId());
                }
            }
        }
    }
}
