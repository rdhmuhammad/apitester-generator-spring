package com.apitester.generator.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostmanMapItem {
    @Builder.Default
    private String id = "";

    private String key;
    private String value;
    private String src;

    private String type;

    private String category;

    private String description;

    public PostmanMapItem(String key, String value, String description) {
        this.key = key;
        this.value = value;
        this.description = description;
    }
}
