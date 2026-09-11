package com.api.klarfinance.geo.service;

import com.api.klarfinance.geo.dto.response.RoutineSummaryResponse;
import com.api.klarfinance.geo.model.LocationPing;
import com.api.klarfinance.geo.repository.LocationPingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutineInferenceServiceTest {

    @Mock
    private LocationPingRepository pingRepository;

    private RoutineInferenceService service;

    @BeforeEach
    void setUp() {
        service = new RoutineInferenceService(pingRepository);
    }

    private LocationPing pingAt(double lat, double lng, LocalDateTime capturedAt) {
        return LocationPing.builder().latitude(lat).longitude(lng).capturedAt(capturedAt).build();
    }

    @Test
    void inferRoutine_noPingsReturnsEmptySummary() {
        when(pingRepository.findByUserIdAndCapturedAtAfterOrderByCapturedAtAsc(anyInt(), any())).thenReturn(List.of());

        RoutineSummaryResponse summary = service.inferRoutine(1);

        assertThat(summary.getTotalPingsAnalyzed()).isZero();
        assertThat(summary.getDaysCovered()).isZero();
        assertThat(summary.getHomeLocation()).isNull();
        assertThat(summary.getDaytimeLocation()).isNull();
    }

    @Test
    void inferRoutine_belowMinimumSamplesReturnsNullClusters() {
        // Single night ping only - below MIN_SAMPLES_FOR_PATTERN (2)
        LocalDateTime night = LocalDateTime.of(2026, 9, 1, 20, 0);
        when(pingRepository.findByUserIdAndCapturedAtAfterOrderByCapturedAtAsc(anyInt(), any()))
                .thenReturn(List.of(pingAt(-6.2, 106.8, night)));

        RoutineSummaryResponse summary = service.inferRoutine(1);

        assertThat(summary.getTotalPingsAnalyzed()).isEqualTo(1);
        assertThat(summary.getHomeLocation()).isNull();
    }

    @Test
    void inferRoutine_identifiesDominantNightClusterAsHome() {
        LocalDateTime day1Night = LocalDateTime.of(2026, 9, 1, 20, 0);
        LocalDateTime day2Night = LocalDateTime.of(2026, 9, 2, 21, 0);
        LocalDateTime day3Night = LocalDateTime.of(2026, 9, 3, 4, 30); // still "night" (hour < 5)

        List<LocationPing> pings = List.of(
                pingAt(-6.200000, 106.800000, day1Night),
                pingAt(-6.200001, 106.800001, day2Night), // same grid cell (rounds to same 3dp)
                pingAt(-6.500000, 106.900000, day3Night)  // different, one-off
        );
        when(pingRepository.findByUserIdAndCapturedAtAfterOrderByCapturedAtAsc(anyInt(), any())).thenReturn(pings);

        RoutineSummaryResponse summary = service.inferRoutine(1);

        assertThat(summary.getTotalPingsAnalyzed()).isEqualTo(3);
        assertThat(summary.getHomeLocation()).isNotNull();
        assertThat(summary.getHomeLocation().getSampleCount()).isEqualTo(2);
        assertThat(summary.getHomeLocation().getLatitude()).isCloseTo(-6.2, org.assertj.core.data.Offset.offset(0.001));
        assertThat(summary.getDaytimeLocation()).isNull(); // no daytime pings at all
    }

    @Test
    void inferRoutine_daysCoveredCountsDistinctCalendarDays() {
        LocalDateTime day1 = LocalDateTime.of(2026, 9, 1, 10, 0);
        LocalDateTime day1Later = LocalDateTime.of(2026, 9, 1, 14, 0);
        LocalDateTime day2 = LocalDateTime.of(2026, 9, 2, 10, 0);
        when(pingRepository.findByUserIdAndCapturedAtAfterOrderByCapturedAtAsc(anyInt(), any()))
                .thenReturn(List.of(pingAt(1, 1, day1), pingAt(1, 1, day1Later), pingAt(1, 1, day2)));

        RoutineSummaryResponse summary = service.inferRoutine(1);

        assertThat(summary.getDaysCovered()).isEqualTo(2);
    }

    @Test
    void inferRoutine_boundaryHoursClassifiedCorrectly() {
        // hour 5 (exactly) is daytime, hour 18 (exactly) is night - per isNight: hour>=18 || hour<5
        LocalDateTime fiveAm = LocalDateTime.of(2026, 9, 1, 5, 0);
        LocalDateTime fiveAmAgain = LocalDateTime.of(2026, 9, 2, 5, 0);
        LocalDateTime sixPm = LocalDateTime.of(2026, 9, 1, 18, 0);
        LocalDateTime sixPmAgain = LocalDateTime.of(2026, 9, 2, 18, 0);

        when(pingRepository.findByUserIdAndCapturedAtAfterOrderByCapturedAtAsc(anyInt(), any())).thenReturn(List.of(
                pingAt(10.0, 20.0, fiveAm), pingAt(10.0, 20.0, fiveAmAgain),
                pingAt(30.0, 40.0, sixPm), pingAt(30.0, 40.0, sixPmAgain)
        ));

        RoutineSummaryResponse summary = service.inferRoutine(1);

        assertThat(summary.getDaytimeLocation()).isNotNull();
        assertThat(summary.getDaytimeLocation().getLatitude()).isCloseTo(10.0, org.assertj.core.data.Offset.offset(0.001));
        assertThat(summary.getHomeLocation()).isNotNull();
        assertThat(summary.getHomeLocation().getLatitude()).isCloseTo(30.0, org.assertj.core.data.Offset.offset(0.001));
    }
}
