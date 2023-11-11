package com.example.walletservice.rest;

import com.example.walletservice.data.Wallet;
import com.example.walletservice.data.user.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class WalletRestModel {
    private Wallet wallet;
    private User user;

    public WalletRestModel(Wallet wallet, User user) {
        this.wallet = wallet;
        this.user = user;
        wallet.setUser_id(null);
    }
}
