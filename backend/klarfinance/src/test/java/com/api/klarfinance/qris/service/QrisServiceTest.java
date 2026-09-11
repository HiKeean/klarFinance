package com.api.klarfinance.qris.service;

import com.api.klarfinance.auth.model.Role;
import com.api.klarfinance.auth.model.User;
import com.api.klarfinance.auth.repository.UserRepository;
import com.api.klarfinance.fin.model.Loan;
import com.api.klarfinance.fin.model.LoanDetails;
import com.api.klarfinance.fin.service.LoanService;
import com.api.klarfinance.los.model.ActiveLimit;
import com.api.klarfinance.los.repository.ActiveLimitRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QrisServiceTest {

    @Mock private MerchantRepository merchantRepository;
    @Mock private QrisTransactionRepository qrisTransactionRepository;
    @Mock private ActiveLimitRepository activeLimitRepository;
    @Mock private UserRepository userRepository;
    @Mock private LoanService loanService;

    private QrisService qrisService;

    @BeforeEach
    void setUp() {
        qrisService = new QrisService(merchantRepository, qrisTransactionRepository, activeLimitRepository, userRepository, loanService);
    }

    private User nasabah() {
        return User.builder().id(1).identity("628111111111").role(Role.builder().name("NASABAH").build()).build();
    }

    private Principal principalFor(String identity) {
        return () -> identity;
    }

    @Test
    void generateMerchant_rejectsBlankName() {
        GenerateMerchantRequest request = new GenerateMerchantRequest();
        request.setName("  ");

        assertThatThrownBy(() -> qrisService.generateMerchant(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name is required");
    }

    @Test
    void generateMerchant_savesTrimmedNameWithGeneratedCode() {
        GenerateMerchantRequest request = new GenerateMerchantRequest();
        request.setName("  Toko Sederhana  ");
        when(merchantRepository.findByMerchantCode(anyString())).thenReturn(Optional.empty());
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(invocation -> {
            Merchant m = invocation.getArgument(0);
            m.setId(1);
            return m;
        });

        MerchantResponse response = qrisService.generateMerchant(request);

        assertThat(response.getName()).isEqualTo("Toko Sederhana");
        assertThat(response.getMerchantCode()).hasSize(8);
    }

    @Test
    void scan_rejectsWhenActiveLimitBelowEligibilityThreshold() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        Merchant merchant = Merchant.builder().id(1).name("Toko").merchantCode("ABCDEFGH").build();
        when(merchantRepository.findByMerchantCode("ABCDEFGH")).thenReturn(Optional.of(merchant));
        ActiveLimit limit = ActiveLimit.builder().totalLimit(new BigDecimal("1000000")).build();
        when(activeLimitRepository.findByUserIdAndIsActiveTrue(1)).thenReturn(Optional.of(limit));

        QrisScanRequest request = new QrisScanRequest();
        request.setMerchantCode("abcdefgh");

        assertThatThrownBy(() -> qrisService.scan(request, principalFor("628111111111")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("2.000.000");
    }

    @Test
    void scan_eligibleCreatesPendingTransaction() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        Merchant merchant = Merchant.builder().id(1).name("Toko").merchantCode("ABCDEFGH").build();
        when(merchantRepository.findByMerchantCode("ABCDEFGH")).thenReturn(Optional.of(merchant));
        ActiveLimit limit = ActiveLimit.builder().totalLimit(new BigDecimal("5000000")).build();
        when(activeLimitRepository.findByUserIdAndIsActiveTrue(1)).thenReturn(Optional.of(limit));
        when(qrisTransactionRepository.save(any(QrisTransaction.class))).thenAnswer(invocation -> {
            QrisTransaction t = invocation.getArgument(0);
            t.setId(1);
            return t;
        });

        QrisScanRequest request = new QrisScanRequest();
        request.setMerchantCode("abcdefgh");

        QrisScanResponse response = qrisService.scan(request, principalFor("628111111111"));

        assertThat(response.getMerchantName()).isEqualTo("Toko");
        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void scan_unknownMerchantCodeThrows() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        when(merchantRepository.findByMerchantCode("ZZZZZZZZ")).thenReturn(Optional.empty());

        QrisScanRequest request = new QrisScanRequest();
        request.setMerchantCode("zzzzzzzz");

        assertThatThrownBy(() -> qrisService.scan(request, principalFor("628111111111")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private QrisTransaction pendingTransaction(User nasabah, Merchant merchant) {
        return QrisTransaction.builder()
                .id(1)
                .merchant(merchant)
                .user(nasabah)
                .token("tok-123")
                .status(QrisTransactionStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
    }

    @Test
    void confirm_rejectsAlreadyProcessedTransaction() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        Merchant merchant = Merchant.builder().id(1).name("Toko").build();
        QrisTransaction transaction = pendingTransaction(nasabah, merchant);
        transaction.setStatus(QrisTransactionStatus.CONFIRMED);
        when(qrisTransactionRepository.findByToken("tok-123")).thenReturn(Optional.of(transaction));

        QrisConfirmRequest request = new QrisConfirmRequest();
        request.setAmount(new BigDecimal("50000"));

        assertThatThrownBy(() -> qrisService.confirm("tok-123", request, principalFor("628111111111")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sudah selesai");
    }

    @Test
    void confirm_rejectsExpiredTransaction() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        Merchant merchant = Merchant.builder().id(1).name("Toko").build();
        QrisTransaction transaction = pendingTransaction(nasabah, merchant);
        transaction.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(qrisTransactionRepository.findByToken("tok-123")).thenReturn(Optional.of(transaction));

        QrisConfirmRequest request = new QrisConfirmRequest();
        request.setAmount(new BigDecimal("50000"));

        assertThatThrownBy(() -> qrisService.confirm("tok-123", request, principalFor("628111111111")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("kedaluwarsa");
    }

    @Test
    void confirm_rejectsNonPositiveAmount() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        Merchant merchant = Merchant.builder().id(1).name("Toko").build();
        QrisTransaction transaction = pendingTransaction(nasabah, merchant);
        when(qrisTransactionRepository.findByToken("tok-123")).thenReturn(Optional.of(transaction));

        QrisConfirmRequest request = new QrisConfirmRequest();
        request.setAmount(BigDecimal.ZERO);

        assertThatThrownBy(() -> qrisService.confirm("tok-123", request, principalFor("628111111111")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void confirm_rejectsAmountExceedingAvailableLimit() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        Merchant merchant = Merchant.builder().id(1).name("Toko").build();
        QrisTransaction transaction = pendingTransaction(nasabah, merchant);
        when(qrisTransactionRepository.findByToken("tok-123")).thenReturn(Optional.of(transaction));
        ActiveLimit limit = ActiveLimit.builder().totalLimit(new BigDecimal("5000000")).availableLimit(new BigDecimal("10000")).build();
        when(activeLimitRepository.findByUserIdAndIsActiveTrue(1)).thenReturn(Optional.of(limit));

        QrisConfirmRequest request = new QrisConfirmRequest();
        request.setAmount(new BigDecimal("50000"));

        assertThatThrownBy(() -> qrisService.confirm("tok-123", request, principalFor("628111111111")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("available limit");
    }

    @Test
    void confirm_rejectsWhenExceedsQrisQuota() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        Merchant merchant = Merchant.builder().id(1).name("Toko").build();
        QrisTransaction transaction = pendingTransaction(nasabah, merchant);
        when(qrisTransactionRepository.findByToken("tok-123")).thenReturn(Optional.of(transaction));
        // totalLimit 5jt -> qris quota 1jt (capped); usedSoFar 950rb, request 100rb -> exceeds
        ActiveLimit limit = ActiveLimit.builder().totalLimit(new BigDecimal("5000000")).availableLimit(new BigDecimal("5000000")).build();
        when(activeLimitRepository.findByUserIdAndIsActiveTrue(1)).thenReturn(Optional.of(limit));
        when(loanService.getQrisUsedAmount(limit)).thenReturn(new BigDecimal("950000"));

        QrisConfirmRequest request = new QrisConfirmRequest();
        request.setAmount(new BigDecimal("100000"));

        assertThatThrownBy(() -> qrisService.confirm("tok-123", request, principalFor("628111111111")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kuota QRIS");
    }

    @Test
    void confirm_successFinalizesLoanAndMarksTransactionConfirmed() {
        User nasabah = nasabah();
        when(userRepository.findByIdentity("628111111111")).thenReturn(Optional.of(nasabah));
        Merchant merchant = Merchant.builder().id(1).name("Toko").build();
        QrisTransaction transaction = pendingTransaction(nasabah, merchant);
        when(qrisTransactionRepository.findByToken("tok-123")).thenReturn(Optional.of(transaction));
        ActiveLimit limit = ActiveLimit.builder().totalLimit(new BigDecimal("5000000")).availableLimit(new BigDecimal("5000000")).build();
        when(activeLimitRepository.findByUserIdAndIsActiveTrue(1)).thenReturn(Optional.of(limit));
        when(loanService.getQrisUsedAmount(limit)).thenReturn(BigDecimal.ZERO);

        LoanDetails details = LoanDetails.builder().requestedAmount(new BigDecimal("100000")).totalAmountDue(new BigDecimal("103500")).build();
        Loan loan = Loan.builder().id(55).loanDetails(details).createdAt(LocalDateTime.now()).build();
        when(loanService.finalizeLoanForMerchant(limit, nasabah, new BigDecimal("100000"), merchant)).thenReturn(loan);

        QrisConfirmRequest request = new QrisConfirmRequest();
        request.setAmount(new BigDecimal("100000"));

        QrisConfirmResponse response = qrisService.confirm("tok-123", request, principalFor("628111111111"));

        assertThat(response.getLoanId()).isEqualTo(55);
        assertThat(response.getTotalAmountDue()).isEqualByComparingTo("103500");
        assertThat(transaction.getStatus()).isEqualTo(QrisTransactionStatus.CONFIRMED);
        assertThat(transaction.getAmount()).isEqualByComparingTo("100000");
    }

    @Test
    void currentNasabah_rejectsNonNasabahRole() {
        User staff = User.builder().id(2).identity("staff1").role(Role.builder().name("BM").build()).build();
        when(userRepository.findByIdentity("staff1")).thenReturn(Optional.of(staff));

        QrisScanRequest scanRequest = new QrisScanRequest();
        scanRequest.setMerchantCode("ABCDEFGH");

        assertThatThrownBy(() -> qrisService.scan(scanRequest, principalFor("staff1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only Nasabah");
    }
}
