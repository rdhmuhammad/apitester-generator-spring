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
public class PostmanRequest {

    @Builder.Default
    private String funIden = "";

    private String method;

    @Builder.Default
    private List<PostmanMapItem> header = new ArrayList<>();

    private PostmanBody body;

    private PostmanUrl url;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PostmanBody {

        private String mode;
        private String raw;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<PostmanMapItem> formdata;
    }
}
