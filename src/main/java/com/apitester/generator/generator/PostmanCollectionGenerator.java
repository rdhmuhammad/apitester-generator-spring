package com.apitester.generator.generator;

import com.apitester.generator.config.ApiTesterProperties;
import com.apitester.generator.model.*;
import com.apitester.generator.processor.EndpointProcessor;
import com.apitester.generator.scanner.ControllerScanner;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PostmanCollectionGenerator {

    private final ApiTesterProperties properties;

    public PostmanCollectionGenerator(ApiTesterProperties properties) {
        this.properties = properties;
    }

    public PostmanCollection generate(List<ControllerScanner.ControllerInfo> controllers,
                                       EndpointProcessor endpointProcessor) {
        String collectionName = properties.getCollection().getName();
        String collectionId = UUID.nameUUIDFromBytes(("collection:" + collectionName).getBytes(StandardCharsets.UTF_8)).toString();

        PostmanCollection.PostmanInfo info = PostmanCollection.PostmanInfo.builder()
                ._postman_id(collectionId)
                .name(collectionName)
                .description(properties.getCollection().getDescription())
                .build();

        List<PostmanItem> items = new ArrayList<>();
        Map<String, PostmanItem> folderMap = new LinkedHashMap<>();

        for (ControllerScanner.ControllerInfo controller : controllers) {
            String controllerClassName = controller.getControllerClass().getSimpleName();
            String folderSeed = "controller:" + controllerClassName;
            String folderId = UUID.nameUUIDFromBytes(folderSeed.getBytes(StandardCharsets.UTF_8)).toString();

            PostmanItem controllerFolder = PostmanItem.builder()
                    .name(controller.getControllerFolderName())
                    .id(folderId)
                    .funIden(folderSeed)
                    .item(new ArrayList<>())
                    .build();

            Set<Class<?>> ignoreParams = Set.of(controller.getIgnoreParams());
            List<EndpointProcessor.EndpointInfo> endpoints =
                    endpointProcessor.processController(controller.getControllerClass(), ignoreParams);

            for (EndpointProcessor.EndpointInfo endpoint : endpoints) {
                List<PostmanMapItem> globalHeaders = controller.getGlobalHeaders()
                        .entrySet()
                        .stream()
                        .map(dt -> new PostmanMapItem(dt.getKey(), dt.getValue(), ""))
                        .collect(Collectors.toList());

                endpoint.getRequest().setHeader(Stream
                                .concat(globalHeaders.stream(), endpoint.getRequest().getHeader().stream())
                                .collect(Collectors.toList()));

                String methodName = endpoint.getMethodName() != null ? endpoint.getMethodName() : endpoint.getName();
                String endpointSeed = controllerClassName + "#" + methodName;
                String requestId = UUID.nameUUIDFromBytes(endpointSeed.getBytes(StandardCharsets.UTF_8)).toString();

                endpoint.getRequest().setFunIden(endpointSeed);

                if (endpoint.getRequest().getHeader() != null) {
                    for (PostmanMapItem header : endpoint.getRequest().getHeader()) {
                        if (header.getKey() != null && (header.getId() == null || header.getId().isEmpty())) {
                            header.setId(UUID.nameUUIDFromBytes((endpointSeed + ":header:" + header.getKey()).getBytes(StandardCharsets.UTF_8)).toString());
                        }
                    }
                }

                if (endpoint.getRequest().getUrl() != null && endpoint.getRequest().getUrl().getQuery() != null) {
                    for (PostmanMapItem query : endpoint.getRequest().getUrl().getQuery()) {
                        if (query.getKey() != null && (query.getId() == null || query.getId().isEmpty())) {
                            query.setId(UUID.nameUUIDFromBytes((endpointSeed + ":query:" + query.getKey()).getBytes(StandardCharsets.UTF_8)).toString());
                        }
                    }
                }

                String responseId = UUID.nameUUIDFromBytes((endpointSeed + ":response:200").getBytes(StandardCharsets.UTF_8)).toString();

                PostmanItem requestItem = PostmanItem.builder()
                        .name(endpoint.getName())
                        .funIden(endpointSeed)
                        .request(endpoint.getRequest())
                        .id(requestId)
                        .response(List.of(PostmanResponse.builder()
                                .id(responseId)
                                .funIden(endpointSeed)
                                .name("Success")
                                .status("OK")
                                .code(200)
                                .build()))
                        .build();
                controllerFolder.getItem().add(requestItem);
            }

            String[] parentFolders = controller.getParentFolders();
            if (parentFolders.length == 0) {
                items.add(controllerFolder);
            } else {
                PostmanItem parent = ensureParentFolders(items, folderMap, parentFolders);
                parent.getItem().add(controllerFolder);
            }
        }

        List<PostmanMapItem> globalVariables = new ArrayList<>();
        if (Objects.nonNull(properties.getCollection().getBaseUrl())) {
            globalVariables.add(PostmanMapItem.builder()
                    .id(UUID.nameUUIDFromBytes("variable:baseUrl".getBytes(StandardCharsets.UTF_8)).toString())
                    .key("baseUrl")
                    .category("BASE_URL")
                    .value(properties.getCollection().getBaseUrl())
                    .build());
        }
        if (properties.getGlobalVariables() != null) {
            for (PostmanMapItem var : properties.getGlobalVariables()) {
                if (var.getId() == null || var.getId().isEmpty()) {
                    var.setId(UUID.nameUUIDFromBytes(("variable:" + var.getKey()).getBytes(StandardCharsets.UTF_8)).toString());
                }
                globalVariables.add(var);
            }
        }

        return PostmanCollection.builder()
                .info(info)
                .item(items)
                .variable(globalVariables)
                .build();
    }

    private PostmanItem ensureParentFolders(List<PostmanItem> items,
                                             Map<String, PostmanItem> folderMap,
                                             String[] parentFolders) {
        String currentPath = "";

        for (int i = 0; i < parentFolders.length; i++) {
            String part = parentFolders[i];
            String pathKey = currentPath.isEmpty() ? part : currentPath + "/" + part;

            if (!folderMap.containsKey(pathKey)) {
                String folderId = UUID.nameUUIDFromBytes(("folder:" + pathKey).getBytes(StandardCharsets.UTF_8)).toString();
                PostmanItem folder = PostmanItem.builder()
                        .name(part)
                        .id(folderId)
                        .funIden("folder:" + pathKey)
                        .item(new ArrayList<>())
                        .build();

                if (i == 0) {
                    items.add(folder);
                } else {
                    folderMap.get(currentPath).getItem().add(folder);
                }

                folderMap.put(pathKey, folder);
            }

            currentPath = pathKey;
        }

        return folderMap.get(currentPath);
    }
}
