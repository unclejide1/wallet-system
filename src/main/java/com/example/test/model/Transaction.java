package com.example.test.model;


import com.example.test.model.enums.TransactionStatus;
import com.example.test.model.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_transactions_source_account_timestamp", columnList = "source_account_number, timestamp"),
                @Index(name = "idx_transactions_destination_account_timestamp", columnList = "destination_account_number, timestamp"),
                @Index(name = "idx_transactions_external_reference", columnList = "external_reference")
        }
)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_ref", unique = true, nullable = false, length = 50)
    private String transactionRef;

    @Column(name = "source_account_number", nullable = false, length = 10)
    private String sourceAccountNumber;

    @Column(name = "destination_account_number", nullable = false, length = 10)
    private String destinationAccountNumber;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionStatus status;

    private String narration;

    @Column(name = "external_reference", unique = true, length = 100)
    private String externalReference;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;
}
