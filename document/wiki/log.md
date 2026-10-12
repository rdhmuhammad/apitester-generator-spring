# Wiki Log

**Summary**: Append-only record of all wiki operations, document ingestions, and structural changes.
**Sources**: `document/raw/concept/Preserve user changes.md`, `document/raw/concept/Three Arrow Merging Slide.md`, `document/raw/decision/endpoint_spec.md`
**Last updated**: 2026-10-12.

## 2026-10-12 - Documentation: Synchronize README.md with Embedded Web UI and Three-Way Merge

**Context**:
- Project purpose and technology drifted from a standalone startup Postman exporter to a full-featured in-app API testing toolkit with an embedded React Web UI and non-destructive three-way merge engine.
- External frontend web UI repository was created and needed explicit linking in the documentation.

**Changes Made**:
- Updated `README.md` to document the embedded interactive Web UI hosted at `/apitester`, default credentials (`admin`/`secret`), and link to frontend source code at `https://github.com/rdhmuhammad/apitester`.
- Documented non-destructive Three-Way Collection Merge powered by RFC 6902 JSON patch (`zjsonpatch`) and deterministic UUIDs preserving user edits across restarts.
- Corrected JitPack dependency coordinates and clone URL from `anomalyco` to `rdhmuhammad`.
- Corrected runtime prerequisites to Java 11+ (aligned with `sourceCompatibility = '11'`).
- Synchronized configuration properties table with `ApiTesterProperties`: added `ui-path`, `auth.username`, `auth.password`, `collection.generate`, `collection.preserve-user-changes`, `collection.cache-path`, and purged dead properties (`base-package`, `format`, `timeout`).
- Updated usage cases and example project instructions to describe decoupled generation and Web UI verification.

## 2026-10-03 - Refactor: Codebase Simplification and Dead Code Elimination (Ponytail Audit)

**Context**:
- Executed whole-repo audit to eliminate unused dependencies, dead code, speculative abstractions, redundant boilerplate, and orphaned collection files.

**Changes Made**:
- Removed unused and duplicate dependencies: `com.github.loki4j:loki-logback-appender`, `org.modelmapper:modelmapper` (duplicated in root and example `build.gradle`), `org.json:json`, and redundant `spring-boot-starter` / explicit `jackson-databind`.
- Deleted orphaned root and duplicate files: `ASLI LOYAL.postman_collection-2026.json`, `example/postman_collection.json`, `build.gradle.example`, `tests/http-client.private.env.json`.
- Deleted duplicate unreferenced test controllers in root test package (`TestAdminController.java`, `TestMemberController.java`).
- Inlined single-caller utility `ValueUtils.java` into `EndpointProcessor` and removed `ValueUtils.java`.
- Removed dead uncalled `buildEndpointName` method and replaced HashMap allocation per annotation in `EndpointProcessor` with a concise switch.
- Simplified `DtoAnalyzer` naming strategy conversion by eliminating duplicate regex pattern and extracting switch.
- Streamlined `DocsContent`: deleted unreferenced `CreateCollectionRequest` and `UpdateCollectionRequest`, eliminated manual getter/setter boilerplate on `@Getter`/`@Setter` annotated records.
- Cleaned dead unread configuration properties in `ApiTesterProperties` (`basePackage`, `parentFolder`, `format`, `includeHeaders`, `timeout`, `generateEnvFile`, `jwtSecret`).
- De-duplicated `PathResourceResolver` anonymous registration in `ApiTesterAutoConfiguration` and simplified proxy class unwrapping using Spring's `ClassUtils.getUserClass`.
- Consolidated UTF-8 BOM-stripping file reading logic into reusable `Files.readString` helper methods across `ApiTesterService` and `StartupGeneratorListener`.
- Replaced `Optional` and `Stream` ceremony on non-null annotation arrays in `ControllerScanner` with direct `for` loop.

## 2026-10-02 - Feature: Preserve User Changes via Three-Way Collection Merge

**Context**:
- When developers customized request bodies, headers, scripts, or variables in Postman or ApiTester UI, subsequent generation runs destroyed those additions.
- Implemented non-destructive Three-Way Collection Merge reconciling Base (shadow cache), Target (user collection), and Update (Spring controllers).

