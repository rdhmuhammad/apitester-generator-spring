# API Tester Generator for Spring Boot

Automatically generate a [Postman Collection](https://www.postman.com/collection/) from your Spring Boot REST controllers at application startup — no manual export needed.

> **Group:** `com.apitester`  &nbsp;|&nbsp; **Artifact:** `apitester-generator-spring`  &nbsp;|&nbsp; **Version:** `0.0.1`

---

## Table of Contents

- [Features](#features)
- [Installation](#installation)
  - [Local JAR](#local-jar)
  - [JitPack (recommended)](#jitpack-recommended)
- [Quick Start](#quick-start)
- [Configuration Properties](#configuration-properties)
- [Annotations](#annotations)
  - [@EnableApiTester](#enableapitester)
  - [@ApiTester](#apitester)
- [Usage Cases](#usage-cases)
  - [1. Basic GET / POST with @RequestBody](#1-basic-get--post-with-requestbody)
  - [2. Path Variables (@PathVariable)](#2-path-variables-pathvariable)
  - [3. Query Parameters (@RequestParam)](#3-query-parameters-requestparam)
  - [4. DTO as Query Parameters](#4-dto-as-query-parameters)
  - [5. JSON Naming Strategies (SnakeCase / KebabCase)](#5-json-naming-strategies-snakecase--kebabcase)
  - [6. @JsonProperty on DTO Fields](#6-jsonproperty-on-dto-fields)
  - [7. Custom Setter Naming](#7-custom-setter-naming)
  - [8. Multipart File Upload (@RequestPart / MultipartFile)](#8-multipart-file-upload-requestpart--multipartfile)
  - [9. Form Data (@ModelAttribute)](#9-form-data-modelattribute)
  - [10. Mixed @ModelAttribute + @RequestPart](#10-mixed-modelattribute--requestpart)
  - [11. Parent Folder Nesting](#11-parent-folder-nesting)
  - [12. Merging Controllers Under Same Folder](#12-merging-controllers-under-same-folder)
  - [13. Global Headers per Controller](#13-global-headers-per-controller)
  - [14. Collection-Level Global Variables](#14-collection-level-global-variables)
  - [15. Ignoring Parameters](#15-ignoring-parameters)
  - [16. Disabling Generation](#16-disabling-generation)
- [Example Project](#example-project)
- [Building from Source](#building-from-source)
- [License](#license)

---

## Features

- **Zero runtime annotation scanning** — collection is generated once at application startup via `ApplicationReadyEvent`.
- Supports `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`, and `@RequestMapping`.
- **Request body** → JSON example auto-generated from DTO fields.
- **Query parameters** → `@RequestParam`, unannotated simple types, and DTO resolution.
- **Path variables** → `@PathVariable` with `{param}` → `:param` conversion.
- **Multipart file upload** → `@RequestPart` and `MultipartFile` produce `formdata` body.
- **Form data** → `@ModelAttribute` resolved as `formdata` entries.
- **Custom naming** — `@JsonProperty`, `@JsonNaming` (SnakeCase / KebabCase / LowerCase / UpperCamelCase), and custom setter detection.
- **Hierarchical folders** — nest controllers under multi-level parent folders.
- **Global headers / variables** — per-controller headers and collection-wide variables.
- **Parameter ignore list** — skip framework types + custom ignore types.
- Configurable via `application.properties`.

---

## Installation

### Local JAR

Build the project and place the JAR in your project's `libs/` directory:

```bash
./gradlew bootJar
cp build/libs/apitester-generator-spring-0.0.1-plain.jar your-project/libs/
```

**Gradle (`build.gradle`):**

```groovy
repositories {
    mavenCentral()
    flatDir { dirs 'libs' }
}

dependencies {
    implementation 'com.apitester:apitester-generator-spring:0.0.1'
}
```

**Maven (`pom.xml`):**

```xml
<dependency>
    <groupId>com.apitester</groupId>
    <artifactId>apitester-generator-spring</artifactId>
    <version>0.0.1</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/apitester-generator-spring-0.0.1-plain.jar</systemPath>
</dependency>
```

### JitPack (recommended)

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.anomalyco:apitester-generator-spring:0.0.1'
}
```

---

## Quick Start

**1.** Add the dependency (see [Installation](#installation)).

**2.** Annotate your Spring Boot main class:

```java
import com.apitester.generator.annotation.EnableApiTester;

@SpringBootApplication
@EnableApiTester
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

**3.** Annotate your controllers:

```java
import com.apitester.generator.annotation.ApiTester;

@RestController
@RequestMapping("/api/v1/users")
@ApiTester
public class UserController {

    @GetMapping("/{id}")
    public String getById(@PathVariable Long id) {
        return "user";
    }

    @PostMapping
    public String create(@RequestBody CreateUserRequest body) {
        return "created";
    }
}
```

**4.** Start your application. The collection is written to `postman_collection.json` by default.

---

## Configuration Properties

All properties go under the `apitester.*` prefix in `application.properties`:

| Property | Default | Description |
|---|---|---|
| `apitester.enabled` | `true` | Master switch. Set to `false` to disable generation. |
| `apitester.collection.name` | `API Collection` | Postman collection display name. |
| `apitester.collection.description` | (empty) | Collection description. |
| `apitester.collection.base-url` | `http://localhost:8080` | Used as the `{{baseUrl}}` collection variable. |
| `apitester.collection.output-path` | `postman_collection.json` | Output file path (relative to working directory). |
| `apitester.base-package` | (empty) | Reserved for future use. |
| `apitester.format` | `json` | Output format (currently only `json`). |
| `apitester.timeout` | `5000` | Reserved for future use. |

**Example:**

```properties
apitester.enabled=true
apitester.collection.name=My API
apitester.collection.description=Auto-generated from Spring controllers
apitester.collection.base-url=https://api.example.com
apitester.collection.output-path=docs/postman.json
```

---

## Annotations

### @EnableApiTester

Place on your `@SpringBootApplication` main class. Activates the auto-configuration.

| Attribute | Type | Default | Description |
|---|---|---|---|
| `globalVariables` | `Variable[]` | `{}` | Collection-level variables injected into the generated Postman collection. |

```java
@SpringBootApplication
@EnableApiTester(globalVariables = {
    @EnableApiTester.Variable(key = "token", value = "your-jwt-token"),
    @EnableApiTester.Variable(key = "version", value = "v1")
})
public class MyApplication { }
```

### @ApiTester

Place on each `@RestController` you want to include in the collection.

| Attribute | Type | Default | Description |
|---|---|---|---|
| `folders` | `String[]` | `{}` | Hierarchical parent folder path (e.g., `{"Admin", "Dashboard"}`). |
| `ignoreParams` | `Class<?>[]` | `{}` | Parameter types to skip during endpoint processing. |
| `globalHeaders` | `Headers[]` | `{}` | Headers added to every request from this controller. |

**Headers inner annotation:**

| Attribute | Type | Default | Description |
|---|---|---|---|
| `key` | `String` | `""` | Header name (e.g., `Authorization`). |
| `value` | `String` | `""` | Header value. |
| `description` | `String` | `""` | Optional description. |

---

## Usage Cases

### 1. Basic GET / POST with @RequestBody

```java
@RestController
@RequestMapping("/api/v1/products")
@ApiTester
public class ProductController {

    @GetMapping
    public List<Product> list(ProductFilter filter) { ... }

    @PostMapping
    public Product create(@RequestBody CreateProductRequest body) { ... }
}
```

The `@RequestBody` DTO is introspected and a JSON example body is generated. For the GET, the `ProductFilter` DTO fields are resolved as query parameters.

### 2. Path Variables (@PathVariable)

```java
@GetMapping("/{id}")
public Product getById(@PathVariable Long id) { ... }

@GetMapping("/{category}/{id}")
public Product getByCategory(@PathVariable String category, @PathVariable Long id) { ... }
```

`{param}` placeholders in the URL template become `:param` in the raw Postman URL.

### 3. Query Parameters (@RequestParam)

```java
@GetMapping("/search")
public List<Product> search(@RequestParam String keyword,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "20") int size) { ... }
```

All `@RequestParam` parameters appear as query params in the generated request.

### 4. DTO as Query Parameters

When a method parameter is a complex type (not annotated with `@RequestBody`, `@RequestParam`, etc.), its fields are automatically resolved as query parameters:

```java
@GetMapping("/filter")
public List<Product> filter(ProductFilter filter) { ... }
```

```java
public class ProductFilter {
    private String name;      // → ?name=
    private Integer minPrice; // → ?minPrice=0
    private Boolean active;   // → ?active=false
}
```

### 5. JSON Naming Strategies (SnakeCase / KebabCase)

```java
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProductFilter {
    private String productName;  // → product_name
    private Integer maxPrice;    // → max_price
}

@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public class SearchFilter {
    private String searchTerm;  // → search-term
}
```

Supported strategies: `SnakeCaseStrategy`, `KebabCaseStrategy`, `LowerCaseStrategy`, `UpperCamelCaseStrategy`, `LowerCamelCaseStrategy`.

### 6. @JsonProperty on DTO Fields

```java
public class SearchRequest {
    @JsonProperty("q")
    private String query;       // → ?q=

    @JsonProperty("sort_by")
    private String sortBy;      // → ?sort_by=
}
```

`@JsonProperty` takes priority over field name and naming strategy.

### 7. Custom Setter Naming

If a field has a non-standard setter (the setter name does not follow `set` + capitalized field name), the setter-derived name is used:

```java
public class UserFilter {
    private String userId;

    public String getUserId() { return userId; }
    public void setUser_id(String userId) { this.userId = userId; }
    //                                ↑ custom setter → param name: "user_id"
}
```

### 8. Multipart File Upload (@RequestPart / MultipartFile)

```java
@PostMapping("/upload")
public String upload(@RequestPart("file") MultipartFile file,
                     @RequestParam("description") String description) { ... }
```

Generates a `formdata` body with:
- `file` → type `file`
- `description` → type `text`

Array of files is supported:

```java
@PostMapping("/gallery")
public String uploadMultiple(@RequestPart("images") MultipartFile[] images,
                             @RequestParam("album") String album) { ... }
```

### 9. Form Data (@ModelAttribute)

```java
@PostMapping("/profile")
public String updateProfile(@ModelAttribute ProfileForm form) { ... }
```

```java
public class ProfileForm {
    private String username;  // → formdata text field
    private String email;     // → formdata text field
    private int age;          // → formdata text field
}
```

All DTO fields are resolved as `formdata` text entries. No query parameters are generated — the body uses `mode: "formdata"`.

### 10. Mixed @ModelAttribute + @RequestPart

```java
@PostMapping("/document")
public String submit(@ModelAttribute MetadataForm meta,
                     @RequestPart("file") MultipartFile file) { ... }
```

Both DTO fields and file parts land in the same `formdata` body.

### 11. Parent Folder Nesting

```java
@RestController
@RequestMapping("/api/admin/users")
@ApiTester(folders = {"Admin", "Users"})
public class AdminUserController { ... }
```

Postman collection structure:
```
Collection
└── Admin/              ← parent folder
    └── Users/          ← parent folder
        └── Admin User  ← controller folder (derived from class name)
            ├── Get All Users
            └── Create User
```

The controller folder name is auto-derived from the class name by stripping `Controller` and inserting spaces before uppercase letters. `AdminUserController` → `"Admin User"`.

### 12. Merging Controllers Under Same Folder

Multiple controllers sharing the same `folders` path are merged:

```java
@ApiTester(folders = {"Management"})  // Both use same parent
public class ProductManagementController { ... }

@ApiTester(folders = {"Management"})
public class MerchantManagementController { ... }
```

Result:
```
Management/
├── Product Management/
│   └── ...
└── Merchant Management/
    └── ...
```

### 13. Global Headers per Controller

```java
@ApiTester(
    folders = {"API"},
    globalHeaders = {
        @ApiTester.Headers(key = "Authorization", value = "Bearer {{token}}"),
        @ApiTester.Headers(key = "X-API-Version", value = "1", description = "API version header")
    }
)
public class SecuredController { ... }
```

Every request from this controller includes the specified headers.

### 14. Collection-Level Global Variables

```java
@SpringBootApplication
@EnableApiTester(globalVariables = {
    @EnableApiTester.Variable(key = "token", value = "eyJhbGciOi..."),
    @EnableApiTester.Variable(key = "apiKey", value = "sk-abc123")
})
public class MyApplication { }
```

These are injected into the collection's `variable` array (alongside the auto-generated `baseUrl`):

```json
"variable": [
    { "key": "baseUrl", "value": "http://localhost:8080", "category": "BASE_URL" },
    { "key": "token",   "value": "eyJhbGciOi..." },
    { "key": "apiKey",  "value": "sk-abc123" }
]
```

### 15. Ignoring Parameters

Spring framework types are automatically ignored: `HttpServletRequest`, `HttpServletResponse`, `HttpSession`, `Principal`, `BindingResult`, `Model`, `RedirectAttributes`.

To ignore additional types, use `ignoreParams`:

```java
@ApiTester(ignoreParams = { Authentication.class, MultipartFile.class })
public class MyController { ... }
```

### 16. Disabling Generation

```properties
apitester.enabled=false
```

Or simply remove `@EnableApiTester` from your main class.

---

## Example Project

A complete demo application is included in the [`example/`](example/) directory. It demonstrates:

- JWT authentication with `AuthController`
- CRUD controllers (`ProductManagementController`, `MerchantManagementController`) nested under a `Management` parent folder
- Controllers merging under shared parent folders
- Real DTOs with request body introspection
- Custom output path (`document/postman_collection.json`)

**Run the example:**

```bash
cd example
../gradlew bootRun
```

The generated Postman collection is written to `example/document/postman_collection.json`.

---

## Building from Source

**Prerequisites:** Java 17+

```bash
git clone https://github.com/anomalyco/apitester-generator-spring.git
cd apitester-generator-spring

# Build the library
./gradlew bootJar

# Run tests
./gradlew test

# Build and publish to local Maven repo (optional)
./gradlew publishToMavenLocal
```

The output JARs are in `build/libs/`:
- `apitester-generator-spring-0.0.1-plain.jar` — plain JAR (recommended for library consumers)
- `apitester-generator-spring-0.0.1-boot.jar` — executable Spring Boot JAR

---

## License

This project is open source. See the repository for license details.
