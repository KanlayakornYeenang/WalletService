package com.example.walletservice.data.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User implements Serializable {
    @Id
    private String _id;
    private String email;
    @LastModifiedDate
    private Date updated_at;
    @CreatedDate
    private Date created_at;
    private UserInfo info;

    public User() {
    }

    public User(String _id, String email, Date updated_at, Date created_at, UserInfo info) {
        this._id = _id;
        this.email = email;
        this.updated_at = updated_at;
        this.created_at = created_at;
        this.info = info;
    }
}
