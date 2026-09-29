# Environment Variables

**Summary**: Specifications for private environment storage conventions, on-disk schemas, and array-to-map transformations for IntelliJ and Postman environments.
**Sources**: `document/raw/decision/endpoint_spec.md`
**Last updated**: 2026-09-23.

---

## File Convention

Environment variables for test executions are stored alongside the collection file using the standard IntelliJ HTTP Client / Postman environment convention:

```text
<collection_directory>/tests/http-client.private.env.json
```

If the file does not exist when an environment read operation is executed, the backend auto-initializes the file with an empty JSON object (`{}`) and returns an empty environments list.

## On-Disk vs. In-Transit Schemas

### 1. On-Disk Structure (Map of Maps)
On the filesystem, environments are organized as nested dictionaries (`Map<String, Map<String, String>>`):

```json
{
  "development": {
    "baseUrl": "http://localhost:8080",
    "apiKey": "dev-key"
  },
  "staging": {
    "baseUrl": "https://staging.example.com",
    "apiKey": "stage-key"
  }
}
```

### 2. In-Transit API Structure (Array of Named Objects)
For client consumption, the backend flattens the dictionary into a list of environment objects:

```json
{
  "environments": [
    {
      "name": "development",
      "variables": {
        "baseUrl": "http://localhost:8080",
        "apiKey": "dev-key"
      }
    },
    {
      "name": "staging",
      "variables": {
        "baseUrl": "https://staging.example.com",
        "apiKey": "stage-key"
      }
    }
  ]
}
```

## Transformation & Persistence Rules

1. **Read Operation**:
   - Parse on-disk JSON map into memory.
   - Map each dictionary key to `name` and child map to `variables`.
   - Deliver in API response envelope under `data.environments`.
2. **Write Operation**:
   - Receive array of `{ name, variables }`.
   - Reconstruct the nested dictionary `map[string]map[string]string`.
   - Format with 2-space indentation.
   - Write to disk using `0644` permissions.

## Related pages

- [[decisions/endpoint-spec-and-architecture]]
- [[concepts/postman-collection-schema]]
- [[concepts/response-envelope]]
