# Postman Collection Schema

**Summary**: Specifications for Postman Collection v2.1 format, recursive node hierarchies, unique UUID injection for UI navigation, and variable classification rules.
**Sources**: `document/raw/decision/endpoint_spec.md`, `document/raw/concept/Three Arrow Merging Slide.md`
**Last updated**: 2026-09-23.

---

## Schema Overview

The system interacts with collections compliant with the Postman Collection Format v2.1 (`https://schema.getpostman.com/json/collection/v2.1.0/collection.json`). The root JSON object contains four primary sections:

1. **`info`**: Metadata including `_postman_id`, `name`, `description`, and the `schema` URI.
2. **`item`**: A recursive array containing folders and request definitions.
3. **`variable`**: Collection-level variables (e.g., target host URLs).
4. **`auth`**: Top-level authentication configuration (e.g., bearer tokens).

## Recursive Node Hierarchy

Postman collections are structured as a tree. Elements inside `"item"` can be either:
- **Folders**: Nodes containing further nested `"item"` arrays.
- **Requests**: Leaf nodes containing `request` (method, headers, url, body) and recorded `response` snapshots.

## Recursive UUID Injection (`setId`)

A key requirement when loading collections for the frontend editor is ensuring every node has a stable, unique identifier. When parsing collection files from disk:
- The system recursively traverses every `item`, sub-folder, `request`, `header`, `query` parameter, and `formdata` entry.
- If an entity lacks an `id`, a UUID is generated and populated.
- This allows the frontend tree views and editors to select and manipulate items without key collisions.

## Variable Classification (`BASE_URL`)

Collection variables define reusable values. When reading a collection:
- Each variable key is checked against the case-insensitive regular expression:
  ```regex
  (?i)(base.*url|url.*base)
  ```
- If a match is found and the variable has no predefined custom category, its `category` property is automatically assigned to `"BASE_URL"`.
- This enables frontend clients to present target environment switchers automatically.

## UTF-8 Byte Order Mark (BOM)

Collection files exported by various operating systems or IDEs may include a UTF-8 BOM (`\uFEFF`). The collection parser must strip this preamble prior to JSON deserialization to avoid parsing errors.

## Related pages

- [[patterns/collection/collection-file-watcher]]
- [[decisions/endpoint-spec-and-architecture]]
- [[concepts/three-way-collection-merge]]
- [[decisions/deterministic-endpoint-keys]]
- [[decisions/deterministic-node-uuids]]
