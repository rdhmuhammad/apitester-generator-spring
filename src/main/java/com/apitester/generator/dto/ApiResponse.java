package com.apitester.generator.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String messageTitle;
    private String message;
    private String errorServer;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .messageTitle("Success")
                .message("Success")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .messageTitle("Success")
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> successMessage(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .messageTitle("Success")
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .messageTitle("Invalid data.")
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> serverError(String message, String errorServer) {
        return ApiResponse.<T>builder()
                .success(false)
                .messageTitle("Oops, something went wrong.")
                .message(message)
                .errorServer(errorServer)
                .build();
    }
}
