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

    private static final Pattern WORD_BOUNDARY = Pattern.compile("([a-z])([A-Z]+)");

    public Map<String, Class<?>> resolveQueryParams(Class<?> dtoClass) {
        Map<String, Class<?>> result = new LinkedHashMap<>();

        Class<? extends PropertyNamingStrategy> namingStrategyClass = resolveNamingStrategyClass(dtoClass);
        Field[] fields = dtoClass.getDeclaredFields();

        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (Modifier.isTransient(field.getModifiers())) continue;
            if (field.isSynthetic()) continue;

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
        switch (className) {
            case "SnakeCaseStrategy":
                return WORD_BOUNDARY.matcher(fieldName).replaceAll("$1_$2").toLowerCase();
            case "KebabCaseStrategy":
                return WORD_BOUNDARY.matcher(fieldName).replaceAll("$1-$2").toLowerCase();
            case "LowerCaseStrategy":
                return fieldName.toLowerCase();
            case "UpperCamelCaseStrategy":
                return fieldName.isEmpty() ? fieldName : Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            case "LowerCamelCaseStrategy":
                return Introspector.decapitalize(fieldName);
            default:
                return fieldName;
        }
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
