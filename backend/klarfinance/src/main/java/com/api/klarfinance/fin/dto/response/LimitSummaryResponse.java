package com.api.klarfinance.fin.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LimitSummaryResponse {
    private BigDecimal totalLimit;
    private BigDecimal usedLimit;
    private BigDecimal availableLimit;

    /** Kuota QRIS kumulatif (QrisPolicy.quota) dan berapa yang sudah kepakai - null kalau
     * nasabah belum eligible (plafond < Rp2jt, lihat QrisPolicy.isEligible). */
    private BigDecimal qrisQuota;
    private BigDecimal qrisUsedAmount;

    /** true kalau nasabah punya pengajuan tarik tunai yang masih PENDING_BM (LoanReviewRequest) -
     * selama true, LoanService.requestLoan() menolak pengajuan tarik tunai baru apapun
     * nominalnya. TIDAK memengaruhi QRIS (QrisService tidak mengecek field ini sama sekali,
     * konfirmasi user pengajuan >30% cuma berlaku untuk tarik tunai). */
    private boolean hasPendingLoanReview;
}
