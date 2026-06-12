package com.example.test.model;

import com.example.test.model.enums.WalletType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "accounts",
        indexes = {
                @Index(name = "idx_accounts_user_id", columnList = "user_id")
        },
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "wallet_type"})
)
public class Account implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_ref", unique = true, nullable = false, updatable = false, length = 50)
    private String accountRef;

    @Column(name = "account_number", unique = true, nullable = false, length = 10)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_type", nullable = false, length = 20)
    private WalletType walletType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "wallet_balance_id", referencedColumnName = "id")
    private WalletBalance walletBalance;

    @PrePersist
    public void initAccountMetadata() {
        if (this.accountRef == null) {
            this.accountRef = "acc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
    }
}
