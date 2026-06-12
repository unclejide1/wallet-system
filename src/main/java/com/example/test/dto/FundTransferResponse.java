package com.example.test.dto;

import com.example.test.model.Account;
import com.example.test.model.Transaction;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class FundTransferResponse {
    private final String transactionRef;
    private final String sourceAccountNumber;
    private final String destinationAccountNumber;
    private final BigDecimal amount;
    private final String status;
    private final String narration;
    private final String clientReference;
    private final BigDecimal sourceAvailableBalance;
    private final BigDecimal destinationAvailableBalance;
    private final Instant timestamp;

    public static FundTransferResponse of(Transaction debitTransaction, Account sourceAccount, Account destinationAccount) {
        return FundTransferResponse.builder()
                .transactionRef(debitTransaction.getTransactionRef())
                .sourceAccountNumber(debitTransaction.getSourceAccountNumber())
                .destinationAccountNumber(debitTransaction.getDestinationAccountNumber())
                .amount(debitTransaction.getAmount())
                .status(debitTransaction.getStatus().name())
                .narration(debitTransaction.getNarration())
                .clientReference(debitTransaction.getExternalReference())
                .sourceAvailableBalance(sourceAccount.getWalletBalance().getAvailableAmount())
                .destinationAvailableBalance(destinationAccount.getWalletBalance().getAvailableAmount())
                .timestamp(debitTransaction.getTimestamp())
                .build();
    }
}
