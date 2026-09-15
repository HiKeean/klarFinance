package com.api.klarfinance.fin.service;

import com.api.klarfinance.auth.model.Role;
import com.api.klarfinance.auth.model.User;
import com.api.klarfinance.auth.repository.UserRepository;
import com.api.klarfinance.fin.InstallmentStatus;
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
import com.api.klarfinance.los.model.PefindoInquiry;
import com.api.klarfinance.qris.model.Merchant;
import com.api.klarfinance.referral.service.ReferralService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers repay() proportional-principal-restore (see LoanService javadoc, fixed 2026-09-15),
 * requestLoan()'s 30%-threshold routing, and the Transjakarta/QRIS/bank-transfer drawdown
 * pipelines. Pure Mockito unit tests, no Spring context - mirrors QrisServiceTest's style.
 */
@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ActiveLimitRepository activeLimitRepository;
    @Mock private LoanRepository loanRepository;
    @Mock private LoanDetailsRepository loanDetailsRepository;
    @Mock private DrawdownRepository drawdownRepository;
    @Mock private InstallmentRepository installmentRepository;
    @Mock private LoanReviewRequestRepository loanReviewRequestRepository;
    @Mock private SavedBankAccountRepository savedBankAccountRepository;
    @Mock private RepaymentRepository repaymentRepository;
    @Mock private ReferralService referralService;
    @Mock private MockPefindoService mockPefindoService;

    private LoanService loanService;

    @BeforeEach
    void setUp() {
        loanService = new LoanService(userRepository, activeLimitRepository, loanRepository, loanDetailsRepository,
                drawdownRepository, installmentRepository, loanReviewRequestRepository, savedBankAccountRepository,
                repaymentRepository, referralService, mockPefindoService);
    }

    private User nasabah() {
        return User.builder().id(1).identity("628111111111").role(Role.builder().name("NASABAH").build()).build();
    }

    private Principal principalFor(String identity) {
        return () -> identity;
    }

    private ActiveLimit activeLimit(BigDecimal total, BigDecimal used, BigDecimal available) {
        return ActiveLimit.builder().id(10).totalLimit(total).usedLimit(used).availableLimit(available).build();
    }

    private Loan loanFor(ActiveLimit limit, User user, BigDecimal requested, BigDecimal totalDue, String channel) {
        Drawdown drawdown = Drawdown.builder().channel(channel).build();
        LoanDetails details = LoanDetails.builder().requestedAmount(requested).totalAmountDue(totalDue).build();
        ActiveLimit limitWithUser = limit;
        limitWithUser.setUser(user);
        return Loan.builder().id(100).limit(limitWithUser).drawdown(drawdown).loanDetails(details).tenor(6)
                .createdAt(LocalDateTime.now()).build();
    }

    private Installment installment(Loan loan, int number, BigDecimal amount, LocalDateTime dueDate, String status) {
        return Installment.builder().id(number).loan(loan).installmentNumber(number).amount(amount)
                .dueDate(dueDate).status(status).build();
    }

    // ------------------------------------------------------------------
    // getMyLimitSummary
    // ------------------------------------------------------------------

    @Test
    void getMyLimitSummary_throwsWhenNoActiveLimit() {
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah()));
        when(activeLimitRepository.findByUserIdAndIsActiveTrue(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.getMyLimitSummary(principalFor("628111111111")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No active limit found");
    }

    @Test
    void getMyLimitSummary_qrisEligibleIncludesQuotaFields() {
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah()));
        ActiveLimit limit = activeLimit(new BigDecimal("5000000"), new BigDecimal("1000000"), new BigDecimal("4000000"));
        when(activeLimitRepository.findByUserIdAndIsActiveTrue(1)).thenReturn(Optional.of(limit));
        when(loanRepository.sumAmountByLimitIdAndChannel(10, "QRIS")).thenReturn(new BigDecimal("200000"));
        when(loanReviewRequestRepository.existsByUserAndStatus(any(User.class), anyString())).thenReturn(false);

        LimitSummaryResponse response = loanService.getMyLimitSummary(principalFor("628111111111"));

        assertThat(response.getTotalLimit()).isEqualByComparingTo("5000000");
        assertThat(response.getQrisQuota()).isNotNull();
        assertThat(response.getQrisUsedAmount()).isEqualByComparingTo("200000");
        assertThat(response.isHasPendingLoanReview()).isFalse();
    }

    @Test
    void getMyLimitSummary_notQrisEligibleLeavesQuotaFieldsNull() {
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah()));
        ActiveLimit limit = activeLimit(new BigDecimal("1000000"), BigDecimal.ZERO, new BigDecimal("1000000"));
        when(activeLimitRepository.findByUserIdAndIsActiveTrue(1)).thenReturn(Optional.of(limit));
        when(loanReviewRequestRepository.existsByUserAndStatus(any(User.class), anyString())).thenReturn(true);

        LimitSummaryResponse response = loanService.getMyLimitSummary(principalFor("628111111111"));

        assertThat(response.getQrisQuota()).isNull();
        assertThat(response.getQrisUsedAmount()).isNull();
        assertThat(response.isHasPendingLoanReview()).isTrue();
    }

    // ------------------------------------------------------------------
    // getMyBankAccounts / getQrisUsedAmount
    // ------------------------------------------------------------------

    @Test
    void getMyBankAccounts_mapsToResponseList() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        SavedBankAccount account = SavedBankAccount.builder().id(1).user(nasabah).bankCode("BCA").bankAccountNumber("12345").build();
        when(savedBankAccountRepository.findByUserOrderByCreatedAtDesc(nasabah)).thenReturn(List.of(account));

        List<BankAccountResponse> response = loanService.getMyBankAccounts(principalFor("628111111111"));

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getBankCode()).isEqualTo("BCA");
        assertThat(response.get(0).getBankAccountNumber()).isEqualTo("12345");
    }

    @Test
    void getQrisUsedAmount_delegatesToRepository() {
        ActiveLimit limit = activeLimit(new BigDecimal("5000000"), BigDecimal.ZERO, new BigDecimal("5000000"));
        when(loanRepository.sumAmountByLimitIdAndChannel(10, "QRIS")).thenReturn(new BigDecimal("300000"));

        BigDecimal used = loanService.getQrisUsedAmount(limit);

        assertThat(used).isEqualByComparingTo("300000");
    }

    // ------------------------------------------------------------------
    // getMyLoanHistory
    // ------------------------------------------------------------------

    @Test
    void getMyLoanHistory_emptyWhenNoLoans() {
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah()));
        when(loanRepository.findByLimit_User_IdOrderByCreatedAtDesc(1)).thenReturn(List.of());

        List<LoanHistoryItemResponse> history = loanService.getMyLoanHistory(principalFor("628111111111"));

        assertThat(history).isEmpty();
    }

    @Test
    void getMyLoanHistory_activeLoanBeforeDueDate() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        ActiveLimit limit = activeLimit(new BigDecimal("5000000"), BigDecimal.ZERO, new BigDecimal("5000000"));
        Loan loan = loanFor(limit, nasabah, new BigDecimal("1000000"), new BigDecimal("1050000"), "BANK_TRANSFER");
        when(loanRepository.findByLimit_User_IdOrderByCreatedAtDesc(1)).thenReturn(List.of(loan));
        Installment unpaid = installment(loan, 1, new BigDecimal("1050000"), LocalDateTime.now().plusDays(10), InstallmentStatus.UNPAID);
        when(installmentRepository.findByLoan_IdInOrderByDueDateAsc(List.of(100))).thenReturn(List.of(unpaid));

        List<LoanHistoryItemResponse> history = loanService.getMyLoanHistory(principalFor("628111111111"));

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getType()).isEqualTo("LOAN");
        assertThat(history.get(0).getStatus()).isEqualTo("ACTIVE");
        assertThat(history.get(0).getPaidInstallments()).isZero();
        assertThat(history.get(0).getTotalInstallments()).isEqualTo(1);
    }

    @Test
    void getMyLoanHistory_overdueLoanAfterDueDate() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        ActiveLimit limit = activeLimit(new BigDecimal("5000000"), BigDecimal.ZERO, new BigDecimal("5000000"));
        Loan loan = loanFor(limit, nasabah, new BigDecimal("1000000"), new BigDecimal("1050000"), "BANK_TRANSFER");
        when(loanRepository.findByLimit_User_IdOrderByCreatedAtDesc(1)).thenReturn(List.of(loan));
        Installment overdue = installment(loan, 1, new BigDecimal("1050000"), LocalDateTime.now().minusDays(1), InstallmentStatus.UNPAID);
        when(installmentRepository.findByLoan_IdInOrderByDueDateAsc(List.of(100))).thenReturn(List.of(overdue));

        List<LoanHistoryItemResponse> history = loanService.getMyLoanHistory(principalFor("628111111111"));

        assertThat(history.get(0).getStatus()).isEqualTo("OVERDUE");
    }

    @Test
    void getMyLoanHistory_paidOffWhenAllInstallmentsPaid() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        ActiveLimit limit = activeLimit(new BigDecimal("5000000"), BigDecimal.ZERO, new BigDecimal("5000000"));
        Loan loan = loanFor(limit, nasabah, new BigDecimal("1000000"), new BigDecimal("1050000"), "BANK_TRANSFER");
        when(loanRepository.findByLimit_User_IdOrderByCreatedAtDesc(1)).thenReturn(List.of(loan));
        Installment paid = installment(loan, 1, BigDecimal.ZERO, LocalDateTime.now().minusDays(1), InstallmentStatus.PAID);
        when(installmentRepository.findByLoan_IdInOrderByDueDateAsc(List.of(100))).thenReturn(List.of(paid));

        List<LoanHistoryItemResponse> history = loanService.getMyLoanHistory(principalFor("628111111111"));

        assertThat(history.get(0).getStatus()).isEqualTo("PAID_OFF");
        assertThat(history.get(0).getPaidInstallments()).isEqualTo(1);
    }

    @Test
    void getMyLoanHistory_qrisChannelIncludesMerchantName() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        ActiveLimit limit = activeLimit(new BigDecimal("5000000"), BigDecimal.ZERO, new BigDecimal("5000000"));
        Loan loan = loanFor(limit, nasabah, new BigDecimal("100000"), new BigDecimal("103500"), "QRIS");
        loan.getDrawdown().setMerchant(Merchant.builder().id(1).name("Toko Sederhana").build());
        when(loanRepository.findByLimit_User_IdOrderByCreatedAtDesc(1)).thenReturn(List.of(loan));
        when(installmentRepository.findByLoan_IdInOrderByDueDateAsc(List.of(100))).thenReturn(List.of());

        List<LoanHistoryItemResponse> history = loanService.getMyLoanHistory(principalFor("628111111111"));

        assertThat(history.get(0).getType()).isEqualTo("QRIS_PAYMENT");
        assertThat(history.get(0).getMerchantName()).isEqualTo("Toko Sederhana");
    }

    @Test
    void getMyLoanHistory_transjakartaChannelHasNoMerchantName() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        ActiveLimit limit = activeLimit(new BigDecimal("5000000"), BigDecimal.ZERO, new BigDecimal("5000000"));
        Loan loan = loanFor(limit, nasabah, new BigDecimal("50000"), new BigDecimal("51750"), "TRANSJAKARTA");
        when(loanRepository.findByLimit_User_IdOrderByCreatedAtDesc(1)).thenReturn(List.of(loan));
        when(installmentRepository.findByLoan_IdInOrderByDueDateAsc(List.of(100))).thenReturn(List.of());

        List<LoanHistoryItemResponse> history = loanService.getMyLoanHistory(principalFor("628111111111"));

        assertThat(history.get(0).getType()).isEqualTo("TRANSJAKARTA_BILL");
        assertThat(history.get(0).getMerchantName()).isNull();
    }

    // ------------------------------------------------------------------
    // repay()
    // ------------------------------------------------------------------

    @Test
    void repay_loanNotFoundThrows() {
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah()));
        when(loanRepository.findById(100)).thenReturn(Optional.empty());
        RepaymentRequest request = new RepaymentRequest();
        request.setAmount(new BigDecimal("100000"));

        assertThatThrownBy(() -> loanService.repay(100, request, principalFor("628111111111")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Loan not found");
    }

    @Test
    void repay_notOwnerThrows() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        User otherOwner = User.builder().id(99).identity("other").role(Role.builder().name("NASABAH").build()).build();
        ActiveLimit limit = activeLimit(new BigDecimal("5000000"), BigDecimal.ZERO, new BigDecimal("5000000"));
        Loan loan = loanFor(limit, otherOwner, new BigDecimal("1000000"), new BigDecimal("1050000"), "BANK_TRANSFER");
        when(loanRepository.findById(100)).thenReturn(Optional.of(loan));

        RepaymentRequest request = new RepaymentRequest();
        request.setAmount(new BigDecimal("100000"));

        assertThatThrownBy(() -> loanService.repay(100, request, principalFor("628111111111")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not belong to you");
    }

    @Test
    void repay_nullAmountThrows() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        ActiveLimit limit = activeLimit(new BigDecimal("5000000"), BigDecimal.ZERO, new BigDecimal("5000000"));
        Loan loan = loanFor(limit, nasabah, new BigDecimal("1000000"), new BigDecimal("1050000"), "BANK_TRANSFER");
        when(loanRepository.findById(100)).thenReturn(Optional.of(loan));

        RepaymentRequest request = new RepaymentRequest();

        assertThatThrownBy(() -> loanService.repay(100, request, principalFor("628111111111")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void repay_noUnpaidInstallmentsThrowsAlreadyPaidOff() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        ActiveLimit limit = activeLimit(new BigDecimal("5000000"), BigDecimal.ZERO, new BigDecimal("5000000"));
        Loan loan = loanFor(limit, nasabah, new BigDecimal("1000000"), new BigDecimal("1050000"), "BANK_TRANSFER");
        when(loanRepository.findById(100)).thenReturn(Optional.of(loan));
        when(installmentRepository.findByLoan_IdInOrderByDueDateAsc(List.of(100))).thenReturn(List.of());

        RepaymentRequest request = new RepaymentRequest();
        request.setAmount(new BigDecimal("100000"));

        assertThatThrownBy(() -> loanService.repay(100, request, principalFor("628111111111")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sudah lunas");
    }

    @Test
    void repay_belowMinimumWhenDueThrows() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        ActiveLimit limit = activeLimit(new BigDecimal("5000000"), BigDecimal.ZERO, new BigDecimal("5000000"));
        Loan loan = loanFor(limit, nasabah, new BigDecimal("1000000"), new BigDecimal("1050000"), "BANK_TRANSFER");
        when(loanRepository.findById(100)).thenReturn(Optional.of(loan));
        Installment due = installment(loan, 1, new BigDecimal("175000"), LocalDateTime.now().minusDays(1), InstallmentStatus.UNPAID);
        when(installmentRepository.findByLoan_IdInOrderByDueDateAsc(List.of(100))).thenReturn(List.of(due));

        RepaymentRequest request = new RepaymentRequest();
        request.setAmount(new BigDecimal("50000"));

        assertThatThrownBy(() -> loanService.repay(100, request, principalFor("628111111111")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimum pembayaran");
    }

    @Test
    void repay_exceedsTotalRemainingThrows() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        ActiveLimit limit = activeLimit(new BigDecimal("5000000"), BigDecimal.ZERO, new BigDecimal("5000000"));
        Loan loan = loanFor(limit, nasabah, new BigDecimal("1000000"), new BigDecimal("1050000"), "BANK_TRANSFER");
        when(loanRepository.findById(100)).thenReturn(Optional.of(loan));
        Installment notYetDue = installment(loan, 1, new BigDecimal("175000"), LocalDateTime.now().plusDays(10), InstallmentStatus.UNPAID);
        when(installmentRepository.findByLoan_IdInOrderByDueDateAsc(List.of(100))).thenReturn(List.of(notYetDue));

        RepaymentRequest request = new RepaymentRequest();
        request.setAmount(new BigDecimal("999999999"));

        assertThatThrownBy(() -> loanService.repay(100, request, principalFor("628111111111")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("melebihi total sisa tagihan");
    }

    @Test
    void repay_fullSingleInstallmentRestoresProportionalPrincipalAndMarksPaid() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        // requested=1,000,000 totalDue=1,050,000 (5% bunga) - satu-satunya cicilan, full payoff.
        ActiveLimit limit = activeLimit(new BigDecimal("5000000"), new BigDecimal("1000000"), new BigDecimal("4000000"));
        Loan loan = loanFor(limit, nasabah, new BigDecimal("1000000"), new BigDecimal("1050000"), "BANK_TRANSFER");
        when(loanRepository.findById(100)).thenReturn(Optional.of(loan));
        Installment due = installment(loan, 1, new BigDecimal("1050000"), LocalDateTime.now().minusDays(1), InstallmentStatus.UNPAID);
        when(installmentRepository.findByLoan_IdInOrderByDueDateAsc(List.of(100))).thenReturn(List.of(due), List.of(due));
        when(installmentRepository.save(any(Installment.class))).thenAnswer(inv -> inv.getArgument(0));

        RepaymentRequest request = new RepaymentRequest();
        request.setAmount(new BigDecimal("1050000"));

        loanService.repay(100, request, principalFor("628111111111"));

        assertThat(due.getStatus()).isEqualTo(InstallmentStatus.PAID);
        assertThat(due.getAmount()).isEqualByComparingTo("0");
        // Full payoff -> full principal (1,000,000) restored, none of the 50,000 bunga touches the limit.
        assertThat(limit.getUsedLimit()).isEqualByComparingTo("0");
        assertThat(limit.getAvailableLimit()).isEqualByComparingTo("5000000");
        verify(repaymentRepository, times(1)).save(any());
        verify(activeLimitRepository, times(1)).save(limit);
    }

    @Test
    void repay_partialPaymentBeforeDueDateReducesAmountKeepsUnpaid() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        ActiveLimit limit = activeLimit(new BigDecimal("5000000"), new BigDecimal("1000000"), new BigDecimal("4000000"));
        Loan loan = loanFor(limit, nasabah, new BigDecimal("1000000"), new BigDecimal("1050000"), "BANK_TRANSFER");
        when(loanRepository.findById(100)).thenReturn(Optional.of(loan));
        Installment notYetDue = installment(loan, 1, new BigDecimal("1050000"), LocalDateTime.now().plusDays(10), InstallmentStatus.UNPAID);
        when(installmentRepository.findByLoan_IdInOrderByDueDateAsc(List.of(100))).thenReturn(List.of(notYetDue), List.of(notYetDue));
        when(installmentRepository.save(any(Installment.class))).thenAnswer(inv -> inv.getArgument(0));

        RepaymentRequest request = new RepaymentRequest();
        request.setAmount(new BigDecimal("500000"));

        loanService.repay(100, request, principalFor("628111111111"));

        assertThat(notYetDue.getStatus()).isEqualTo(InstallmentStatus.UNPAID);
        assertThat(notYetDue.getAmount()).isEqualByComparingTo("550000");
        // Proportional principal restored for the 500,000 paid out of 1,050,000 total due:
        // 1,000,000 * 500,000/1,050,000 = 476,190.48
        assertThat(limit.getAvailableLimit()).isEqualByComparingTo("4476190.48");
    }

    @Test
    void repay_paymentSpanningTwoInstallmentsTouchesBoth() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        ActiveLimit limit = activeLimit(new BigDecimal("5000000"), new BigDecimal("2000000"), new BigDecimal("3000000"));
        Loan loan = loanFor(limit, nasabah, new BigDecimal("2000000"), new BigDecimal("2100000"), "BANK_TRANSFER");
        when(loanRepository.findById(100)).thenReturn(Optional.of(loan));
        Installment first = installment(loan, 1, new BigDecimal("1050000"), LocalDateTime.now().minusDays(1), InstallmentStatus.UNPAID);
        Installment second = installment(loan, 2, new BigDecimal("1050000"), LocalDateTime.now().plusDays(29), InstallmentStatus.UNPAID);
        when(installmentRepository.findByLoan_IdInOrderByDueDateAsc(List.of(100)))
                .thenReturn(List.of(first, second), List.of(first, second));
        when(installmentRepository.save(any(Installment.class))).thenAnswer(inv -> inv.getArgument(0));

        RepaymentRequest request = new RepaymentRequest();
        // Pays off installment 1 in full (1,050,000) plus 200,000 into installment 2.
        request.setAmount(new BigDecimal("1250000"));

        loanService.repay(100, request, principalFor("628111111111"));

        assertThat(first.getStatus()).isEqualTo(InstallmentStatus.PAID);
        assertThat(second.getStatus()).isEqualTo(InstallmentStatus.UNPAID);
        assertThat(second.getAmount()).isEqualByComparingTo("850000");
        verify(repaymentRepository, times(2)).save(any());
    }

    // ------------------------------------------------------------------
    // requestLoan()
    // ------------------------------------------------------------------

    private LoanRequest validLoanRequest(BigDecimal amount) {
        LoanRequest request = new LoanRequest();
        request.setAmount(amount);
        request.setTenorMonths(6);
        request.setBankAccountNumber("12345");
        request.setBankCode("BCA");
        return request;
    }

    @Test
    void requestLoan_nonPositiveAmountThrows() {
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah()));

        assertThatThrownBy(() -> loanService.requestLoan(validLoanRequest(BigDecimal.ZERO), principalFor("628111111111")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void requestLoan_invalidTenorThrows() {
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah()));
        LoanRequest request = validLoanRequest(new BigDecimal("1000000"));
        request.setTenorMonths(2);

        assertThatThrownBy(() -> loanService.requestLoan(request, principalFor("628111111111")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenorMonths");
    }

    @Test
    void requestLoan_missingBankDetailsThrows() {
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah()));
        LoanRequest request = validLoanRequest(new BigDecimal("1000000"));
        request.setBankCode(null);

        assertThatThrownBy(() -> loanService.requestLoan(request, principalFor("628111111111")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bankAccountNumber and bankCode");
    }

    @Test
    void requestLoan_noActiveLimitThrows() {
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah()));
        when(activeLimitRepository.findByUserIdAndIsActiveTrue(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.requestLoan(validLoanRequest(new BigDecimal("1000000")), principalFor("628111111111")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No active limit found");
    }

    @Test
    void requestLoan_blockedByExistingPendingReview() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        ActiveLimit limit = activeLimit(new BigDecimal("10000000"), BigDecimal.ZERO, new BigDecimal("10000000"));
        when(activeLimitRepository.findByUserIdAndIsActiveTrue(1)).thenReturn(Optional.of(limit));
        when(loanReviewRequestRepository.existsByUserAndStatus(nasabah, EngineStatus.PENDING_BM)).thenReturn(true);

        assertThatThrownBy(() -> loanService.requestLoan(validLoanRequest(new BigDecimal("1000000")), principalFor("628111111111")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sedang direview");
    }

    @Test
    void requestLoan_amountExceedsAvailableLimitThrows() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        ActiveLimit limit = activeLimit(new BigDecimal("10000000"), BigDecimal.ZERO, new BigDecimal("500000"));
        when(activeLimitRepository.findByUserIdAndIsActiveTrue(1)).thenReturn(Optional.of(limit));
        when(loanReviewRequestRepository.existsByUserAndStatus(nasabah, EngineStatus.PENDING_BM)).thenReturn(false);

        assertThatThrownBy(() -> loanService.requestLoan(validLoanRequest(new BigDecimal("1000000")), principalFor("628111111111")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds your available limit");
    }

    @Test
    void requestLoan_exceedsThirtyPercentThresholdCreatesReviewRequest() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        // usedLimit 0 + requested 4,000,000 vs totalLimit 10,000,000 = 40% > 30% threshold.
        ActiveLimit limit = activeLimit(new BigDecimal("10000000"), BigDecimal.ZERO, new BigDecimal("10000000"));
        when(activeLimitRepository.findByUserIdAndIsActiveTrue(1)).thenReturn(Optional.of(limit));
        when(loanReviewRequestRepository.existsByUserAndStatus(nasabah, EngineStatus.PENDING_BM)).thenReturn(false);
        when(mockPefindoService.check()).thenReturn(new PefindoResult(PefindoInquiry.builder().score("700").colStatus(1).build()));
        when(loanReviewRequestRepository.save(any())).thenAnswer(inv -> {
            var review = inv.getArgument(0, com.api.klarfinance.fin.model.LoanReviewRequest.class);
            review.setId(555);
            return review;
        });

        LoanResponse response = loanService.requestLoan(validLoanRequest(new BigDecimal("4000000")), principalFor("628111111111"));

        assertThat(response.isReviewRequired()).isTrue();
        assertThat(response.getReviewRequestId()).isEqualTo(555);
        assertThat(response.getMessage()).contains("30%");
        verify(loanRepository, never()).save(any());
    }

    @Test
    void requestLoan_underThresholdFinalizesImmediately() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        ActiveLimit limit = activeLimit(new BigDecimal("10000000"), BigDecimal.ZERO, new BigDecimal("10000000"));
        when(activeLimitRepository.findByUserIdAndIsActiveTrue(1)).thenReturn(Optional.of(limit));
        when(loanReviewRequestRepository.existsByUserAndStatus(nasabah, EngineStatus.PENDING_BM)).thenReturn(false);
        when(savedBankAccountRepository.existsByUserAndBankCodeAndBankAccountNumber(nasabah, "BCA", "12345")).thenReturn(false);
        when(referralService.findAvailableRewardForApply(nasabah)).thenReturn(Optional.empty());
        when(drawdownRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loanDetailsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loanRepository.save(any())).thenAnswer(inv -> {
            Loan loan = inv.getArgument(0);
            loan.setId(200);
            return loan;
        });

        // 1,000,000 requested = 10% of 10,000,000 total limit, well under the 30% review threshold.
        LoanResponse response = loanService.requestLoan(validLoanRequest(new BigDecimal("1000000")), principalFor("628111111111"));

        assertThat(response.isReviewRequired()).isFalse();
        assertThat(response.getLoanId()).isEqualTo(200);
        assertThat(response.getRequestedAmount()).isEqualByComparingTo("1000000");
        // Admin fee 1% -> disbursed = 990,000.
        assertThat(response.getAdminFee()).isEqualByComparingTo("10000");
        assertThat(response.getDisbursedAmount()).isEqualByComparingTo("990000");
        assertThat(limit.getUsedLimit()).isEqualByComparingTo("1000000");
        assertThat(limit.getAvailableLimit()).isEqualByComparingTo("9000000");
        verify(savedBankAccountRepository).save(any());
        verify(referralService).onLoanRequested(nasabah, new BigDecimal("1000000"));
    }

    @Test
    void requestLoan_doesNotDuplicateSavedBankAccount() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        ActiveLimit limit = activeLimit(new BigDecimal("10000000"), BigDecimal.ZERO, new BigDecimal("10000000"));
        when(activeLimitRepository.findByUserIdAndIsActiveTrue(1)).thenReturn(Optional.of(limit));
        when(loanReviewRequestRepository.existsByUserAndStatus(nasabah, EngineStatus.PENDING_BM)).thenReturn(false);
        when(savedBankAccountRepository.existsByUserAndBankCodeAndBankAccountNumber(nasabah, "BCA", "12345")).thenReturn(true);
        when(referralService.findAvailableRewardForApply(nasabah)).thenReturn(Optional.empty());
        when(drawdownRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loanDetailsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        loanService.requestLoan(validLoanRequest(new BigDecimal("1000000")), principalFor("628111111111"));

        verify(savedBankAccountRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // finalizeLoanForMerchant() (QRIS)
    // ------------------------------------------------------------------

    @Test
    void finalizeLoanForMerchant_createsQrisDrawdownWithTenorOne() {
        User nasabah = nasabah();
        ActiveLimit limit = activeLimit(new BigDecimal("5000000"), BigDecimal.ZERO, new BigDecimal("5000000"));
        Merchant merchant = Merchant.builder().id(1).name("Toko").build();
        when(referralService.findAvailableRewardForApply(nasabah)).thenReturn(Optional.empty());
        when(drawdownRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loanDetailsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loanRepository.save(any())).thenAnswer(inv -> {
            Loan loan = inv.getArgument(0);
            loan.setId(300);
            return loan;
        });

        Loan loan = loanService.finalizeLoanForMerchant(limit, nasabah, new BigDecimal("100000"), merchant);

        assertThat(loan.getTenor()).isEqualTo(1);
        assertThat(loan.getDrawdown().getChannel()).isEqualTo("QRIS");
        assertThat(loan.getDrawdown().getMerchant()).isEqualTo(merchant);
    }

    // ------------------------------------------------------------------
    // purchaseTransjakartaTicket()
    // ------------------------------------------------------------------

    @Test
    void purchaseTransjakartaTicket_noActiveLimitThrows() {
        User nasabah = nasabah();
        when(activeLimitRepository.findByUserIdAndIsActiveTrue(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.purchaseTransjakartaTicket(nasabah, new BigDecimal("5000")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No active limit found");
    }

    @Test
    void purchaseTransjakartaTicket_exceedsAvailableLimitThrows() {
        User nasabah = nasabah();
        ActiveLimit limit = activeLimit(new BigDecimal("5000000"), new BigDecimal("4990000"), new BigDecimal("10000"));
        when(activeLimitRepository.findByUserIdAndIsActiveTrue(1)).thenReturn(Optional.of(limit));

        assertThatThrownBy(() -> loanService.purchaseTransjakartaTicket(nasabah, new BigDecimal("50000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tidak mencukupi");
    }

    @Test
    void purchaseTransjakartaTicket_createsNewBillWhenNoneOpenThisMonth() {
        User nasabah = nasabah();
        ActiveLimit limit = activeLimit(new BigDecimal("5000000"), BigDecimal.ZERO, new BigDecimal("5000000"));
        when(activeLimitRepository.findByUserIdAndIsActiveTrue(1)).thenReturn(Optional.of(limit));
        when(loanRepository.findOpenTransjakartaBill(eq(10), anyString())).thenReturn(Optional.empty());
        when(drawdownRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loanDetailsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loanRepository.save(any())).thenAnswer(inv -> {
            Loan loan = inv.getArgument(0);
            loan.setId(400);
            return loan;
        });

        Loan loan = loanService.purchaseTransjakartaTicket(nasabah, new BigDecimal("3500"));

        assertThat(loan.getId()).isEqualTo(400);
        assertThat(loan.getBillingCycle()).isNotBlank();
        assertThat(limit.getUsedLimit()).isEqualByComparingTo("3500");
        verify(installmentRepository).save(any(Installment.class));
    }

    @Test
    void purchaseTransjakartaTicket_addsToExistingBillAndReopensPaidInstallment() {
        User nasabah = nasabah();
        ActiveLimit limit = activeLimit(new BigDecimal("5000000"), new BigDecimal("3500"), new BigDecimal("4996500"));
        when(activeLimitRepository.findByUserIdAndIsActiveTrue(1)).thenReturn(Optional.of(limit));

        Drawdown drawdown = Drawdown.builder().channel("TRANSJAKARTA").drawdownAmount(new BigDecimal("3500")).build();
        LoanDetails details = LoanDetails.builder().requestedAmount(new BigDecimal("3500"))
                .totalAmountDue(new BigDecimal("3623")).interestFee(new BigDecimal("123")).build();
        Loan existing = Loan.builder().id(400).limit(limit).drawdown(drawdown).loanDetails(details).tenor(1).build();
        when(loanRepository.findOpenTransjakartaBill(eq(10), anyString())).thenReturn(Optional.of(existing));

        Installment alreadyPaid = installment(existing, 1, BigDecimal.ZERO, LocalDateTime.now().plusDays(5), InstallmentStatus.PAID);
        alreadyPaid.setPaidAt(LocalDateTime.now());
        when(installmentRepository.findByLoan_IdInOrderByDueDateAsc(List.of(400))).thenReturn(List.of(alreadyPaid));
        when(installmentRepository.save(any(Installment.class))).thenAnswer(inv -> inv.getArgument(0));

        loanService.purchaseTransjakartaTicket(nasabah, new BigDecimal("3500"));

        assertThat(alreadyPaid.getStatus()).isEqualTo(InstallmentStatus.UNPAID);
        assertThat(alreadyPaid.getPaidAt()).isNull();
        assertThat(details.getRequestedAmount()).isEqualByComparingTo("7000");
        assertThat(limit.getUsedLimit()).isEqualByComparingTo("7000");
    }

    // Static import helper (Mockito's eq isn't imported above to keep the static-import list short).
    private static Integer eq(int value) { return org.mockito.ArgumentMatchers.eq(value); }
}
