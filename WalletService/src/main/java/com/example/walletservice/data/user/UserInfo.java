package com.example.walletservice.data.user;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class UserInfo implements Serializable {
    private String first_name;
    private String last_name;

    public UserInfo() {
    }

    public UserInfo(String first_name, String last_name) {
        this.first_name = first_name;
        this.last_name = last_name;
    }
}