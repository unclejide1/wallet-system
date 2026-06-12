package com.example.test.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Health check payload")
public record HealthStatusResponse(
        @Schema(description = "Application name", example = "wallet")
        String application,
        @Schema(description = "Current service status", example = "UP")
        String status
) {
}
