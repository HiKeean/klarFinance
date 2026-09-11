package com.api.klarfinance.transjakarta.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.api.klarfinance.auth.model.User;
import com.api.klarfinance.fin.model.Loan;

/**
 * Satu tiket yang dibeli nasabah (harga flat, lihat TransjakartaTicketService.TICKET_PRICE) -
 * beli qty N sekaligus = N baris (masing-masing bisa ditandai used/belum independen, konfirmasi
 * user). BEDA dari QrisTransaction: gak berdiri sendiri sebagai Loan-nya sendiri - semua tiket
 * dalam bulan kalender yang sama numpuk ke SATU Loan (loan.billingCycle), lihat
 * LoanService#purchaseTransjakartaTicket. Baris ini yang jadi "rincian" per-tiket kalau nasabah
 * mau lihat detail tagihan "Transportasi"-nya. ticketCode (UUID) yang di-encode ke QR mockup -
 * bukan id database (konfirmasi user "kalau discan isinya id tiket aja", tapi sengaja bukan
 * primary key numerik yang gampang ditebak).
 *
 * TIDAK ADA halte/rute (dibuang - konfirmasi user 2026-09-10 sesi lanjutan: skip GTFS/maps
 * total, tiket generik "berlaku buat rute standar", BUKAN rute jauh seperti Bogor-PIK2).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dbh_ticket", schema = "transjakarta")
public class TransjakartaTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ticket_code", unique = true, nullable = false)
    private String ticketCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(precision = 18, scale = 2)
    private BigDecimal amount;

    /** Mockup - gak ada validasi gate beneran, nasabah sendiri yang tandain lewat tap di app
     * (konfirmasi user "dari pencet used atau belumnya saja"). @ColumnDefault (bukan cuma
     * @Builder.Default) - tanpa ini, Hibernate ddl-auto=update generate "ALTER TABLE ... ADD
     * used bit NOT NULL" TANPA default, yang ditolak SQL Server begitu tabelnya udah ada baris
     * (dari testing sebelum kolom ini ada) - "Column 'used' cannot be added to non-empty table".
     * @ColumnDefault bikin Hibernate nyertain "DEFAULT 0" di DDL-nya, aman ditambahkan ke tabel
     * yang udah keisi data. */
    @Builder.Default
    @ColumnDefault("0")
    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "purchased_at")
    private LocalDateTime purchasedAt;

    @PrePersist
    protected void onCreate() {
        purchasedAt = LocalDateTime.now();
    }
}
