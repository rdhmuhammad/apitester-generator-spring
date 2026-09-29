# Endpoint Specification and Architecture

**Summary**: Architectural decision establishing Clean Architecture layering, route composition under /api/v1, and multi-language implementation guidelines.
**Sources**: `document/raw/decision/endpoint_spec.md`
**Last updated**: 2026-09-23.

---

## Architectural Context

The backend implements a **Clean Architecture / Ports-and-Adapters** structure to decouple transport protocols, business use cases, and storage layers.

```
┌─────────────────────────────────────────────────────────────┐
│                       HTTP Layer                            │
│  - Base Route Prefix: /api/v1                               │
│  - Middlewares: CORS (credentials=true), CookieAuth (JWT)   │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                    Controllers / Handlers                   │
│  - WatchController (/collection/...)                        │
│  - EnvironmentController (/collection/:id/environments)     │
│  - AuthController (/auth/...)                               │
│  - Envelope serialization: { success, message, data }       │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                     Usecase Services                        │
│  - WatchService: BOM strip, setId() recursion, BaseURL regex│
│  - EnvService: Read/Write http-client.private.env.json      │
│  - AuthService: Bcrypt verify, JWT issuance, admin seeding  │
└──────────────────────────────┬──────────────────────────────┘
                               │
                ┌───────────────┴───────────────┐
                ▼                               ▼
┌──────────────────────────────┐ ┌──────────────────────────────┐
│      Storage / DB Repo       │ │       Filesystem Layer       │
│  - Key-Value / SQL Database  │ │  - Read/Write collection JSON│
│  - Tables: Collection,       │ │  - Read/Write env JSON       │
│    SelectedCollection, User  │ │  - File change notification  │
└──────────────────────────────┘ └──────────────────────────────┘
```

## Route Catalog

All routes reside under the `/api/v1` prefix:

### 1. Watch & Collection Module (`/api/v1/collection`)
* `GET /list`: Enumerate all collection metadata records.
* `GET /read/:id`: Load, strip BOM, classify variables, inject UUIDs, and return collection JSON.
* `PUT /select/:id`: Mark target collection active and re-arm file watcher.
* `PUT /write/:id`: Save edited collection JSON to disk (`0644`) and update watcher cache.
* `GET /get-active`: Retrieve metadata of the currently selected collection.
* `GET /read-selected`: Shortcut to load the content of the currently active collection.

### 2. Environment Module (`/api/v1/collection/:id/environments`)
* `GET /`: Read `<collection_dir>/tests/http-client.private.env.json` and return flattened list.
* `PUT /`: Convert list of environments back to dictionary and save with two-space indentation.

### 3. Auth Module (`/api/v1/auth`)
* `POST /login`: Authenticate credentials, set HttpOnly session cookie, return JWT info.
* `GET /me`: Validate session cookie and return user profile.

## Key Nuances to Preserve During Porting

1. **Envelope Compatibility**: The frontend relies on [[concepts/response-envelope]] (`data.data` for payloads and `data.message` for toast banners).
2. **Recursive Node UUIDs (`setId`)**: Every request, folder, param, header, and formdata node must have an assigned `id` for frontend tree traversal.
3. **Environment Directory Convention**: Store private environments adjacent to the collection file at `tests/http-client.private.env.json`.
4. **Cookie Security**: Auth token must be transported in an `HttpOnly` cookie with `Path: "/"`.

## Related pages

- [[concepts/response-envelope]]
- [[concepts/postman-collection-schema]]
- [[concepts/environment-variables]]
- [[patterns/collection/collection-file-watcher]]
- [[patterns/auth/cookie-jwt-auth]]
