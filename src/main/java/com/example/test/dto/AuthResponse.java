package com.example.test.dto;


import lombok.Builder;
import lombok.Getter;
import java.util.Set;

@Getter
@Builder
public class AuthResponse {
    private final String token;
    private final String userRef;
    private final String email;
    private final Set<String> roles;
}
