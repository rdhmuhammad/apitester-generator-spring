package com.apitester.generator.config;

import com.apitester.generator.model.PostmanMapItem;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "apitester")
public class ApiTesterProperties {

    private boolean enabled = true;
    private String basePackage = "";
    private String parentFolder = "";
    private String format = "json";
    private String includeHeaders = "";
    private int timeout = 5000;
    private boolean generateEnvFile = true;
    private Collection collection = new Collection();
    private List<PostmanMapItem> globalVariables = new ArrayList<>();

    @Getter
    @Setter
    public static class Collection {
        private String name = "API Collection";
        private String description = "";
        private String baseUrl = "http://localhost:8080";
        private String outputPath = "postman_collection.json";
    }
}
