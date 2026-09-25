package com.api.klarfinance.deskcall;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.api.klarfinance.config.AppConfigProperties;
import com.api.klarfinance.deskcall.dto.DeskcallCreateCallResponse;
import com.api.klarfinance.deskcall.dto.StartCallResponse;
import com.api.klarfinance.fin.dto.response.LoanCollectionContext;
import com.api.klarfinance.fin.service.FinDashboardService;
import com.api.klarfinance.global.PushNotificationService;

import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Tombol Call di NPL Report (DEMO): data tagihan dari fin -> deskcall membuat room LiveKit +
 * mengirim AI agent -> FCM data-only ke HP nasabah -> app menampilkan layar panggilan masuk dan
 * join room dengan token dari payload. Nominal/tanggal/nama hanya dari data sistem (aturan deskcall).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeskcallCallService {
    public static final String PUSH_TYPE_INCOMING_CALL = "DESKCALL_INCOMING_CALL";
    private static final int DEFAULT_RING_TIMEOUT_SECONDS = 45;

    private final FinDashboardService finDashboardService;
    private final DeskcallClient deskcallClient;
    private final PushNotificationService pushNotificationService;
    private final AppConfigProperties properties;

    public StartCallResponse startCall(Integer loanId) {
        LoanCollectionContext loan = finDashboardService.getCollectionContext(loanId);
        if (!StringUtils.hasText(loan.getFcmToken())) {
            throw new IllegalArgumentException("Nasabah " + loan.getCustomerName()
                    + " belum login di app KlarFinance (FCM token kosong), panggilan tidak bisa dikirim");
        }
        if (loan.getBirthDate() == null && !StringUtils.hasText(loan.getAddress())) {
            throw new IllegalArgumentException("Tanggal lahir/alamat nasabah kosong, verifikasi identitas tidak bisa dilakukan");
        }

        AppConfigProperties.Deskcall config = properties.getDeskcall();
        DeskcallCreateCallResponse call = deskcallClient.createCall(callContext(loan, config));
        int ringTimeout = call.ringTimeoutSeconds() != null ? call.ringTimeoutSeconds() : DEFAULT_RING_TIMEOUT_SECONDS;

        Map<String, String> data = Map.of(
                "type", PUSH_TYPE_INCOMING_CALL,
                "callId", call.callId(),
                "livekitUrl", call.livekitUrl(),
                "token", call.customerToken(),
                "callerName", config.getCompanyName(),
                "ringDeadlineEpochMs", String.valueOf(Instant.now().plusSeconds(ringTimeout).toEpochMilli()));
        boolean pushSent = pushNotificationService.sendData(loan.getFcmToken(), data, Duration.ofSeconds(ringTimeout));
        log.info("deskcall call {} untuk loan {} dibuat, push terkirim={}", call.callId(), loanId, pushSent);

        return new StartCallResponse(call.callId(), loanId, loan.getCustomerName(), ringTimeout, pushSent);
    }

    private Map<String, Object> callContext(LoanCollectionContext loan, AppConfigProperties.Deskcall config) {
        Map<String, Object> body = new HashMap<>();
        body.put("customerName", loan.getCustomerName());
        body.put("aiName", config.getAiName());
        body.put("companyName", config.getCompanyName());
        body.put("installmentAmount", loan.getInstallmentAmount().setScale(0, RoundingMode.HALF_UP));
        body.put("penaltyAmount", loan.getPenaltyAmount().setScale(0, RoundingMode.HALF_UP));
        body.put("dueDate", loan.getDueDate().toString());
        if (loan.getBirthDate() != null) body.put("birthDate", loan.getBirthDate().toString());
        if (StringUtils.hasText(loan.getAddress())) body.put("address", loan.getAddress());
        body.put("externalCustomerId", String.valueOf(loan.getUserId()));
        body.put("externalLoanId", String.valueOf(loan.getLoanId()));
        body.put("discloseAi", !Boolean.FALSE.equals(config.getDiscloseAi()));
        return body;
    }
}
