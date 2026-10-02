# Collection File Watcher Pattern

**Summary**: Implementation pattern for collection file loading, UTF-8 BOM sanitization, recursive node UUID assignment, and file-watcher synchronization.
**Sources**: `document/raw/decision/endpoint_spec.md`
**Last updated**: 2026-09-23.

---

## Architectural Context

The collection service acts as the bridge between filesystem storage and the frontend editor. It reads, normalizes, watches, and updates Postman JSON files on disk.

## Core Operations

### 1. Reading & Normalization (`GET /read/:id`)
1. **Lookup**: Retrieve the collection metadata record by database ID.
2. **Read File**: Read raw bytes from the target filesystem path.
3. **BOM Sanitization**: Check for and strip the UTF-8 Byte Order Mark (`\uFEFF`) from the byte stream.
4. **JSON Deserialization**: Parse into a strongly-typed collection DTO model (`DocsContent`, matching `internal/usecase/watch/dto.go`).
5. **Auto-Categorize Base URL**:
   Inspect all entries in `variable`. Any variable whose key matches `(?i)(base.*url|url.*base)` and has no custom category is categorized as `"BASE_URL"`.
6. **Recursive UUID Injection (`setId`)**:
   Recursively traverse every entity:
   - Root items and nested folder items
   - Request objects
   - Request headers
   - URL query parameters
   - Form-data payload entries
   If any item lacks an `id`, inject a newly generated UUID string.

### 2. File Writing & Watcher Sync (`PUT /write/:id`, `PUT /write-selected`)
1. **Strongly Typed Request Body**: The endpoint accepts `@RequestBody DocsContent content` rather than untyped `JsonNode` or raw strings.
2. **Validate**: Ensure request payload is valid and non-null.
3. **Resolve Path**: Lookup file location from the collection record.
4. **Persist to Disk**: Write formatted collection JSON with two-space indentation using `objectMapper.writerWithDefaultPrettyPrinter()`.
5. **Watcher Debouncing / State Update**:
   Update the in-memory file watcher cache with the newly written content and timestamp. This prevents the filesystem watcher from firing a false-positive "external file edit" event back to the frontend.

### 3. Collection Activation (`PUT /select/:id`)
1. Reset `is_selected = false` across all database records and flag target collection as `is_selected = true`.
2. Update the `SelectedCollection` pointer bucket.
3. Re-arm the filesystem watcher on the newly activated collection's file path (`watcher.Watch(path)`).

## Related pages

- [[concepts/postman-collection-schema]]
- [[decisions/endpoint-spec-and-architecture]]
- [[concepts/response-envelope]]
