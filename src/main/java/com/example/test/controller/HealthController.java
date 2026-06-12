package com.example.test.controller;


import com.example.test.common.api.ApiResponse;
import com.example.test.dto.HealthStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "System", description = "System endpoints")
public class HealthController {

    @GetMapping
    @Operation(summary = "Check service health")
    public ResponseEntity<ApiResponse<HealthStatusResponse>> health(HttpServletRequest request) {
        HealthStatusResponse payload = new HealthStatusResponse("wallet", "UP");

        return ResponseEntity.ok(ApiResponse.success(
                "Wallet service is running",
                payload,
                request.getRequestURI()
        ));
    }
}
