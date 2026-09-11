package com.api.klarfinance.los.service;

import org.springframework.stereotype.Component;

import com.api.klarfinance.los.EngineStatus;
import com.api.klarfinance.los.model.PefindoInquiry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Semi-clean exception (see backend-architecture knowledge): Engine Scoring
 * business rules live here as plain methods instead of being spread across
 * the orchestrating service, and stay free of any repository/DB dependency
 * so they're cheap to unit test directly.
 *
 * Rules encoded here (confirmed with product owner):
 * - Vida gate: UNCLEAR -> RETAKE_PHOTO (hard stop, needs new photos). REJECTED ->
 *   EngineStatus.REJECTED (hard stop, final, gak lewat Pefindo sama sekali - identitasnya
 *   sendiri belum lolos verifikasi jadi gak ada dasar buat kasih limit).
 *   REVISI 2026-09-02 (konfirmasi user): Vida REJECTED HARUS auto-final-REJECTED dan
 *   dibalikin ke user langsung - TIDAK BOLEH sampai masuk antrean Checker sama sekali.
 *   Ini membalikkan revisi 2026-08-31 sebelumnya (yang sempat bikin REJECTED tetap
 *   PENDING_CHECKER sebagai rekomendasi) - revisi itu sekarang dianggap keliru/dibatalkan.
 * - KOL table (SLIK OJK collectibility: 1=Lancar, 2=Dalam Perhatian Khusus,
 *   3=Kurang Lancar, 4=Diragukan, 5=Macet) determines the base limit -
 *   colStatus is always an explicit 1-5, "Lancar" is KOL 1, never null.
 *   TIDAK diubah oleh revisi 2026-08-31 - KOL 5 tetap dapat limit (Rp 5jt), BUKAN 0,
 *   karena itu rule lama yang udah confirmed duluan. "Recommendation REJECTED" di
 *   response cuma dari Vida gagal, bukan dari nilai KOL Pefindo manapun.
 * - Detected pinjol-app count applies a multiplier on top of that base
 *   (0 apps x100%, 1-2 apps x90%, 3-5 apps x75%, 6+ apps x50%), rounded down
 *   to the nearest Rp 100,000. This does NOT replace the KOL table with a
 *   flat 3jt-10jt range - it only scales the KOL-derived base down.
 * - "Engine Score"/Risk Category/Key Factors (buat tampilan halaman Checker) itu
 *   narasi rule-based sederhana dari sinyal yang udah ada (bukan model AI/ML -
 *   credit scoring AI/ML eksternal memang out-of-scope MVP per BRD).
 */
@Component
public class EngineScoringDomain {

    private static final BigDecimal ROUND_UNIT = BigDecimal.valueOf(100_000);

    /**
     * KOL (Kolektibilitas) table per project_rules.md section 4B, corrected
     * 2026-08-31: KOL 1 = Lancar is an explicit status, not "no history".
     */
    public BigDecimal determineBaseLimit(PefindoInquiry inquiry) {
        Integer colStatus = inquiry.getColStatus();
        Integer yearsAgo = inquiry.getColHistoryYearsAgo();

        if (colStatus == null || colStatus == 1) {
            // KOL 1 - Lancar
            return BigDecimal.valueOf(20_000_000);
        }
        if (colStatus == 5) {
            // KOL 5 - Macet, historinya sudah lewat 2 tahun (mock hanya generate skenario ini)
            return BigDecimal.valueOf(5_000_000);
        }
        if (colStatus == 2 && yearsAgo != null && yearsAgo < 2) {
            // Medium kasus 2: KOL 2 (Dalam Perhatian Khusus), riwayat belum 2 tahun
            return BigDecimal.valueOf(10_000_000);
        }
        if (colStatus <= 4 && yearsAgo != null && yearsAgo > 2) {
            // Medium kasus 1: KOL 2-4, riwayat lebih dari 2 tahun lalu
            return BigDecimal.valueOf(10_000_000);
        }
        return BigDecimal.valueOf(10_000_000);
    }

    public BigDecimal applyPinjolAdjustment(BigDecimal baseLimit, int pinjolAppCount) {
        BigDecimal multiplier;
        if (pinjolAppCount <= 0) {
            multiplier = BigDecimal.ONE;
        } else if (pinjolAppCount <= 2) {
            multiplier = new BigDecimal("0.90");
        } else if (pinjolAppCount <= 5) {
            multiplier = new BigDecimal("0.75");
        } else {
            multiplier = new BigDecimal("0.50");
        }

        BigDecimal adjusted = baseLimit.multiply(multiplier);
        return adjusted.divideToIntegralValue(ROUND_UNIT).multiply(ROUND_UNIT).setScale(0, RoundingMode.DOWN);
    }

    public EngineDecision decide(VidaResult vidaResult, PefindoResult pefindoResult, int pinjolAppCount) {
        if (VidaResult.UNCLEAR.equals(vidaResult.status())) {
            return new EngineDecision(EngineStatus.RETAKE_PHOTO, null, null, null, null, null);
        }
        if (VidaResult.REJECTED.equals(vidaResult.status())) {
            // Identitas belum lolos verifikasi - auto-final-REJECTED, gak pernah masuk
            // antrean Checker sama sekali (konfirmasi user 2026-09-02).
            return new EngineDecision(
                    EngineStatus.REJECTED,
                    BigDecimal.ZERO,
                    EngineDecision.RECOMMENDATION_REJECTED,
                    EngineDecision.RISK_HIGH,
                    List.of("Verifikasi KTP/Wajah Vida Gagal"),
                    null);
        }

        PefindoInquiry inquiry = pefindoResult.inquiry();
        BigDecimal baseLimit = determineBaseLimit(inquiry);
        BigDecimal suggestedLimit = applyPinjolAdjustment(baseLimit, pinjolAppCount);

        return new EngineDecision(
                EngineStatus.PENDING_CHECKER,
                suggestedLimit,
                EngineDecision.RECOMMENDATION_APPROVED,
                riskCategoryFor(inquiry.getColStatus()),
                keyFactorsFor(inquiry, vidaResult, pinjolAppCount),
                parseScore(inquiry.getScore()));
    }

    private String riskCategoryFor(Integer colStatus) {
        if (colStatus == null || colStatus == 1) return EngineDecision.RISK_LOW;
        if (colStatus == 5) return EngineDecision.RISK_HIGH;
        return EngineDecision.RISK_MEDIUM;
    }

    private List<String> keyFactorsFor(PefindoInquiry inquiry, VidaResult vidaResult, int pinjolAppCount) {
        List<String> factors = new ArrayList<>();
        Integer colStatus = inquiry.getColStatus();
        if (colStatus == null || colStatus == 1) {
            factors.add("Riwayat Kredit Bersih (KOL 1)");
        } else if (colStatus == 5) {
            factors.add("Riwayat Kredit Macet (KOL 5)");
        } else {
            factors.add("Riwayat Kredit Perlu Perhatian (KOL " + colStatus + ")");
        }

        factors.add(vidaResult.employed() ? "Status Kerja Terverifikasi" : "Status Kerja Belum Terverifikasi");

        if (pinjolAppCount <= 0) {
            factors.add("Tidak Terdeteksi App Pinjol Lain");
        } else if (pinjolAppCount <= 2) {
            factors.add("Sedikit App Pinjol Lain Terdeteksi");
        } else {
            factors.add("Banyak App Pinjol Lain Terdeteksi");
        }
        return factors;
    }

    private Integer parseScore(String score) {
        try {
            return score == null ? null : Integer.valueOf(score);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
