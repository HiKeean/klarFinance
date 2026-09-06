/**
 * Item antrean approval BM/Checker - unified (backend: ApprovalQueueItemResponse). `type`
 * membedakan pengajuan limit baru vs pengajuan pinjaman >30% yang butuh review ulang BM (lihat
 * bm-approval-lock.md / knowledge "loan-review-30-percent"). Field yang tidak relevan untuk
 * salah satu type null.
 */
export interface LimitApplicationSummary {
  type: 'LIMIT_APPLICATION' | 'LOAN_REVIEW';
  id: number;
  customerIdentity: string;
  customerName: string | null;
  status: string;
  createdAt: string;

  // --- LIMIT_APPLICATION only ---
  incomeAmount: number | null;
  engineSuggestionLimit: number | null;
  checkerPurposeLimit: number | null;
  finalApprovedLimit: number | null;

  // --- LOAN_REVIEW only ---
  requestedAmount: number | null;
  tenorMonths: number | null;
  usedLimitSnapshot: number | null;
  totalLimitSnapshot: number | null;
  utilizationPercent: number | null;

  pefindoScore: string | null;
  pefindoColStatus: number | null;

  // Status lock BM bucket-per-branch - lihat bm-approval-lock.md. Null/false kalau belum ada
  // yang buka detail-nya sama sekali.
  lockedByIdentity: string | null;
  lockedByName: string | null;
  lockedByMe: boolean;
}

export interface DecisionRequest {
  action: 'APPROVE' | 'REJECT';
  purposeLimit?: number;
  finalLimit?: number;
  reason?: string;
}

/** Keputusan BM untuk LoanReviewRequest (pinjaman >30%) - tidak ada negosiasi nominal seperti
 * DecisionRequest punya LimitApplication, cuma APPROVE persis sesuai amount yang diminta atau
 * REJECT (reason wajib). */
export interface LoanReviewDecisionRequest {
  action: 'APPROVE' | 'REJECT';
  reason?: string;
}

export interface LoanReviewDetail {
  id: number;
  status: string;
  customerIdentity: string;
  customerName: string | null;
  customerAddress: string | null;
  provinceName: string | null;
  regencyName: string | null;
  requestedAmount: number;
  tenorMonths: number;
  bankAccountNumber: string | null;
  bankCode: string | null;
  usedLimitSnapshot: number;
  totalLimitSnapshot: number;
  utilizationPercent: number;
  pefindoScore: string | null;
  pefindoColStatus: number | null;
  pefindoRiskLabel: string | null;
  pinjolAppsCount: number;
  bankingAppsCount: number;
  bmReason: string | null;
  createdAt: string;
  lockedByIdentity: string | null;
  lockedByName: string | null;
  lockedByMe: boolean;
  canReview: boolean;
}

export interface VidaDetail {
  kycStatus: string | null;
  kycFaceScore: string | null;
  kycVendorTrxId: string | null;
  incomeVerificationStatus: string | null;
  incomeVerificationSource: string | null;
  incomeIsEmployed: string | null;
  incomeVerifiedMonthly: string | null;
  createdAt: string | null;
}

export interface PefindoDetail {
  score: string | null;
  colStatus: number | null;
  riskLabel: string | null;
  pdfPathFile: string | null;
  createdAt: string | null;
}

export interface LimitApplicationDetail {
  id: number;
  applicationCode: string | null;
  status: string;
  customerIdentity: string;
  customerName: string | null;
  customerAddress: string | null;
  customerAddress2: string | null;
  branchName: string | null;
  provinceName: string | null;
  regencyName: string | null;
  districtName: string | null;
  villageName: string | null;
  // Nasabah upload atau enggak (data lama sebelum foto jadi wajib bisa null) - bukan URL, sengaja
  // cuma presence flag. Fetch bytes-nya lewat ApprovalApiService.getPicture (blob, butuh HMAC+JWT
  // header yang gak bisa ditempel ke <img src> polos).
  hasFotoKtp: boolean;
  hasFotoKyc: boolean;
  incomeAmount: number | null;
  vida: VidaDetail | null;
  pefindo: PefindoDetail | null;
  positiveAppsCount: number;
  pinjolAppsCount: number;
  judolAppsCount: number;
  bankingAppsCount: number;
  engineScore: number | null;
  engineRiskCategory: string | null;
  engineRecommendation: string | null;
  engineKeyFactors: string[];
  engineSuggestionLimit: number | null;
  checkerLimitMin: number | null;
  checkerLimitMax: number | null;
  checkerRecommendation: string | null;
  checkerPurposeLimit: number | null;
  checkerReason: string | null;
  finalApprovedLimit: number | null;
  bmReason: string | null;
  createdAt: string;
  // Status lock BM (bm-approval-lock.md). canReview: false berarti aplikasi sedang dikunci BM
  // lain - tombol Approve/Reject harus di-disable. Selalu true untuk Checker (lock system-nya
  // beda, ditangani CheckerRealtimeService, bukan field ini).
  lockedByIdentity: string | null;
  lockedByName: string | null;
  lockedByMe: boolean;
  canReview: boolean;
}
