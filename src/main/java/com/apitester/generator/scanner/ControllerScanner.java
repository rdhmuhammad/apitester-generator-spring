package com.apitester.generator.scanner;

import com.apitester.generator.annotation.ApiTester;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Stream;

@Slf4j
public class ControllerScanner {

    public List<ControllerInfo> scan(ApplicationContext applicationContext) {
        List<ControllerInfo> controllers = new ArrayList<>();

        Map<String, Object> controllerBeans = applicationContext.getBeansWithAnnotation(RestController.class);

        for (Map.Entry<String, Object> entry : controllerBeans.entrySet()) {
            Class<?> controllerClass = entry.getValue().getClass();

            if (isProxyClass(controllerClass)) {
                controllerClass = controllerClass.getSuperclass();
            }

            ApiTester apiTester = controllerClass.getAnnotation(ApiTester.class);
            if (apiTester == null) continue;

            String controllerName = deriveControllerName(controllerClass.getSimpleName());
            String[] folders = apiTester.folders();
            Class<?>[] ignoreParams = apiTester.ignoreParams();
            Map<String, String> globalHeadersMap = new HashMap<>();
            Optional<ApiTester.Headers[]> globalHeaders = Optional.ofNullable(apiTester.globalHeaders());
            globalHeaders.ifPresent(headers -> Stream.of(headers).forEach(h -> globalHeadersMap.put(h.key(), h.value())));

            controllers.add(new ControllerInfo(
                    controllerClass, controllerName,
                    folders, globalHeadersMap, ignoreParams));
            log.info("Scanned controller: {} -> folder: {}, parent folders: {}",
                    controllerClass.getSimpleName(), controllerName, folders);
        }

        return controllers;
    }

    private String deriveControllerName(String simpleName) {
        if (simpleName.endsWith("Controller")) {
            String substring = simpleName.substring(0, simpleName.length() - "Controller".length());
            return substring.replaceAll("(?<!^)(?=[A-Z])", " ");
        }
        return simpleName;
    }

    private boolean isProxyClass(Class<?> clazz) {
        return clazz.getSimpleName().contains("$$");
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class ControllerInfo {
        private final Class<?> controllerClass;
        private final String controllerFolderName;
        private final String[] parentFolders;
        private final Map<String, String> globalHeaders;
        private final Class<?>[] ignoreParams;
    }
}
