package com.baber.identityservice.identityservice.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.core.GrantedAuthority;
@Getter
@Setter
@Table("t_roles")
public class Role implements GrantedAuthority {
    @Id
    private Long id;
    private String name;
    private String description;
    @Override
    public String getAuthority() {
        return name;
    }
}
