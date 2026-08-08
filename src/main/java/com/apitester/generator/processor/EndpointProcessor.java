package com.apitester.generator.processor;

import com.apitester.generator.common.ValueUtils;
import com.apitester.generator.model.PostmanMapItem;
import com.apitester.generator.model.PostmanRequest;
import com.apitester.generator.model.PostmanUrl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class EndpointProcessor {

    private static final Set<Class<?>> DEFAULT_IGNORED_TYPES = Set.of(
            javax.servlet.http.HttpServletRequest.class,
            javax.servlet.http.HttpServletResponse.class,
            javax.servlet.http.HttpSession.class,
            java.security.Principal.class,
            org.springframework.validation.BindingResult.class,
            org.springframework.ui.Model.class,
            org.springframework.web.servlet.mvc.support.RedirectAttributes.class
    );

    private final DtoAnalyzer dtoAnalyzer;
    private final ObjectMapper objectMapper;

    public EndpointProcessor(DtoAnalyzer dtoAnalyzer) {
        this.dtoAnalyzer = dtoAnalyzer;
        this.objectMapper = new ObjectMapper();
    }

    public List<EndpointInfo> processController(Class<?> controllerClass, Set<Class<?>> ignoreParams) {
        List<EndpointInfo> endpoints = new ArrayList<>();

        String classLevelPath = extractClassLevelPath(controllerClass);
        Method[] methods = controllerClass.getDeclaredMethods();

        for (Method method : methods) {
            RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
            String httpMethod = null;
            String[] methodPaths = null;

            if (requestMapping != null) {
                httpMethod = extractHttpMethod(requestMapping);
                methodPaths = requestMapping.value().length > 0 ? requestMapping.value() : new String[]{""};
            } else {
                Annotation[] annotations = method.getAnnotations();
                for (Annotation ann : annotations) {
                    String extracted = extractHttpMethodFromAnnotation(ann);
                    if (extracted != null) {
                        httpMethod = extracted;
                        methodPaths = extractPathFromAnnotation(ann);
                        break;
                    }
                }
            }

            if (httpMethod == null) continue;

            for (String methodPath : methodPaths) {
                EndpointInfo info = buildEndpointInfo(
                        controllerClass.getSimpleName(), classLevelPath,
                        methodPath, httpMethod, method, ignoreParams);

                endpoints.add(info);
            }
        }

        return endpoints;
    }

    private EndpointInfo buildEndpointInfo(
            String controllerName, String classLevelPath,
            String methodPath, String httpMethod,
            Method method, Set<Class<?>> ignoreParams) {

        Map<String, String> pathVariables = new LinkedHashMap<>();
        List<PostmanMapItem> queryParams = new ArrayList<>();
        List<PostmanMapItem> formdataParams = new ArrayList<>();
        String requestBodyJson = null;
        boolean isMultipart = false;

        for (Parameter param : method.getParameters()) {
            if (shouldIgnore(param, ignoreParams)) continue;

            PathVariable pathVar = param.getAnnotation(PathVariable.class);
            if (pathVar != null) {
                String name = pathVar.value().isEmpty() ? param.getName() : pathVar.value();
                pathVariables.put(name, param.getType().getSimpleName());
                continue;
            }

            RequestBody requestBody = param.getAnnotation(RequestBody.class);
            if (requestBody != null) {
                requestBodyJson = generateRequestBodyJson(param.getType());
                continue;
            }

            ModelAttribute modelAttr = param.getAnnotation(ModelAttribute.class);
            if (modelAttr != null) {
                isMultipart = true;
                Map<String, Class<?>> dtoParams = dtoAnalyzer.resolveQueryParams(param.getType());
                for (Map.Entry<String, Class<?>> entry : dtoParams.entrySet()) {
                    formdataParams.add(PostmanMapItem.builder()
                            .key(entry.getKey())
                            .value(String.valueOf(dtoAnalyzer.generateExampleValue(entry.getValue())))
                            .type("text")
                            .build());
                }
                continue;
            }

            RequestPart requestPart = param.getAnnotation(RequestPart.class);
            if (requestPart != null || MultipartFile.class.isAssignableFrom(param.getType())) {
                isMultipart = true;
                String name = (requestPart != null && !requestPart.value().isEmpty())
                        ? requestPart.value() : param.getName();
                formdataParams.add(PostmanMapItem.builder()
                        .key(name)
                        .type("file")
                        .src("")
                        .build());
                continue;
            }

            RequestParam requestParam = param.getAnnotation(RequestParam.class);
            if (requestParam != null) {
                String name = requestParam.value().isEmpty() ? param.getName() : requestParam.value();
                PostmanMapItem item = PostmanMapItem.builder()
                        .key(name)
                        .value(String.valueOf(dtoAnalyzer.generateExampleValue(param.getType())))
                        .type("text")
                        .build();
                if (isMultipart) {
                    formdataParams.add(item);
                } else {
                    queryParams.add(item);
                }
                continue;
            }

            if (!isSimpleType(param.getType()) && !isStandardLibraryType(param.getType())) {
                Map<String, Class<?>> dtoParams = dtoAnalyzer.resolveQueryParams(param.getType());
                for (Map.Entry<String, Class<?>> entry : dtoParams.entrySet()) {
                    PostmanMapItem item = PostmanMapItem.builder()
                            .key(entry.getKey())
                            .value(String.valueOf(dtoAnalyzer.generateExampleValue(entry.getValue())))
                            .type("text")
                            .build();
                    if (isMultipart) {
                        formdataParams.add(item);
                    } else {
                        queryParams.add(item);
                    }
                }
                continue;
            }

            PostmanMapItem item = PostmanMapItem.builder()
                    .key(param.getName())
                    .value(String.valueOf(dtoAnalyzer.generateExampleValue(param.getType())))
                    .type("text")
                    .build();
            if (isMultipart) {
                formdataParams.add(item);
            } else {
                queryParams.add(item);
            }
        }

        String fullPath = buildFullPath(classLevelPath, methodPath);

        PostmanUrl url = PostmanUrl.builder()
                .raw(buildRawUrl(fullPath, queryParams, pathVariables))
                .host(List.of("{{baseUrl}}"))
                .path(buildPathSegments(fullPath, pathVariables.keySet()))
                .query(queryParams.isEmpty() ? null : queryParams)
                .build();

        PostmanRequest request = PostmanRequest.builder()
                .method(httpMethod)
                .url(url)
                .build();

        if (requestBodyJson != null) {
            request.setBody(PostmanRequest.PostmanBody.builder()
                    .mode("raw")
                    .raw(requestBodyJson)
                    .build());
            request.getHeader().add(new PostmanMapItem("Content-Type", "application/json", ""));
        } else if (isMultipart) {
            request.setBody(PostmanRequest.PostmanBody.builder()
                    .mode("formdata")
                    .formdata(formdataParams)
                    .build());
        }

        return EndpointInfo.builder()
                .name(ValueUtils.CemalToWords(method.getName()))
                .request(request)
                .build();
    }

    private String buildEndpointName(String methodPath, String httpMethod) {
        if (methodPath == null || methodPath.isEmpty() || methodPath.equals("/")) {
            return httpMethod + " Root";
        }
        String name = methodPath.replaceAll("^/|/$", "");
        name = name.replace('/', ' ').replace('-', ' ').replace('_', ' ');
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1));
                }
            }
        }
        return sb.toString();
    }

    String buildFullPath(String classLevelPath, String methodPath) {
        String classPath = classLevelPath.replaceAll("/+$", "").replaceAll("^/+", "/");
        String methodP = methodPath.replaceAll("/+$", "").replaceAll("^/+", "");
        if (classPath.isEmpty() && methodP.isEmpty()) return "/";
        if (classPath.isEmpty()) return "/" + methodP;
        if (methodP.isEmpty()) return classPath;
        return classPath + "/" + methodP;
    }

    private String buildRawUrl(String fullPath, List<PostmanMapItem> queryParams,
                                Map<String, String> pathVariables) {
        String path = fullPath;
        for (String var : pathVariables.keySet()) {
            path = path.replace("{" + var + "}", ":" + var);
        }
        if (queryParams != null && !queryParams.isEmpty()) {
            StringBuilder sb = new StringBuilder("{{baseUrl}}").append(path).append("?");
            for (int i = 0; i < queryParams.size(); i++) {
                if (i > 0) sb.append("&");
                sb.append(queryParams.get(i).getKey()).append("=").append(queryParams.get(i).getValue());
            }
            return sb.toString();
        }
        return "{{baseUrl}}" + path;
    }

    private List<String> buildPathSegments(String fullPath, Set<String> pathVariableNames) {
        List<String> segments = new ArrayList<>();
        String cleaned = fullPath.replaceAll("^/|/$", "");
        if (cleaned.isEmpty()) return segments;
        for (String segment : cleaned.split("/")) {
            String stripped = segment.replaceAll("[{}]", "");
            segments.add(stripped);
        }
        return segments;
    }

    private String generateRequestBodyJson(Class<?> type) {
        Map<String, Object> example = buildExampleJson(type);
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(example);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Map<String, Object> buildExampleJson(Class<?> type) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (Modifier.isTransient(field.getModifiers())) continue;
            map.put(field.getName(), dtoAnalyzer.generateExampleValue(field.getType()));
        }
        return map;
    }

    boolean shouldIgnore(Parameter param, Set<Class<?>> ignoreParams) {
        Class<?> paramType = param.getType();
        if (ignoreParams.contains(paramType)) return true;
        for (Class<?> ignoredType : DEFAULT_IGNORED_TYPES) {
            if (ignoredType.isAssignableFrom(paramType)) return true;
        }
        return false;
    }

    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || Number.class.isAssignableFrom(type)
                || Boolean.class == type
                || Character.class == type
                || type == java.util.Date.class
                || type == java.time.LocalDate.class
                || type == java.time.LocalDateTime.class
                || type == java.time.Instant.class;
    }

    private boolean isStandardLibraryType(Class<?> type) {
        String name = type.getName();
        return name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jakarta.");
    }

    private String extractClassLevelPath(Class<?> controllerClass) {
        RequestMapping rm = controllerClass.getAnnotation(RequestMapping.class);
        if (rm != null && rm.value().length > 0) {
            return rm.value()[0];
        }
        return "";
    }

    private String extractHttpMethod(RequestMapping mapping) {
        if (mapping.method().length > 0) {
            return mapping.method()[0].name();
        }
        return "GET";
    }

    private String extractHttpMethodFromAnnotation(Annotation ann) {
        String className = ann.annotationType().getSimpleName();
        Map<String, String> mapping = new HashMap<>();
        mapping.put("GetMapping", "GET");
        mapping.put("PostMapping", "POST");
        mapping.put("PutMapping", "PUT");
        mapping.put("DeleteMapping", "DELETE");
        mapping.put("PatchMapping", "PATCH");
        return mapping.get(className);
    }

    private String[] extractPathFromAnnotation(Annotation ann) {
        try {
            Method valueMethod = ann.annotationType().getMethod("value");
            String[] values = (String[]) valueMethod.invoke(ann);
            if (values.length > 0) return values;
        } catch (Exception ignored) {
        }
        try {
            Method pathMethod = ann.annotationType().getMethod("path");
            String[] values = (String[]) pathMethod.invoke(ann);
            if (values.length > 0) return values;
        } catch (Exception ignored) {
        }
        return new String[]{""};
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EndpointInfo {
        private String name;
        private PostmanRequest request;
    }
}
