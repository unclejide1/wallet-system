package com.example.test.dto;

import com.example.test.model.Transaction;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class TransactionResponse {
    private final String transactionRef;
    private final String sourceAccountNumber;
    private final String destinationAccountNumber;
    private final BigDecimal amount;
    private final String type;
    private final String status;
    private final String narration;
    private final String externalReference;
    private final Instant timestamp;

    public static TransactionResponse fromEntity(Transaction transaction) {
        return TransactionResponse.builder()
                .transactionRef(transaction.getTransactionRef())
                .sourceAccountNumber(transaction.getSourceAccountNumber())
                .destinationAccountNumber(transaction.getDestinationAccountNumber())
                .amount(transaction.getAmount())
                .type(transaction.getType().name())
                .status(transaction.getStatus().name())
                .narration(transaction.getNarration())
                .externalReference(transaction.getExternalReference())
                .timestamp(transaction.getTimestamp())
                .build();
    }
}
