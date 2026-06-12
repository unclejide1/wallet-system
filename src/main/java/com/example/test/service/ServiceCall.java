package com.example.test.service;

import com.example.test.dto.DoTransDto;
import com.example.test.model.User;

public interface ServiceCall {

    void createUserAndAccount(User user);

    void doIntraTransfer(DoTransDto request);

}
