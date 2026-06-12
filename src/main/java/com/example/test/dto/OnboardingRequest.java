package com.example.test.dto;

import com.example.test.model.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OnboardingRequest {

    @NotBlank(message = "First Name Cannot Be Left Blank")
    private String firstName;

    @NotBlank(message = "Last Name Cannot Be Left Blank")
    private String lastName;

    private String otherName;

    @NotNull(message = "Gender is required. Options: MALE, FEMALE, OTHER")
    private Gender gender;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "State of Origin is required")
    private String stateOfOrigin;

    @NotBlank(message = "Email Cannot Be Left Blank")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^0[0-9]{10}$", message = "Invalid Nigerian phone number format. Must be 11 digits starting with 0")
    private String phoneNumber;

    @Pattern(regexp = "^0[0-9]{10}$", message = "Invalid alternative phone number format")
    private String alternativePhoneNumber;
}