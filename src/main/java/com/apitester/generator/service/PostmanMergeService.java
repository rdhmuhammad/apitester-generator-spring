package com.apitester.generator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.flipkart.zjsonpatch.JsonPatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class PostmanMergeService {

    private final ObjectMapper mapper;

    public PostmanMergeService() {
        this.mapper = new ObjectMapper();
    }

    public PostmanMergeService(ObjectMapper mapper) {
        this.mapper = mapper != null ? mapper : new ObjectMapper();
    }

    /**
     * Executes non-destructive three-way collection merge.
     *
     * @param base   Last generated state (Ancestor)
     * @param target Current user collection on disk/export (with user customizations)
     * @param update Current generator output (Spring controllers truth)
     * @return Merged collection JsonNode
     */
    public JsonNode executeThreeWayMerge(JsonNode base, JsonNode target, JsonNode update) {
        if (update == null || update.isNull() || update.isEmpty()) {
            return target != null ? target.deepCopy() : mapper.createObjectNode();
        }
        if (target == null || target.isNull() || target.isEmpty()) {
            return update.deepCopy();
        }
        if (base == null || base.isNull() || base.isEmpty()) {
            return executeTwoWayMerge(target, update);
        }

        // 1. Flatten request items to associative maps
        Map<String, JsonNode> flatBase = flattenRequests(base);
        Map<String, JsonNode> flatTarget = flattenRequests(target);
        Map<String, JsonNode> flatUpdate = flattenRequests(update);

        ObjectNode baseMap = toObjectNode(flatBase);
        ObjectNode targetMap = toObjectNode(flatTarget);
        ObjectNode updateMap = toObjectNode(flatUpdate);

        // 2. Compute RFC 6902 User Patch (Base -> Target)
        JsonNode rawUserPatch = JsonDiff.asJson(baseMap, targetMap);

        // 3. Filter patch based on Field-Level Ownership Rules
        ArrayNode filteredPatch = filterUserPatch(rawUserPatch);

        // 4. Apply filtered patch to Update state
        JsonNode mergedFlatNode;
        try {
            mergedFlatNode = JsonPatch.apply(filteredPatch, updateMap);
        } catch (Exception e) {
            log.warn("Patch apply failed, falling back to field-level merge: {}", e.getMessage());
            mergedFlatNode = manualMerge(flatBase, flatTarget, flatUpdate);
        }

        // 5. Deep merge request JSON body payloads
        deepMergeBodies(flatTarget, mergedFlatNode);

        // 6. Unflatten back into Postman hierarchical structure using update as template
        JsonNode mergedCollection = unflatten(update, mergedFlatNode, flatTarget, flatBase);

        // 7. Merge collection variables and info
        mergeVariables(base, target, update, mergedCollection);

        return mergedCollection;
    }

    /**
     * Fallback 2-way merge when no base state exists yet (e.g. user had an existing collection).
     */
    public JsonNode executeTwoWayMerge(JsonNode target, JsonNode update) {
        Map<String, JsonNode> flatTarget = flattenRequests(target);
        Map<String, JsonNode> flatUpdate = flattenRequests(update);

        ObjectNode mergedFlat = mapper.createObjectNode();
        for (Map.Entry<String, JsonNode> entry : flatUpdate.entrySet()) {
            String key = entry.getKey();
            JsonNode updateItem = entry.getValue().deepCopy();
            if (flatTarget.containsKey(key)) {
                JsonNode targetItem = flatTarget.get(key);
                applyUserOverrides(targetItem, updateItem);
            }
            mergedFlat.set(key, updateItem);
        }

        deepMergeBodies(flatTarget, mergedFlat);
        JsonNode mergedCollection = unflatten(update, mergedFlat, flatTarget, Collections.emptyMap());
        mergeVariables(null, target, update, mergedCollection);
        return mergedCollection;
    }

    /**
     * Filters RFC 6902 patch operations according to Field-Level Ownership Rules.
     */
    public ArrayNode filterUserPatch(JsonNode rawPatch) {
        ArrayNode filtered = mapper.createArrayNode();
        if (rawPatch == null || !rawPatch.isArray()) {
            return filtered;
        }

        for (JsonNode op : rawPatch) {
            String path = op.path("path").asText("");

            // Generator-Owned structural fields: drop user changes
            if (path.contains("/request/url/raw") ||
                path.contains("/request/url/path") ||
                path.contains("/request/url/host") ||
                path.contains("/request/url/port") ||
                path.contains("/request/method") ||
                path.endsWith("/id") ||
                path.contains("/funIden")) {
                continue;
            }

            // User-Owned fields: retain changes
            if (path.contains("/request/body") ||
                path.contains("/request/header") ||
                path.contains("/request/url/query") ||
                path.contains("/event") ||
                path.contains("/name") ||
                path.contains("/description") ||
                path.contains("/request/auth") ||
                op.path("op").asText("").equals("add")) {
                filtered.add(op);
            }
        }
        return filtered;
    }

    /**
     * Recursively extracts all request items into a map keyed by deterministic ID or METHOD:PATH.
     */
    public Map<String, JsonNode> flattenRequests(JsonNode root) {
        Map<String, JsonNode> map = new LinkedHashMap<>();
        if (root == null || root.isNull()) {
            return map;
        }
        ArrayNode items = (ArrayNode) root.path("item");
        if (items != null) {
            flattenItemsRecursive(items, map);
        }
        return map;
    }

    private void flattenItemsRecursive(ArrayNode items, Map<String, JsonNode> map) {
        for (JsonNode item : items) {
            if (item.has("item") && item.path("item").isArray()) {
                flattenItemsRecursive((ArrayNode) item.path("item"), map);
            } else if (item.has("request")) {
                String key = buildRequestKey(item);
                map.put(key, item.deepCopy());
            }
        }
    }

    public String buildRequestKey(JsonNode item) {
        // 1. funIden is the most specific and stable key (Controller#method)
        if (item.has("funIden") && !item.path("funIden").asText().isEmpty()) {
            return item.path("funIden").asText();
        }
        JsonNode req = item.path("request");
        if (req.has("funIden") && !req.path("funIden").asText().isEmpty()) {
            return req.path("funIden").asText();
        }
        // 2. Fallback to METHOD:PATH
        String method = req.path("method").asText("GET").toUpperCase();
        String path = req.path("url").path("raw").asText("");
        if (path.isEmpty() && req.path("url").path("path").isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode seg : req.path("url").path("path")) {
                sb.append("/").append(seg.asText());
            }
            path = sb.toString();
        }
        // Normalize path: strip query params and baseUrl prefix
        path = path.replaceAll("^\\{\\{baseUrl\\}\\}", "");
        if (path.contains("?")) {
            path = path.substring(0, path.indexOf("?"));
        }
        return method + ":" + path;
    }

    private ObjectNode toObjectNode(Map<String, JsonNode> map) {
        ObjectNode obj = mapper.createObjectNode();
        for (Map.Entry<String, JsonNode> entry : map.entrySet()) {
            obj.set(entry.getKey(), entry.getValue());
        }
        return obj;
    }

    private void deepMergeBodies(Map<String, JsonNode> flatTarget, JsonNode mergedFlatNode) {
        if (!mergedFlatNode.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = mergedFlatNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            JsonNode mergedItem = entry.getValue();

            if (flatTarget.containsKey(key)) {
                JsonNode targetItem = flatTarget.get(key);
                mergeRequestBodyJson(targetItem, mergedItem);
            }
        }
    }

    private void mergeRequestBodyJson(JsonNode targetItem, JsonNode mergedItem) {
        JsonNode targetBody = targetItem.path("request").path("body");
        JsonNode mergedBody = mergedItem.path("request").path("body");

        if (targetBody.path("mode").asText("").equals("raw") &&
            mergedBody.path("mode").asText("").equals("raw")) {
            String targetRaw = targetBody.path("raw").asText("");
            String mergedRaw = mergedBody.path("raw").asText("");

            if (!targetRaw.isEmpty() && !mergedRaw.isEmpty()) {
                try {
                    JsonNode targetJson = mapper.readTree(targetRaw);
                    JsonNode mergedJson = mapper.readTree(mergedRaw);

                    if (targetJson.isObject() && mergedJson.isObject()) {
                        ObjectNode combined = ((ObjectNode) mergedJson).deepCopy();
                        deepMergeJsonObjects((ObjectNode) targetJson, combined);
                        ((ObjectNode) mergedBody).put("raw", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(combined));
                    }
                } catch (Exception ignored) {
                    // If parsing fails (not valid JSON), preserve target's raw payload
                    ((ObjectNode) mergedBody).put("raw", targetRaw);
                }
            } else if (!targetRaw.isEmpty()) {
                ((ObjectNode) mergedBody).put("raw", targetRaw);
            }
        }
    }

    private void deepMergeJsonObjects(ObjectNode sourceUser, ObjectNode targetCombined) {
        Iterator<Map.Entry<String, JsonNode>> fields = sourceUser.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            JsonNode userVal = field.getValue();

            if (targetCombined.has(key)) {
                JsonNode updateVal = targetCombined.get(key);
                if (userVal.isObject() && updateVal.isObject()) {
                    deepMergeJsonObjects((ObjectNode) userVal, (ObjectNode) updateVal);
                } else {
                    targetCombined.set(key, userVal.deepCopy());
                }
            } else {
                targetCombined.set(key, userVal.deepCopy());
            }
        }
    }

    private void applyUserOverrides(JsonNode targetItem, JsonNode updateItem) {
        // Copy user body
        JsonNode targetBody = targetItem.path("request").path("body");
        if (!targetBody.isMissingNode()) {
            ((ObjectNode) updateItem.path("request")).set("body", targetBody.deepCopy());
        }
        // Copy user events / scripts
        JsonNode targetEvents = targetItem.path("event");
        if (!targetEvents.isMissingNode()) {
            ((ObjectNode) updateItem).set("event", targetEvents.deepCopy());
        }
        // Copy user auth
        JsonNode targetAuth = targetItem.path("request").path("auth");
        if (!targetAuth.isMissingNode()) {
            ((ObjectNode) updateItem.path("request")).set("auth", targetAuth.deepCopy());
        }
    }

    private JsonNode manualMerge(Map<String, JsonNode> base, Map<String, JsonNode> target, Map<String, JsonNode> update) {
        ObjectNode result = mapper.createObjectNode();
        for (Map.Entry<String, JsonNode> entry : update.entrySet()) {
            String key = entry.getKey();
            JsonNode updateItem = entry.getValue().deepCopy();
            if (target.containsKey(key)) {
                applyUserOverrides(target.get(key), updateItem);
            }
            result.set(key, updateItem);
        }
        return result;
    }

    /**
     * Reconstructs the collection hierarchy using update as template, inserting merged items.
     */
    private JsonNode unflatten(JsonNode updateTemplate, JsonNode mergedFlatNode, Map<String, JsonNode> flatTarget, Map<String, JsonNode> flatBase) {
        ObjectNode result = updateTemplate.deepCopy();
        ArrayNode rootItems = mapper.createArrayNode();

        ArrayNode templateItems = (ArrayNode) updateTemplate.path("item");
        if (templateItems != null) {
            for (JsonNode item : templateItems) {
                JsonNode rebuilt = rebuildItem(item, mergedFlatNode);
                if (rebuilt != null) {
                    rootItems.add(rebuilt);
                }
            }
        }

        // Add only standalone user-created requests from Target that were never part of Base
        Set<String> processedKeys = new HashSet<>();
        collectProcessedKeys(rootItems, processedKeys);

        ArrayNode customItems = mapper.createArrayNode();
        for (Map.Entry<String, JsonNode> entry : flatTarget.entrySet()) {
            boolean wasInBase = flatBase != null && flatBase.containsKey(entry.getKey());
            if (!processedKeys.contains(entry.getKey()) && !wasInBase) {
                customItems.add(entry.getValue());
            }
        }
        if (customItems.size() > 0) {
            ObjectNode customFolder = mapper.createObjectNode();
            customFolder.put("name", "Custom Requests");
            customFolder.set("item", customItems);
            rootItems.add(customFolder);
        }

        result.set("item", rootItems);
        return result;
    }

    private JsonNode rebuildItem(JsonNode item, JsonNode mergedFlatNode) {
        if (item.has("item") && item.path("item").isArray()) {
            // Folder node
            ObjectNode folder = item.deepCopy();
            ArrayNode subItems = mapper.createArrayNode();
            for (JsonNode child : item.path("item")) {
                JsonNode rebuiltChild = rebuildItem(child, mergedFlatNode);
                if (rebuiltChild != null) {
                    subItems.add(rebuiltChild);
                }
            }
            folder.set("item", subItems);
            return folder;
        } else if (item.has("request")) {
            // Request leaf node
            String key = buildRequestKey(item);
            if (mergedFlatNode.has(key)) {
                return mergedFlatNode.get(key).deepCopy();
            }
            return item.deepCopy();
        }
        return item.deepCopy();
    }

    private void collectProcessedKeys(ArrayNode items, Set<String> keys) {
        for (JsonNode item : items) {
            if (item.has("item") && item.path("item").isArray()) {
                collectProcessedKeys((ArrayNode) item.path("item"), keys);
            } else if (item.has("request")) {
                keys.add(buildRequestKey(item));
            }
        }
    }

    private void mergeVariables(JsonNode base, JsonNode target, JsonNode update, JsonNode result) {
        if (target == null || !target.has("variable") || !target.path("variable").isArray()) {
            return;
        }

        Map<String, JsonNode> targetVars = new LinkedHashMap<>();
        for (JsonNode v : target.path("variable")) {
            if (v.has("key")) {
                targetVars.put(v.path("key").asText(), v);
            }
        }

        ArrayNode mergedVars = mapper.createArrayNode();
        Set<String> addedKeys = new HashSet<>();

        // Start with update variables, overriding with target values where customized
        if (update != null && update.has("variable") && update.path("variable").isArray()) {
            for (JsonNode uVar : update.path("variable")) {
                String key = uVar.path("key").asText("");
                if (targetVars.containsKey(key)) {
                    ObjectNode vCopy = uVar.deepCopy();
                    vCopy.set("value", targetVars.get(key).path("value"));
                    mergedVars.add(vCopy);
                } else {
                    mergedVars.add(uVar.deepCopy());
                }
                addedKeys.add(key);
            }
        }

        // Add any extra variables defined by user
        for (Map.Entry<String, JsonNode> entry : targetVars.entrySet()) {
            if (!addedKeys.contains(entry.getKey())) {
                mergedVars.add(entry.getValue().deepCopy());
            }
        }

        ((ObjectNode) result).set("variable", mergedVars);
    }
}
