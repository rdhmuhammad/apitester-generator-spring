package com.apitester.generator.processor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.extern.slf4j.Slf4j;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
public class DtoAnalyzer {

    private static final Pattern CAMEL_TO_SNAKE = Pattern.compile("([a-z])([A-Z]+)");
    private static final Pattern CAMEL_TO_KEBAB = Pattern.compile("([a-z])([A-Z]+)");

    public Map<String, Class<?>> resolveQueryParams(Class<?> dtoClass) {
        Map<String, Class<?>> result = new LinkedHashMap<>();

        Class<? extends PropertyNamingStrategy> namingStrategyClass = resolveNamingStrategyClass(dtoClass);
        Field[] fields = dtoClass.getDeclaredFields();

        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (Modifier.isTransient(field.getModifiers())) continue;

            String paramName = resolveFieldParamName(field, namingStrategyClass, dtoClass);
            result.put(paramName, field.getType());
        }

        return result;
    }

    private String resolveFieldParamName(Field field, Class<? extends PropertyNamingStrategy> namingStrategyClass,
                                          Class<?> dtoClass) {
        String fieldName = field.getName();

        JsonProperty jsonProp = field.getAnnotation(JsonProperty.class);
        if (jsonProp != null && !jsonProp.value().isEmpty()) {
            return jsonProp.value();
        }

        String customSetterName = deriveFromCustomSetter(field, dtoClass);
        if (customSetterName != null) {
            return customSetterName;
        }

        return applyNamingStrategy(fieldName, namingStrategyClass);
    }

    private String deriveFromCustomSetter(Field field, Class<?> dtoClass) {
        String fieldName = field.getName();
        String standardSetterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        Method customSetter = null;

        for (Method method : dtoClass.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) continue;
            if (method.getParameterCount() != 1) continue;
            if (!method.getName().startsWith("set")) continue;
            if (!void.class.equals(method.getReturnType())
                    && !method.getReturnType().equals(method.getDeclaringClass())) continue;
            if (!method.getParameterTypes()[0].isAssignableFrom(field.getType())
                    && !field.getType().isAssignableFrom(method.getParameterTypes()[0])) continue;

            if (!method.getName().equals(standardSetterName)) {
                customSetter = method;
                break;
            }
        }

        if (customSetter != null) {
            return Introspector.decapitalize(customSetter.getName().substring(3));
        }

        return null;
    }

    Class<? extends PropertyNamingStrategy> resolveNamingStrategyClass(Class<?> dtoClass) {
        JsonNaming jsonNaming = dtoClass.getAnnotation(JsonNaming.class);
        if (jsonNaming == null) return null;
        return jsonNaming.value();
    }

    String applyNamingStrategy(String fieldName, Class<? extends PropertyNamingStrategy> strategyClass) {
        if (strategyClass == null) return fieldName;

        String className = strategyClass.getSimpleName();

        if ("SnakeCaseStrategy".equals(className)) {
            return camelToSnake(fieldName);
        }
        if ("KebabCaseStrategy".equals(className)) {
            return camelToKebab(fieldName);
        }
        if ("LowerCaseStrategy".equals(className)) {
            return fieldName.toLowerCase();
        }
        if ("UpperCamelCaseStrategy".equals(className)) {
            if (fieldName.isEmpty()) return fieldName;
            return Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        }
        if ("LowerCamelCaseStrategy".equals(className)) {
            return Introspector.decapitalize(fieldName);
        }

        return fieldName;
    }

    private String camelToSnake(String input) {
        String result = CAMEL_TO_SNAKE.matcher(input).replaceAll("$1_$2");
        return result.toLowerCase();
    }

    private String camelToKebab(String input) {
        String result = CAMEL_TO_KEBAB.matcher(input).replaceAll("$1-$2");
        return result.toLowerCase();
    }

    public Object generateExampleValue(Class<?> type) {
        if (type == String.class) return "";
        if (type == Integer.class || type == int.class) return 0;
        if (type == Long.class || type == long.class) return 0L;
        if (type == Double.class || type == double.class) return 0.0;
        if (type == Float.class || type == float.class) return 0.0f;
        if (type == Boolean.class || type == boolean.class) return false;
        if (type == Short.class || type == short.class) return 0;
        if (type == Byte.class || type == byte.class) return 0;
        if (type == Character.class || type == char.class) return "";
        return "";
    }
}
