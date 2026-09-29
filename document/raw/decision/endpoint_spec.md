
---

### Architectural Overview & Composition Pattern

The backend follows a **Clean Architecture / Ports-and-Adapters** pattern structured into three distinct layers:

1. **Routing & Server Assembly** ([`shared/api/api.go`](file:///d:/personal/apitester/shared/api/api.go#L11-L40), [`shared/api/default.go`](file:///d:/personal/apitester/shared/api/default.go#L17-L47)):
   - Base Path Prefix: `/api/v1`
   - CORS Middleware: Dynamic origin reflection with `Access-Control-Allow-Credentials: true` ([`pkg/middleware/cors.go#L7-L28`](file:///d:/personal/apitester/pkg/middleware/cors.go#L7-L28))
   - Database: Key-Value storage (currently BoltDB / `bbolt`) managing 3 buckets: `Collection`, `SelectedCollection`, and `User`.
2. **Controller Layer (`internal/usecase/<module>/controller.go`)**:
   - Handles HTTP transport (Gin Context), parameter validation, cookie setting, and standardized response envelope serialization.
3. **Usecase Layer (`internal/usecase/<module>/usecase.go`)**:
   - Encapsulates pure business logic, file I/O (reading/writing Postman collections and environment files), bcrypt hashing, JWT issuance, and in-memory file watching.
4. **Domain Layer (`internal/domain/`)**:
   - Entities: [`Collection`](file:///d:/personal/apitester/internal/domain/collection.go#L7-L14), [`SelectedCollection`](file:///d:/personal/apitester/internal/domain/collection.go#L16-L20), and [`User`](file:///d:/personal/apitester/internal/domain/user.go#L5-L10).

---

### Standard Envelope & Error Response Spec

Every endpoint follows a uniform JSON envelope format managed by [`shared/payload/response.go`](file:///d:/personal/apitester/shared/payload/response.go) and [`pkg/mapper/errors.go`](file:///d:/personal/apitester/pkg/mapper/errors.go):

#### 1. Success Response Envelope
```json
{
  "success": true,
  "messageTitle": "Success",
  "message": "Success", 
  "data": { ... } // null for NoData endpoints
}
```

#### 2. Client / Validation Error (`400 Bad Request`)
```json
{
  "success": false,
  "messageTitle": "Invalid data.",
  "message": "Specific error description"
}
```

#### 3. Unauthorized Error (`401 Unauthorized`)
```json
{
  "message": "Authentication required" // or "Invalid or expired token"
}
```

#### 4. Server Error (`500 Internal Server Error`)
```json
{
  "success": false,
  "messageTitle": "Oops, something went wrong.",
  "message": "Internal error message",
  "errorServer": "raw error string",
  "data": null
}
```

---

### Comprehensive Endpoint Specifications

#### 1. Watch & Collection Module (`internal/usecase/watch`)
Mounted under router group: `/api/v1/collection` ([`internal/usecase/watch/controller.go#L81-L89`](file:///d:/personal/apitester/internal/usecase/watch/controller.go#L81-L89))

---

##### `GET /api/v1/collection/list`
* **Controller**: [`Controller.ListCollections`](file:///d:/personal/apitester/internal/usecase/watch/controller.go#L71-L74)
* **Usecase**: [`Usecase.ListCollections`](file:///d:/personal/apitester/internal/usecase/watch/usecase.go#L95-L97)
* **Description**: Lists all imported/known collection metadata records stored in the database.
* **Request**: No query/body parameters.
* **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "messageTitle": "Success",
    "message": "Success",
    "data": [
      {
        "id": "uuid-string",
        "name": "My Collection",
        "is_selected": true,
        "path": "/absolute/path/to/collection.json",
        "updated_at": "2026-09-21T14:45:00Z",
        "created_at": "2026-09-21T14:45:00Z"
      }
    ]
  }
  ```

---

##### `GET /api/v1/collection/read/:id`
* **Controller**: [`Controller.Read`](file:///d:/personal/apitester/internal/usecase/watch/controller.go#L35-L41)
* **Usecase**: [`Usecase.Read`](file:///d:/personal/apitester/internal/usecase/watch/usecase.go#L99-L131)
* **Description**: Reads the underlying Postman v2.1 collection JSON file from disk by collection ID, normalizes it, enriches it with unique IDs, and categorizes base URL variables.
* **Path Parameters**: `id` (string) — Collection database ID.
* **Business Logic & Side Effects**:
  1. Looks up the collection record from database; returns `400` if not found.
  2. Reads file bytes from `collection.Path`.
  3. Strips UTF-8 BOM if present (`\uFEFF`).
  4. Parses into Postman DTO (`DocsContent`).
  5. **Auto-Categorize Variables**: Evaluates each variable key with regex `(?i)(base.*url|url.*base)`. If matched and variable has no custom ID, sets its category to `"BASE_URL"`.
  6. **Recursive UUID Injection** (`setId`): Traverses every item, sub-folder, request, header, query parameter, and formData to generate and populate UUIDs (`id`) so the frontend can reference them uniquely.
* **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "messageTitle": "Success",
    "message": "Success",
    "data": {
      "changed": false,
      "updatedAt": "2026-09-21T14:45:00Z",
      "content": {
        "info": {
          "_postman_id": "...",
          "name": "...",
          "description": "...",
          "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
        },
        "item": [ /* recursive collection items with funIden, id, request, response, etc. */ ],
        "variable": [
          { "id": "...", "key": "baseUrl", "category": "BASE_URL", "value": "http://localhost:8080", "type": "string" }
        ],
        "auth": { "type": "bearer", "bearer": [...] }
      }
    }
  }
  ```

---

##### `PUT /api/v1/collection/select/:id`
* **Controller**: [`Controller.SelectCollection`](file:///d:/personal/apitester/internal/usecase/watch/controller.go#L43-L47)
* **Usecase**: [`Usecase.SelectCollection`](file:///d:/personal/apitester/internal/usecase/watch/usecase.go#L133-L177)
* **Description**: Sets the active collection.
* **Path Parameters**: `id` (string) — Collection ID to activate.
* **Business Logic & Side Effects**:
  1. Iterates over all collections in the repository; unsets `is_selected = false` for all and sets `is_selected = true` for the target ID.
  2. Purges old entries from `SelectedCollection` database bucket and creates a new active pointer record.
  3. Re-arms file watcher (`watcher.Watch(selected.Path)`) to track filesystem changes.
* **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "messageTitle": "Success",
    "message": "Collection selected",
    "data": {
      "id": "uuid",
      "name": "...",
      "is_selected": true,
      "path": "..."
    }
  }
  ```

---

##### `PUT /api/v1/collection/write/:id`
* **Controller**: [`Controller.WriteCollection`](file:///d:/personal/apitester/internal/usecase/watch/controller.go#L49-L64)
* **Usecase**: [`Usecase.WriteCollection`](file:///d:/personal/apitester/internal/usecase/watch/usecase.go#L203-L234)
* **Description**: Writes updated raw JSON content directly back to the collection file on disk.
* **Path Parameters**: `id` (string)
* **Request Body**: Raw JSON string (`Content-Type: application/json`). Must not be empty.
* **Business Logic**:
  1. Validates body content is non-empty.
  2. Resolves file path from database for the collection.
  3. Writes file using mode `0644`.
  4. Updates in-memory watcher state with current file content and timestamp so it doesn't fire a false positive "external edit" event.
* **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "messageTitle": "Success",
    "message": "Collection written successfully"
  }
  ```

---

##### `GET /api/v1/collection/get-active`
* **Controller**: [`Controller.GetActiveCollection`](file:///d:/personal/apitester/internal/usecase/watch/controller.go#L66-L69)
* **Usecase**: [`Usecase.GetActiveCollection`](file:///d:/personal/apitester/internal/usecase/watch/usecase.go#L179-L185)
* **Description**: Returns metadata for the currently active collection. Returns `400 Bad Request` (`"No active collection"`) if none is selected.
* **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "messageTitle": "Success",
    "message": "Success",
    "data": {
      "id": "uuid",
      "name": "Active Collection",
      "is_selected": true,
      "path": "..."
    }
  }
  ```

---

##### `GET /api/v1/collection/read-selected`
* **Controller**: [`Controller.ReadSelectedCollection`](file:///d:/personal/apitester/internal/usecase/watch/controller.go#L76-L79)
* **Usecase**: [`Usecase.ReadSelectedCollection`](file:///d:/personal/apitester/internal/usecase/watch/usecase.go#L187-L201)
* **Description**: Convenience shortcut that reads the latest active collection record from `SelectedCollection` bucket and immediately runs `Read(id)`.
* **Response** (`200 OK`): Same payload schema as `GET /api/v1/collection/read/:id`.

---

#### 2. Environment Module (`internal/usecase/environment`)
Mounted under router group: `/api/v1/collection` ([`internal/usecase/environment/controller.go#L54-L58`](file:///d:/personal/apitester/internal/usecase/environment/controller.go#L54-L58))

---

##### `GET /api/v1/collection/:id/environments`
* **Controller**: [`Controller.ReadEnvironments`](file:///d:/personal/apitester/internal/usecase/environment/controller.go#L30-L34)
* **Usecase**: [`Usecase.ReadEnvironments`](file:///d:/personal/apitester/internal/usecase/environment/usecase.go#L33-L72)
* **Description**: Reads environment variables from the standard IntelliJ/Postman private environment file.
* **File Location Convention**:
  Resolves to `<dir(collection.Path)>/tests/http-client.private.env.json`.
* **Business Logic**:
  1. Finds collection by ID.
  2. If file does not exist, auto-initializes it with empty JSON `{}` and returns `{"environments": []}`.
  3. The on-disk schema is:
     ```json
     {
       "development": { "host": "localhost", "token": "xxx" },
       "staging": { "host": "api.staging.com", "token": "yyy" }
     }
     ```
  4. Flattens into array format for client consumption.
* **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "messageTitle": "Success",
    "message": "Success",
    "data": {
      "environments": [
        {
          "name": "development",
          "variables": {
            "baseUrl": "http://localhost:3000",
            "apiKey": "dev-key"
          }
        }
      ]
    }
  }
  ```

---

##### `PUT /api/v1/collection/:id/environments`
* **Controller**: [`Controller.WriteEnvironments`](file:///d:/personal/apitester/internal/usecase/environment/controller.go#L36-L52)
* **Usecase**: [`Usecase.WriteEnvironments`](file:///d:/personal/apitester/internal/usecase/environment/usecase.go#L74-L99)
* **Description**: Writes environment entries back to `<collection_dir>/tests/http-client.private.env.json`.
* **Request Body** (`application/json`):
  ```json
  {
    "environments": [
      {
        "name": "development",
        "variables": {
          "baseUrl": "http://localhost:3000",
          "apiKey": "dev-key"
        }
      }
    ]
  }
  ```
* **Business Logic**:
  1. Converts incoming `[{name, variables}]` array back to dictionary map `map[string]map[string]string`.
  2. Indents JSON with two spaces (`json.MarshalIndent`).
  3. Writes to disk (`0644`).
* **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "messageTitle": "Success",
    "message": "Environments written successfully"
  }
  ```

---

#### 3. Auth Module (`internal/usecase/auth`)
Mounted under router group: `/api/v1/auth` ([`internal/usecase/auth/controller.go#L73-L79`](file:///d:/personal/apitester/internal/usecase/auth/controller.go#L73-L79))

---

##### `POST /api/v1/auth/login`
* **Controller**: [`Controller.Login`](file:///d:/personal/apitester/internal/usecase/auth/controller.go#L35-L58)
* **Usecase**: [`Usecase.Login`](file:///d:/personal/apitester/internal/usecase/auth/usecase.go#L76-L121)
* **Description**: Authenticates user against credentials in the database, sets an HTTP-Only session cookie, and returns JWT details.
* **Request Body**:
  ```json
  {
    "username": "admin",
    "password": "secretpassword",
    "rememberMe": true
  }
  ```
* **Business Logic**:
  1. Admin auto-seeding: When the app starts with `ADMIN_USERNAME` and `ADMIN_PASSWORD` env vars set, it creates an initial admin record if none exists ([`usecase.go#L45-L74`](file:///d:/personal/apitester/internal/usecase/auth/usecase.go#L45-L74)).
  2. Validates password using `bcrypt.CompareHashAndPassword`. Returns `400 Bad Request` with `"Invalid credentials"` on mismatch.
  3. Expiration calculation:
     - `rememberMe == true`: 30 days (`30 * 24h`)
     - `rememberMe == false`: 24 hours
  4. Generates HS256 JWT token using secret from `JWT_SECRET` (fallback: `"default-secret-change-me"`).
  5. Sets response cookie:
     `Set-Cookie: token=<jwt>; Path=/; Max-Age=<seconds>; HttpOnly`
* **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "messageTitle": "Success",
    "message": "Login successful",
    "data": {
      "username": "admin",
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "expiresAt": 1790000000
    }
  }
  ```

---

##### `GET /api/v1/auth/me`
* **Controller**: [`Controller.Me`](file:///d:/personal/apitester/internal/usecase/auth/controller.go#L60-L71)
* **Middleware**: Protected by [`CookieAuth(jwtSecret)`](file:///d:/personal/apitester/pkg/middleware/auth.go#L11-L39).
* **Description**: Returns authenticated user profile based on `token` cookie.
* **Authentication**: Requires cookie `token=<jwt>`. Returns `401 Unauthorized` if missing, invalid, or expired.
* **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "messageTitle": "Success",
    "message": "Success",
    "data": {
      "username": "admin",
      "authenticated": true
    }
  }
  ```

---

### Rewrite Blueprint for Another Language (TypeScript / Rust / Python / C#)

If you rewrite this backend in another language (e.g. **TypeScript with Fastify/NestJS**, **Python with FastAPI**, or **Rust with Axum**), replicate this composition:

```
┌─────────────────────────────────────────────────────────────┐
│                       HTTP Layer                            │
│  - Fastify / Express / FastAPI / Axum                       │
│  - Global Prefix: /api/v1                                   │
│  - Middlewares: CORS (credentials=true), CookieAuth (JWT)   │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                    Controllers / Handlers                   │
│  - WatchController (/collection/...)                        │
│  - EnvironmentController (/collection/:id/environments)     │
│  - AuthController (/auth/...)                               │
│  - Formats output to { success, messageTitle, message, data}│
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                     Usecase Services                        │
│  - WatchService: BOM strip, setId() UUID recursion,         │
│                  BaseURL regex detection, sync FileWatcher  │
│  - EnvService: Read/Write http-client.private.env.json      │
│  - AuthService: Bcrypt verify, JWT HS256 issue, admin seed  │
└──────────────────────────────┬──────────────────────────────┘
                               │
               ┌───────────────┴───────────────┐
               ▼                               ▼
┌──────────────────────────────┐ ┌──────────────────────────────┐
│      Storage / DB Repo       │ │       Filesystem Layer       │
│  - SQLite / Key-Value store  │ │  - Read/Write collection JSON│
│  - Tables: Collection,       │ │  - Read/Write env JSON       │
│    SelectedCollection, User  │ │  - File change notification  │
└──────────────────────────────┘ └──────────────────────────────┘
```

#### Key Implementation Nuances to Preserve
1. **Response Envelope Compatibility**: The frontend Redux slices and Axios interceptor ([`frontend/src/config/axios.ts`](file:///d:/personal/apitester/frontend/src/config/axios.ts), [`frontend/src/layout/services/`](file:///d:/personal/apitester/frontend/src/layout/services/)) expect `response.data.data` for the entity and `response.data.message` for toast notifications.
2. **Recursive ID Assignment (`setId`)**: When reading collections, every level of `item`, `request.header`, `request.url.query`, and `request.body.formdata` must have an assigned UUID `id` to allow the frontend editor tree to select and modify nodes without key collisions.
3. **Variable Classification**: Any variable matching `/base.*url|url.*base/i` without a category must be assigned category `"BASE_URL"`.
4. **Environment File Convention**: Environment files are stored beside the collection at `<collection_dir>/tests/http-client.private.env.json`. On read, auto-create `{}` if not found.
5. **Cookie Security**: Auth token must be stored in `token` HTTP cookie with `HttpOnly: true`, `Path: "/"`, and CORS must set `Access-Control-Allow-Credentials: true`.