package com.api.klarfinance.fin.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.api.klarfinance.qris.model.Merchant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dbh_drawdown", schema = "fin")
public class Drawdown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "drawdown_amount", precision = 18, scale = 2)
    private BigDecimal drawdownAmount;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_code")
    private String bankCode;

    /** "BANK_TRANSFER" (default, pencairan biasa) atau "QRIS" (bayar merchant, lihat
     * QrisService/LoanService#finalizeLoanForMerchant) - dua channel disbursement yang sama-sama
     * jadi Loan di sistem yang sama, cuma tujuan dananya beda. */
    @Builder.Default
    private String channel = "BANK_TRANSFER";

    /** Terisi kalau channel=QRIS, null kalau BANK_TRANSFER - fin depend ke qris.model.Merchant,
     * konsisten sama fin yang sudah depend ke los.model.ActiveLimit. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    private String status;
    private LocalDateTime createdAt;
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
