package com.example.test.service;

import com.example.test.dto.AuthResponse;
import com.example.test.dto.LoginRequest;
import com.example.test.dto.OnboardingRequest;
import com.example.test.dto.UserResponse;

public interface AuthService {
    UserResponse onboardUser(OnboardingRequest request);
    AuthResponse login(LoginRequest request);
}