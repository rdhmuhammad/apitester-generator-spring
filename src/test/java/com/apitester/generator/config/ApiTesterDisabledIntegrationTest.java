package com.apitester.generator.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ApiTesterDisabledIntegrationTest.DisabledApp.class)
@AutoConfigureMockMvc
class ApiTesterDisabledIntegrationTest {

    @SpringBootApplication
    static class DisabledApp {
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("When @EnableApiTester is not present, /api/v1/collection/list should return 404")
    void testEndpointsDisabledWithoutAnnotation() throws Exception {
        mockMvc.perform(get("/api/v1/collection/list"))
                .andExpect(status().isNotFound());
    }
}
