package com.api.klarfinance.fin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.api.klarfinance.auth.model.User;
import com.api.klarfinance.auth.repository.UserRepository;
import com.api.klarfinance.fin.InstallmentStatus;
import com.api.klarfinance.fin.LoanStatusBucket;
import com.api.klarfinance.fin.dto.request.LoanRequest;
import com.api.klarfinance.fin.dto.request.RepaymentRequest;
import com.api.klarfinance.fin.dto.response.BankAccountResponse;
import com.api.klarfinance.fin.dto.response.LimitSummaryResponse;
import com.api.klarfinance.fin.dto.response.LoanHistoryItemResponse;
import com.api.klarfinance.fin.dto.response.LoanResponse;
import com.api.klarfinance.fin.model.Drawdown;
import com.api.klarfinance.fin.model.Installment;
import com.api.klarfinance.fin.model.Loan;
import com.api.klarfinance.fin.model.LoanDetails;
import com.api.klarfinance.fin.model.LoanReviewRequest;
import com.api.klarfinance.fin.model.Repayment;
import com.api.klarfinance.fin.model.SavedBankAccount;
import com.api.klarfinance.fin.repository.DrawdownRepository;
import com.api.klarfinance.fin.repository.InstallmentRepository;
import com.api.klarfinance.fin.repository.LoanDetailsRepository;
import com.api.klarfinance.fin.repository.LoanRepository;
import com.api.klarfinance.fin.repository.LoanReviewRequestRepository;
import com.api.klarfinance.fin.repository.RepaymentRepository;
import com.api.klarfinance.fin.repository.SavedBankAccountRepository;
import com.api.klarfinance.los.EngineStatus;
import com.api.klarfinance.los.model.ActiveLimit;
import com.api.klarfinance.los.repository.ActiveLimitRepository;
import com.api.klarfinance.los.service.MockPefindoService;
import com.api.klarfinance.los.service.PefindoResult;
import com.api.klarfinance.qris.QrisPolicy;
import com.api.klarfinance.qris.model.Merchant;
import com.api.klarfinance.referral.model.ReferralReward;
import com.api.klarfinance.referral.service.ReferralService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanService {
    private static final int DAYS_PER_INSTALLMENT = 30;

    /** Tanggal jatuh tempo tetap tiap bulan buat tagihan "Transportasi" (channel TRANSJAKARTA) -
     * public karena transjakarta module butuh ini juga buat nampilin due date dari billingCycle
     * yang tersimpan di Loan, tanpa query balik ke fin module (lihat TransjakartaTicketService). */
    public static final int TRANSJAKARTA_DUE_DAY = 25;

    private final UserRepository userRepository;
    private final ActiveLimitRepository activeLimitRepository;
    private final LoanRepository loanRepository;
    private final LoanDetailsRepository loanDetailsRepository;
    private final DrawdownRepository drawdownRepository;
    private final InstallmentRepository installmentRepository;
    private final LoanReviewRequestRepository loanReviewRequestRepository;
    private final SavedBankAccountRepository savedBankAccountRepository;
    private final RepaymentRepository repaymentRepository;
    private final ReferralService referralService;
    private final MockPefindoService mockPefindoService;

    /** Backs the "Available Loan" card on the Android Home screen (see kotlin-nasabah-app
     * knowledge) - previously hardcoded placeholder numbers there, this is what wires it to the
     * real totalLimit/usedLimit/availableLimit tracked on ActiveLimit (kept in sync by
     * requestLoan below every time the nasabah actually submits a pengajuan). */
    public LimitSummaryResponse getMyLimitSummary(Principal principal) {
        User nasabah = currentNasabah(principal);
        ActiveLimit activeLimit = activeLimitRepository.findByUserIdAndIsActiveTrue(nasabah.getId())
                .orElseThrow(() -> new IllegalStateException("No active limit found — complete your credit application first"));

        boolean qrisEligible = QrisPolicy.isEligible(activeLimit.getTotalLimit());
        return LimitSummaryResponse.builder()
                .totalLimit(activeLimit.getTotalLimit())
                .usedLimit(activeLimit.getUsedLimit())
                .availableLimit(activeLimit.getAvailableLimit())
                .qrisQuota(qrisEligible ? QrisPolicy.quota(activeLimit.getTotalLimit()) : null)
                .qrisUsedAmount(qrisEligible ? getQrisUsedAmount(activeLimit) : null)
                .hasPendingLoanReview(loanReviewRequestRepository.existsByUserAndStatus(nasabah, EngineStatus.PENDING_BM))
                .build();
    }

    /** Rekening tujuan pencairan yang pernah dipakai nasabah ini (konfirmasi user 2026-09-07) -
     * dipakai isi dropdown "Rekening Tujuan" di Android supaya nasabah gak perlu ketik ulang
     * tiap pengajuan. Kosong kalau belum pernah ada Loan bank-transfer yang berhasil sama sekali. */
    public List<BankAccountResponse> getMyBankAccounts(Principal principal) {
        User nasabah = currentNasabah(principal);
        return savedBankAccountRepository.findByUserOrderByCreatedAtDesc(nasabah).stream()
                .map(account -> BankAccountResponse.builder()
                        .id(account.getId())
                        .bankCode(account.getBankCode())
                        .bankAccountNumber(account.getBankAccountNumber())
                        .build())
                .toList();
    }

    /** Akumulasi drawdown channel QRIS untuk ActiveLimit ini - satu-satunya tempat yang boleh
     * dipanggil qris module (cross-module lewat service, bukan repository langsung - lihat
     * aturan modular di backend-architecture.md). Dipakai dashboard (di atas) dan QrisService. */
    public BigDecimal getQrisUsedAmount(ActiveLimit activeLimit) {
        return loanRepository.sumAmountByLimitIdAndChannel(activeLimit.getId(), "QRIS");
    }

    /** Backs Android's "History" page - semua Loan nasabah ini, tarik tunai (BANK_TRANSFER) dan
     * bayar QRIS (channel QRIS) sama-sama muncul di satu list yang sama, dibedakan lewat field
     * `type` (keduanya sama-sama disimpan sebagai Loan, lihat Drawdown.channel). Status per-loan
     * diturunkan dari Installment-nya masing-masing, pola yang sama dengan
     * FinDashboardService's per-branch bucket (CURRENT/OVERDUE, tanpa grace period) ditambah
     * PAID_OFF kalau semua cicilan sudah lunas. */
    public List<LoanHistoryItemResponse> getMyLoanHistory(Principal principal) {
        User nasabah = currentNasabah(principal);
        List<Loan> loans = loanRepository.findByLimit_User_IdOrderByCreatedAtDesc(nasabah.getId());
        if (loans.isEmpty()) {
            return List.of();
        }

        List<Integer> loanIds = loans.stream().map(Loan::getId).toList();
        Map<Integer, List<Installment>> installmentsByLoan = installmentRepository
                .findByLoan_IdInOrderByDueDateAsc(loanIds).stream()
                .collect(Collectors.groupingBy(installment -> installment.getLoan().getId()));

        LocalDateTime now = LocalDateTime.now();
        return loans.stream().map(loan -> toHistoryItem(loan, installmentsByLoan.getOrDefault(loan.getId(), List.of()), now)).toList();
    }

    private LoanHistoryItemResponse toHistoryItem(Loan loan, List<Installment> installments, LocalDateTime now) {
        Drawdown drawdown = loan.getDrawdown();
        boolean isQris = "QRIS".equals(drawdown.getChannel());
        boolean isTransjakarta = "TRANSJAKARTA".equals(drawdown.getChannel());

        long paidCount = installments.stream().filter(i -> InstallmentStatus.PAID.equals(i.getStatus())).count();
        Optional<Installment> nextUnpaid = installments.stream()
                .filter(i -> InstallmentStatus.UNPAID.equals(i.getStatus()))
                .min(Comparator.comparing(Installment::getDueDate));

        String status = nextUnpaid
                .map(installment -> now.isBefore(installment.getDueDate()) ? LoanStatusBucket.CURRENT : LoanStatusBucket.OVERDUE)
                .map(bucket -> LoanStatusBucket.OVERDUE.equals(bucket) ? "OVERDUE" : "ACTIVE")
                .orElse("PAID_OFF");

        List<LoanHistoryItemResponse.InstallmentItem> installmentItems = installments.stream()
                .sorted(Comparator.comparing(Installment::getInstallmentNumber))
                .map(installment -> LoanHistoryItemResponse.InstallmentItem.builder()
                        .installmentNumber(installment.getInstallmentNumber())
                        .dueDate(installment.getDueDate())
                        .amount(installment.getAmount())
                        .status(installment.getStatus())
                        .paidAt(installment.getPaidAt())
                        .build())
                .toList();

        return LoanHistoryItemResponse.builder()
                .loanId(loan.getId())
                .type(isQris ? "QRIS_PAYMENT" : isTransjakarta ? "TRANSJAKARTA_BILL" : "LOAN")
                .merchantName(isQris && drawdown.getMerchant() != null ? drawdown.getMerchant().getName() : null)
                .requestedAmount(loan.getLoanDetails().getRequestedAmount())
                .totalAmountDue(loan.getLoanDetails().getTotalAmountDue())
                .tenorMonths(loan.getTenor())
                .status(status)
                .paidInstallments((int) paidCount)
                .totalInstallments(installments.size())
                .nextDueDate(nextUnpaid.map(Installment::getDueDate).orElse(null))
                .nextDueAmount(nextUnpaid.map(Installment::getAmount).orElse(null))
                .createdAt(loan.getCreatedAt())
                .installments(installmentItems)
                .build();
    }

    /**
     * Fitur "Bayar" (konfirmasi user): nasabah TIDAK wajib bayar persis nominal cicilan.
     * <ul>
     *   <li>Kalau cicilan terdekat yang belum lunas SUDAH jatuh tempo (due date &lt;= sekarang,
     *   tanpa grace period - konsisten sama {@link LoanStatusBucket}) - minimum bayar = nominal
     *   cicilan itu (harus cukup buat melunasi cicilan yang jatuh tempo).</li>
     *   <li>Kalau BELUM jatuh tempo - boleh bayar berapa saja (asal &gt; 0), langsung mengurangi
     *   pokok cicilan terdekat (bukan cuma dicatat, prinsipal-nya beneran turun).</li>
     * </ul>
     * Pembayaran dialokasikan ke cicilan UNPAID paling awal dulu (installmentNumber ascending) -
     * kalau nominalnya lebih dari cukup buat satu cicilan, sisanya otomatis "nyicil di muka" ke
     * cicilan berikutnya (bisa melunasi/mengurangi beberapa cicilan sekaligus dalam satu bayar).
     * Satu baris {@link Repayment} ditulis PER cicilan yang tersentuh (entity ini FK ke satu
     * Installment, bukan satu Loan) - audit trail nominal yang genuinely dialokasikan ke cicilan
     * itu, bukan total yang diinput nasabah.
     */
    @Transactional
    public LoanHistoryItemResponse repay(Integer loanId, RepaymentRequest request, Principal principal) {
        User nasabah = currentNasabah(principal);
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));
        if (!loan.getLimit().getUser().getId().equals(nasabah.getId())) {
            throw new IllegalStateException("This loan does not belong to you");
        }
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }

        List<Installment> unpaid = installmentRepository.findByLoan_IdInOrderByDueDateAsc(List.of(loanId)).stream()
                .filter(i -> InstallmentStatus.UNPAID.equals(i.getStatus()))
                .sorted(Comparator.comparing(Installment::getInstallmentNumber))
                .toList();
        if (unpaid.isEmpty()) {
            throw new IllegalStateException("Pinjaman ini sudah lunas");
        }

        LocalDateTime now = LocalDateTime.now();
        Installment nextUnpaid = unpaid.get(0);
        boolean isDue = !now.isBefore(nextUnpaid.getDueDate());
        BigDecimal minimumAmount = isDue ? nextUnpaid.getAmount() : BigDecimal.ZERO;
        if (request.getAmount().compareTo(minimumAmount) < 0) {
            throw new IllegalArgumentException(
                    "Cicilan sudah jatuh tempo - minimum pembayaran Rp " + minimumAmount.toPlainString());
        }

        BigDecimal totalRemaining = unpaid.stream().map(Installment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (request.getAmount().compareTo(totalRemaining) > 0) {
            throw new IllegalArgumentException("Nominal melebihi total sisa tagihan Rp " + totalRemaining.toPlainString());
        }

        BigDecimal pool = request.getAmount();
        for (Installment installment : unpaid) {
            if (pool.signum() <= 0) {
                break;
            }
            BigDecimal applied;
            if (pool.compareTo(installment.getAmount()) >= 0) {
                applied = installment.getAmount();
                installment.setAmount(BigDecimal.ZERO);
                installment.setStatus(InstallmentStatus.PAID);
                installment.setPaidAt(now);
            } else {
                applied = pool;
                installment.setAmount(installment.getAmount().subtract(pool));
            }
            pool = pool.subtract(applied);
            installmentRepository.save(installment);
            repaymentRepository.save(Repayment.builder()
                    .installment(installment)
                    .paymentAmount(applied)
                    .paymentMethod("MANUAL")
                    .paymentChannel("APP")
                    .status("SUCCESS")
                    .build());
        }

        List<Installment> refreshed = installmentRepository.findByLoan_IdInOrderByDueDateAsc(List.of(loanId));
        return toHistoryItem(loan, refreshed, now);
    }

    @Transactional
    public LoanResponse requestLoan(LoanRequest request, Principal principal) {
        User nasabah = currentNasabah(principal);
        validateRequest(request);

        ActiveLimit activeLimit = activeLimitRepository.findByUserIdAndIsActiveTrue(nasabah.getId())
                .orElseThrow(() -> new IllegalStateException("No active limit found — complete your credit application first"));

        // Konfirmasi user (2026-09-07): selama masih ada pengajuan tarik tunai yang PENDING_BM,
        // nasabah tidak boleh mengajukan tarik tunai APAPUN lagi (nominal berapapun) sampai itu
        // diputuskan - berlaku HANYA untuk tarik tunai (requestLoan ini), TIDAK untuk QRIS
        // (QrisService.confirm tidak menyentuh LoanReviewRequest sama sekali, sengaja dibiarkan
        // tetap bisa dipakai selama review berjalan).
        if (loanReviewRequestRepository.existsByUserAndStatus(nasabah, EngineStatus.PENDING_BM)) {
            throw new IllegalStateException(
                    "Anda memiliki pengajuan pinjaman tunai yang sedang direview oleh Branch Manager - tidak bisa mengajukan pinjaman tunai baru sampai itu diputuskan.");
        }

        if (request.getAmount().compareTo(activeLimit.getAvailableLimit()) > 0) {
            throw new IllegalArgumentException("Requested amount exceeds your available limit");
        }

        // Konfirmasi user: (pinjaman berjalan + pinjaman baru) > 30% dari plafond -> wajib
        // direview ulang BM (skip Checker) sebelum cair, BUKAN langsung diproses seperti biasa.
        if (LoanInterestPolicy.exceedsReviewThreshold(activeLimit.getUsedLimit(), request.getAmount(), activeLimit.getTotalLimit())) {
            return createLoanReviewRequest(nasabah, activeLimit, request);
        }

        return finalizeLoan(activeLimit, nasabah, request.getAmount(), request.getTenorMonths(),
                request.getBankAccountNumber(), request.getBankCode());
    }

    /**
     * Ditahan sebagai LoanReviewRequest (PENDING_BM) - tidak menyentuh ActiveLimit/membuat Loan
     * sama sekali sampai BM memutuskan (lihat LoanReviewService.bmDecide, yang pada APPROVE
     * memanggil {@link #finalizeLoan} dengan data yang sama persis yang disimpan di sini).
     */
    private LoanResponse createLoanReviewRequest(User nasabah, ActiveLimit activeLimit, LoanRequest request) {
        BigDecimal utilization = LoanInterestPolicy.utilizationPercentAfter(
                activeLimit.getUsedLimit(), request.getAmount(), activeLimit.getTotalLimit());

        // Refresh KOL/score - sinyal risiko bisa berubah sejak pengajuan limit awal, BM perlu lihat
        // data terbaru, bukan PefindoInquiry lama yang nempel di LimitApplication awal.
        PefindoResult pefindoResult = mockPefindoService.check();

        LoanReviewRequest review = loanReviewRequestRepository.save(LoanReviewRequest.builder()
                .user(nasabah)
                .activeLimit(activeLimit)
                .requestedAmount(request.getAmount())
                .tenorMonths(request.getTenorMonths())
                .bankAccountNumber(request.getBankAccountNumber())
                .bankCode(request.getBankCode())
                .usedLimitSnapshot(activeLimit.getUsedLimit())
                .totalLimitSnapshot(activeLimit.getTotalLimit())
                .utilizationPercent(utilization)
                .detectedPinjolApps(join(request.getPinjolApps()))
                .detectedBankApps(join(request.getBankApps()))
                .pefindoScore(pefindoResult.inquiry().getScore())
                .pefindoColStatus(pefindoResult.inquiry().getColStatus())
                .status(EngineStatus.PENDING_BM)
                .build());

        return LoanResponse.builder()
                .reviewRequired(true)
                .reviewRequestId(review.getId())
                .message("Pengajuan Anda melebihi 30% dari limit yang tersedia - sedang direview lebih lanjut oleh Branch Manager.")
                .build();
    }

    /** Logic pencairan sebenarnya (Loan/LoanDetails/Drawdown/Installment + update ActiveLimit +
     * referral) - dipakai LANGSUNG oleh requestLoan (kalau di bawah threshold 30%) dan oleh
     * LoanReviewService.bmDecide (kalau BM approve pengajuan yang sempat ditahan di atas). */
    @Transactional
    public LoanResponse finalizeLoan(ActiveLimit activeLimit, User nasabah, BigDecimal amount,
                                      int tenorMonths, String bankAccountNumber, String bankCode) {
        // Biaya admin (konfirmasi user 2026-09-06) dipotong dari dana yang cair ke rekening
        // nasabah - bunga/tagihan tetap dihitung dari nominal pinjaman PENUH (lihat createLoan).
        BigDecimal adminFee = LoanInterestPolicy.calculateAdminFee(amount);
        BigDecimal disbursedAmount = amount.subtract(adminFee);

        Drawdown drawdown = drawdownRepository.save(Drawdown.builder()
                .drawdownAmount(disbursedAmount)
                .bankAccountNumber(bankAccountNumber)
                .bankCode(bankCode)
                .channel("BANK_TRANSFER")
                .status("PENDING") // pencairan manual — payment gateway otomatis di luar scope MVP
                .build());
        saveBankAccountIfNew(nasabah, bankCode, bankAccountNumber);

        LoanCreationResult result = createLoan(activeLimit, nasabah, amount, tenorMonths, drawdown, adminFee);
        return LoanResponse.builder()
                .loanId(result.loan().getId())
                .requestedAmount(amount)
                .disbursedAmount(disbursedAmount)
                .adminFee(adminFee)
                .totalAmountDue(result.totalAmountDue())
                .tenorMonths(tenorMonths)
                .installmentAmount(result.installmentAmount())
                .firstDueDate(LocalDateTime.now().plusDays(DAYS_PER_INSTALLMENT))
                .referralDiscountApplied(result.referralDiscount())
                .reviewRequired(false)
                .build();
    }

    /** Sama persis pipeline-nya dengan {@link #finalizeLoan}, cuma dana "cair" ke Merchant
     * (channel QRIS) bukan ke rekening bank - dipakai QrisService.confirm() setelah lolos
     * validasi kuota/eligibility QRIS (QrisPolicy). Tenor SELALU 1 bulan (konfirmasi user). */
    @Transactional
    public Loan finalizeLoanForMerchant(ActiveLimit activeLimit, User nasabah, BigDecimal amount, Merchant merchant) {
        Drawdown drawdown = drawdownRepository.save(Drawdown.builder()
                .drawdownAmount(amount)
                .merchant(merchant)
                .channel("QRIS")
                .status("PENDING")
                .build());

        return createLoan(activeLimit, nasabah, amount, 1, drawdown, BigDecimal.ZERO).loan();
    }

    /**
     * Beli tiket Transjakarta (konfirmasi user 2026-09-10) - beda TOTAL dari finalizeLoanForMerchant
     * (QRIS): QRIS bikin Loan BARU tiap transaksi, ini malah NUMPUK semua pembelian dalam satu
     * bulan kalender ke SATU Loan ("billingCycle" = YearMonth pembelian), biar muncul sebagai
     * SATU baris "Transportasi" di History, jatuh tempo tanggal {@value #TRANSJAKARTA_DUE_DAY}
     * bulan BERIKUTNYA - bukan rolling +30 hari kayak Loan lain. Gak ada kuota terpisah kayak QRIS
     * (QrisPolicy) - cukup dicek amount &lt;= availableLimit, langsung potong plafond umum.
     * Bunga tetap dihitung PER TIKET saat beli (bukan sekali di akhir siklus) lalu ditambahkan ke
     * total yang sudah ada - biar preview di app konsisten sama QRIS/pinjaman lain.
     */
    @Transactional
    public Loan purchaseTransjakartaTicket(User nasabah, BigDecimal amount) {
        ActiveLimit activeLimit = activeLimitRepository.findByUserIdAndIsActiveTrue(nasabah.getId())
                .orElseThrow(() -> new IllegalStateException("No active limit found — complete your credit application first"));
        if (amount.compareTo(activeLimit.getAvailableLimit()) > 0) {
            throw new IllegalArgumentException("Saldo plafond tidak mencukupi untuk membeli tiket ini");
        }

        String billingCycle = YearMonth.now().toString();
        return loanRepository.findOpenTransjakartaBill(activeLimit.getId(), billingCycle)
                .map(existing -> addToTransjakartaBill(existing, amount, activeLimit))
                .orElseGet(() -> createTransjakartaBill(activeLimit, amount, billingCycle));
    }

    private Loan createTransjakartaBill(ActiveLimit activeLimit, BigDecimal amount, String billingCycle) {
        Drawdown drawdown = drawdownRepository.save(Drawdown.builder()
                .drawdownAmount(amount)
                .channel("TRANSJAKARTA")
                .status("PENDING")
                .build());

        BigDecimal monthlyRate = LoanInterestPolicy.monthlyRatePercent(1);
        BigDecimal interestFee = ticketInterest(amount, monthlyRate);
        BigDecimal totalAmountDue = amount.add(interestFee);

        LoanDetails loanDetails = loanDetailsRepository.save(LoanDetails.builder()
                .requestedAmount(amount)
                .totalAmountDue(totalAmountDue)
                .adminFee(BigDecimal.ZERO)
                .interestPercent(monthlyRate)
                .interestFee(interestFee)
                .penaltyFee(BigDecimal.ZERO)
                .referralDiscount(BigDecimal.ZERO)
                .build());

        Loan loan = loanRepository.save(Loan.builder()
                .limit(activeLimit)
                .drawdown(drawdown)
                .loanDetails(loanDetails)
                .tenor(1)
                .billingCycle(billingCycle)
                .build());

        installmentRepository.save(Installment.builder()
                .loan(loan)
                .installmentNumber(1)
                .dueDate(transjakartaDueDate())
                .amount(totalAmountDue)
                .penalty(BigDecimal.ZERO)
                .status(InstallmentStatus.UNPAID)
                .build());

        applyLimitUsage(activeLimit, amount);
        return loan;
    }

    private Loan addToTransjakartaBill(Loan loan, BigDecimal amount, ActiveLimit activeLimit) {
        BigDecimal monthlyRate = LoanInterestPolicy.monthlyRatePercent(1);
        BigDecimal interestFee = ticketInterest(amount, monthlyRate);
        BigDecimal totalIncrement = amount.add(interestFee);

        Drawdown drawdown = loan.getDrawdown();
        drawdown.setDrawdownAmount(drawdown.getDrawdownAmount().add(amount));
        drawdownRepository.save(drawdown);

        LoanDetails details = loan.getLoanDetails();
        details.setRequestedAmount(details.getRequestedAmount().add(amount));
        details.setInterestFee(details.getInterestFee().add(interestFee));
        details.setTotalAmountDue(details.getTotalAmountDue().add(totalIncrement));
        loanDetailsRepository.save(details);

        // Tenor Transjakarta selalu 1 - satu-satunya Installment yang ada.
        Installment installment = installmentRepository.findByLoan_IdInOrderByDueDateAsc(List.of(loan.getId())).get(0);
        installment.setAmount(installment.getAmount().add(totalIncrement));
        // Kalau sempat dilunasi lebih awal (repay() manual) sebelum tiket baru ini dibeli, ada
        // tambahan utang baru bulan yang sama - buka lagi installment-nya, JANGAN biarkan status
        // PAID padahal amount-nya sekarang > 0.
        if (InstallmentStatus.PAID.equals(installment.getStatus())) {
            installment.setStatus(InstallmentStatus.UNPAID);
            installment.setPaidAt(null);
        }
        installmentRepository.save(installment);

        applyLimitUsage(activeLimit, amount);
        return loan;
    }

    private BigDecimal ticketInterest(BigDecimal amount, BigDecimal monthlyRate) {
        return amount.multiply(monthlyRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private void applyLimitUsage(ActiveLimit activeLimit, BigDecimal amount) {
        activeLimit.setUsedLimit(activeLimit.getUsedLimit().add(amount));
        activeLimit.setAvailableLimit(activeLimit.getAvailableLimit().subtract(amount));
        activeLimitRepository.save(activeLimit);
    }

    /** Siklus kalender 1-31 (konfirmasi user) - tiket dibeli bulan berapa aja jatuh temponya
     * SELALU tanggal {@value #TRANSJAKARTA_DUE_DAY} BULAN BERIKUTNYA, bukan rolling dari tanggal
     * beli kayak Loan lain (DAYS_PER_INSTALLMENT). */
    private LocalDateTime transjakartaDueDate() {
        return YearMonth.now().plusMonths(1).atDay(TRANSJAKARTA_DUE_DAY).atStartOfDay();
    }

    private record LoanCreationResult(Loan loan, BigDecimal totalAmountDue, BigDecimal installmentAmount,
                                       BigDecimal referralDiscount) {
    }

    /** Inti pembuatan Loan (LoanDetails/Installment/update ActiveLimit/referral) - Drawdown-nya
     * sudah dibuat oleh caller (finalizeLoan/finalizeLoanForMerchant) karena cara bikinnya beda
     * tergantung channel (bank transfer vs QRIS/merchant). */
    private LoanCreationResult createLoan(ActiveLimit activeLimit, User nasabah, BigDecimal amount,
                                           int tenorMonths, Drawdown drawdown, BigDecimal adminFee) {
        BigDecimal monthlyRate = LoanInterestPolicy.monthlyRatePercent(tenorMonths);
        BigDecimal interestFee = amount
                .multiply(monthlyRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(tenorMonths));
        BigDecimal totalAmountDue = amount.add(interestFee);

        // At most ONE referral reward applied per loan request (FIFO) - see
        // ReferralService.findAvailableRewardForApply/applyReward. Capped so the discount never
        // pushes totalAmountDue below the principal itself.
        Optional<ReferralReward> referralReward = referralService.findAvailableRewardForApply(nasabah);
        BigDecimal referralDiscount = referralReward
                .map(ReferralReward::getDiscountAmount)
                .map(discount -> discount.min(interestFee))
                .orElse(BigDecimal.ZERO);
        totalAmountDue = totalAmountDue.subtract(referralDiscount);

        LoanDetails loanDetails = loanDetailsRepository.save(LoanDetails.builder()
                .requestedAmount(amount)
                .totalAmountDue(totalAmountDue)
                .adminFee(adminFee)
                .interestPercent(monthlyRate)
                .interestFee(interestFee)
                .penaltyFee(BigDecimal.ZERO)
                .referralDiscount(referralDiscount)
                .build());

        Loan loan = loanRepository.save(Loan.builder()
                .limit(activeLimit)
                .drawdown(drawdown)
                .loanDetails(loanDetails)
                .tenor(tenorMonths)
                .build());

        BigDecimal installmentAmount = totalAmountDue.divide(BigDecimal.valueOf(tenorMonths), 2, RoundingMode.HALF_UP);
        generateInstallments(loan, totalAmountDue, installmentAmount, tenorMonths);

        activeLimit.setUsedLimit(activeLimit.getUsedLimit().add(amount));
        activeLimit.setAvailableLimit(activeLimit.getAvailableLimit().subtract(amount));
        activeLimitRepository.save(activeLimit);

        referralReward.ifPresent(reward -> referralService.applyReward(reward, loan));
        // Separate from the discount above: this checks whether NASABAH THEMSELVES is an
        // invitee whose referrer is owed a reward now that this loan meets the minimum.
        referralService.onLoanRequested(nasabah, amount);

        return new LoanCreationResult(loan, totalAmountDue, installmentAmount, referralDiscount);
    }

    /** Otomatis tersimpan begitu Loan bank-transfer berhasil (bukan langkah "simpan" terpisah
     * dari nasabah, konfirmasi user 2026-09-07) - dicek dulu biar gak numpuk duplikat kalau
     * nasabah pakai rekening yang sama berkali-kali. Cuma dipanggil dari finalizeLoan (bank
     * transfer), TIDAK dari finalizeLoanForMerchant (QRIS gak punya rekening bank tujuan). */
    private void saveBankAccountIfNew(User nasabah, String bankCode, String bankAccountNumber) {
        if (savedBankAccountRepository.existsByUserAndBankCodeAndBankAccountNumber(nasabah, bankCode, bankAccountNumber)) {
            return;
        }
        savedBankAccountRepository.save(SavedBankAccount.builder()
                .user(nasabah)
                .bankCode(bankCode)
                .bankAccountNumber(bankAccountNumber)
                .build());
    }

    private String join(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join(",", values);
    }

    private void generateInstallments(Loan loan, BigDecimal totalAmountDue, BigDecimal installmentAmount, int tenorMonths) {
        BigDecimal allocated = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();
        for (int i = 1; i <= tenorMonths; i++) {
            boolean isLast = i == tenorMonths;
            BigDecimal amount = isLast ? totalAmountDue.subtract(allocated) : installmentAmount;
            allocated = allocated.add(amount);

            installmentRepository.save(Installment.builder()
                    .loan(loan)
                    .installmentNumber(i)
                    .dueDate(now.plusDays((long) DAYS_PER_INSTALLMENT * i))
                    .amount(amount)
                    .penalty(BigDecimal.ZERO)
                    .status(InstallmentStatus.UNPAID)
                    .build());
        }
    }

    private void validateRequest(LoanRequest request) {
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (!LoanInterestPolicy.isValidTenor(request.getTenorMonths())) {
            throw new IllegalArgumentException("tenorMonths must be one of 1, 3, 6, 9, 12");
        }
        if (!StringUtils.hasText(request.getBankAccountNumber()) || !StringUtils.hasText(request.getBankCode())) {
            throw new IllegalArgumentException("bankAccountNumber and bankCode are required");
        }
    }

    private User currentNasabah(Principal principal) {
        if (principal == null) throw new IllegalStateException("Authentication required");
        User user = userRepository.findByIdentity(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        if (user.getRole() == null || !"NASABAH".equalsIgnoreCase(user.getRole().getName())) {
            throw new IllegalStateException("Only Nasabah can request a loan");
        }
        return user;
    }
}
