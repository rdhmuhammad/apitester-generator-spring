package com.apitester.generator;

public class TestDtoClasses {

    public static class SimpleQueryDto {
        private String name;
        private String email;
        private int age;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }

    @com.fasterxml.jackson.databind.annotation.JsonNaming(
            com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class SnakeCaseDto {
        private String userId;
        private String firstName;
        public String getUserId() { return userId; }
        public String getFirstName() { return firstName; }
    }

    public static class CustomSetterDto {
        private String userId;

        public String getUserId() { return userId; }
        public void setUser_id(String userId) { this.userId = userId; }
    }

    public static class JsonPropertyDto {
        @com.fasterxml.jackson.annotation.JsonProperty("custom_name")
        private String fieldName;

        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    }

    public static class MixedDto {
        @com.fasterxml.jackson.annotation.JsonProperty("user_id")
        private String userId;
        private String email;
        private boolean active;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}
