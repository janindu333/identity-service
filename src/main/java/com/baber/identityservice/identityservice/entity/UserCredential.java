package com.baber.identityservice.identityservice.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@Setter
@Table("t_user_credentials")
public class UserCredential {

    @Id
    private Integer id;
    private String name;
    private String email;
    private String password;
    private Long roleId;
    private String phoneNumber;
    private String latitude;
    private String longitude;

    public UserDetails toUserDetails() {
        return new org.springframework.security.core.userdetails.User(
                name, password, Collections.singleton(new SimpleGrantedAuthority("ADMIN"))
        );
    }
}
