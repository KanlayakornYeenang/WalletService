package com.example.walletservice.rest;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class TopUpRestModel {
    private String wallet_id;
    private BigDecimal amount;
    private String description;
}
