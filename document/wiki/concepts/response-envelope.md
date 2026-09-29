# API Response Envelope

**Summary**: Standardized JSON response envelope contracts and error schemas enforced across all backend HTTP endpoints.
**Sources**: `document/raw/decision/endpoint_spec.md`
**Last updated**: 2026-09-23.

---

## Overview

All backend endpoints wrap payloads in a uniform response envelope. Frontend clients (Axios interceptors, Redux slices, and UI notification systems) rely strictly on these field conventions:
- `response.data.data`: Contains the payload entity.
- `response.data.message`: Contains the notification or error message for UI toast dialogs.

## Envelope Schemas

### 1. Success Response (`200 OK`)

Returned for successful queries and operations. For endpoints with no data payload, `data` is returned as `null`.

```json
{
  "success": true,
  "messageTitle": "Success",
  "message": "Success",
  "data": { ... }
}
```

### 2. Client / Validation Error (`400 Bad Request`)

Returned when parameters fail validation, entity lookups fail, or invalid inputs are supplied.

```json
{
  "success": false,
  "messageTitle": "Invalid data.",
  "message": "Specific error description"
}
```

### 3. Unauthorized Error (`401 Unauthorized`)

Returned when a protected endpoint is accessed without a valid or unexpired session cookie.

```json
{
  "message": "Authentication required"
}
```

*(Note: Depending on context, message may be `"Invalid or expired token"`).*

### 4. Server Error (`500 Internal Server Error`)

Returned on unhandled exceptions or internal system failures.

```json
{
  "success": false,
  "messageTitle": "Oops, something went wrong.",
  "message": "Internal error message",
  "errorServer": "raw error string",
  "data": null
}
```

## Related pages

- [[decisions/endpoint-spec-and-architecture]]
- [[patterns/auth/cookie-jwt-auth]]
- [[patterns/collection/collection-file-watcher]]
