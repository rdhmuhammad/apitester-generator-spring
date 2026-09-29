# Three-Way Merge Service Pattern

**Summary**: Implementation pattern in Spring Boot using Jackson and zjsonpatch to execute non-destructive 3-way Postman collection synchronization.
**Sources**: `document/raw/concept/Three Arrow Merging Slide.md`, `document/raw/concept/Preserve user changes.md`
**Last updated**: 2026-09-23.

---

## Architectural Context

The merge service implements the [[concepts/flatten-diff-merge-unflatten]] pattern to merge developer edits from the Postman UI with new controller definitions from Spring code. It avoids array index shift corruption by mapping endpoint items to composite keys ([[decisions/deterministic-endpoint-keys]]) before applying RFC 6902 patches.

## Key Dependencies

- `com.fasterxml.jackson.core:jackson-databind`: JSON tree manipulation via `JsonNode` and `ObjectNode`.
- `com.flipkart.zjsonpatch:zjsonpatch`: RFC 6902 JSON Diff and JSON Patch computation (`JsonDiff`, `JsonPatch`).

## Reference Implementation

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.flipkart.zjsonpatch.JsonPatch;
import org.springframework.stereotype.Service;

import java.util.Iterator;

@Service
public class PostmanMergeService {

    private final ObjectMapper mapper = new ObjectMapper();

    public JsonNode executeThreeWayMerge(JsonNode base, JsonNode target, JsonNode update) {
        // 1. Flatten items array into maps keyed by HTTP_METHOD:URL_PATH
        JsonNode flatBase = flattenItemsToMap(base);
        JsonNode flatTarget = flattenItemsToMap(target);
        JsonNode flatUpdate = flattenItemsToMap(update);

        // 2. Generate the RFC 6902 user patch (Base -> Target)
        JsonNode rawUserPatch = JsonDiff.asJson(flatBase, flatTarget);

        // 3. Filter patch: keep user-owned fields (body, values), discard structural mutations
        JsonNode filteredPatch = filterUserPatch(rawUserPatch);

        // 4. Apply filtered patch on top of freshly generated controllers
        JsonNode mergedFlatNode = JsonPatch.apply(filteredPatch, flatUpdate);

        // 5. Convert back into standard Postman hierarchical "item" array
        return unflattenMapToItems(mergedFlatNode, update);
    }

    private JsonNode filterUserPatch(JsonNode patchArray) {
        ArrayNode filtered = mapper.createArrayNode();
        for (JsonNode op : patchArray) {
            String path = op.path("path").asText();

            // Generator owns URL paths, HTTP methods, and query parameter names
            if (path.contains("/request/url/raw") || path.contains("/request/method")) {
                continue;
            }

            // User owns body payloads, parameter values, auth tokens, scripts
            if (path.contains("/request/body/raw") || 
                path.contains("/value") || 
                path.contains("/script/")) {
                filtered.add(op);
            }
        }
        return filtered;
    }

    private JsonNode flattenItemsToMap(JsonNode collectionRoot) {
        ObjectNode flattened = mapper.createObjectNode();
        ArrayNode items = (ArrayNode) collectionRoot.path("item");
        if (items != null) {
            flattenItemsRecursive(items, flattened);
        }
        return flattened;
    }

    private void flattenItemsRecursive(ArrayNode items, ObjectNode targetMap) {
        for (JsonNode item : items) {
            if (item.has("item")) {
                // Folder node - recurse
                flattenItemsRecursive((ArrayNode) item.path("item"), targetMap);
            } else if (item.has("request")) {
                // Request leaf node - key by METHOD:PATH
                String method = item.path("request").path("method").asText("GET");
                String url = item.path("request").path("url").path("raw").asText();
                String key = method + ":" + url;
                targetMap.set(key, item);
            }
        }
    }

    private JsonNode unflattenMapToItems(JsonNode mergedFlatNode, JsonNode template) {
        ObjectNode result = template.deepCopy();
        ArrayNode itemsArray = mapper.createArrayNode();
        Iterator<JsonNode> elements = mergedFlatNode.elements();
        while (elements.hasNext()) {
            itemsArray.add(elements.next());
        }
        result.set("item", itemsArray);
        return result;
    }
}
```

## Related pages

- [[concepts/three-way-collection-merge]]
- [[concepts/flatten-diff-merge-unflatten]]
- [[decisions/field-level-ownership-rules]]
- [[decisions/deterministic-endpoint-keys]]
