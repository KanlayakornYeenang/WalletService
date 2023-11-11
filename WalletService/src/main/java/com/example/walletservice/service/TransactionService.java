package com.example.walletservice.service;

import com.example.walletservice.data.Transaction;
import com.example.walletservice.data.TransactionRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {
    @Autowired
    private TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @RabbitListener(queues = "GetTransactionById")
    public Transaction getTransaction(String transaction_id) {
        return transactionRepository.findTransactionById(transaction_id);
    }

    @RabbitListener(queues = "SaveTransaction")
    public Transaction saveTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }
}