**Changes Made**:
- Added `com.flipkart.zjsonpatch:zjsonpatch:0.4.14` dependency in `build.gradle`.
- Added `preserveUserChanges` (default `true`) and `cachePath` (default `.apitester-cache.json`) in `ApiTesterProperties`.
- Updated `.gitignore` to ignore `.apitester-cache.json` and `*.generator-cache.json`.
- Implemented deterministic UUID generation in `PostmanCollectionGenerator` using `ControllerFileName + methodName` seeds and populated `funIden`.
- Implemented `PostmanMergeService` executing Flatten-Diff-Merge-Unflatten with field-level ownership filtering, deep JSON body merging, and deleted endpoint pruning (Option A).
- Integrated Three-Way Merge lifecycle into `StartupGeneratorListener` and wired `PostmanMergeService` in `ApiTesterAutoConfiguration`.
- Added unit and integration test suite (`DeterministicUuidTest`, `PostmanMergeServiceTest`, `StartupGeneratorListenerIntegrationTest`).
- Updated pattern documentation in `patterns/generator/three-way-merge-service.md`.

## 2026-10-02 - Decision: Deterministic Node UUIDs for Stable Three-Way Merge

**Context**:
- Using `UUID.randomUUID()` during collection generation introduced artificial ID diff churn between `Base` and `Update` states during three-way merging.
- Generated IDs changed across runs even when underlying controller methods were identical, risking matching failures, unnecessary diff operations, and invalidation of client UI state (editor tabs, selection, test runner history).

**Changes Made**:
- Recorded technical decision in `document/wiki/decisions/deterministic-node-uuids.md`.
- Specified deterministic UUID generation based on `ControllerFileName + methodName` (e.g. `UserController#getUserProfile`) using name-based hashing (UUIDv3 / UUIDv5).
- Defined hierarchical seeding for nested child nodes (`headers`, `queryParams`, `formData`, `responses`).
- Updated `document/wiki/index.md` to link and describe the new decision.

## 2026-09-30 - Refactor: Strongly Typed DocsContent DTO for Collection Read/Write

**Context**:
- Collection content in `CollectionContentResponse` previously used untyped `JsonNode`, and `ApiTesterCollectionController` write endpoints accepted untyped strings.
- Replaced with strongly-typed `DocsContent` DTO modeling `D:\personal\apitester\internal\usecase\watch\dto.go`.

**Changes Made**:
- Created `com.apitester.generator.dto.DocsContent` replicating Go's `DocsContent` hierarchy (`CollectionInfo`, `CollectionItem`, `CollectionResponse`, `ResponseCookie`, `Request`, `ReqAuth`, `Header`, `RequestBody`, `RequestUrl`, `CollectionAuth`, `Property`, `CollectionEvent`, `EventScript`, `CollectionVar`).
- Refactored `CollectionContentResponse` field `content` from `JsonNode` to `DocsContent`.
- Updated `ApiTesterCollectionController` write endpoints (`PUT /write` and `PUT /write/{id}`) to accept `@RequestBody DocsContent content`.
- Updated `ApiTesterService` to read, normalize (`setId` UUID injection, `BASE_URL` classification), and write using strongly-typed `DocsContent` instances.
- Updated `patterns/collection/collection-file-watcher.md` to reflect `DocsContent` usage.
- Added comprehensive integration tests in `ApiTesterControllerIntegrationTest` verifying end-to-end read and write with structured `DocsContent` DTO.

## 2026-09-30 - Architecture: Isolate ApiTester Controller Prefix to /apitester/api/v1

**Context**:
- When embedded into host Spring Boot microservices, ApiTester controller mappings at `/api/v1/...` collided with host application endpoints (e.g. host `/api/v1/auth/login`).

**Changes Made**:
- Updated `ApiTesterAuthController` `@RequestMapping` to exclusively map to `/apitester/api/v1/auth`.
- Updated `ApiTesterCollectionController` `@RequestMapping` to exclusively map to `/apitester/api/v1/collection`.
- Removed `/api/v1/**` from `apiTesterCorsFilter` registration in `ApiTesterAutoConfiguration`, retaining CORS scoping exclusively on `/apitester/**` to prevent polluting host application CORS policies.
- Updated `ApiTesterControllerIntegrationTest` and `ApiTesterDisabledIntegrationTest` to verify all endpoints are accessed via `/apitester/api/v1/...` and asserted that `/api/v1/...` routes return 404.
- Updated wiki pattern `patterns/ui/frontend-static-resource-embedding.md` to document the exclusive prefixing.

