# Three-Way Merge Service Pattern

**Summary**: Implementation pattern in Spring Boot using Jackson and zjsonpatch to execute non-destructive 3-way Postman collection synchronization.
**Sources**: `document/raw/concept/Three Arrow Merging Slide.md`, `document/raw/concept/Preserve user changes.md`
**Last updated**: 2026-10-02.

---

## Architectural Context

The merge service implements the [[concepts/flatten-diff-merge-unflatten]] pattern to merge developer edits from the Postman UI with new controller definitions from Spring code. It avoids array index shift corruption by mapping endpoint items to composite keys ([[decisions/deterministic-endpoint-keys]]) or `funIden` ([[decisions/deterministic-node-uuids]]) before applying RFC 6902 patches.

## Key Dependencies

- `com.fasterxml.jackson.core:jackson-databind`: JSON tree manipulation via `JsonNode`, `ArrayNode`, and `ObjectNode`.
- `com.flipkart.zjsonpatch:zjsonpatch`: RFC 6902 JSON Diff and JSON Patch computation (`JsonDiff`, `JsonPatch`).

## Implementation Highlights

1. **Flattening**: Traverses hierarchical `"item"` arrays into a map keyed by `funIden` (e.g. `UserController#getUser`) or normalized `METHOD:PATH`.
2. **RFC 6902 User Diff**: Uses `JsonDiff.asJson(flatBase, flatTarget)` to capture all user changes made since the last generation.
3. **Field-Level Ownership Filtering**:
   - Drops structural generator-owned mutations: `/request/url/raw`, `/request/method`, `/id`, `/funIden`.
   - Retains user-owned customizations: `/request/body`, `/request/header`, `/request/url/query`, `/event` (scripts), `/request/auth`.
4. **Deep Request Body Merging**:
   - Parses JSON payloads in both Target and Update.
   - Preserves user custom values for existing fields while injecting newly declared fields from Spring DTOs with default placeholder values.
5. **Template-Driven Unflattening**:
   - Reconstructs collection folders using the freshly generated `Update` structure as the master template.
   - Replaces request nodes with merged items.
   - Deletes endpoints that were present in `Base` but removed in `Update` (Option A removal).
   - Preserves standalone custom requests created directly by the user in `Target`.
6. **Collection Variable Merging**: Preserves user overrides and newly defined collection variables while retaining base URL defaults.

## Reference Service

See [`PostmanMergeService.java`](file:///d:/personal/apitester-generator-spring/src/main/java/com/apitester/generator/service/PostmanMergeService.java) for the complete implementation.

## Related pages

- [[concepts/three-way-collection-merge]]
- [[concepts/flatten-diff-merge-unflatten]]
- [[decisions/field-level-ownership-rules]]
- [[decisions/deterministic-endpoint-keys]]
- [[decisions/deterministic-node-uuids]]
