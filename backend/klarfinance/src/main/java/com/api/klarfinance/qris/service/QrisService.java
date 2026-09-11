package com.api.klarfinance.qris.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.api.klarfinance.auth.model.User;
import com.api.klarfinance.auth.repository.UserRepository;
import com.api.klarfinance.fin.model.Loan;
import com.api.klarfinance.fin.service.LoanService;
import com.api.klarfinance.los.model.ActiveLimit;
import com.api.klarfinance.los.repository.ActiveLimitRepository;
import com.api.klarfinance.qris.QrisPolicy;
import com.api.klarfinance.qris.QrisTransactionStatus;
import com.api.klarfinance.qris.dto.request.GenerateMerchantRequest;
import com.api.klarfinance.qris.dto.request.QrisConfirmRequest;
import com.api.klarfinance.qris.dto.request.QrisScanRequest;
import com.api.klarfinance.qris.dto.response.MerchantResponse;
import com.api.klarfinance.qris.dto.response.QrisConfirmResponse;
import com.api.klarfinance.qris.dto.response.QrisScanResponse;
import com.api.klarfinance.qris.model.Merchant;
import com.api.klarfinance.qris.model.QrisTransaction;
import com.api.klarfinance.qris.repository.MerchantRepository;
import com.api.klarfinance.qris.repository.QrisTransactionRepository;

import java.math.BigDecimal;
import java.security.Principal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mockup pembayaran QRIS - lihat knowledge/plan "Fitur QRIS". Kuota QRIS (QrisPolicy) BEDA
 * TOTAL dari aturan ">30% pengajuan pinjaman tunai" (LoanInterestPolicy.exceedsReviewThreshold,
 * fin module) - dua aturan independen, jangan disamakan/dicampur.
 */
@Service
@RequiredArgsConstructor
public class QrisService {
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I
    private static final int CODE_LENGTH = 8;
    private static final int TOKEN_VALIDITY_MINUTES = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final MerchantRepository merchantRepository;
    private final QrisTransactionRepository qrisTransactionRepository;
    private final ActiveLimitRepository activeLimitRepository;
    private final UserRepository userRepository;
    private final LoanService loanService;

    public MerchantResponse generateMerchant(GenerateMerchantRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("name is required");
        }
        Merchant merchant = merchantRepository.save(Merchant.builder()
                .name(request.getName().trim())
                .merchantCode(generateUniqueCode())
                .build());
        return MerchantResponse.builder().merchantCode(merchant.getMerchantCode()).name(merchant.getName()).build();
    }

    @Transactional
    public QrisScanResponse scan(QrisScanRequest request, Principal principal) {
        User nasabah = currentNasabah(principal);
        Merchant merchant = merchantRepository.findByMerchantCode(normalizeCode(request.getMerchantCode()))
                .orElseThrow(() -> new IllegalArgumentException("QR tidak valid atau toko tidak ditemukan"));

        ActiveLimit activeLimit = activeLimitRepository.findByUserIdAndIsActiveTrue(nasabah.getId())
                .orElseThrow(() -> new IllegalStateException("No active limit found — complete your credit application first"));
        // Tolak dari awal SEBELUM bikin token kalau plafond belum eligible - biar nasabah gak
        // keburu masukin nominal dulu baru ditolak pas akhir.
        if (!QrisPolicy.isEligible(activeLimit.getTotalLimit())) {
            throw new IllegalStateException("Plafond Anda belum memenuhi syarat minimum Rp 2.000.000 untuk QRIS");
        }

        QrisTransaction transaction = qrisTransactionRepository.save(QrisTransaction.builder()
                .merchant(merchant)
                .user(nasabah)
                .token(UUID.randomUUID().toString())
                .status(QrisTransactionStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES))
                .build());

        return QrisScanResponse.builder()
                .token(transaction.getToken())
                .merchantName(merchant.getName())
                .expiresAt(transaction.getExpiresAt())
                .build();
    }

    @Transactional
    public QrisConfirmResponse confirm(String token, QrisConfirmRequest request, Principal principal) {
        User nasabah = currentNasabah(principal);
        QrisTransaction transaction = qrisTransactionRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Transaksi QRIS tidak ditemukan"));

        if (!QrisTransactionStatus.PENDING.equals(transaction.getStatus())) {
            throw new IllegalStateException("Transaksi ini sudah selesai diproses");
        }
        if (transaction.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Sesi QRIS sudah kedaluwarsa - silakan scan ulang");
        }
        if (!transaction.getUser().getId().equals(nasabah.getId())) {
            throw new IllegalStateException("Transaksi ini bukan milik Anda");
        }
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }

        ActiveLimit activeLimit = activeLimitRepository.findByUserIdAndIsActiveTrue(nasabah.getId())
                .orElseThrow(() -> new IllegalStateException("No active limit found — complete your credit application first"));

        if (!QrisPolicy.isEligible(activeLimit.getTotalLimit())) {
            throw new IllegalStateException("Plafond Anda belum memenuhi syarat minimum Rp 2.000.000 untuk QRIS");
        }
        if (request.getAmount().compareTo(activeLimit.getAvailableLimit()) > 0) {
            throw new IllegalArgumentException("Requested amount exceeds your available limit");
        }

        BigDecimal qrisUsedSoFar = loanService.getQrisUsedAmount(activeLimit);
        BigDecimal qrisQuota = QrisPolicy.quota(activeLimit.getTotalLimit());
        if (qrisUsedSoFar.add(request.getAmount()).compareTo(qrisQuota) > 0) {
            throw new IllegalArgumentException("Melebihi kuota QRIS Anda (Rp " + qrisQuota.toPlainString() + ")");
        }

        Loan loan = loanService.finalizeLoanForMerchant(activeLimit, nasabah, request.getAmount(), transaction.getMerchant());

        transaction.setStatus(QrisTransactionStatus.CONFIRMED);
        transaction.setAmount(request.getAmount());
        transaction.setLoan(loan);
        qrisTransactionRepository.save(transaction);

        // Tenor QRIS SELALU 1 bulan (finalizeLoanForMerchant) - jadi cuma ada 1 installment,
        // nominalnya otomatis sama persis dengan totalAmountDue (gak perlu query Installment
        // terpisah).
        return QrisConfirmResponse.builder()
                .loanId(loan.getId())
                .merchantName(transaction.getMerchant().getName())
                .requestedAmount(loan.getLoanDetails().getRequestedAmount())
                .totalAmountDue(loan.getLoanDetails().getTotalAmountDue())
                .installmentAmount(loan.getLoanDetails().getTotalAmountDue())
                .dueDate(loan.getCreatedAt().plusDays(30))
                .build();
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (merchantRepository.findByMerchantCode(code).isPresent());
        return code;
    }

    private User currentNasabah(Principal principal) {
        if (principal == null) throw new IllegalStateException("Authentication required");
        User user = userRepository.findByIdentity(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        if (user.getRole() == null || !"NASABAH".equalsIgnoreCase(user.getRole().getName())) {
            throw new IllegalStateException("Only Nasabah can use QRIS");
        }
        return user;
    }
}
