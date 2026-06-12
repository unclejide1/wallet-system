package com.example.test.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Data@AllArgsConstructor@NoArgsConstructor@ToString
public class DoTransDto {
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
}
