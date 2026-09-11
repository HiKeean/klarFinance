package com.api.klarfinance.los.service;

import com.api.klarfinance.los.repository.LimitApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationCodeGeneratorTest {

    @Mock
    private LimitApplicationRepository limitApplicationRepository;

    private ApplicationCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ApplicationCodeGenerator(limitApplicationRepository);
    }

    @Test
    void generate_returnsCodeStartingWithTodaysDateAndTwoDigitSuffix() {
        when(limitApplicationRepository.existsByApplicationCode(anyString())).thenReturn(false);

        String code = generator.generate();

        String expectedDatePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        assertThat(code).hasSize(8);
        assertThat(code).startsWith(expectedDatePart);
        assertThat(code.substring(6)).matches("\\d{2}");
    }

    @Test
    void generate_retriesOnCollisionUntilUniqueCodeFound() {
        when(limitApplicationRepository.existsByApplicationCode(anyString()))
                .thenReturn(true, true, false);

        String code = generator.generate();

        assertThat(code).hasSize(8);
        org.mockito.Mockito.verify(limitApplicationRepository, org.mockito.Mockito.times(3))
                .existsByApplicationCode(anyString());
    }

    @Test
    void generate_throwsAfterMaxAttemptsExhausted() {
        when(limitApplicationRepository.existsByApplicationCode(anyString())).thenReturn(true);

        assertThatThrownBy(() -> generator.generate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Gagal generate App ID unik");
    }
}
