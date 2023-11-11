package com.example.walletservice.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

@Data
@Builder
@Document(collection = "Wallet")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Wallet implements Serializable {
    @Id
    private String _id;
    private String user_id;
    private WalletInfo info;

    public Wallet() {}

    public Wallet(String _id, String user_id, WalletInfo info) {
        this._id = _id;
        this.user_id = user_id;
        this.info = info;
    }
}
