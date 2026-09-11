package com.api.klarfinance.dbo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Wilayah (regency) yang ditanggung jawabin satu Branch (konfirmasi user 2026-09-01) - dipakai
 * buat akuntabilitas NPL/tunggakan per wilayah. Satu regency maks dipegang 1 branch, KECUALI
 * regency di provinsi DKI Jakarta yang boleh maks 2 (lihat BranchTerritoryService buat aturan
 * lengkapnya - validasi batas ini di level aplikasi, bukan DB constraint, karena beda per wilayah).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dbh_branch_territory", schema = "dbo")
public class BranchTerritory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regency_id", nullable = false)
    private Regency regency;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
