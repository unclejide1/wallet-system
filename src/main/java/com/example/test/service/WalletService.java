package com.example.test.service;

import com.example.test.dto.AccountDetailsResponse;
import com.example.test.dto.FundTransferResponse;
import com.example.test.dto.FundTransferRequest;
import com.example.test.dto.FundWalletRequest;
import com.example.test.dto.TransactionResponse;
import com.example.test.dto.CreateAccountRequest;
import com.example.test.dto.WalletFundingResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface WalletService {
    AccountDetailsResponse createWalletAccount(String userEmail, CreateAccountRequest request);
    List<AccountDetailsResponse> getUserAccounts(String userEmail);
    WalletFundingResponse fundWallet(String userEmail, FundWalletRequest request);
    FundTransferResponse transferFunds(String userEmail, FundTransferRequest request);
    Page<TransactionResponse> getTransactionHistory(String requesterEmail, boolean adminRequest, String accountNumber, int page, int size);
}
