package com.example.test.service;

import com.example.test.dto.OnboardingRequest;
import com.example.test.dto.UpdateProfileRequest;
import com.example.test.dto.UserResponse;
import org.springframework.data.domain.Page;

public interface UserService {
    UserResponse getUserProfile(String email);
    UserResponse updateProfile(String currentEmail, UpdateProfileRequest request);
}
