package com.apitester.generator.service;

import com.apitester.generator.config.ApiTesterProperties;
import com.apitester.generator.dto.AuthDto;
import com.apitester.generator.dto.CollectionContentResponse;
import com.apitester.generator.dto.CollectionMetadata;
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
                JsonNode root = objectMapper.readTree(file);
                JsonNode postmanId = root.path("info").path("_postman_id");
                if (!postmanId.isMissingNode() && !postmanId.asText().isEmpty()) {
                    cachedCollectionId = postmanId.asText();
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
                JsonNode root = objectMapper.readTree(file);
                JsonNode nameNode = root.path("info").path("name");
                if (!nameNode.isMissingNode() && !nameNode.asText().isEmpty()) {
                    name = nameNode.asText();
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

        byte[] bytes = Files.readAllBytes(file.toPath());
        String contentStr = new String(bytes, StandardCharsets.UTF_8);
        if (contentStr.startsWith("\uFEFF")) {
            contentStr = contentStr.substring(1);
        }

        JsonNode rootNode = objectMapper.readTree(contentStr);
        if (rootNode instanceof ObjectNode) {
            normalizeCollection((ObjectNode) rootNode);
        }

        String updatedAt = ISO_FORMATTER.format(Instant.ofEpochMilli(file.lastModified()));
        return CollectionContentResponse.builder()
                .changed(false)
                .updatedAt(updatedAt)
                .content(rootNode)
                .build();
    }

    public CollectionContentResponse readSelectedCollection() throws IOException {
        CollectionMetadata active = getActiveCollection();
        if (active == null) {
            throw new IllegalArgumentException("No active collection");
        }
        return readCollection(active.getId());
    }

    public void writeSelectedCollection(String rawJson) throws IOException {
        CollectionMetadata active = getActiveCollection();
        String targetId = (active != null) ? active.getId() : getOrCreateCollectionId();
        writeCollection(targetId, rawJson);
    }

    public void writeCollection(String id, String rawJson) throws IOException {
        String currentId = getOrCreateCollectionId();
        if (!currentId.equals(id)) {
            throw new IllegalArgumentException("Collection not found");
        }
        if (rawJson == null || rawJson.trim().isEmpty()) {
            throw new IllegalArgumentException("Collection content must not be empty");
        }

        File file = getCollectionFile();
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        Files.write(file.toPath(), rawJson.getBytes(StandardCharsets.UTF_8));
    }

    private void normalizeCollection(ObjectNode rootNode) {
        JsonNode variableNode = rootNode.get("variable");
        if (variableNode instanceof ArrayNode) {
            for (JsonNode item : variableNode) {
                if (item instanceof ObjectNode) {
                    ObjectNode obj = (ObjectNode) item;
                    JsonNode keyNode = obj.get("key");
                    if (keyNode != null && BASE_URL_REGEX.matcher(keyNode.asText()).find()) {
                        JsonNode catNode = obj.get("category");
                        if (catNode == null || catNode.asText().isEmpty()) {
                            obj.put("category", "BASE_URL");
                        }
                    }
                    JsonNode idNode = obj.get("id");
                    if (idNode == null || idNode.asText().isEmpty()) {
                        obj.put("id", UUID.randomUUID().toString());
                    }
                }
            }
        }

        JsonNode itemNode = rootNode.get("item");
        if (itemNode instanceof ArrayNode) {
            injectIds((ArrayNode) itemNode);
        }
    }

    private void injectIds(ArrayNode items) {
        for (JsonNode node : items) {
            if (node instanceof ObjectNode) {
                ObjectNode itemObj = (ObjectNode) node;
                JsonNode idNode = itemObj.get("id");
                if (idNode == null || idNode.asText().isEmpty()) {
                    itemObj.put("id", UUID.randomUUID().toString());
                }

                JsonNode reqNode = itemObj.get("request");
                if (reqNode instanceof ObjectNode) {
                    ObjectNode reqObj = (ObjectNode) reqNode;
                    injectListIds(reqObj, "header");

                    JsonNode urlNode = reqObj.get("url");
                    if (urlNode instanceof ObjectNode) {
                        injectListIds((ObjectNode) urlNode, "query");
                    }

                    JsonNode bodyNode = reqObj.get("body");
                    if (bodyNode instanceof ObjectNode) {
                        injectListIds((ObjectNode) bodyNode, "formdata");
                    }
                }

                JsonNode subItems = itemObj.get("item");
                if (subItems instanceof ArrayNode) {
                    injectIds((ArrayNode) subItems);
                }
            }
        }
    }

    private void injectListIds(ObjectNode parent, String fieldName) {
        JsonNode listNode = parent.get(fieldName);
        if (listNode instanceof ArrayNode) {
            for (JsonNode elem : listNode) {
                if (elem instanceof ObjectNode) {
                    ObjectNode elemObj = (ObjectNode) elem;
                    JsonNode idNode = elemObj.get("id");
                    if (idNode == null || idNode.asText().isEmpty()) {
                        elemObj.put("id", UUID.randomUUID().toString());
                    }
                }
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
