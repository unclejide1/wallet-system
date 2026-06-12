package com.example.test.dto;


import com.example.test.model.Account;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@Builder
public class AccountDetailsResponse {
    private final String accountRef;
    private final String accountNumber;
    private final String walletType;
    private final BigDecimal availableBalance;
    private final BigDecimal ledgerBalance;
    private final String currency;

    public static AccountDetailsResponse fromEntity(Account account) {
        return AccountDetailsResponse.builder()
                .accountRef(account.getAccountRef())
                .accountNumber(account.getAccountNumber())
                .walletType(account.getWalletType().name())
                .availableBalance(account.getWalletBalance().getAvailableAmount())
                .ledgerBalance(account.getWalletBalance().getLedgerAmount())
                .currency(account.getWalletBalance().getCurrency())
                .build();
    }
}
