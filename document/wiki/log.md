# Wiki Log

**Summary**: Append-only record of all wiki operations, document ingestions, and structural changes.
**Sources**: `document/raw/concept/Preserve user changes.md`, `document/raw/concept/Three Arrow Merging Slide.md`, `document/raw/decision/endpoint_spec.md`
**Last updated**: 2026-09-29.

---

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
