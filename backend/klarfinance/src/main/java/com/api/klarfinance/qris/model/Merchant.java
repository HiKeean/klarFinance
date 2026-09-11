package com.api.klarfinance.qris.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Toko yang generate QR lewat halaman publik qris-generator/ (lihat knowledge/plan) -
 * merchantCode-lah yang di-encode jadi QR, discan nasabah buat mulai transaksi
 * (QrisTransaction). Mockup - bukan integrasi jaringan QRIS resmi BI/EMVCo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dbh_merchant", schema = "qris")
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @Column(name = "merchant_code", unique = true, length = 8)
    private String merchantCode;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
