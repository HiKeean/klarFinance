package com.api.klarfinance.deskcall;

import com.api.klarfinance.config.AppConfigProperties;
import com.api.klarfinance.deskcall.dto.DeskcallCreateCallResponse;
import com.api.klarfinance.deskcall.dto.StartCallResponse;
import com.api.klarfinance.fin.dto.response.LoanCollectionContext;
import com.api.klarfinance.fin.service.FinDashboardService;
import com.api.klarfinance.global.PushNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeskcallCallServiceTest {

    @Mock
    private FinDashboardService finDashboardService;
    @Mock
    private DeskcallClient deskcallClient;
    @Mock
    private PushNotificationService pushNotificationService;

    private DeskcallCallService service;

    @BeforeEach
    void setUp() {
        AppConfigProperties properties = new AppConfigProperties();
        properties.setDeskcall(new AppConfigProperties.Deskcall());
        service = new DeskcallCallService(finDashboardService, deskcallClient, pushNotificationService, properties);
    }

    private LoanCollectionContext loan(String fcmToken) {
        return LoanCollectionContext.builder()
                .loanId(164).userId(42).customerName("Budi").birthDate(LocalDate.of(1990, 8, 17))
                .address("Jl. Mawar 1").fcmToken(fcmToken)
                .installmentAmount(new BigDecimal("2070000.00")).penaltyAmount(new BigDecimal("496800.00"))
                .dueDate(LocalDate.of(2026, 9, 13)).daysOverdue(12)
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void createsCallFromSystemDataAndSendsDataOnlyPushWithToken() {
        when(finDashboardService.getCollectionContext(164)).thenReturn(loan("fcm-1"));
        when(deskcallClient.createCall(anyMap())).thenReturn(new DeskcallCreateCallResponse(
                "call-1", "CREATED", "deskcall-call-1", "wss://lk", "jwt-customer", 30));
        when(pushNotificationService.sendData(eq("fcm-1"), anyMap(), any())).thenReturn(true);

        StartCallResponse response = service.startCall(164);

        ArgumentCaptor<Map<String, Object>> body = ArgumentCaptor.forClass(Map.class);
        verify(deskcallClient).createCall(body.capture());
        assertThat(body.getValue())
                .containsEntry("customerName", "Budi")
                .containsEntry("aiName", "Babi")
                .containsEntry("companyName", "KlarFinance")
                .containsEntry("installmentAmount", new BigDecimal("2070000"))
                .containsEntry("penaltyAmount", new BigDecimal("496800"))
                .containsEntry("dueDate", "2026-09-13")
                .containsEntry("birthDate", "1990-08-17")
                .containsEntry("externalLoanId", "164")
                .containsEntry("externalCustomerId", "42")
                .containsEntry("discloseAi", true);

        ArgumentCaptor<Map<String, String>> push = ArgumentCaptor.forClass(Map.class);
        verify(pushNotificationService).sendData(eq("fcm-1"), push.capture(), eq(Duration.ofSeconds(30)));
        assertThat(push.getValue())
                .containsEntry("type", DeskcallCallService.PUSH_TYPE_INCOMING_CALL)
                .containsEntry("callId", "call-1")
                .containsEntry("livekitUrl", "wss://lk")
                .containsEntry("token", "jwt-customer")
                .containsEntry("callerName", "KlarFinance")
                .containsKey("ringDeadlineEpochMs");
        assertThat(response.pushSent()).isTrue();
        assertThat(response.callId()).isEqualTo("call-1");
    }

    @Test
    void rejectsCustomerWithoutFcmTokenBeforeCreatingCall() {
        when(finDashboardService.getCollectionContext(164)).thenReturn(loan(null));

        assertThatThrownBy(() -> service.startCall(164))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FCM token kosong");
        verifyNoInteractions(deskcallClient, pushNotificationService);
    }
}
