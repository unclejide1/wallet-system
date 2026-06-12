package com.example.test.service;

import com.example.test.dto.UserResponse;
import org.springframework.data.domain.Page;

public interface AdminUserService {
    Page<UserResponse> getAllUsers(int page, int size);
    void softDeleteUserByRef(String userRef);
}
