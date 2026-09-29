# Field-Level Ownership Rules

**Summary**: Technical decision defining the conflict resolution boundary between code-generated schema definitions and user-authored testing data.
**Sources**: `document/raw/concept/Preserve user changes.md`, `document/raw/concept/Three Arrow Merging Slide.md`
**Last updated**: 2026-09-23.

---

## Decision Rationale

During collection synchronization, neither a blanket "always overwrite" nor an "always preserve user changes" policy is viable:
- An "always overwrite" policy destroys custom test payloads and scripts.
- An "always preserve" policy ignores controller refactorings (e.g. changing an endpoint path or adding mandatory headers).

To balance automated synchronization with customization persistence, fields in a Postman request are partitioned into **Generator-Owned** and **User-Owned** tiers.

## Ownership Matrix

| Field Category | Owner | Behavior on Merge |
| --- | --- | --- |
| **HTTP Method & URL Path** | Generator | **Always Overwrite**: Changes in `@GetMapping`, `@PostMapping`, or `@RequestMapping` take immediate precedence. |
| **Query Parameter Declarations** | Generator | **Always Overwrite**: Keys declared by `@RequestParam` are updated automatically. |
| **Content-Type / Headers Schema** | Generator | **Always Overwrite**: Values derived from `consumes` / `produces` attributes override collection defaults. |
| **Parameter & Header Values** | User | **Always Preserve**: Custom values, dummy test data, and tokens entered by the user are kept. |
| **Request Body Payload** | User (Deep Merge) | **Preserve & Extend**: Existing keys retain user values; new fields from Spring DTOs are injected with defaults. |
| **Scripts (`event`)** | User | **Always Preserve**: Pre-request and post-response test assertions are never removed or overwritten. |
| **Environment Variable Tokens** | User | **Always Preserve**: Interpolation tokens like `{{baseUrl}}` or `{{token}}` are retained. |

## Request Body Merging Heuristic

Because request bodies represent the primary test data entered by developers:
1. Parse both Target (User) and Update (Generator) JSON bodies into structured key-value trees.
2. For each key in the Update schema:
   - If present in the Target body, preserve the Target's customized value.
   - If absent in the Target body (a new field was added to the Spring model), insert the key with the generator's default placeholder value.
3. Keys present in Target but deleted in Update are either retained or pruned based on schema validation settings.

## Related pages

- [[concepts/three-way-collection-merge]]
- [[concepts/flatten-diff-merge-unflatten]]
- [[decisions/deterministic-endpoint-keys]]
- [[patterns/generator/three-way-merge-service]]
