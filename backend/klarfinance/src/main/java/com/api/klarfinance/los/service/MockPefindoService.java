package com.api.klarfinance.los.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.api.klarfinance.los.model.PefindoInquiry;
import com.api.klarfinance.los.repository.PefindoInquiryRepository;

import java.security.SecureRandom;

/**
 * Dummy Pefindo credit-score check. Only generates the raw COL signal
 * (colStatus/colHistoryYearsAgo/score) with realistic distribution.
 * Turning that signal into a base plafond is {@link EngineScoringDomain}'s
 * job, kept separate so the COL -> limit rule (project_rules.md section 4B)
 * stays pure and unit-testable without needing to fight randomness here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockPefindoService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PefindoInquiryRepository pefindoInquiryRepository;

    public PefindoResult check() {
        int roll = RANDOM.nextInt(100);
        Integer colStatus;
        Integer colHistoryYearsAgo;
        int score;

        if (roll < 40) {
            // KOL 1 - Lancar
            colStatus = 1;
            colHistoryYearsAgo = null;
            score = 750 + RANDOM.nextInt(100);
        } else if (roll < 65) {
            // Medium case 1: KOL 2-4, more than 2 years ago
            colStatus = 2 + RANDOM.nextInt(3);
            colHistoryYearsAgo = 3 + RANDOM.nextInt(3);
            score = 600 + RANDOM.nextInt(100);
        } else if (roll < 90) {
            // Medium case 2: KOL 2 only, less than 2 years ago
            colStatus = 2;
            colHistoryYearsAgo = RANDOM.nextInt(2);
            score = 550 + RANDOM.nextInt(100);
        } else {
            // High risk: KOL 5 (Macet), more than 2 years ago
            colStatus = 5;
            colHistoryYearsAgo = 3 + RANDOM.nextInt(4);
            score = 400 + RANDOM.nextInt(100);
        }

        PefindoInquiry inquiry = PefindoInquiry.builder()
                .score(String.valueOf(score))
                .colStatus(colStatus)
                .colHistoryYearsAgo(colHistoryYearsAgo)
                .rawResponse("{\"mock\":true,\"colStatus\":" + colStatus + ",\"colHistoryYearsAgo\":" + colHistoryYearsAgo + "}")
                .build();
        pefindoInquiryRepository.save(inquiry);

        log.info("Mock Pefindo check -> colStatus={}, yearsAgo={}", colStatus, colHistoryYearsAgo);
        return new PefindoResult(inquiry);
    }
}
