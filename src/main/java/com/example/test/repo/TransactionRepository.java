package com.example.test.repo;

import com.example.test.model.Transaction;
import com.example.test.model.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByExternalReference(String externalReference);

    @Query("""
            SELECT t
            FROM Transaction t
            WHERE (t.type = :debitType AND t.sourceAccountNumber = :accountNumber)
               OR (t.type = :creditType AND t.destinationAccountNumber = :accountNumber)
            """)
    Page<Transaction> findStatementEntries(
            @Param("accountNumber") String accountNumber,
            @Param("debitType") TransactionType debitType,
            @Param("creditType") TransactionType creditType,
            Pageable pageable
    );
}
