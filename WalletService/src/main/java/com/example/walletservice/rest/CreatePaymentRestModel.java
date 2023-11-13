package com.example.walletservice.rest;

import com.example.walletservice.data.user.User;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Data
public class CreatePaymentRestModel {
    private List<OrderRestModel> orders;
    private BigDecimal amount;
    private BigDecimal tax;
}
