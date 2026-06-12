package com.example.test.dto;

import com.example.test.model.User;
import lombok.Builder;
import lombok.Getter;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Builder
public class UserResponse {
    private final String userRef; // Database ID is safely hidden
    private final String firstName;
    private final String lastName;
    private final String otherName;
    private final String gender;
    private final String address;
    private final String stateOfOrigin;
    private final String email;
    private final String phoneNumber;
    private final String alternativePhoneNumber;
    private final Set<String> roles;

    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .userRef(user.getUserRef())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .otherName(user.getOtherName())
                .gender(user.getGender().name())
                .address(user.getAddress())
                .stateOfOrigin(user.getStateOfOrigin())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .alternativePhoneNumber(user.getAlternativePhoneNumber())
                .roles(user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()))
                .build();
    }
}