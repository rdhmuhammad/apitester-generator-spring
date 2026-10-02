# Wiki Index

**Summary**: Grouped table of contents and page registry for the LLM Wiki.
**Sources**: `document/raw/concept/Preserve user changes.md`, `document/raw/concept/Three Arrow Merging Slide.md`, `document/raw/decision/endpoint_spec.md`
**Last updated**: 2026-10-02.

---

Welcome to the knowledge base for the `apitester-generator-spring` project. Below is the grouped index of all architectural patterns, decisions, and domain concepts.

## Patterns

Implementation designs and code patterns organized by functional module.

### UI
- [[patterns/ui/frontend-static-resource-embedding]]: Implementation pattern for embedding Vite React dist assets into Spring Boot starter JARs with SPA history fallback and dynamic .env discovery.

### Generator
- [[patterns/generator/three-way-merge-service]]: Implementation pattern in Spring Boot using Jackson and zjsonpatch to execute non-destructive 3-way collection merging.

### Collection
- [[patterns/collection/collection-file-watcher]]: Implementation pattern for collection file loading, UTF-8 BOM sanitization, recursive node UUID assignment, and file-watcher synchronization.

### Auth
- [[patterns/auth/cookie-jwt-auth]]: Implementation pattern for admin credential auto-seeding, Bcrypt validation, HS256 JWT generation, and secure HttpOnly cookie session management.

## Decisions

Recorded architectural and technical decisions establishing project standards.

- [[decisions/endpoint-spec-and-architecture]]: Architectural decision establishing Clean Architecture layering, route composition under `/api/v1`, and multi-language implementation guidelines.
- [[decisions/field-level-ownership-rules]]: Technical decision defining the conflict resolution boundary between code-generated schema definitions and user-authored testing data.
- [[decisions/deterministic-endpoint-keys]]: Technical decision adopting composite HTTP Method and URL Path keys to reliably identify and match collection requests across merge operations.
- [[decisions/deterministic-node-uuids]]: Technical decision adopting deterministic UUID generation from ControllerFileName and handler method name to eliminate UUID drift during three-way merging.

## Concepts

Reusable domain and architectural concepts underpinning the codebase.

- [[concepts/three-way-collection-merge]]: A non-destructive synchronization model that reconciles Spring controller changes with manual Postman edits by tracking three distinct collection states.
- [[concepts/flatten-diff-merge-unflatten]]: An algorithmic pattern that converts JSON arrays into deterministic key-value maps to enable reliable RFC 6902 JSON patch merging without array-index shifting corruption.
- [[concepts/postman-collection-schema]]: Specifications for Postman Collection v2.1 format, recursive node hierarchies, unique UUID injection for UI navigation, and variable classification rules.
- [[concepts/response-envelope]]: Standardized JSON response envelope contracts and error schemas enforced across all backend HTTP endpoints.
- [[concepts/environment-variables]]: Specifications for private environment storage conventions, on-disk schemas, and array-to-map transformations for IntelliJ and Postman environments.

## Related pages

- [[document/wiki/log]]
