package com.api.klarfinance.qris.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.api.klarfinance.auth.model.User;
import com.api.klarfinance.fin.model.Loan;

/**
 * Sesi bayar QRIS - mulai dari scan (PENDING, lihat QrisService.scan) sampai confirm
 * (CONFIRMED, terhubung ke Loan yang beneran dibuat) atau expired/gagal (token dibiarkan
 * PENDING kalau confirm gagal validasi kuota/plafond - lihat QrisPolicy - supaya nasabah bisa
 * coba nominal lain tanpa scan ulang, selama belum melewati expiresAt).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dbh_qris_transaction", schema = "qris")
public class QrisTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(unique = true, nullable = false)
    private String token;

    /** PENDING/CONFIRMED/EXPIRED - string biasa, bukan reuse EngineStatus (beda domain sama sekali). */
    private String status;

    @Column(precision = 18, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
