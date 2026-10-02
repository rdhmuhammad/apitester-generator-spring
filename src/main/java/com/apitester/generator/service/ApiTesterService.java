package com.apitester.generator.service;

import com.apitester.generator.config.ApiTesterProperties;
import com.apitester.generator.dto.AuthDto;
import com.apitester.generator.dto.CollectionContentResponse;
import com.apitester.generator.dto.CollectionMetadata;
import com.apitester.generator.dto.DocsContent;
import com.apitester.generator.dto.EnvironmentDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class ApiTesterService {

    private static final String DEFAULT_SECRET = "default-secret-change-me";
    private static final Pattern BASE_URL_REGEX = Pattern.compile("(?i)(base.*url|url.*base)");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final ApiTesterProperties properties;
    private final ObjectMapper objectMapper;

    private String cachedCollectionId;
    private String selectedCollectionId;

    public File getCollectionFile() {
        return new File(properties.getCollection().getOutputPath());
    }

    public synchronized String getOrCreateCollectionId() {
        if (cachedCollectionId != null) {
            return cachedCollectionId;
        }

        File file = getCollectionFile();
        if (file.exists()) {
            try {
                byte[] bytes = Files.readAllBytes(file.toPath());
                String contentStr = new String(bytes, StandardCharsets.UTF_8);
                if (contentStr.startsWith("\uFEFF")) {
                    contentStr = contentStr.substring(1);
                }
                DocsContent docsContent = objectMapper.readValue(contentStr, DocsContent.class);
                if (docsContent.getInfo() != null && docsContent.getInfo().getPostmanId() != null
                        && !docsContent.getInfo().getPostmanId().isEmpty()) {
                    cachedCollectionId = docsContent.getInfo().getPostmanId();
                    if (selectedCollectionId == null) {
                        selectedCollectionId = cachedCollectionId;
                    }
                    return cachedCollectionId;
                }
            } catch (Exception e) {
                log.warn("Could not read _postman_id from collection file: {}", e.getMessage());
            }
        }

        cachedCollectionId = UUID.randomUUID().toString();
        if (selectedCollectionId == null) {
            selectedCollectionId = cachedCollectionId;
        }
        return cachedCollectionId;
    }

    public List<CollectionMetadata> listCollections() {
        File file = getCollectionFile();
        String id = getOrCreateCollectionId();
        String name = properties.getCollection().getName();

        if (file.exists()) {
            try {
                byte[] bytes = Files.readAllBytes(file.toPath());
                String contentStr = new String(bytes, StandardCharsets.UTF_8);
                if (contentStr.startsWith("\uFEFF")) {
                    contentStr = contentStr.substring(1);
                }
                DocsContent docsContent = objectMapper.readValue(contentStr, DocsContent.class);
                if (docsContent.getInfo() != null && docsContent.getInfo().getName() != null
                        && !docsContent.getInfo().getName().isEmpty()) {
                    name = docsContent.getInfo().getName();
                }
            } catch (Exception ignored) {
            }
        }

        String path = file.getAbsolutePath();
        String updatedAt = file.exists()
                ? ISO_FORMATTER.format(Instant.ofEpochMilli(file.lastModified()))
                : ISO_FORMATTER.format(Instant.now());
        String createdAt = updatedAt;
        boolean isSelected = id.equals(selectedCollectionId);

        CollectionMetadata meta = CollectionMetadata.builder()
                .id(id)
                .name(name)
                .isSelected(isSelected)
                .path(path)
                .updatedAt(updatedAt)
                .createdAt(createdAt)
                .build();

        return List.of(meta);
    }

    public CollectionMetadata getActiveCollection() {
        List<CollectionMetadata> all = listCollections();
        return all.stream()
                .filter(CollectionMetadata::isSelected)
                .findFirst()
                .orElse(all.isEmpty() ? null : all.get(0));
    }

    public CollectionMetadata selectCollection(String id) {
        String currentId = getOrCreateCollectionId();
        if (!currentId.equals(id)) {
            throw new IllegalArgumentException("Collection not found");
        }
        selectedCollectionId = id;
        return getActiveCollection();
    }

    public CollectionContentResponse readCollection(String id) throws IOException {
        String currentId = getOrCreateCollectionId();
        if (!currentId.equals(id)) {
            throw new IllegalArgumentException("Collection not found");
        }

        File file = getCollectionFile();
        if (!file.exists()) {
            throw new IllegalArgumentException("Collection not found");
        }
        log.info("Reading collection at {}", file.getAbsolutePath());

        byte[] bytes = Files.readAllBytes(file.toPath());
        String contentStr = new String(bytes, StandardCharsets.UTF_8);
        if (contentStr.startsWith("\uFEFF")) {
            contentStr = contentStr.substring(1);
        }
        log.trace("Read content: {}", contentStr);

        DocsContent docsContent = objectMapper.readValue(contentStr, DocsContent.class);
        normalizeCollection(docsContent);

        String updatedAt = ISO_FORMATTER.format(Instant.ofEpochMilli(file.lastModified()));
        return CollectionContentResponse.builder()
                .changed(false)
                .updatedAt(updatedAt)
                .content(docsContent)
                .build();
    }

    public CollectionContentResponse readSelectedCollection() throws IOException {
        CollectionMetadata active = getActiveCollection();
        if (active == null) {
            throw new IllegalArgumentException("No active collection");
        }
        return readCollection(active.getId());
    }

    public void writeSelectedCollection(DocsContent content) throws IOException {
        CollectionMetadata active = getActiveCollection();
        String targetId = (active != null) ? active.getId() : getOrCreateCollectionId();
        writeCollection(targetId, content);
    }

    public void writeCollection(String id, DocsContent content) throws IOException {
        String currentId = getOrCreateCollectionId();
        if (!currentId.equals(id)) {
            throw new IllegalArgumentException("Collection not found");
        }
        if (content == null) {
            throw new IllegalArgumentException("Collection content must not be empty");
        }

        if (content.getInfo() != null) {
            if (content.getInfo().getPostmanId() == null || content.getInfo().getPostmanId().isEmpty()) {
                content.getInfo().setPostmanId(id);
            }
            if (content.getInfo().getSchema() == null || content.getInfo().getSchema().isEmpty()) {
                content.getInfo().setSchema("https://schema.getpostman.com/json/collection/v2.1.0/collection.json");
            }
        }

        File file = getCollectionFile();
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        printContent("Write collection with value", content);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, content);
    }

    public void printContent(String message, Object content){
        try {
            String contentStr = objectMapper.writeValueAsString(content);
            log.trace("{}: {}", message, contentStr);
        } catch (Exception e) {
            log.error("Error while reading content: {}", e.getMessage());
        }
    }

    private void normalizeCollection(DocsContent content) {
        if (content == null) {
            return;
        }

        if (content.getVariable() != null) {
            for (DocsContent.CollectionVar var : content.getVariable()) {
                if (var.getKey() != null && BASE_URL_REGEX.matcher(var.getKey()).find()) {
                    if (var.getCategory() == null || var.getCategory().isEmpty()) {
                        var.setCategory("BASE_URL");
                    }
                }
                if (var.getId() == null || var.getId().isEmpty()) {
                    var.setId(UUID.randomUUID().toString());
                }
            }
        }

        if (content.getItem() != null) {
            injectIds(content.getItem());
        }
    }

    private void injectIds(List<DocsContent.CollectionItem> items) {
        if (items == null) {
            return;
        }
        for (DocsContent.CollectionItem item : items) {
            if (item.getId() == null || item.getId().isEmpty()) {
                item.setId(UUID.randomUUID().toString());
            }

            if (item.getRequest() != null) {
                DocsContent.Request req = item.getRequest();
                if (req.getHeader() != null) {
                    for (DocsContent.Header h : req.getHeader()) {
                        if (h.getId() == null || h.getId().isEmpty()) {
                            h.setId(UUID.randomUUID().toString());
                        }
                    }
                }
                if (req.getUrl() != null && req.getUrl().getQuery() != null) {
                    for (DocsContent.Property q : req.getUrl().getQuery()) {
                        if (q.getId() == null || q.getId().isEmpty()) {
                            q.setId(UUID.randomUUID().toString());
                        }
                    }
                }
                if (req.getBody() != null && req.getBody().getFormdata() != null) {
                    for (DocsContent.Property f : req.getBody().getFormdata()) {
                        if (f.getId() == null || f.getId().isEmpty()) {
                            f.setId(UUID.randomUUID().toString());
                        }
                    }
                }
                if (req.getAuth() != null && req.getAuth().getBearer() != null) {
                    for (DocsContent.Property b : req.getAuth().getBearer()) {
                        if (b.getId() == null || b.getId().isEmpty()) {
                            b.setId(UUID.randomUUID().toString());
                        }
                    }
                }
            }

            if (item.getResponse() != null) {
                for (DocsContent.CollectionResponse resp : item.getResponse()) {
                    if (resp.getId() == null || resp.getId().isEmpty()) {
                        resp.setId(UUID.randomUUID().toString());
                    }
                }
            }

            if (item.getItem() != null) {
                injectIds(item.getItem());
            }
        }
    }

    private File getEnvFile(String id) {
        String currentId = getOrCreateCollectionId();
        if (!currentId.equals(id)) {
            throw new IllegalArgumentException("Collection not found");
        }
        File collectionFile = getCollectionFile().getAbsoluteFile();
        File testsDir = new File(collectionFile.getParentFile(), "tests");
        return new File(testsDir, "http-client.private.env.json");
    }

    public List<EnvironmentDto.EnvironmentEntry> readEnvironments(String id) throws IOException {
        File envFile = getEnvFile(id);
        if (!envFile.exists()) {
            File parent = envFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            Files.write(envFile.toPath(), "{}".getBytes(StandardCharsets.UTF_8));
            return new ArrayList<>();
        }

        byte[] bytes = Files.readAllBytes(envFile.toPath());
        if (bytes.length == 0) {
            return new ArrayList<>();
        }

        JsonNode rootNode = objectMapper.readTree(bytes);
        List<EnvironmentDto.EnvironmentEntry> entries = new ArrayList<>();
        if (rootNode instanceof ObjectNode) {
            ObjectNode obj = (ObjectNode) rootNode;
            Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                Map<String, String> vars = new HashMap<>();
                if (field.getValue() instanceof ObjectNode) {
                    ObjectNode varObj = (ObjectNode) field.getValue();
                    Iterator<Map.Entry<String, JsonNode>> varFields = varObj.fields();
                    while (varFields.hasNext()) {
                        Map.Entry<String, JsonNode> varField = varFields.next();
                        vars.put(varField.getKey(), varField.getValue().asText());
                    }
                }
                entries.add(EnvironmentDto.EnvironmentEntry.builder()
                        .name(field.getKey())
                        .variables(vars)
                        .build());
            }
        }
        return entries;
    }

    public void writeEnvironments(String id, List<EnvironmentDto.EnvironmentEntry> entries) throws IOException {
        File envFile = getEnvFile(id);
        File parent = envFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        if (entries != null) {
            for (EnvironmentDto.EnvironmentEntry entry : entries) {
                map.put(entry.getName(), entry.getVariables());
            }
        }

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(envFile, map);
    }

    public AuthDto.LoginData login(AuthDto.LoginRequest req) {
        String expectedUsername = properties.getResolvedUsername();
        String expectedPassword = properties.getResolvedPassword();

        if (expectedUsername != null && !expectedUsername.isEmpty()) {
            if (!expectedUsername.equals(req.getUsername()) || !expectedPassword.equals(req.getPassword())) {
                throw new IllegalArgumentException("Invalid credentials");
            }
        }

        long durationSeconds = req.isRememberMe() ? Duration.ofDays(30).toSeconds() : Duration.ofDays(1).toSeconds();
        long expiresAt = Instant.now().getEpochSecond() + durationSeconds;
        String token = generateJwt(req.getUsername(), expiresAt);
        return AuthDto.LoginData.builder()
                .username(req.getUsername())
                .token(token)
                .expiresAt(expiresAt)
                .build();
    }

    public AuthDto.MeData me() {
        return AuthDto.MeData.builder()
                .username(properties.getResolvedUsername())
                .authenticated(true)
                .build();
    }

    private String generateJwt(String username, long expiresAt) {
        try {
            String header = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", username);
            claims.put("exp", expiresAt);
            claims.put("iat", Instant.now().getEpochSecond());
            String payload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(claims));

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(DEFAULT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal((header + "." + payload).getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(sig);

            return header + "." + payload + "." + signature;
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}
