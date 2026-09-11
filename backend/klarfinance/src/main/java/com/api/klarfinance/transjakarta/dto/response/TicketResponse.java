package com.api.klarfinance.transjakarta.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
    private Integer ticketId;

    /** Isi QR mockup - "kalau discan isinya id tiket aja" (konfirmasi user), lihat
     * TransjakartaTicket.ticketCode. */
    private String ticketCode;

    private BigDecimal amount;
    private boolean used;
    private LocalDateTime purchasedAt;

    private Integer loanId;
    /** "YYYY-MM" - siklus billing bulan-kalender pembelian (lihat Loan.billingCycle). */
    private String billingCycle;
    /** Tanggal 25 bulan BERIKUTNYA dari billingCycle - lihat LoanService.TRANSJAKARTA_DUE_DAY. */
    private LocalDateTime dueDate;
}
