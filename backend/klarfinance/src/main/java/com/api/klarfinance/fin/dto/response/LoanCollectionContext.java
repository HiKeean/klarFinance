package com.api.klarfinance.fin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Data satu pinjaman untuk panggilan penagihan deskcall (demo, tombol Call di NPL Report
 * webadmin) - lihat FinDashboardService#getCollectionContext. Tagihan = cicilan UNPAID terdekat,
 * denda dari LoanInterestPolicy (2%/hari, sama dengan NPL Report). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanCollectionContext {
    private Integer loanId;
    private Integer userId;
    private String customerName;
    private LocalDate birthDate;
    private String address;
    private String fcmToken;
    private BigDecimal installmentAmount;
    private BigDecimal penaltyAmount;
    private LocalDate dueDate;
    private long daysOverdue;
}
