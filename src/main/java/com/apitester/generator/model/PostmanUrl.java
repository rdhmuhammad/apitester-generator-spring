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
public class PostmanUrl {

    private String raw;

    @Builder.Default
    private List<String> host = new ArrayList<>();

    @Builder.Default
    private List<String> path = new ArrayList<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<PostmanQueryParam> query;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PostmanQueryParam {

        @Builder.Default
        private String id = "";

        private String key;
        private String value;
    }
}
