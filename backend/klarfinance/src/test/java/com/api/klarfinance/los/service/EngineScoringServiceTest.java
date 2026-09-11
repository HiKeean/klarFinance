package com.api.klarfinance.los.service;

import com.api.klarfinance.auth.model.CustomerDetails;
import com.api.klarfinance.auth.model.User;
import com.api.klarfinance.los.EngineStatus;
import com.api.klarfinance.los.model.ApplicationLog;
import com.api.klarfinance.los.model.LimitApplication;
import com.api.klarfinance.los.model.PefindoInquiry;
import com.api.klarfinance.los.repository.ApplicationLogRepository;
import com.api.klarfinance.los.repository.LimitApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EngineScoringServiceTest {

    @Mock private MockVidaService mockVidaService;
    @Mock private MockPefindoService mockPefindoService;
    @Mock private LimitApplicationRepository limitApplicationRepository;
    @Mock private ApplicationLogRepository applicationLogRepository;
    @Mock private CheckerAssignmentService checkerAssignmentService;
    @Mock private ApplicationCodeGenerator applicationCodeGenerator;

    private final EngineScoringDomain engineScoringDomain = new EngineScoringDomain();

    private EngineScoringService service;

    @BeforeEach
    void setUp() {
        service = new EngineScoringService(mockVidaService, mockPefindoService, engineScoringDomain,
                limitApplicationRepository, applicationLogRepository, checkerAssignmentService, applicationCodeGenerator);
        when(limitApplicationRepository.save(any(LimitApplication.class))).thenAnswer(invocation -> {
            LimitApplication app = invocation.getArgument(0);
            app.setId(1);
            return app;
        });
    }

    private User user() {
        return User.builder().id(1).identity("628111111111").build();
    }

    @Test
    void runScoring_vidaUnclear_retakePhotoDoesNotGenerateApplicationCodeOrAssignChecker() {
        User user = user();
        CustomerDetails details = CustomerDetails.builder().user(user).build();
        when(mockVidaService.check(details, BigDecimal.TEN)).thenReturn(new VidaResult(VidaResult.UNCLEAR, false, null));

        EngineScoringResult result = service.runScoring(user, details, BigDecimal.TEN, null, null);

        assertThat(result.status()).isEqualTo(EngineStatus.RETAKE_PHOTO);
        verify(applicationCodeGenerator, never()).generate();
        verify(checkerAssignmentService, never()).assignNewApplication(any());
        verifyNoInteractions(mockPefindoService);
    }

    @Test
    void runScoring_vidaRejected_autoRejectedNeverReachesChecker() {
        User user = user();
        CustomerDetails details = CustomerDetails.builder().user(user).build();
        when(mockVidaService.check(any(), any())).thenReturn(new VidaResult(VidaResult.REJECTED, false, null));

        EngineScoringResult result = service.runScoring(user, details, BigDecimal.TEN, null, null);

        assertThat(result.status()).isEqualTo(EngineStatus.REJECTED);
        verify(checkerAssignmentService, never()).assignNewApplication(any());
    }

    @Test
    void runScoring_vidaApproved_generatesApplicationCodeAndAssignsChecker() {
        User user = user();
        CustomerDetails details = CustomerDetails.builder().user(user).build();
        when(mockVidaService.check(any(), any())).thenReturn(new VidaResult(VidaResult.APPROVED, true, "5000000"));
        PefindoInquiry inquiry = PefindoInquiry.builder().colStatus(1).score("742").build();
        when(mockPefindoService.check()).thenReturn(new PefindoResult(inquiry));
        when(applicationCodeGenerator.generate()).thenReturn("2609100" + "1");

        EngineScoringResult result = service.runScoring(user, details, BigDecimal.TEN,
                List.of("Kredivo"), List.of("BCA"));

        assertThat(result.status()).isEqualTo(EngineStatus.PENDING_CHECKER);
        assertThat(result.suggestedLimit()).isEqualByComparingTo("18000000");

        ArgumentCaptor<LimitApplication> captor = ArgumentCaptor.forClass(LimitApplication.class);
        verify(limitApplicationRepository).save(captor.capture());
        LimitApplication saved = captor.getValue();
        assertThat(saved.getApplicationCode()).isNotBlank();
        assertThat(saved.getDetectedPinjolApps()).isEqualTo("Kredivo");
        assertThat(saved.getDetectedBankApps()).isEqualTo("BCA");
        assertThat(saved.getEngineScore()).isEqualTo(742);

        verify(checkerAssignmentService).assignNewApplication(saved);
        verify(applicationLogRepository).save(any(ApplicationLog.class));
    }

    @Test
    void runScoring_nullAppLists_joinedAsNull() {
        User user = user();
        CustomerDetails details = CustomerDetails.builder().user(user).build();
        when(mockVidaService.check(any(), any())).thenReturn(new VidaResult(VidaResult.UNCLEAR, false, null));

        service.runScoring(user, details, BigDecimal.TEN, null, null);

        ArgumentCaptor<LimitApplication> captor = ArgumentCaptor.forClass(LimitApplication.class);
        verify(limitApplicationRepository).save(captor.capture());
        assertThat(captor.getValue().getDetectedPinjolApps()).isNull();
        assertThat(captor.getValue().getDetectedBankApps()).isNull();
    }
}
