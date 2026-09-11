package com.api.klarfinance.dbo.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.api.klarfinance.auth.model.User;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dbh_api_param", schema = "dbo")
public class ApiParameter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "client_type")
    private String clientType;

    private Boolean isActive;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="created_by")
    private User createdBy;

    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="updated_by")
    private User updatedBy;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
