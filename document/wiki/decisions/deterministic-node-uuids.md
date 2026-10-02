# Deterministic Node UUIDs

**Summary**: Technical decision adopting deterministic UUID generation from ControllerFileName and handler method name to eliminate UUID drift during three-way merging.
**Sources**: `document/raw/concept/Preserve user changes.md`, `document/raw/concept/Three Arrow Merging Slide.md`
**Last updated**: 2026-10-02.

---

## Problem Statement

During collection generation and [[concepts/three-way-collection-merge]], every request item and child entity (headers, query parameters, formdata, responses) requires an `id` attribute for frontend tree selection and Postman schema compliance.

Previously, `PostmanCollectionGenerator` generated random UUIDs using `UUID.randomUUID()` (UUIDv4) on every execution. In a three-way merge cycle, this caused severe issues:
1. **Artificial Diff Churn**: A second generation run without any controller code changes produces an `Update` collection where every single request has a different UUID than the `Base` collection (`uuid-1` $\rightarrow$ `uuid-2`). A JSON diff between Base and Update yields unnecessary replace operations for every `/id`.
2. **Identity Fragility**: If request matching relies on node `id`, matching fails completely because the IDs differ across generations.
3. **Client State Invalidation**: If the generator overwrites the user's active `Target` collection with new random IDs, client UI state in the frontend editor (such as open tabs, tree node selection, and test runner history) is destroyed.

## Evaluated Alternatives

1. **Random UUIDs with Diff Filtering**:
   Keep `UUID.randomUUID()` during generation, but strip or ignore `/id` fields during diff computation and patch application.
   - *Drawback*: Requires complex pre-normalization and post-merge ID copying; fails to guarantee consistent IDs across repeated standalone generations.
2. **Composite Key as ID (`METHOD:PATH`)**:
   Use the string `"POST:/api/v1/users"` directly as the `id`.
   - *Drawback*: Violates Postman collection v2.1 expectations for UUID-formatted identifiers; breaks if the user edits the HTTP method or path in the UI.
3. **Deterministic UUIDv3 / UUIDv5 from Controller Class + Method Name**:
   Generate name-based UUIDs by hashing the stable combination of the controller class name (or controller file name) and the handler method name (e.g., `UserController#getUser`).
   - *Chosen*: Guaranteed unique per endpoint, 100% deterministic across generator runs, compliant with standard UUID formats, and stable even if the user modifies request attributes (e.g. method or payload).

## Chosen Strategy

Adopt **deterministic UUID generation** seeded with the combination of **`ControllerFileName + methodName`** (or `ControllerClass#methodName`).

```java
String endpointSeed = controllerClassName + "#" + methodName;
UUID requestUuid = UUID.nameUUIDFromBytes(endpointSeed.getBytes(StandardCharsets.UTF_8));
```

### Hierarchical Seeding for Nested Elements

To ensure all child entities within a request also possess deterministic and unique UUIDs:
- **Request Headers**: Seed with `endpointSeed + ":header:" + headerKey`
- **Query Parameters**: Seed with `endpointSeed + ":query:" + paramKey`
- **Form Data**: Seed with `endpointSeed + ":formdata:" + formKey`
- **Responses**: Seed with `endpointSeed + ":response:" + statusCode`

### Controller Folders

Folder items representing controllers are seeded using the controller class/file name:
```java
UUID folderUuid = UUID.nameUUIDFromBytes(controllerClassName.getBytes(StandardCharsets.UTF_8));
```

## Impact on Three-Way Merge

1. **Zero ID Diff Churn**: When the code does not change, `Base.id == Update.id`. The diff engine (`zjsonpatch`) sees zero modifications on the `/id` paths.
2. **Stable Endpoint Correlation**: The generator output remains identical across runs, allowing the [[concepts/flatten-diff-merge-unflatten]] pipeline to reliably match requests even if a user manually modified other fields like the HTTP method or body payload.
3. **Client Session Continuity**: Frontend clients and test runners can rely on permanent node identifiers across multiple code-generation cycles.

## Related pages

- [[concepts/three-way-collection-merge]]
- [[concepts/flatten-diff-merge-unflatten]]
- [[concepts/postman-collection-schema]]
- [[decisions/deterministic-endpoint-keys]]
- [[decisions/field-level-ownership-rules]]
- [[patterns/generator/three-way-merge-service]]
