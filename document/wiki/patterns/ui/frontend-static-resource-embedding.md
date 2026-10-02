# Frontend Static Resource Embedding and SPA Routing

**Summary**: Architectural pattern for packaging the Vite React frontend dist inside a Spring Boot starter JAR (META-INF/resources/apitester/), configuring SPA history routing fallback, and synchronizing dynamic environment variables.
**Sources**: `document/raw/decision/endpoint_spec.md`
**Last updated**: 2026-09-30.

---

## Overview

The `apitester-generator-spring` project is designed as both a standalone runner and an embeddable Spring Boot starter library. When embedded as a dependency in another host application, it must serve the frontend web client without conflicting with the host application's own root static files (such as `static/index.html`) or API routes.

## Classpath Placement Standard

Web assets built by Vite are placed under the Servlet 3.0+ web resource directory:

```
src/main/resources/
└── META-INF/resources/apitester/
    ├── index.html
    ├── vite.svg
    ├── app.ico
    ├── .env
    └── assets/
        ├── index-*.js
        ├── index-*.css
        └── vendor-*.js
```

Placing files under `META-INF/resources/apitester/` guarantees that:
1. When packaged into a `.jar`, Spring Boot's resource loader automatically discovers and serves them from the classpath.
2. The assets are namespaced under `/apitester/`, avoiding namespace collision with the host application's static files.

## Single Page Application (SPA) Routing & Fallback

The client application is built with Vite using relative paths (`base: './'`) and `react-router-dom` for client-side routing.

Spring MVC is configured via `WebMvcConfigurer` to handle two critical requirements:
1. **Trailing Slash Redirection**: Visiting `/apitester` redirects to `/apitester/` (HTTP 302). This ensures the browser evaluates relative paths (e.g. `./assets/...`) relative to `/apitester/` rather than the host root `/`.
2. **HTML5 History Fallback**: Non-file URLs under `/apitester/**` (e.g. `/apitester/collection/uuid-123`) resolve to `index.html` via a custom `PathResourceResolver` so client-side navigation survives browser reloads.

```java
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(annotation = EnableApiTester.class)
public static class ApiTesterWebConfiguration implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/apitester", "/apitester/");
        registry.addViewController("/apitester/").setViewName("forward:/apitester/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/apitester/**")
                .addResourceLocations("classpath:/META-INF/resources/apitester/", "classpath:/static/apitester/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        return (requestedResource.exists() && requestedResource.isReadable())
                                ? requestedResource
                                : location.createRelative("index.html");
                    }
                });
    }
}
```

## Route Prefix & Dynamic `.env` Synchronization

To avoid colliding with existing `/api/v1/...` routes in target host applications:
1. **Isolated Route Mapping**: Controllers are mapped exclusively to `/apitester/api/v1/...` prefix:
   - `ApiTesterCollectionController`: `@RequestMapping("/apitester/api/v1/collection")`
   - `ApiTesterAuthController`: `@RequestMapping("/apitester/api/v1/auth")`
   - Endpoint aliases: `@GetMapping({"/read", "/read-selected"})` and `@PutMapping({"/write", "/write-selected"})` map unparameterized calls from the frontend directly to the active collection.
2. **Dynamic `.env` Endpoint**: On initialization, Axios calls `fetch("/.env")` to discover runtime configuration. `ApiTesterUiController` responds to both `/.env` and `/apitester/.env` with:
   ```
   PORT=<server-port>
   VITE_API_URL=/apitester/api/v1
   ```

## Automated Build Synchronization

In `build.gradle`, Gradle copy tasks can sync build outputs from the frontend directory:

```groovy
task copyFrontendDist(type: Copy) {
    from "${rootDir}/../apitester/frontend/dist"
    into "${buildDir}/resources/main/META-INF/resources/apitester"
}
processResources.dependsOn copyFrontendDist
```

## Related pages

- [[decisions/endpoint-spec-and-architecture]]
- [[concepts/response-envelope]]
- [[patterns/collection/collection-file-watcher]]
- [[patterns/auth/cookie-jwt-auth]]
