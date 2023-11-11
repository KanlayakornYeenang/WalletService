package com.example.walletservice.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@Document(collection = "Transaction")
public class Transaction implements Serializable {
    @Id
    private String _id;
    private String timestamp;
    private String payer_wallet_id;
    private String payee_wallet_id;
    private BigDecimal amount;
    private String description;

    public Transaction() {
    }

    public Transaction(String _id, String timestamp, String payer_wallet_id, String payee_wallet_id, BigDecimal amount, String description) {
        this._id = _id;
        this.timestamp = timestamp;
        this.payer_wallet_id = payer_wallet_id;
        this.payee_wallet_id = payee_wallet_id;
        this.amount = amount;
        this.description = description;
    }
}
