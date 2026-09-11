package com.api.klarfinance.fin.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.api.klarfinance.los.model.ActiveLimit;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dbh_loan", schema = "fin")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "limit_id", nullable = false)
    private ActiveLimit limit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drawdown_id", nullable = false)
    private Drawdown drawdown;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_details_id", nullable = false)
    private LoanDetails loanDetails;

    private Integer tenor;

    /** Cuma terisi buat channel TRANSJAKARTA - kunci "YYYY-MM" siklus billing bulan-kalender
     * pembelian (bukan bulan jatuh tempo). Beberapa TransjakartaTicket bisa numpuk ke SATU Loan
     * yang sama selama billingCycle-nya sama (lihat LoanService#purchaseTransjakartaTicket) -
     * beda dari QRIS/tarik tunai yang selalu 1 Loan baru per transaksi. */
    @Column(name = "billing_cycle", length = 7)
    private String billingCycle;

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
