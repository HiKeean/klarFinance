package com.api.klarfinance.fin.dto.request;

import lombok.Data;

@Data
public class LoanReviewDecisionRequest {
    /** APPROVE atau REJECT - tidak ada negosiasi nominal (beda dari BmDecisionRequest punya
     * LimitApplication), pengajuan ini cuma disetujui persis sesuai amount yang diminta atau
     * ditolak sama sekali. */
    private String action;
    /** Wajib kalau action = REJECT. */
    private String reason;
}
