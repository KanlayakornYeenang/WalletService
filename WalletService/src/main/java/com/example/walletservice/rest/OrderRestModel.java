package com.example.walletservice.rest;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderRestModel {
    private String wallet_id;
    private BigDecimal price;
}
