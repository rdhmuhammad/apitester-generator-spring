package com.apitester.generator.model;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class PostmanCollection {

    private PostmanInfo info;

    @Builder.Default
    private List<PostmanItem> item = new ArrayList<>();

    private List<PostmanMapItem> variable = new ArrayList<>();

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PostmanInfo {

        @Builder.Default
        private String _postman_id = "";

        private String name;

        @Builder.Default
        private String description = "";

        @Builder.Default
        private String schema = "https://schema.getpostman.com/json/collection/v2.1.0/collection.json";
    }
}
