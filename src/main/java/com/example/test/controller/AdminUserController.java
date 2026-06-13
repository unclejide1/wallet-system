package com.example.test.controller;


import com.example.test.common.api.ApiResponse;
import com.example.test.dto.UserResponse;
import com.example.test.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "3. Administrative Directory System Controls", description = "High privilege endpoints for managing user directory records")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Fetch a paginated block slice collection containing system wide active user records (Admin Only)")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be greater than or equal to 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be at least 1") @Max(value = 50, message = "size must not be greater than 50") int size,
            HttpServletRequest request
    ) {
        Page<UserResponse> data = adminUserService.getAllUsers(page, size);
        return ResponseEntity.ok(ApiResponse.success("System directory ledger context slice loaded successfully", data, request.getRequestURI()));
    }

    @DeleteMapping(value = "/{userRef}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-archive an existing user account profile via hidden public token keys (Admin Only)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String userRef, HttpServletRequest request) {
        adminUserService.softDeleteUserByRef(userRef);
        return ResponseEntity.ok(ApiResponse.success("Account directory mapping node structural soft-drop succeeded", null, request.getRequestURI()));
    }
}
