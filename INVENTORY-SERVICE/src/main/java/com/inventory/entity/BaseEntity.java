package com.inventory.entity;

import com.inventory.util.DateTimeUtil;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
public class BaseEntity implements Serializable {

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = DateTimeUtil.nowDubai();
        this.updatedAt = DateTimeUtil.nowDubai();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = DateTimeUtil.nowDubai();
    }
}
