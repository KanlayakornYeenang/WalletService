package com.example.walletservice.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serializable;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationModel implements Serializable {
    @Id
    private String _id;
    private String user_id;
    private String title;
    private String content;

    private String transaction_id;
    private String order_id;
}
