package com.example.test.dto;

import com.example.test.model.Account;
import com.example.test.model.Transaction;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class WalletFundingResponse {
    private final String transactionRef;
    private final String paymentReference;
    private final String accountNumber;
    private final BigDecimal amount;
    private final String status;
    private final String narration;
    private final BigDecimal availableBalance;
    private final BigDecimal ledgerBalance;
    private final Instant timestamp;

    public static WalletFundingResponse of(Transaction transaction, Account account) {
        return WalletFundingResponse.builder()
                .transactionRef(transaction.getTransactionRef())
                .paymentReference(transaction.getExternalReference())
                .accountNumber(account.getAccountNumber())
                .amount(transaction.getAmount())
                .status(transaction.getStatus().name())
                .narration(transaction.getNarration())
                .availableBalance(account.getWalletBalance().getAvailableAmount())
                .ledgerBalance(account.getWalletBalance().getLedgerAmount())
                .timestamp(transaction.getTimestamp())
                .build();
    }
}
