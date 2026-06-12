package com.example.test.repo;

import com.example.test.model.WalletBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletBalanceRepo extends JpaRepository<WalletBalance,Long> {
}
