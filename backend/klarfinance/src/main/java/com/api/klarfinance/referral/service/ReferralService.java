package com.api.klarfinance.referral.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.api.klarfinance.auth.model.CustomerDetails;
import com.api.klarfinance.auth.model.User;
import com.api.klarfinance.auth.repository.CustomerDetailsRepository;
import com.api.klarfinance.fin.model.Loan;
import com.api.klarfinance.referral.dto.response.ReferralSummaryResponse;
import com.api.klarfinance.referral.model.ReferralInvite;
import com.api.klarfinance.referral.model.ReferralReward;
import com.api.klarfinance.referral.repository.ReferralInviteRepository;
import com.api.klarfinance.referral.repository.ReferralRewardRepository;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * "Ajak 1 orang, dapat diskon Rp100rb" referral scheme, confirmed with user 2026-09-04:
 * - Reward goes to the REFERRER (code owner), never the invitee.
 * - Qualifying event is the INVITEE's own loan reaching {@link #MIN_QUALIFYING_LOAN_AMOUNT},
 *   not registration itself - see {@link #onLoanRequested}.
 * - One reward per qualifying invitee (not a one-time cap on how many people can be invited).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralService {

    public static final BigDecimal MIN_QUALIFYING_LOAN_AMOUNT = BigDecimal.valueOf(1_000_000);
    public static final BigDecimal REWARD_DISCOUNT_AMOUNT = BigDecimal.valueOf(100_000);

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I - avoid look-alikes
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CustomerDetailsRepository customerDetailsRepository;
    private final ReferralInviteRepository referralInviteRepository;
    private final ReferralRewardRepository referralRewardRepository;

    /** Called once at registration (AuthService.register) - assigns a permanent referral code to
     * a newly-created CustomerDetails, and records the invite relationship if the new user
     * signed up with someone else's code. An invalid/unknown code is logged and ignored rather
     * than failing registration - a typo'd referral code shouldn't block onboarding. */
    @Transactional
    public void onCustomerRegistered(CustomerDetails newCustomer, String referralCodeUsed) {
        newCustomer.setReferralCode(generateUniqueCode());
        customerDetailsRepository.save(newCustomer);

        if (!StringUtils.hasText(referralCodeUsed)) {
            return;
        }

        Optional<CustomerDetails> referrer = customerDetailsRepository.findByReferralCode(referralCodeUsed.trim().toUpperCase());
        if (referrer.isEmpty()) {
            log.warn("Referral code {} used at registration does not match any nasabah - ignored", referralCodeUsed);
            return;
        }
        if (referrer.get().getUser().getId().equals(newCustomer.getUser().getId())) {
            log.warn("User {} tried to use their own referral code - ignored", newCustomer.getUser().getIdentity());
            return;
        }

        referralInviteRepository.save(ReferralInvite.builder()
                .referrerUser(referrer.get().getUser())
                .inviteeUser(newCustomer.getUser())
                .codeUsed(referralCodeUsed.trim().toUpperCase())
                .build());
    }

    /** Called from LoanService.requestLoan for EVERY nasabah loan request (referrer or not) -
     * checks whether this user is an unqualified invitee and, if this loan meets the minimum,
     * qualifies the invite and creates an AVAILABLE reward for the referrer. */
    @Transactional
    public void onLoanRequested(User invitee, BigDecimal loanAmount) {
        if (loanAmount == null || loanAmount.compareTo(MIN_QUALIFYING_LOAN_AMOUNT) < 0) {
            return;
        }

        referralInviteRepository.findByInviteeUser_Id(invitee.getId())
                .filter(invite -> invite.getQualifiedAt() == null)
                .ifPresent(invite -> {
                    invite.setQualifiedAt(LocalDateTime.now());
                    referralInviteRepository.save(invite);

                    referralRewardRepository.save(ReferralReward.builder()
                            .referrerUser(invite.getReferrerUser())
                            .referralInvite(invite)
                            .discountAmount(REWARD_DISCOUNT_AMOUNT)
                            .status(ReferralReward.STATUS_AVAILABLE)
                            .build());
                    log.info("Referral reward created for referrer {} (invitee {} qualified with loan {})",
                            invite.getReferrerUser().getIdentity(), invitee.getIdentity(), loanAmount);
                });
    }

    /** Oldest AVAILABLE reward for this user, FIFO - at most one is applied per loan request, see
     * LoanService.requestLoan. Does not mark it APPLIED by itself; call {@link #applyReward}
     * once the loan it's attached to is actually persisted. */
    public Optional<ReferralReward> findAvailableRewardForApply(User referrer) {
        return referralRewardRepository.findFirstByReferrerUser_IdAndStatusOrderByCreatedAtAsc(
                referrer.getId(), ReferralReward.STATUS_AVAILABLE);
    }

    @Transactional
    public void applyReward(ReferralReward reward, Loan appliedLoan) {
        reward.setStatus(ReferralReward.STATUS_APPLIED);
        reward.setAppliedLoan(appliedLoan);
        reward.setAppliedAt(LocalDateTime.now());
        referralRewardRepository.save(reward);
    }

    @Transactional
    public ReferralSummaryResponse getSummary(User user) {
        CustomerDetails details = customerDetailsRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Customer profile not found"));

        // Lazy backfill for nasabah who registered BEFORE this feature existed (their
        // referralCode was never set at registration time - onCustomerRegistered only runs
        // during register() itself).
        if (!StringUtils.hasText(details.getReferralCode())) {
            details.setReferralCode(generateUniqueCode());
            customerDetailsRepository.save(details);
        }

        long totalInvited = referralInviteRepository.countByReferrerUser_Id(user.getId());
        long totalQualified = referralInviteRepository.countByReferrerUser_IdAndQualifiedAtIsNotNull(user.getId());
        var availableRewards = referralRewardRepository.findByReferrerUser_IdAndStatus(user.getId(), ReferralReward.STATUS_AVAILABLE);
        BigDecimal availableDiscountTotal = availableRewards.stream()
                .map(ReferralReward::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ReferralSummaryResponse.builder()
                .referralCode(details.getReferralCode())
                .minimumLoanAmount(MIN_QUALIFYING_LOAN_AMOUNT)
                .rewardAmount(REWARD_DISCOUNT_AMOUNT)
                .totalInvited(totalInvited)
                .totalQualified(totalQualified)
                .availableRewardsCount(availableRewards.size())
                .availableDiscountTotal(availableDiscountTotal)
                .build();
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (customerDetailsRepository.findByReferralCode(code).isPresent());
        return code;
    }
}
