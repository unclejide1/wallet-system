package com.example.test.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FundWalletRequest {

    @NotBlank(message = "Destination account number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Destination account number must be exactly 10 digits")
    private String accountNumber;

    @NotNull(message = "Funding amount is required")
    @DecimalMin(value = "1.00", message = "Minimum funding amount is 1.00 NGN")
    @Digits(integer = 16, fraction = 2, message = "Funding amount can have at most 2 decimal places")
    private BigDecimal amount;

    @Size(max = 255, message = "Narration must not exceed 255 characters")
    private String narration;

    @Size(max = 100, message = "Payment reference must not exceed 100 characters")
    private String paymentReference;
}
