# Cookie-Based JWT Authentication Pattern

**Summary**: Implementation pattern for admin credential auto-seeding, Bcrypt validation, HS256 JWT generation, and secure HttpOnly cookie session management.
**Sources**: `document/raw/decision/endpoint_spec.md`
**Last updated**: 2026-09-23.

---

## Architectural Context

Authentication protects backend APIs and synchronizes session state between the browser and server. To prevent cross-site scripting (XSS) token theft, JWTs are stored in an `HttpOnly` browser cookie rather than in browser local storage.

## Key Mechanisms

### 1. Admin Auto-Seeding
On application startup, the auth service inspects the user database. If no user exists and the environment variables `ADMIN_USERNAME` and `ADMIN_PASSWORD` are configured:
- The raw password is salted and hashed using Bcrypt.
- An initial admin user record is persisted to the database.

### 2. Login Flow (`POST /api/v1/auth/login`)
1. **Validate Credentials**:
   Compare the provided plaintext password against the stored Bcrypt hash. Return `400 Bad Request` with message `"Invalid credentials"` on mismatch.
2. **Calculate Session Expiration**:
   - If `rememberMe == true`: 30 days (`30 * 24 * 3600` seconds).
   - If `rememberMe == false`: 24 hours (`24 * 3600` seconds).
3. **Issue HS256 JWT**:
   Sign the token using the secret resolved from `JWT_SECRET` (with a default fallback).
4. **Set Response Cookie**:
   Attach the token in the HTTP response header:
   ```text
   Set-Cookie: token=<jwt>; Path=/; Max-Age=<seconds>; HttpOnly
   ```
5. **CORS Requirement**:
   CORS middleware must allow credentials (`Access-Control-Allow-Credentials: true`) and reflect the allowed origin dynamically to enable cross-origin cookie transport during development.

### 3. Session Verification (`GET /api/v1/auth/me`)
Protected routes extract the `token` cookie via auth middleware:
- If cookie is missing, expired, or signature is invalid, return `401 Unauthorized` (`"Authentication required"` or `"Invalid or expired token"`).
- If valid, return user profile `{ username, authenticated: true }`.

## Related pages

- [[decisions/endpoint-spec-and-architecture]]
- [[concepts/response-envelope]]
