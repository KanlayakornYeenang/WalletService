package com.example.walletservice.data;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class WalletInfo implements Serializable {
    private BigDecimal balance;
    private List<String> transactions;

    public WalletInfo() {
        this(BigDecimal.ZERO, new ArrayList<>());
    }

    public WalletInfo(BigDecimal balance, List<String> transactions) {
        this.balance = balance;
        this.transactions = transactions;
    }
}
