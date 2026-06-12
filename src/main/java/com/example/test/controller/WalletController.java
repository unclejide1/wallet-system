package com.example.test.controller;

import com.example.test.common.api.ApiResponse;
import com.example.test.dto.AccountDetailsResponse;
import com.example.test.dto.CreateAccountRequest;
import com.example.test.dto.FundTransferRequest;
import com.example.test.dto.FundTransferResponse;
import com.example.test.dto.FundWalletRequest;
import com.example.test.dto.TransactionResponse;
import com.example.test.dto.WalletFundingResponse;
import com.example.test.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "3. Core Wallet Engine", description = "Protected transactional endpoints handling account creations, ledgers, and cash transfers")
public class WalletController {

    private final WalletService walletService;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Generate a fresh 10-digit wallet account tied to the authenticated user")
    public ResponseEntity<ApiResponse<AccountDetailsResponse>> createWallet(
            Principal principal,
            @Valid @RequestBody CreateAccountRequest payload,
            HttpServletRequest request
    ) {
        AccountDetailsResponse data = walletService.createWalletAccount(principal.getName(), payload);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Wallet account opened successfully", data, request.getRequestURI()));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Fetch all active wallet books tied to the authenticated identity")
    public ResponseEntity<ApiResponse<List<AccountDetailsResponse>>> getMyWallets(Principal principal, HttpServletRequest request) {
        List<AccountDetailsResponse> data = walletService.getUserAccounts(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Wallets fetched successfully", data, request.getRequestURI()));
    }

    @PostMapping(value = "/fund", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Simulate an external top-up into one of the authenticated user's wallets")
    public ResponseEntity<ApiResponse<WalletFundingResponse>> fundWallet(
            Principal principal,
            @Valid @RequestBody FundWalletRequest payload,
            HttpServletRequest request
    ) {
        WalletFundingResponse data = walletService.fundWallet(principal.getName(), payload);
        return ResponseEntity.ok(ApiResponse.success("Wallet funded successfully", data, request.getRequestURI()));
    }

    @PostMapping(value = "/transfer", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Perform an atomic, pessimistically locked ledger cash transfer between platform coordinates")
    public ResponseEntity<ApiResponse<FundTransferResponse>> transferFunds(
            Principal principal,
            @Valid @RequestBody FundTransferRequest payload,
            HttpServletRequest request
    ) {
        FundTransferResponse data = walletService.transferFunds(principal.getName(), payload);
        return ResponseEntity.ok(ApiResponse.success("Transfer completed successfully", data, request.getRequestURI()));
    }

    @GetMapping(value = "/{accountNumber}/statement", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Retrieve paginated double-entry ledger historical logs corresponding to specified account target")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getStatement(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal,
            Authentication authentication,
            HttpServletRequest request
    ) {
        boolean adminRequest = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        Page<TransactionResponse> data = walletService.getTransactionHistory(principal.getName(), adminRequest, accountNumber, page, size);
        return ResponseEntity.ok(ApiResponse.success("Account statement generated successfully", data, request.getRequestURI()));
    }
}
