package com.apitester.generator.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostmanItem {

    @Builder.Default
    private String funIden = "";

    private String name;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<PostmanItem> item;

    private PostmanRequest request;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<PostmanResponse> response;

    private String id;
}
