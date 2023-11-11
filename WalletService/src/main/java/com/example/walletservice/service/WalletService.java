package com.example.walletservice.service;

import com.example.walletservice.data.Wallet;
import com.example.walletservice.data.WalletRepository;
import com.example.walletservice.data.user.User;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WalletService {
    @Autowired
    private WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @RabbitListener(queues = "SaveWallet")
    public Wallet saveWallet(Wallet wallet) {
        return walletRepository.save(wallet);
    }

    @RabbitListener(queues = "GetWalletByUserId")
    public Wallet getWalletByUserId(String user_id) {
        return walletRepository.findWalletByUserId(user_id);
    }

    @RabbitListener(queues = "GetWalletById")
    public Wallet getWalletById(String wallet_id) {
        return walletRepository.findWalletById(wallet_id);
    }

    @RabbitListener(queues = "GetUserIdByWalletId")
    public String getUserByWalletId(String wallet_id) {
        return walletRepository.findUserIdByWalletId(wallet_id).getUser_id();
    }
}
