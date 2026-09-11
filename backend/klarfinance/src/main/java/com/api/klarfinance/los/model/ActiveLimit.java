package com.api.klarfinance.los.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.api.klarfinance.auth.model.User;
import com.api.klarfinance.dbo.model.Branch;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dbh_active_limits", schema = "los")
public class ActiveLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_limit", precision = 18, scale = 2)
    private BigDecimal totalLimit;

    @Column(name = "used_limit", precision = 18, scale = 2)
    private BigDecimal usedLimit;

    @Column(name = "available_limit", precision = 18, scale = 2)
    private BigDecimal availableLimit;

    /** Cabang BM yang approve pengajuan ini — dipakai buat scoping data "wilayah Anda" di dashboard BM. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    private LocalDateTime expiredAt;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
