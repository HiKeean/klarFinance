package com.api.klarfinance.dbo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "dbh_districts", schema = "dbo")
public class District {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "regency_id", nullable = false)
    private Regency regency;

    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;


}