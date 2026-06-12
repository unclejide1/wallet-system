package com.example.test.service;

import com.example.test.dto.DoTransDto;
import com.example.test.model.User;
import com.example.test.repo.AccountRepo;
import com.example.test.repo.UserRepo;
import com.example.test.repo.WalletBalanceRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DoService implements ServiceCall{

    @Autowired
    private UserRepo userRepo;
    @Autowired
    private AccountRepo accountRepo;
    @Autowired
    private WalletBalanceRepo walletBalanceRepo;


    @Override
    public void createUserAndAccount(User user) {

    }

    @Override
    public void doIntraTransfer(DoTransDto request) {

    }
}
