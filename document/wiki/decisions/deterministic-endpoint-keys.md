# Deterministic Endpoint Keys

**Summary**: Technical decision adopting composite HTTP Method and URL Path keys to reliably identify and match collection requests across merge operations.
**Sources**: `document/raw/concept/Preserve user changes.md`, `document/raw/concept/Three Arrow Merging Slide.md`
**Last updated**: 2026-09-23.

---

## Problem Statement

Postman collections organize requests using human-readable names (e.g., `"Fetch User Details"`, `"Login User"`) within arbitrary folder hierarchies. Users frequently rename requests or reorder folders inside the Postman UI.

If the merge engine matched requests by their display name or folder index, any rename or reorganization would cause matching failures, resulting in duplicate requests or accidental deletions.

## Evaluated Alternatives

1. **Request Name Matching**: Fragile; breaks immediately when a developer renames a request in Postman.
2. **Injected Invisible Metadata (Custom Header or Description Tag)**: Intrusive; requires adding synthetic headers or polluting documentation blocks.
3. **HTTP Method + Normalized URL Path (`METHOD:PATH`)**: Robust and deterministic; maps directly to Spring `@RequestMapping` annotations and remains stable even if requests are renamed or moved between folders.

## Chosen Strategy

Adopt **`HTTP_METHOD:URL_PATH`** as the deterministic primary key when transforming collection arrays into associative maps (e.g. `"POST:/api/v1/auth/login"`, `"GET:/api/v1/collection/list"`).

### Key Normalization Rules
1. **Method Uppercasing**: Method strings must be normalized to uppercase (`GET`, `POST`, `PUT`, `DELETE`).
2. **Path Variable Canonicalization**: Path parameters declared with colon syntax (`:id`) or brace syntax (`{id}`) are normalized to a consistent format.
3. **Query Parameter Exclusion**: Query parameters (`?sort=asc`) are excluded from the primary key string and compared separately as query components.
4. **Trailing Slash Stripping**: Redundant trailing slashes are removed prior to key generation.

## Related pages

- [[concepts/flatten-diff-merge-unflatten]]
- [[concepts/three-way-collection-merge]]
- [[decisions/field-level-ownership-rules]]
- [[patterns/generator/three-way-merge-service]]
