package com.example.test.service.impl;

import com.example.test.common.exception.ApiException;
import com.example.test.dto.AccountDetailsResponse;
import com.example.test.dto.CreateAccountRequest;
import com.example.test.dto.FundTransferResponse;
import com.example.test.dto.FundTransferRequest;
import com.example.test.dto.FundWalletRequest;
import com.example.test.dto.TransactionResponse;
import com.example.test.dto.WalletFundingResponse;
import com.example.test.model.Account;
import com.example.test.model.Transaction;
import com.example.test.model.User;
import com.example.test.model.WalletBalance;
import com.example.test.model.enums.TransactionStatus;
import com.example.test.model.enums.TransactionType;
import com.example.test.repo.AccountRepo;
import com.example.test.repo.TransactionRepository;
import com.example.test.repo.UserRepo;
import com.example.test.service.AccountNumberGenerator;
import com.example.test.service.WalletService;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private static final String SYSTEM_FUNDING_ACCOUNT = "0000000000";
    private static final int MAX_STATEMENT_PAGE_SIZE = 50;
    private static final String DEFAULT_FUNDING_NARRATION = "Wallet funding";
    private static final String DEFAULT_TRANSFER_NARRATION = "Wallet transfer";
    private static final String FUNDING_REFERENCE_PREFIX = "FUND_";

    private final AccountRepo accountRepository;
    private final UserRepo userRepository;
    private final TransactionRepository transactionRepository;
    private final AccountNumberGenerator accountNumberGenerator;

    @Override
    @Transactional
    public AccountDetailsResponse createWalletAccount(String userEmail, CreateAccountRequest request) {
        User user = lockUserByEmail(userEmail);

        if (accountRepository.existsByUserIdAndWalletType(user.getId(), request.getWalletType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You already have a wallet account of type: " + request.getWalletType());
        }

        String generatedAccountNumber = accountNumberGenerator.generateNextAccountNumber();

        WalletBalance initialBalance = new WalletBalance();
        initialBalance.setAvailableAmount(BigDecimal.ZERO);
        initialBalance.setLedgerAmount(BigDecimal.ZERO);

        Account newAccount = new Account();
        newAccount.setUser(user);
        newAccount.setAccountNumber(generatedAccountNumber);
        newAccount.setWalletType(request.getWalletType());
        newAccount.setWalletBalance(initialBalance);

        Account savedAccount = accountRepository.save(newAccount);
        log.info("Wallet successfully generated via interface abstraction for {}. Account Number: {}", userEmail, generatedAccountNumber);
        return AccountDetailsResponse.fromEntity(savedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountDetailsResponse> getUserAccounts(String userEmail) {
        return accountRepository.findByUserEmail(normalizeEmail(userEmail)).stream()
                .map(AccountDetailsResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public WalletFundingResponse fundWallet(String userEmail, FundWalletRequest request) {
        String destinationAccountNumber = normalizeAccountNumber(request.getAccountNumber());
        String providedPaymentReference = sanitizeOptionalReference(request.getPaymentReference());
        String paymentReference = providedPaymentReference != null
                ? providedPaymentReference
                : generateFundingReference();
        BigDecimal fundingAmount = request.getAmount();
        String narration = resolveNarration(request.getNarration(), DEFAULT_FUNDING_NARRATION);
        String normalizedEmail = normalizeEmail(userEmail);

        if (providedPaymentReference != null) {
            Transaction existingTransaction = transactionRepository.findByExternalReference(paymentReference).orElse(null);
            if (existingTransaction != null) {
                return validateAndResolveExistingFunding(normalizedEmail, destinationAccountNumber, fundingAmount, narration, existingTransaction);
            }
        }

        Account account = lockAccount(destinationAccountNumber, "Destination wallet account missing");
        validateOwnership(account, normalizedEmail);

        if (providedPaymentReference != null) {
            Transaction existingTransaction = transactionRepository.findByExternalReference(paymentReference).orElse(null);
            if (existingTransaction != null) {
                return validateAndResolveExistingFunding(normalizedEmail, destinationAccountNumber, fundingAmount, narration, existingTransaction);
            }
        }

        account.getWalletBalance().credit(fundingAmount);

        Transaction fundingTransaction = transactionRepository.save(Transaction.builder()
                .transactionRef(generateTransactionReference())
                .sourceAccountNumber(SYSTEM_FUNDING_ACCOUNT)
                .destinationAccountNumber(destinationAccountNumber)
                .amount(fundingAmount)
                .type(TransactionType.CREDIT)
                .status(TransactionStatus.SUCCESS)
                .narration(narration)
                .externalReference(paymentReference)
                .timestamp(Instant.now())
                .build());

        return WalletFundingResponse.of(fundingTransaction, account);
    }

    @Override
    @Transactional
    public FundTransferResponse transferFunds(String userEmail, FundTransferRequest request) {
        String sourceAccountNumber = normalizeAccountNumber(request.getSourceAccountNumber());
        String destinationAccountNumber = normalizeAccountNumber(request.getDestinationAccountNumber());
        String clientReference = sanitizeOptionalReference(request.getClientReference());
        String narration = resolveNarration(request.getNarration(), DEFAULT_TRANSFER_NARRATION);
        String normalizedEmail = normalizeEmail(userEmail);

        if (sourceAccountNumber.equals(destinationAccountNumber)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Intra-account transfer to identical target coordinates prohibited");
        }

        BigDecimal transferAmount = request.getAmount();

        if (clientReference != null) {
            Transaction existingTransaction = transactionRepository.findByExternalReference(clientReference).orElse(null);
            if (existingTransaction != null) {
                return validateAndResolveExistingTransfer(
                        normalizedEmail,
                        sourceAccountNumber,
                        destinationAccountNumber,
                        transferAmount,
                        narration,
                        existingTransaction
                );
            }
        }

        Map<String, Account> lockedAccounts = lockAccountsForTransfer(sourceAccountNumber, destinationAccountNumber);
        Account sourceAccount = lockedAccounts.get(sourceAccountNumber);
        Account destAccount = lockedAccounts.get(destinationAccountNumber);
        validateOwnership(sourceAccount, normalizedEmail);

        if (clientReference != null) {
            Transaction existingTransaction = transactionRepository.findByExternalReference(clientReference).orElse(null);
            if (existingTransaction != null) {
                return validateAndResolveExistingTransfer(
                        normalizedEmail,
                        sourceAccountNumber,
                        destinationAccountNumber,
                        transferAmount,
                        narration,
                        existingTransaction
                );
            }
        }

        WalletBalance sourceBalance = sourceAccount.getWalletBalance();
        WalletBalance destBalance = destAccount.getWalletBalance();

        if (!sourceBalance.hasSufficientFunds(transferAmount)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient funds to process transfer request");
        }

        sourceBalance.debit(transferAmount);
        destBalance.credit(transferAmount);

        String trackingRef = generateTransactionReference();
        Instant now = Instant.now();

        Transaction debitAudit = Transaction.builder()
                .transactionRef(trackingRef + "-DR")
                .sourceAccountNumber(sourceAccountNumber)
                .destinationAccountNumber(destinationAccountNumber)
                .amount(transferAmount)
                .type(TransactionType.DEBIT)
                .status(TransactionStatus.SUCCESS)
                .narration(narration)
                .externalReference(clientReference)
                .timestamp(now)
                .build();

        Transaction creditAudit = Transaction.builder()
                .transactionRef(trackingRef + "-CR")
                .sourceAccountNumber(sourceAccountNumber)
                .destinationAccountNumber(destinationAccountNumber)
                .amount(transferAmount)
                .type(TransactionType.CREDIT)
                .status(TransactionStatus.SUCCESS)
                .narration(narration)
                .timestamp(now)
                .build();

        Transaction savedDebitAudit = transactionRepository.save(debitAudit);
        transactionRepository.save(creditAudit);
        return FundTransferResponse.of(savedDebitAudit, sourceAccount, destAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(String requesterEmail, boolean adminRequest, String accountNumber, int page, int size) {
        Account account = accountRepository.findByAccountNumber(normalizeAccountNumber(accountNumber))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account records not found"));

        if (!adminRequest) {
            validateOwnership(account, normalizeEmail(requesterEmail));
        }

        if (size > MAX_STATEMENT_PAGE_SIZE) {
            size = MAX_STATEMENT_PAGE_SIZE;
        }
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return transactionRepository.findStatementEntries(
                account.getAccountNumber(),
                TransactionType.DEBIT,
                TransactionType.CREDIT,
                pageRequest
        ).map(TransactionResponse::fromEntity);
    }

    private User lockUserByEmail(String userEmail) {
        return userRepository.findByEmailForUpdate(normalizeEmail(userEmail))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User profile not found"));
    }

    private void validateOwnership(Account account, String userEmail) {
        if (!account.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only operate on wallet accounts that belong to you");
        }
    }

    private Account lockAccount(String accountNumber, String notFoundMessage) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, notFoundMessage));
    }

    private Account getAccount(String accountNumber, String notFoundMessage) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, notFoundMessage));
    }

    private Map<String, Account> lockAccountsForTransfer(String sourceAccountNumber, String destinationAccountNumber) {
        return Stream.of(sourceAccountNumber, destinationAccountNumber)
                .sorted(Comparator.naturalOrder())
                .map(accountNumber -> lockAccount(
                        accountNumber,
                        accountNumber.equals(sourceAccountNumber) ? "Source wallet account missing" : "Destination wallet account missing"
                ))
                .collect(Collectors.toMap(Account::getAccountNumber, account -> account));
    }

    private String resolveNarration(String narration, String fallbackNarration) {
        if (!StringUtils.hasText(narration)) {
            return fallbackNarration;
        }
        return Jsoup.clean(narration.trim(), Safelist.none());
    }

    private String generateFundingReference() {
        return FUNDING_REFERENCE_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private String sanitizeOptionalReference(String reference) {
        if (!StringUtils.hasText(reference)) {
            return null;
        }

        String sanitizedReference = Jsoup.clean(reference.trim(), Safelist.none());
        return sanitizedReference.isBlank() ? null : sanitizedReference;
    }

    private WalletFundingResponse validateAndResolveExistingFunding(
            String userEmail,
            String destinationAccountNumber,
            BigDecimal amount,
            String narration,
            Transaction existingTransaction
    ) {
        if (!isCompatibleFundingRetry(destinationAccountNumber, amount, narration, existingTransaction)) {
            throw new ApiException(HttpStatus.CONFLICT, "Payment reference has already been used for a different wallet funding request");
        }

        Account account = getAccount(destinationAccountNumber, "Destination wallet account missing");
        validateOwnership(account, userEmail);
        return WalletFundingResponse.of(existingTransaction, account);
    }

    private FundTransferResponse validateAndResolveExistingTransfer(
            String userEmail,
            String sourceAccountNumber,
            String destinationAccountNumber,
            BigDecimal amount,
            String narration,
            Transaction existingTransaction
    ) {
        if (!isCompatibleTransferRetry(sourceAccountNumber, destinationAccountNumber, amount, narration, existingTransaction)) {
            throw new ApiException(HttpStatus.CONFLICT, "Client reference has already been used for a different transfer request");
        }

        Account sourceAccount = getAccount(sourceAccountNumber, "Source wallet account missing");
        Account destinationAccount = getAccount(destinationAccountNumber, "Destination wallet account missing");
        validateOwnership(sourceAccount, userEmail);
        return FundTransferResponse.of(existingTransaction, sourceAccount, destinationAccount);
    }

    private boolean isCompatibleFundingRetry(
            String destinationAccountNumber,
            BigDecimal amount,
            String narration,
            Transaction existingTransaction
    ) {
        return existingTransaction.getType() == TransactionType.CREDIT
                && SYSTEM_FUNDING_ACCOUNT.equals(existingTransaction.getSourceAccountNumber())
                && existingTransaction.getDestinationAccountNumber().equals(destinationAccountNumber)
                && existingTransaction.getAmount().compareTo(amount) == 0
                && Objects.equals(existingTransaction.getNarration(), narration);
    }

    private boolean isCompatibleTransferRetry(
            String sourceAccountNumber,
            String destinationAccountNumber,
            BigDecimal amount,
            String narration,
            Transaction existingTransaction
    ) {
        return existingTransaction.getType() == TransactionType.DEBIT
                && existingTransaction.getSourceAccountNumber().equals(sourceAccountNumber)
                && existingTransaction.getDestinationAccountNumber().equals(destinationAccountNumber)
                && existingTransaction.getAmount().compareTo(amount) == 0
                && Objects.equals(existingTransaction.getNarration(), narration);
    }

    private String generateTransactionReference() {
        return "TXN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase(Locale.ROOT);
    }

    private String normalizeAccountNumber(String accountNumber) {
        return accountNumber.trim();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
