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
public class PostmanResponse {

    @Builder.Default
    private String funIden = "";

    private String name;
    private String status;
    private int code;

    @Builder.Default
    private String _postman_previewlanguage = "Text";

    @Builder.Default
    private List<PostmanMapItem> header = new ArrayList<>();

    private String body;
}
