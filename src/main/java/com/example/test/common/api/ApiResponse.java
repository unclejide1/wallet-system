package com.example.test.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Unified API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "Time the response was created", example = "2026-06-12T09:00:00Z")
    private final Instant timestamp;

    @Schema(description = "HTTP status code", example = "200")
    private final int status;

    @Schema(description = "Whether the request was successful", example = "true")
    private final boolean success;

    @Schema(description = "Summary message for the response", example = "Request completed successfully")
    private final String message;

    @Schema(description = "Requested path", example = "/api/v1/health")
    private final String path;

    @Schema(description = "Response payload")
    private final T data;

    @Schema(description = "Validation or processing errors")
    private final List<ApiError> errors;

    public static <T> ApiResponse<T> success(HttpStatus status, String message, T data, String path) {
        return ApiResponse.<T>builder()
                .timestamp(Instant.now())
                .status(status.value())
                .success(true)
                .message(message)
                .path(path)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data, String path) {
        return success(HttpStatus.OK, message, data, path);
    }

    public static ApiResponse<Void> error(HttpStatus status, String message, List<ApiError> errors, String path) {
        return ApiResponse.<Void>builder()
                .timestamp(Instant.now())
                .status(status.value())
                .success(false)
                .message(message)
                .path(path)
                .errors(errors)
                .build();
    }

    public static ApiResponse<Void> error(HttpStatus status, String message, String path) {
        return error(status, message, List.of(ApiError.global(message)), path);
    }
}
