package com.api.klarfinance.fin.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.api.klarfinance.auth.model.User;

/**
 * Rekening tujuan pencairan yang pernah dipakai nasabah (konfirmasi user 2026-09-07) - otomatis
 * tersimpan begitu {@link com.api.klarfinance.fin.service.LoanService#finalizeLoan} berhasil
 * (bukan langkah "simpan" terpisah dari nasabah), dipakai buat isi dropdown "Rekening Tujuan" di
 * pengajuan pinjaman berikutnya supaya nasabah gak perlu ketik ulang tiap kali.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dbh_saved_bank_account", schema = "fin")
public class SavedBankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "bank_code", nullable = false)
    private String bankCode;

    @Column(name = "bank_account_number", nullable = false)
    private String bankAccountNumber;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
