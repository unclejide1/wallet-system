package com.example.test.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Error details for a failed API response")
public class ApiError {

    @Schema(description = "Field associated with the error when applicable", example = "email")
    private final String field;

    @Schema(description = "Human-readable error message", example = "Email address is required")
    private final String message;

    public static ApiError of(String field, String message) {
        return ApiError.builder()
                .field(field)
                .message(message)
                .build();
    }

    public static ApiError global(String message) {
        return of(null, message);
    }
}