## 2026-09-29 - Pattern: Frontend Static Resource Embedding

**Sources Ingested**:
- `document/raw/decision/endpoint_spec.md`
- Frontend build assets from `d:/personal/apitester/frontend/dist`

**Changes Made**:
- Placed frontend production build in `src/main/resources/META-INF/resources/apitester/`.
- Implemented `WebMvcConfigurer` in `ApiTesterAutoConfiguration.java` with `PathResourceResolver` for SPA client-side history route fallback to `index.html`.
- Added trailing-slash redirect from `/apitester` to `/apitester/` to preserve relative script/style asset resolution.
- Updated `ApiTesterCollectionController` and `ApiTesterAuthController` to support both `/api/v1/...` and `/apitester/api/v1/...` route prefixes, plus added `/read` and `/write` aliases for the frontend.
- Added `ApiTesterUiController` to serve dynamic `/.env` and `/apitester/.env` for runtime `VITE_API_URL=/apitester/api/v1` discovery.
- Expanded CORS configuration to cover `/apitester/**`.
- Added configurable credentials (`username`, `password`) and `uiPath` in `ApiTesterProperties`.
- Added `apitester.collection.generate` property (default: `true`) and `@ConditionalOnProperty` on `StartupGeneratorListener` to allow users to disable collection file generation and solely run the embedded web UI.
- Permitted `/apitester/**` and `/api/v1/**` in example `SecurityConfig`.
- Created Pattern Page:
  - `patterns/ui/frontend-static-resource-embedding.md`: Full architectural reference for packaging, SPA history routing fallback, and `.env` synchronization.
- Updated `index.md`: Added UI patterns section.


## 2026-09-23 - Ingestion: Raw Documents to Wiki

**Sources Ingested**:
- `document/raw/concept/Preserve user changes.md`
- `document/raw/concept/Three Arrow Merging Slide.md`
- `document/raw/decision/endpoint_spec.md`

**Changes Made**:
- Created Concept Pages:
  - `concepts/three-way-collection-merge.md`: Non-destructive 3-way synchronization model (Base, Target, Update).
  - `concepts/flatten-diff-merge-unflatten.md`: Array flattening algorithm avoiding RFC 6902 index-shifting corruption.
  - `concepts/postman-collection-schema.md`: Postman v2.1 structure, recursive `setId` UUID injection, and `BASE_URL` classification.
  - `concepts/response-envelope.md`: Standard JSON response envelope format and error codes (400, 401, 500).
  - `concepts/environment-variables.md`: IntelliJ/Postman private environment file convention and two-way map/array mapping.
- Created Decision Pages:
  - `decisions/endpoint-spec-and-architecture.md`: Ports-and-Adapters Clean Architecture, `/api/v1` routes, and multi-language rewrite blueprint.
  - `decisions/field-level-ownership-rules.md`: Generator-owned structural rules vs. user-owned data persistence rules.
  - `decisions/deterministic-endpoint-keys.md`: Adoption of `HTTP_METHOD:URL_PATH` composite keying for request matching.
- Created Implementation Pattern Pages:
  - `patterns/generator/three-way-merge-service.md`: Java / Spring Boot service implementation using Jackson and `zjsonpatch`.
  - `patterns/collection/collection-file-watcher.md`: File reading, UTF-8 BOM stripping, UUID recursion, and watcher cache synchronization.
  - `patterns/auth/cookie-jwt-auth.md`: Admin seeding, Bcrypt hashing, HS256 JWT, and HttpOnly cookie session management.
- Updated `index.md`: Added grouped sections with one-line descriptions for all newly created pages.

## 2026-09-23 - Initial Setup
- Initialized LLM Wiki directory structure (`document/raw/` and `document/wiki/`).
- Created initial index (`index.md`) and log (`log.md`).

## Related pages

- [[document/wiki/index]]
