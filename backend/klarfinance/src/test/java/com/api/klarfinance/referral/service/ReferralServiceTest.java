package com.api.klarfinance.referral.service;

import com.api.klarfinance.auth.model.CustomerDetails;
import com.api.klarfinance.auth.model.User;
import com.api.klarfinance.auth.repository.CustomerDetailsRepository;
import com.api.klarfinance.fin.model.Loan;
import com.api.klarfinance.referral.dto.response.ReferralSummaryResponse;
import com.api.klarfinance.referral.model.ReferralInvite;
import com.api.klarfinance.referral.model.ReferralReward;
import com.api.klarfinance.referral.repository.ReferralInviteRepository;
import com.api.klarfinance.referral.repository.ReferralRewardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReferralServiceTest {

    @Mock
    private CustomerDetailsRepository customerDetailsRepository;
    @Mock
    private ReferralInviteRepository referralInviteRepository;
    @Mock
    private ReferralRewardRepository referralRewardRepository;

    private ReferralService referralService;

    @BeforeEach
    void setUp() {
        referralService = new ReferralService(customerDetailsRepository, referralInviteRepository, referralRewardRepository);
    }

    private User user(int id, String identity) {
        return User.builder().id(id).identity(identity).build();
    }

    @Test
    void onCustomerRegistered_assignsCodeAndIgnoresBlankReferralCode() {
        User newUser = user(1, "628111111111");
        CustomerDetails newCustomer = CustomerDetails.builder().user(newUser).build();

        referralService.onCustomerRegistered(newCustomer, "  ");

        assertThat(newCustomer.getReferralCode()).isNotBlank();
        assertThat(newCustomer.getReferralCode()).hasSize(6);
        verify(customerDetailsRepository).save(newCustomer);
        verifyNoInteractions(referralInviteRepository);
    }

    @Test
    void onCustomerRegistered_unknownReferralCodeIsIgnored() {
        User newUser = user(1, "628111111111");
        CustomerDetails newCustomer = CustomerDetails.builder().user(newUser).build();
        when(customerDetailsRepository.findByReferralCode(anyString())).thenReturn(Optional.empty());

        referralService.onCustomerRegistered(newCustomer, "BADCODE");

        verify(customerDetailsRepository).save(newCustomer);
        verifyNoInteractions(referralInviteRepository);
    }

    @Test
    void onCustomerRegistered_selfReferralIsIgnored() {
        User sameUser = user(1, "628111111111");
        CustomerDetails newCustomer = CustomerDetails.builder().user(sameUser).build();
        CustomerDetails referrerDetails = CustomerDetails.builder().user(sameUser).referralCode("ABC123").build();
        when(customerDetailsRepository.findByReferralCode(anyString())).thenReturn(Optional.empty());
        when(customerDetailsRepository.findByReferralCode("ABC123")).thenReturn(Optional.of(referrerDetails));

        referralService.onCustomerRegistered(newCustomer, "abc123");

        verifyNoInteractions(referralInviteRepository);
    }

    @Test
    void onCustomerRegistered_validReferralCreatesInvite() {
        User newUser = user(1, "628111111111");
        User referrerUser = user(2, "628222222222");
        CustomerDetails newCustomer = CustomerDetails.builder().user(newUser).build();
        CustomerDetails referrerDetails = CustomerDetails.builder().user(referrerUser).referralCode("ABC123").build();
        when(customerDetailsRepository.findByReferralCode(anyString())).thenReturn(Optional.empty());
        when(customerDetailsRepository.findByReferralCode("ABC123")).thenReturn(Optional.of(referrerDetails));

        referralService.onCustomerRegistered(newCustomer, "abc123");

        ArgumentCaptor<ReferralInvite> captor = ArgumentCaptor.forClass(ReferralInvite.class);
        verify(referralInviteRepository).save(captor.capture());
        ReferralInvite invite = captor.getValue();
        assertThat(invite.getReferrerUser()).isEqualTo(referrerUser);
        assertThat(invite.getInviteeUser()).isEqualTo(newUser);
        assertThat(invite.getCodeUsed()).isEqualTo("ABC123");
    }

    @Test
    void onLoanRequested_belowMinimumDoesNothing() {
        User invitee = user(1, "628111111111");
        referralService.onLoanRequested(invitee, new BigDecimal("999999"));
        verifyNoInteractions(referralInviteRepository);
        verifyNoInteractions(referralRewardRepository);
    }

    @Test
    void onLoanRequested_alreadyQualifiedInviteDoesNothing() {
        User invitee = user(1, "628111111111");
        ReferralInvite invite = ReferralInvite.builder().inviteeUser(invitee).qualifiedAt(java.time.LocalDateTime.now()).build();
        when(referralInviteRepository.findByInviteeUser_Id(1)).thenReturn(Optional.of(invite));

        referralService.onLoanRequested(invitee, ReferralService.MIN_QUALIFYING_LOAN_AMOUNT);

        verify(referralRewardRepository, never()).save(any());
    }

    @Test
    void onLoanRequested_qualifyingLoanCreatesRewardForReferrer() {
        User invitee = user(1, "628111111111");
        User referrer = user(2, "628222222222");
        ReferralInvite invite = ReferralInvite.builder().referrerUser(referrer).inviteeUser(invitee).qualifiedAt(null).build();
        when(referralInviteRepository.findByInviteeUser_Id(1)).thenReturn(Optional.of(invite));

        referralService.onLoanRequested(invitee, ReferralService.MIN_QUALIFYING_LOAN_AMOUNT);

        assertThat(invite.getQualifiedAt()).isNotNull();
        verify(referralInviteRepository).save(invite);

        ArgumentCaptor<ReferralReward> captor = ArgumentCaptor.forClass(ReferralReward.class);
        verify(referralRewardRepository).save(captor.capture());
        ReferralReward reward = captor.getValue();
        assertThat(reward.getReferrerUser()).isEqualTo(referrer);
        assertThat(reward.getDiscountAmount()).isEqualByComparingTo(ReferralService.REWARD_DISCOUNT_AMOUNT);
        assertThat(reward.getStatus()).isEqualTo(ReferralReward.STATUS_AVAILABLE);
    }

    @Test
    void onLoanRequested_noInviteRecordDoesNothing() {
        User invitee = user(1, "628111111111");
        when(referralInviteRepository.findByInviteeUser_Id(1)).thenReturn(Optional.empty());

        referralService.onLoanRequested(invitee, ReferralService.MIN_QUALIFYING_LOAN_AMOUNT);

        verify(referralRewardRepository, never()).save(any());
    }

    @Test
    void findAvailableRewardForApply_delegatesToRepository() {
        User referrer = user(2, "628222222222");
        ReferralReward reward = ReferralReward.builder().referrerUser(referrer).build();
        when(referralRewardRepository.findFirstByReferrerUser_IdAndStatusOrderByCreatedAtAsc(2, ReferralReward.STATUS_AVAILABLE))
                .thenReturn(Optional.of(reward));

        Optional<ReferralReward> result = referralService.findAvailableRewardForApply(referrer);

        assertThat(result).contains(reward);
    }

    @Test
    void applyReward_marksRewardAppliedWithLoan() {
        ReferralReward reward = ReferralReward.builder().status(ReferralReward.STATUS_AVAILABLE).build();
        Loan loan = Loan.builder().id(99).build();

        referralService.applyReward(reward, loan);

        assertThat(reward.getStatus()).isEqualTo(ReferralReward.STATUS_APPLIED);
        assertThat(reward.getAppliedLoan()).isEqualTo(loan);
        assertThat(reward.getAppliedAt()).isNotNull();
        verify(referralRewardRepository).save(reward);
    }

    @Test
    void getSummary_backfillsMissingReferralCodeAndAggregatesTotals() {
        User owner = user(1, "628111111111");
        CustomerDetails details = CustomerDetails.builder().user(owner).referralCode(null).build();
        when(customerDetailsRepository.findByUserId(1)).thenReturn(Optional.of(details));
        when(customerDetailsRepository.findByReferralCode(anyString())).thenReturn(Optional.empty());
        when(referralInviteRepository.countByReferrerUser_Id(1)).thenReturn(3L);
        when(referralInviteRepository.countByReferrerUser_IdAndQualifiedAtIsNotNull(1)).thenReturn(2L);
        ReferralReward reward1 = ReferralReward.builder().discountAmount(new BigDecimal("100000")).build();
        ReferralReward reward2 = ReferralReward.builder().discountAmount(new BigDecimal("100000")).build();
        when(referralRewardRepository.findByReferrerUser_IdAndStatus(1, ReferralReward.STATUS_AVAILABLE))
                .thenReturn(List.of(reward1, reward2));

        ReferralSummaryResponse summary = referralService.getSummary(owner);

        assertThat(details.getReferralCode()).isNotBlank();
        verify(customerDetailsRepository).save(details);
        assertThat(summary.getTotalInvited()).isEqualTo(3L);
        assertThat(summary.getTotalQualified()).isEqualTo(2L);
        assertThat(summary.getAvailableRewardsCount()).isEqualTo(2);
        assertThat(summary.getAvailableDiscountTotal()).isEqualByComparingTo("200000");
        assertThat(summary.getMinimumLoanAmount()).isEqualByComparingTo(ReferralService.MIN_QUALIFYING_LOAN_AMOUNT);
        assertThat(summary.getRewardAmount()).isEqualByComparingTo(ReferralService.REWARD_DISCOUNT_AMOUNT);
    }

    @Test
    void getSummary_throwsWhenCustomerProfileMissing() {
        User owner = user(1, "628111111111");
        when(customerDetailsRepository.findByUserId(1)).thenReturn(Optional.empty());

        try {
            referralService.getSummary(owner);
            org.junit.jupiter.api.Assertions.fail("expected IllegalStateException");
        } catch (IllegalStateException expected) {
            assertThat(expected).hasMessageContaining("Customer profile not found");
        }
    }
}
