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
    private String path = "/apitester";
    private String uiPath = "/apitester";
    private String username;
    private String password;
    private Auth auth = new Auth();
    private Collection collection = new Collection();
    private List<PostmanMapItem> globalVariables = new ArrayList<>();

    public String getResolvedUsername() {
        if (auth != null && auth.getUsername() != null && !auth.getUsername().isEmpty()) {
            return auth.getUsername();
        }
        if (username != null && !username.isEmpty()) {
            return username;
        }
        return "admin";
    }

    public String getResolvedPassword() {
        if (auth != null && auth.getPassword() != null && !auth.getPassword().isEmpty()) {
            return auth.getPassword();
        }
        if (password != null && !password.isEmpty()) {
            return password;
        }
        return "secret";
    }

    public String getResolvedUiPath() {
        if (uiPath != null && !uiPath.isEmpty() && !"/apitester".equals(uiPath)) {
            return normalizePath(uiPath);
        }
        if (path != null && !path.isEmpty()) {
            return normalizePath(path);
        }
        return "/apitester";
    }

    private String normalizePath(String p) {
        if (p == null || p.trim().isEmpty()) return "/apitester";
        String trimmed = p.trim();
        if (!trimmed.startsWith("/")) trimmed = "/" + trimmed;
        while (trimmed.endsWith("/") && trimmed.length() > 1) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    @Getter
    @Setter
    public static class Auth {
        private String username = "admin";
        private String password = "secret";
        private String jwtSecret = "default-secret-change-me";
    }

    @Getter
    @Setter
    public static class Collection {
        private boolean generate = true;
        private String name = "API Collection";
        private String description = "";
        private String baseUrl = "http://localhost:8080";
        private String outputPath = "postman_collection.json";
        private boolean preserveUserChanges = true;
        private String cachePath = ".apitester-cache.json";
    }
}
