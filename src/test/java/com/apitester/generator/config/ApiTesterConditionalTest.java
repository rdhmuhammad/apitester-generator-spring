package com.apitester.generator.config;

import com.apitester.generator.annotation.EnableApiTester;
import com.apitester.generator.controller.ApiTesterAuthController;
import com.apitester.generator.controller.ApiTesterCollectionController;
import com.apitester.generator.service.ApiTesterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ApiTesterConditionalTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ApiTesterAutoConfiguration.class));

    @Configuration
    @EnableApiTester
    static class EnabledConfig {
    }

    @Configuration
    static class DisabledConfig {
    }

    @Test
    @DisplayName("Should create ApiTester controllers and service when @EnableApiTester is present")
    void shouldCreateBeansWhenEnabled() {
        contextRunner.withUserConfiguration(EnabledConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ApiTesterService.class);
                    assertThat(context).hasSingleBean(ApiTesterCollectionController.class);
                    assertThat(context).hasSingleBean(ApiTesterAuthController.class);
                });
    }

    @Test
    @DisplayName("Should NOT create ApiTester controllers and service when @EnableApiTester is absent")
    void shouldNotCreateBeansWhenDisabled() {
        contextRunner.withUserConfiguration(DisabledConfig.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ApiTesterService.class);
                    assertThat(context).doesNotHaveBean(ApiTesterCollectionController.class);
                    assertThat(context).doesNotHaveBean(ApiTesterAuthController.class);
                });
    }
}
