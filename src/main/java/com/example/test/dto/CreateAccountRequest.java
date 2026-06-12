package com.example.test.dto;

import com.example.test.model.enums.WalletType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAccountRequest {
    @NotNull(message = "Wallet type is required")
    private WalletType walletType;
}
