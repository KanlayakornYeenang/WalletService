package com.example.walletservice.rest;

import com.example.walletservice.data.user.User;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;

@Builder
@Data
public class CreatePaymentRestModel {
    @Id
    private String _id;
    private String sender_from_wallet_id;
    private String pay_to_wallet_id;
    private BigDecimal amount;
    private String description;
}
