package com.example.test.service.impl;

import com.example.test.common.exception.ApiException;
import com.example.test.dto.AccountDetailsResponse;
import com.example.test.dto.CreateAccountRequest;
import com.example.test.dto.FundTransferRequest;
import com.example.test.dto.FundTransferResponse;
import com.example.test.dto.FundWalletRequest;
import com.example.test.dto.TransactionResponse;
import com.example.test.dto.WalletFundingResponse;
import com.example.test.model.Account;
import com.example.test.model.Transaction;
import com.example.test.model.User;
import com.example.test.model.WalletBalance;
import com.example.test.model.enums.TransactionStatus;
import com.example.test.model.enums.TransactionType;
import com.example.test.model.enums.WalletType;
import com.example.test.repo.AccountRepo;
import com.example.test.repo.TransactionRepository;
import com.example.test.repo.UserRepo;
import com.example.test.service.AccountNumberGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private AccountRepo accountRepository;

    @Mock
    private UserRepo userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Test
    void createWalletAccountCreatesZeroBalanceWallet() {
        User user = createUser(1L, "owner@example.com", "usr_owner123456");
        CreateAccountRequest request = new CreateAccountRequest();
        request.setWalletType(WalletType.SAVINGS);

        when(userRepository.findByEmailForUpdate("owner@example.com")).thenReturn(Optional.of(user));
        when(accountRepository.existsByUserIdAndWalletType(1L, WalletType.SAVINGS)).thenReturn(false);
        when(accountNumberGenerator.generateNextAccountNumber()).thenReturn("1000000001");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(10L);
            account.setAccountRef("acc_wallet1234");
            return account;
        });

        AccountDetailsResponse response = walletService.createWalletAccount("Owner@Example.com", request);

        assertThat(response.getAccountNumber()).isEqualTo("1000000001");
        assertThat(response.getWalletType()).isEqualTo("SAVINGS");
        assertThat(response.getAvailableBalance()).isEqualByComparingTo("0.00");
        assertThat(response.getLedgerBalance()).isEqualByComparingTo("0.00");

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getUser()).isEqualTo(user);
        assertThat(savedAccount.getWalletBalance().getAvailableAmount()).isEqualByComparingTo("0.00");
        assertThat(savedAccount.getWalletBalance().getLedgerAmount()).isEqualByComparingTo("0.00");
        assertThat(savedAccount.getWalletBalance().getCurrency()).isEqualTo("NGN");
    }

    @Test
    void fundWalletCreditsOwnedWalletAndCreatesTransaction() {
        Account account = createAccount("1000000001", "owner@example.com", "100.00", WalletType.SAVINGS);
        FundWalletRequest request = new FundWalletRequest();
        request.setAccountNumber("1000000001");
        request.setAmount(new BigDecimal("50.00"));
        request.setNarration("Card top-up");
        request.setPaymentReference("PAY-100");

        when(accountRepository.findByAccountNumberForUpdate("1000000001")).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletFundingResponse response = walletService.fundWallet("owner@example.com", request);

        assertThat(response.getAccountNumber()).isEqualTo("1000000001");
        assertThat(response.getPaymentReference()).isEqualTo("PAY-100");
        assertThat(response.getAvailableBalance()).isEqualByComparingTo("150.00");
        assertThat(response.getLedgerBalance()).isEqualByComparingTo("150.00");

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();
        assertThat(savedTransaction.getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(savedTransaction.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(savedTransaction.getSourceAccountNumber()).isEqualTo("0000000000");
        assertThat(savedTransaction.getDestinationAccountNumber()).isEqualTo("1000000001");
        assertThat(savedTransaction.getNarration()).isEqualTo("Card top-up");
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void fundWalletReturnsExistingTransactionWhenDuplicateReferenceAppearsAfterLock() {
        Account account = createAccount("1000000001", "owner@example.com", "150.00", WalletType.SAVINGS);
        Transaction existingTransaction = Transaction.builder()
                .transactionRef("TXN_DUPLICATE")
                .sourceAccountNumber("0000000000")
                .destinationAccountNumber("1000000001")
                .amount(new BigDecimal("50.00"))
                .type(TransactionType.CREDIT)
                .status(TransactionStatus.SUCCESS)
                .narration("Card top-up")
                .externalReference("PAY-100")
                .timestamp(Instant.now())
                .build();

        FundWalletRequest request = new FundWalletRequest();
        request.setAccountNumber("1000000001");
        request.setAmount(new BigDecimal("50.00"));
        request.setNarration("Card top-up");
        request.setPaymentReference("PAY-100");

        when(transactionRepository.findByExternalReference("PAY-100"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingTransaction));
        when(accountRepository.findByAccountNumberForUpdate("1000000001")).thenReturn(Optional.of(account));
        when(accountRepository.findByAccountNumber("1000000001")).thenReturn(Optional.of(account));

        WalletFundingResponse response = walletService.fundWallet("owner@example.com", request);

        assertThat(response.getTransactionRef()).isEqualTo("TXN_DUPLICATE");
        assertThat(response.getAvailableBalance()).isEqualByComparingTo("150.00");
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transferFundsMovesBalanceAndCreatesDoubleEntryTransactions() {
        Account sourceAccount = createAccount("1000000001", "owner@example.com", "5000.00", WalletType.SAVINGS);
        Account destinationAccount = createAccount("1000000002", "beneficiary@example.com", "1000.00", WalletType.BUSINESS);

        FundTransferRequest request = new FundTransferRequest();
        request.setSourceAccountNumber("1000000001");
        request.setDestinationAccountNumber("1000000002");
        request.setAmount(new BigDecimal("1250.00"));
        request.setClientReference("TRF-100");

        when(accountRepository.findByAccountNumberForUpdate("1000000001")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumberForUpdate("1000000002")).thenReturn(Optional.of(destinationAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.findByExternalReference("TRF-100")).thenReturn(Optional.empty());

        FundTransferResponse response = walletService.transferFunds("owner@example.com", request);

        assertThat(response.getSourceAccountNumber()).isEqualTo("1000000001");
        assertThat(response.getDestinationAccountNumber()).isEqualTo("1000000002");
        assertThat(response.getAmount()).isEqualByComparingTo("1250.00");
        assertThat(response.getSourceAvailableBalance()).isEqualByComparingTo("3750.00");
        assertThat(response.getDestinationAvailableBalance()).isEqualByComparingTo("2250.00");
        assertThat(response.getNarration()).isEqualTo("Wallet transfer");
        assertThat(response.getClientReference()).isEqualTo("TRF-100");

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        List<Transaction> savedTransactions = transactionCaptor.getAllValues();
        assertThat(savedTransactions).extracting(Transaction::getType)
                .containsExactly(TransactionType.DEBIT, TransactionType.CREDIT);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void transferFundsReturnsExistingTransactionWhenDuplicateReferenceAppearsAfterLock() {
        Account sourceAccount = createAccount("1000000001", "owner@example.com", "3750.00", WalletType.SAVINGS);
        Account destinationAccount = createAccount("1000000002", "beneficiary@example.com", "2250.00", WalletType.BUSINESS);
        Transaction existingTransaction = Transaction.builder()
                .transactionRef("TXN_EXISTING-DR")
                .sourceAccountNumber("1000000001")
                .destinationAccountNumber("1000000002")
                .amount(new BigDecimal("1250.00"))
                .type(TransactionType.DEBIT)
                .status(TransactionStatus.SUCCESS)
                .narration("Wallet transfer")
                .externalReference("TRF-100")
                .timestamp(Instant.now())
                .build();

        FundTransferRequest request = new FundTransferRequest();
        request.setSourceAccountNumber("1000000001");
        request.setDestinationAccountNumber("1000000002");
        request.setAmount(new BigDecimal("1250.00"));
        request.setClientReference("TRF-100");

        when(transactionRepository.findByExternalReference("TRF-100"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingTransaction));
        when(accountRepository.findByAccountNumberForUpdate("1000000001")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumberForUpdate("1000000002")).thenReturn(Optional.of(destinationAccount));
        when(accountRepository.findByAccountNumber("1000000001")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber("1000000002")).thenReturn(Optional.of(destinationAccount));

        FundTransferResponse response = walletService.transferFunds("owner@example.com", request);

        assertThat(response.getTransactionRef()).isEqualTo("TXN_EXISTING-DR");
        assertThat(response.getSourceAvailableBalance()).isEqualByComparingTo("3750.00");
        assertThat(response.getDestinationAvailableBalance()).isEqualByComparingTo("2250.00");
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transferFundsRejectsSelfTransfer() {
        FundTransferRequest request = new FundTransferRequest();
        request.setSourceAccountNumber("1000000001");
        request.setDestinationAccountNumber("1000000001");
        request.setAmount(new BigDecimal("1250.00"));

        assertThatThrownBy(() -> walletService.transferFunds("owner@example.com", request))
                .isInstanceOf(ApiException.class)
                .extracting("status", "message")
                .containsExactly(HttpStatus.BAD_REQUEST, "Intra-account transfer to identical target coordinates prohibited");
    }

    @Test
    void transferFundsRejectsWhenBalanceIsInsufficient() {
        Account sourceAccount = createAccount("1000000001", "owner@example.com", "100.00", WalletType.SAVINGS);
        Account destinationAccount = createAccount("1000000002", "beneficiary@example.com", "1000.00", WalletType.BUSINESS);

        FundTransferRequest request = new FundTransferRequest();
        request.setSourceAccountNumber("1000000001");
        request.setDestinationAccountNumber("1000000002");
        request.setAmount(new BigDecimal("1250.00"));

        when(accountRepository.findByAccountNumberForUpdate("1000000001")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumberForUpdate("1000000002")).thenReturn(Optional.of(destinationAccount));

        assertThatThrownBy(() -> walletService.transferFunds("owner@example.com", request))
                .isInstanceOf(ApiException.class)
                .extracting("status", "message")
                .containsExactly(HttpStatus.BAD_REQUEST, "Insufficient funds to process transfer request");
    }

    @Test
    void getTransactionHistoryCapsPageSizeAndMapsTransactions() {
        Account account = createAccount("1000000001", "owner@example.com", "5000.00", WalletType.SAVINGS);
        Transaction transaction = Transaction.builder()
                .transactionRef("TXN_123-DR")
                .sourceAccountNumber("1000000001")
                .destinationAccountNumber("1000000002")
                .amount(new BigDecimal("200.00"))
                .type(TransactionType.DEBIT)
                .status(TransactionStatus.SUCCESS)
                .narration("Payment")
                .timestamp(Instant.now())
                .build();

        when(accountRepository.findByAccountNumber("1000000001")).thenReturn(Optional.of(account));
        when(transactionRepository.findStatementEntries(eq("1000000001"), eq(TransactionType.DEBIT), eq(TransactionType.CREDIT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(transaction)));

        Page<TransactionResponse> response = walletService.getTransactionHistory("owner@example.com", false, "1000000001", 0, 100);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getTransactionRef()).isEqualTo("TXN_123-DR");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository).findStatementEntries(eq("1000000001"), eq(TransactionType.DEBIT), eq(TransactionType.CREDIT), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void fundWalletRejectsWalletOwnedByAnotherUser() {
        Account account = createAccount("1000000001", "other@example.com", "100.00", WalletType.SAVINGS);
        FundWalletRequest request = new FundWalletRequest();
        request.setAccountNumber("1000000001");
        request.setAmount(new BigDecimal("50.00"));

        when(accountRepository.findByAccountNumberForUpdate("1000000001")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> walletService.fundWallet("owner@example.com", request))
                .isInstanceOf(ApiException.class)
                .extracting("status", "message")
                .containsExactly(HttpStatus.FORBIDDEN, "You can only operate on wallet accounts that belong to you");
    }

    @Test
    void fundWalletReturnsExistingTransactionForDuplicatePaymentReference() {
        Account account = createAccount("1000000001", "owner@example.com", "150.00", WalletType.SAVINGS);
        Transaction existingTransaction = Transaction.builder()
                .transactionRef("TXN_DUPLICATE")
                .sourceAccountNumber("0000000000")
                .destinationAccountNumber("1000000001")
                .amount(new BigDecimal("50.00"))
                .type(TransactionType.CREDIT)
                .status(TransactionStatus.SUCCESS)
                .narration("Card top-up")
                .externalReference("PAY-100")
                .timestamp(Instant.now())
                .build();

        FundWalletRequest request = new FundWalletRequest();
        request.setAccountNumber("1000000001");
        request.setAmount(new BigDecimal("50.00"));
        request.setNarration("Card top-up");
        request.setPaymentReference("PAY-100");

        when(transactionRepository.findByExternalReference("PAY-100")).thenReturn(Optional.of(existingTransaction));
        when(accountRepository.findByAccountNumber("1000000001")).thenReturn(Optional.of(account));

        WalletFundingResponse response = walletService.fundWallet("owner@example.com", request);

        assertThat(response.getTransactionRef()).isEqualTo("TXN_DUPLICATE");
        assertThat(response.getAvailableBalance()).isEqualByComparingTo("150.00");
        verify(accountRepository, never()).findByAccountNumberForUpdate(anyString());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transferFundsReturnsExistingTransactionForDuplicateClientReference() {
        Account sourceAccount = createAccount("1000000001", "owner@example.com", "3750.00", WalletType.SAVINGS);
        Account destinationAccount = createAccount("1000000002", "beneficiary@example.com", "2250.00", WalletType.BUSINESS);
        Transaction existingTransaction = Transaction.builder()
                .transactionRef("TXN_EXISTING-DR")
                .sourceAccountNumber("1000000001")
                .destinationAccountNumber("1000000002")
                .amount(new BigDecimal("1250.00"))
                .type(TransactionType.DEBIT)
                .status(TransactionStatus.SUCCESS)
                .narration("Wallet transfer")
                .externalReference("TRF-100")
                .timestamp(Instant.now())
                .build();

        FundTransferRequest request = new FundTransferRequest();
        request.setSourceAccountNumber("1000000001");
        request.setDestinationAccountNumber("1000000002");
        request.setAmount(new BigDecimal("1250.00"));
        request.setClientReference("TRF-100");

        when(transactionRepository.findByExternalReference("TRF-100")).thenReturn(Optional.of(existingTransaction));
        when(accountRepository.findByAccountNumber("1000000001")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber("1000000002")).thenReturn(Optional.of(destinationAccount));

        FundTransferResponse response = walletService.transferFunds("owner@example.com", request);

        assertThat(response.getTransactionRef()).isEqualTo("TXN_EXISTING-DR");
        assertThat(response.getClientReference()).isEqualTo("TRF-100");
        assertThat(response.getSourceAvailableBalance()).isEqualByComparingTo("3750.00");
        assertThat(response.getDestinationAvailableBalance()).isEqualByComparingTo("2250.00");
        verify(accountRepository, never()).findByAccountNumberForUpdate(anyString());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    private User createUser(Long id, String email, String userRef) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setUserRef(userRef);
        return user;
    }

    private Account createAccount(String accountNumber, String userEmail, String balance, WalletType walletType) {
        User user = createUser(1L, userEmail, "usr_" + accountNumber);

        WalletBalance walletBalance = new WalletBalance();
        walletBalance.setAvailableAmount(new BigDecimal(balance));
        walletBalance.setLedgerAmount(new BigDecimal(balance));

        Account account = new Account();
        account.setAccountRef("acc_" + accountNumber);
        account.setAccountNumber(accountNumber);
        account.setWalletType(walletType);
        account.setUser(user);
        account.setWalletBalance(walletBalance);
        return account;
    }
}
