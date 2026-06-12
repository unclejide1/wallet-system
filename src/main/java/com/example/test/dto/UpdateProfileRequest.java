package com.example.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @NotBlank(message = "First Name Cannot Be Left Blank")
    private String firstName;

    @NotBlank(message = "Last Name Cannot Be Left Blank")
    private String lastName;

    private String otherName;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^0[0-9]{10}$", message = "Invalid Nigerian phone number format. Must be 11 digits starting with 0")
    private String phoneNumber;

    @Pattern(regexp = "^0[0-9]{10}$", message = "Invalid alternative phone number format")
    private String alternativePhoneNumber;
}