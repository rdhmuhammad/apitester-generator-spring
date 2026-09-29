# Three-Way Collection Merge

**Summary**: A non-destructive synchronization model that reconciles Spring controller changes with manual Postman edits by tracking three distinct collection states.
**Sources**: `document/raw/concept/Preserve user changes.md`, `document/raw/concept/Three Arrow Merging Slide.md`
**Last updated**: 2026-09-23.

---

## Overview

When generating a Postman collection directly from Spring controllers, a simple overwrite results in a destructive two-way sync. Developers frequently customize collections in the Postman client—adding sample request payloads, authorization tokens, test scripts, and environment variable references. Overwriting the collection on every code build obliterates these manual additions.

To prevent data loss, the system uses a **Three-Way Merge** architecture that introduces historical memory of the previous generator execution.

## The Three States

The merge engine tracks three states of the Postman collection:

1. **The Base (`last_generated_state.json`)**: The pure, unedited output of the *previous* generator run. This represents the common ancestor.
2. **The Target (`current_user_collection.json`)**: The active collection currently in Postman, reflecting user-authored changes, payloads, and scripts.
3. **The Update (`newly_generated_state.json`)**: The pure output of the *current* generator run, reflecting the latest Spring controllers and model schemas.

## State Comparison Rules

By comparing the delta between the three states, modifications are deterministically attributed:

| Condition | Attribution | Resolution Action |
| --- | --- | --- |
| Field differs between `Base` and `Target` | User modified | **Preserve** the Target value |
| Field differs between `Base` and `Update` | Generator modified | **Apply** the Update value |
| Endpoint exists in `Update` but not `Base` | New controller endpoint | **Add** the new endpoint |
| Endpoint exists in `Base` but not `Update` | Removed controller endpoint | **Remove** (or mark deprecated) |

Granular conflict boundaries are defined by [[decisions/field-level-ownership-rules]].

## Shadow File Caching

The generator preserves the Base state using a shadow cache file (such as `.generator-cache.json` or a local database record).

Workflow sequence:
1. Fetch the user's active collection (`Target`).
2. Generate the latest collection from Spring controllers in-memory (`Update`).
3. Load the previous generation from the cache (`Base`).
4. Execute the merge logic via [[concepts/flatten-diff-merge-unflatten]].
5. Output the merged collection for Postman usage.
6. Overwrite the shadow cache with the pure `Update` state to serve as `Base` for the next cycle.

## Request Body Merging

Request body payloads represent the most critical user customization. The merge algorithm parses JSON payloads in both Target and Update:
- If a JSON property key exists in the Target payload, preserve the user's custom test value.
- If a new property key appears in Update (e.g., a newly added field on a Spring DTO), inject the new property with default values.

## Alternative: Patch / Overlay Approach

An alternative architectural approach is the **Overlay Model**:
- The generator produces a read-only `collection.json`.
- Users record modifications in a separate `user-overrides.json` file keyed by endpoint URL.
- The generator overlays overrides on top of the base collection.

While simpler to implement, this degrades developer experience by disallowing direct edits in the Postman UI without manual syncing.

## Related pages

- [[concepts/flatten-diff-merge-unflatten]]
- [[concepts/postman-collection-schema]]
- [[decisions/field-level-ownership-rules]]
- [[decisions/deterministic-endpoint-keys]]
- [[patterns/generator/three-way-merge-service]]
