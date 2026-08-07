# AGENTS.md

## Project Overview

**apitester-generator-spring** — Spring Boot API tester/generator application.

- **Java:** 17
- **Spring Boot:** 2.7.17
- **Build:** Gradle 8.5 (Groovy DSL)
- **Group:** `com.github`
- **Base package:** `com.apitester.generator-spring`

## Commands

| Action | Command                 |
|---|-------------------------|
| Build | `.\gradlew.bat bootJar`  |
| Test | `.\gradlew.bat test`    |
| Run | `.\gradlew.bat bootRun` |
| Clean | `.\gradlew.bat clean`   |

No linting, checkstyle, or static analysis commands are configured.

## Key Dependencies

| Dependency | Purpose |
|---|---|
| `spring-boot-starter` | Core Spring Boot |
| `spring-boot-starter-test` | JUnit 5 + Mockito + Spring Test |
| `lombok` (compileOnly + annotationProcessor) | Boilerplate reduction |
| `modelmapper:2.3.8` | DTO/entity object mapping |
| `jackson-databind:2.13.5` | JSON serialization |
| `org.json:json:20230227` | JSON processing |
| `loki-logback-appender:1.4.0` | Loki log aggregation |

## Code Conventions

- **Lombok is required** — use `@Getter`, `@Setter`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor`, `@Slf4j` instead of manual boilerplate.
- **ModelMapper** for DTO-to-entity conversions — inject `ModelMapper` bean, don't write manual mappers.
- **Jackson** for JSON — prefer `jackson-databind` over `org.json` for new code.
- **No comments** in code unless absolutely necessary.
- `application.properties` is gitignored — must be created manually from example/env vars.
- The main class should be annotated with `@SpringBootApplication` and placed in the root package.

## Project Status

Source code has NOT been written yet. The `src/main/java/com/apitester/generator-spring/` directory is empty. This project needs:
1. A `@SpringBootApplication` main class
2. `src/main/resources/application.properties` (gitignored, create from scratch)
3. `src/test/resources/application-test.properties` (gitignored, create from scratch)
4. Controller, service, and repository layers as needed
