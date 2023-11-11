package com.example.walletservice.data;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {
    @Query(value="{_id:'?0'}")
    public Transaction findTransactionById(String _id);
}
