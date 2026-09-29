# Flatten-Diff-Merge-Unflatten Pattern

**Summary**: An algorithmic pattern that converts JSON arrays into deterministic key-value maps to enable reliable RFC 6902 JSON patch merging without array-index shifting corruption.
**Sources**: `document/raw/concept/Three Arrow Merging Slide.md`, `document/raw/concept/Preserve user changes.md`
**Last updated**: 2026-09-23.

---

## The Array-Index Shifting Problem

Postman collections store requests hierarchically inside JSON arrays (`"item": [...]`). Standard RFC 6902 JSON patch engines (such as Flipkart's `zjsonpatch`) identify JSON elements using index-based JSON pointers (e.g., `/item/0/request/body/raw`).

If a code generator adds a new Spring endpoint at the start of the list, every subsequent endpoint shifts down by one index position (item `0` becomes item `1`). When applying a previously computed user diff patch targeting index `0`, the patch is applied to the wrong endpoint, resulting in severe collection corruption.

## Pattern Steps

To resolve index instability, the merge engine implements the **Flatten-Diff-Merge-Unflatten** pipeline:

```
[baseNode]   ──> Flatten ──> [baseMap]   ──┐
                                           ├──> JsonDiff ──> [rawUserPatch] ──> Filter ──┐
[targetNode] ──> Flatten ──> [targetMap] ──┘                                             │
                                                                                         ├──> JsonPatch.apply ──> [mergedMap] ──> Unflatten ──> Final Collection
[updateNode] ──> Flatten ────────────────────────────────────────────────> [updateMap]  ──┘
```

### 1. Load Three States
Read the Base, Target, and Update documents into Jackson `JsonNode` object trees.

### 2. Flatten Arrays into Maps
Traverse the hierarchical `"item"` arrays across all three documents and convert them into associative JSON Objects (Maps). Each request node is assigned a deterministic primary key (e.g., `"POST:/api/v1/users"`). See [[decisions/deterministic-endpoint-keys]].

- `baseMap` = `Flatten(baseNode)`
- `targetMap` = `Flatten(targetNode)`
- `updateMap` = `Flatten(updateNode)`

### 3. Compute the User Patch
Generate a precise RFC 6902 JSON patch representing all user modifications made between the previous generation and the current Postman state:
```java
JsonNode userPatch = JsonDiff.asJson(baseMap, targetMap);
```

### 4. Filter the Patch
Filter the operations array in `userPatch` to enforce ownership boundaries:
- **Drop operations** that attempt to rewrite structural elements (e.g. `/POST:\/api\/users/request/url/raw`).
- **Retain operations** modifying payload content (e.g. `/POST:\/api\/users/request/body/raw`) or headers.
Refer to [[decisions/field-level-ownership-rules]].

### 5. Apply Patch to the Update State
Apply the filtered patch directly on top of the newly generated Spring collection map:
```java
JsonNode mergedMap = JsonPatch.apply(filteredUserPatch, updateMap);
```

### 6. Unflatten and Persist
Convert the `mergedMap` back into Postman's standard `"item": [...]` array schema. Save the resulting JSON as the final output collection and update the shadow cache.

## Related pages

- [[concepts/three-way-collection-merge]]
- [[decisions/deterministic-endpoint-keys]]
- [[decisions/field-level-ownership-rules]]
- [[patterns/generator/three-way-merge-service]]
