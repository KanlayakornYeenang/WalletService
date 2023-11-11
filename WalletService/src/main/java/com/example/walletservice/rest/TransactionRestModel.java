package com.example.walletservice.rest;

import com.example.walletservice.data.user.User;
import com.example.walletservice.data.user.UserInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;

@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionRestModel {
    @Id
    private String _id;
    private String timestamp;
    private User payer;
    private User payee;
    private BigDecimal amount;
    private String description;
    public TransactionRestModel(String _id, String timestamp, User payee, BigDecimal amount, String description) {
        this(_id, timestamp, null, payee, amount, description);
    }

    public TransactionRestModel(String _id, String timestamp, User payer, User payee, BigDecimal amount, String description) {
        this._id = _id;
        this.timestamp = timestamp;

        UserInfo payee_info = UserInfo.builder()
                .first_name(payee.getInfo().getFirst_name())
                .last_name(payee.getInfo().getLast_name())
                .build();
        this.payee = User.builder()._id(payee.get_id()).info(payee_info).build();

        if (payer != null) {
            UserInfo payer_info = UserInfo.builder()
                    .first_name(payer.getInfo().getFirst_name())
                    .last_name(payer.getInfo().getLast_name())
                    .build();
            this.payer = User.builder()._id(payer.get_id()).info(payer_info).build();
        }

        this.amount = amount;
        this.description = description;
    }
}
