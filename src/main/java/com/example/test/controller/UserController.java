package com.example.test.controller;


import com.example.test.common.api.ApiResponse;
import com.example.test.dto.UpdateProfileRequest;
import com.example.test.dto.UserResponse;
import com.example.test.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "2. Account Profile Actions", description = "Protected endpoints for standard users to manage their own profile details")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Fetch profile metadata corresponding to current caller session context")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(Principal principal, HttpServletRequest request) {
        UserResponse data = userService.getUserProfile(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Active directory user profile trace resolved", data, request.getRequestURI()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Modify current account identity configuration structures")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            Principal principal,
            @Valid @RequestBody UpdateProfileRequest updateDto,
            HttpServletRequest request
    ) {
        UserResponse data = userService.updateProfile(principal.getName(), updateDto);
        return ResponseEntity.ok(ApiResponse.success("Account configurations successfully saved", data, request.getRequestURI()));
    }


}