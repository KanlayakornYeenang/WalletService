package com.example.walletservice.data;

import com.example.walletservice.data.user.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletRepository extends MongoRepository<Wallet, String> {
    @Query(value="{user_id:'?0'}")
    public Wallet findWalletByUserId(String user_id);

    @Query(value="{_id:'?0'}")
    public Wallet findWalletById(String _id);

    @Query(value="{_id:'?0'}")
    public Wallet findUserIdByWalletId(String _id);
}
