package com.api.klarfinance.fin.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * One row of the nasabah's "History" page (GET /nasabah/loan/history) - covers BOTH tarik tunai
 * (channel BANK_TRANSFER, type LOAN) and bayar QRIS (channel QRIS, type QRIS_PAYMENT), since
 * both are stored as the same Loan entity in this system (see Drawdown.channel). Status is
 * derived on-read from Installment, same approach as FinDashboardService's per-branch buckets -
 * no status column is persisted on Loan itself.
 */
@Data
@Builder
public class LoanHistoryItemResponse {
    private Integer loanId;

    /** "LOAN" (tarik tunai, bank transfer) atau "QRIS_PAYMENT" (bayar QRIS ke merchant). */
    private String type;

    /** Terisi cuma kalau type=QRIS_PAYMENT. */
    private String merchantName;

    private BigDecimal requestedAmount;
    private BigDecimal totalAmountDue;
    private Integer tenorMonths;

    /** "ACTIVE" (masih ada cicilan UNPAID, belum lewat jatuh tempo), "OVERDUE" (ada cicilan
     * UNPAID yang sudah lewat jatuh tempo - tidak ada grace period, lihat LoanStatusBucket), atau
     * "PAID_OFF" (semua cicilan sudah PAID). */
    private String status;

    private Integer paidInstallments;
    private Integer totalInstallments;

    /** Jatuh tempo cicilan berikutnya yang belum dibayar - null kalau status PAID_OFF. */
    private LocalDateTime nextDueDate;
    private BigDecimal nextDueAmount;

    private LocalDateTime createdAt;

    /** Jadwal LENGKAP semua cicilan (bukan cuma yang berikutnya) - urut installmentNumber -
     * dipakai halaman detail "History" pas nasabah tap salah satu baris buat lihat semua jatuh
     * tempo, bukan cuma yang paling dekat. */
    private List<InstallmentItem> installments;

    @Data
    @Builder
    public static class InstallmentItem {
        private Integer installmentNumber;
        private LocalDateTime dueDate;
        private BigDecimal amount;
        /** "UNPAID" atau "PAID" - lihat InstallmentStatus. */
        private String status;
        private LocalDateTime paidAt;
    }
}
