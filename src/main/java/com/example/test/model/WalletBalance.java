package com.example.test.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "wallet_balances")
public class WalletBalance implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal availableAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal ledgerAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "NGN";

    public boolean hasSufficientFunds(BigDecimal amount) {
        return availableAmount.compareTo(Objects.requireNonNull(amount, "amount")) >= 0;
    }

    public void credit(BigDecimal amount) {
        BigDecimal value = Objects.requireNonNull(amount, "amount");
        availableAmount = availableAmount.add(value);
        ledgerAmount = ledgerAmount.add(value);
    }

    public void debit(BigDecimal amount) {
        BigDecimal value = Objects.requireNonNull(amount, "amount");
        availableAmount = availableAmount.subtract(value);
        ledgerAmount = ledgerAmount.subtract(value);
    }
}
