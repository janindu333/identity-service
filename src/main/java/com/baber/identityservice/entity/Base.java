package com.baber.identityservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.util.Date;

@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class Base implements Serializable {
    @Transient
    private static final long serialVersionUID = -1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;
    @CreatedBy
    @JsonIgnore
    String createdBy;

    @LastModifiedBy
    @JsonIgnore
    String modifiedBy;
    @CreatedDate
    @JsonIgnore
    Date createdOn;
    @LastModifiedDate
    @JsonIgnore
    Date modifiedOn;
    @Version
    @JsonIgnore
    Long version;
    @JsonIgnore
    private Integer deleted = 0;
    
    public void setDeleted() {
        this.deleted = 1;
    }
    
    public Integer getDeleted() {
        return this.deleted;
    }
}
