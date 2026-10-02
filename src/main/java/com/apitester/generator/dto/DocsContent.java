package com.apitester.generator.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocsContent {

    private CollectionInfo info;

    @Builder.Default
    private List<CollectionItem> item = new ArrayList<>();

    private CollectionAuth auth;

    @Builder.Default
    private List<CollectionVar> variable = new ArrayList<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<CollectionEvent> event;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CollectionInfo {

        @JsonProperty("_postman_id")
        private String postmanId;

        private String name;

        @Builder.Default
        private String description = "";

        @Builder.Default
        private String schema = "https://schema.getpostman.com/json/collection/v2.1.0/collection.json";

        @JsonProperty("_postman_id")
        public String getPostmanId() {
            return postmanId;
        }

        @JsonProperty("_postman_id")
        public void setPostmanId(String postmanId) {
            this.postmanId = postmanId;
        }

        @JsonIgnore
        public String get_postman_id() {
            return postmanId;
        }

        @JsonIgnore
        public void set_postman_id(String postmanId) {
            this.postmanId = postmanId;
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CollectionItem {

        @Builder.Default
        private String funIden = "";

        private String name;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<CollectionItem> item;

        private Request request;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<CollectionResponse> response;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<CollectionEvent> event;

        private String id;
        private String description;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CollectionResponse {

        private String id;
        private String name;
        private Request originalRequest;
        private String status;
        private Integer code;

        @JsonProperty("_postman_previewlanguage")
        private String previewLanguage;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @Builder.Default
        private List<Header> header = new ArrayList<>();

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<ResponseCookie> cookie;

        private String body;

        @JsonProperty("_postman_previewlanguage")
        public String getPreviewLanguage() {
            return previewLanguage;
        }

        @JsonProperty("_postman_previewlanguage")
        public void setPreviewLanguage(String previewLanguage) {
            this.previewLanguage = previewLanguage;
        }

        @JsonIgnore
        public String get_postman_previewlanguage() {
            return previewLanguage;
        }

        @JsonIgnore
        public void set_postman_previewlanguage(String previewLanguage) {
            this.previewLanguage = previewLanguage;
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseCookie {

        private String key;
        private String value;
        private String domain;
        private String path;
        private Boolean secure;

        @JsonProperty("httpOnly")
        private Boolean httpOnly;

        @JsonProperty("httpOnly")
        public Boolean getHttpOnly() {
            return httpOnly;
        }

        @JsonProperty("httpOnly")
        public void setHttpOnly(Boolean httpOnly) {
            this.httpOnly = httpOnly;
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Request {

        @Builder.Default
        private String funIden = "";

        private String method;
        private ReqAuth auth;

        @Builder.Default
        private List<Header> header = new ArrayList<>();

        private RequestBody body;
        private RequestUrl url;
        private String description;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReqAuth {

        @Builder.Default
        private String type = "bearer";

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<Property> bearer;

        private String authSource;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {

        private String id;
        private String key;
        private String value;
        private String description;
        private Boolean disabled;
        private String type;

        public Header(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public Header(String id, String key, String value) {
            this.id = id;
            this.key = key;
            this.value = value;
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RequestBody {

        private String mode;
        private String raw;

        @JsonProperty("formdata")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<Property> formdata;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RequestUrl {

        private String raw;

        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        @Builder.Default
        private List<String> host = new ArrayList<>();

        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        @Builder.Default
        private List<String> path = new ArrayList<>();

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<Property> query;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CollectionAuth {

        @Builder.Default
        private String type = "bearer";

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<Property> bearer;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Property {

        private String id;
        private String key;
        private String value;
        private String type;
        private String src;
        private String description;
        private Boolean disabled;

        public Property(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public Property(String id, String key, String value) {
            this.id = id;
            this.key = key;
            this.value = value;
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CollectionEvent {

        private String listen;
        private EventScript script;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EventScript {

        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        @Builder.Default
        private List<String> exec = new ArrayList<>();

        private String type;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CollectionVar {

        private String id;
        private String key;
        private String category;
        private String value;
        private String type;
        private String description;
        private Boolean disabled;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreateCollectionRequest {

        private String name;
        private String path;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UpdateCollectionRequest {

        private String name;
        private String path;
    }
}
