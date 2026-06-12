package com.example.test.controller;


import com.example.test.common.api.ApiResponse;
import com.example.test.dto.AuthResponse;
import com.example.test.dto.LoginRequest;
import com.example.test.dto.OnboardingRequest;
import com.example.test.dto.UserResponse;
import com.example.test.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "1. Session Gateway", description = "Public endpoints managing registration onboard procedures and login credential verification")
public class AuthController {

    private final AuthService authService;

    @PostMapping(value = "/onboard", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Onboard a new User profile resource (Emits standard HTTP 201 + Location Header)")
    public ResponseEntity<ApiResponse<UserResponse>> onboard(
            @Valid @RequestBody OnboardingRequest request,
            HttpServletRequest httpServletRequest
    ) {
        UserResponse data = authService.onboardUser(request);

        // REST Standard compliance: Create a canonical location pointing to the resource's destination
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/admin/users/{userRef}") // Admins query directly via reference token keys
                .buildAndExpand(data.getUserRef())
                .toUri();

        return ResponseEntity.created(location)
                .body(ApiResponse.success(HttpStatus.CREATED, "User onboarding profile completed successfully", data, httpServletRequest.getRequestURI()));
    }

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Authenticate user credentials and issue security bearer JWT context scopes")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        AuthResponse data = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Authentication token validation verified successfully", data, httpServletRequest.getRequestURI()));
    }
}