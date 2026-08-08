package com.apitester.generator.generator;

import com.apitester.generator.config.ApiTesterProperties;
import com.apitester.generator.model.*;
import com.apitester.generator.processor.EndpointProcessor;
import com.apitester.generator.scanner.ControllerScanner;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PostmanCollectionGenerator {

    private final ApiTesterProperties properties;

    public PostmanCollectionGenerator(ApiTesterProperties properties) {
        this.properties = properties;
    }

    public PostmanCollection generate(List<ControllerScanner.ControllerInfo> controllers,
                                       EndpointProcessor endpointProcessor) {
        PostmanCollection.PostmanInfo info = PostmanCollection.PostmanInfo.builder()
                .name(properties.getCollection().getName())
                .description(properties.getCollection().getDescription())
                .build();

        List<PostmanItem> items = new ArrayList<>();
        Map<String, PostmanItem> folderMap = new LinkedHashMap<>();

        for (ControllerScanner.ControllerInfo controller : controllers) {
            PostmanItem controllerFolder = PostmanItem.builder()
                    .name(controller.getControllerFolderName())
                    .item(new ArrayList<>())
                    .build();

            Set<Class<?>> ignoreParams = Set.of(controller.getIgnoreParams());
            List<EndpointProcessor.EndpointInfo> endpoints =
                    endpointProcessor.processController(controller.getControllerClass(), ignoreParams);

            for (EndpointProcessor.EndpointInfo endpoint : endpoints) {
                List<PostmanMapItem> globalHeaders = controller.getGlobalHeaders()
                        .entrySet()
                        .stream()
                        .map(dt-> new PostmanMapItem( dt.getKey(), dt.getValue(), ""))
                        .toList();

                endpoint.getRequest().setHeader(Stream
                                .concat(globalHeaders.stream(), endpoint.getRequest().getHeader().stream())
                                .collect(Collectors.toList()));

                PostmanItem requestItem = PostmanItem.builder()
                        .name(endpoint.getName())
                        .request(endpoint.getRequest())
                        .id(UUID.randomUUID().toString())
                        .response(List.of(PostmanResponse.builder()
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
        if (Objects.nonNull(properties.getCollection().getBaseUrl())){
            globalVariables.add(PostmanMapItem.builder()
                            .key("baseUrl")
                            .category("BASE_URL")
                            .value(properties.getCollection().getBaseUrl())
                    .build());
        }
        if (properties.getGlobalVariables() != null) {
            globalVariables.addAll(properties.getGlobalVariables());
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
                PostmanItem folder = PostmanItem.builder()
                        .name(part)
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
