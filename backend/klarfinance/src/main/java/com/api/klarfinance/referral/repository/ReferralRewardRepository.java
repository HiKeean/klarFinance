package com.api.klarfinance.referral.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.klarfinance.referral.model.ReferralReward;

import java.util.List;
import java.util.Optional;

public interface ReferralRewardRepository extends JpaRepository<ReferralReward, Integer> {
    Optional<ReferralReward> findFirstByReferrerUser_IdAndStatusOrderByCreatedAtAsc(Integer referrerUserId, String status);

    List<ReferralReward> findByReferrerUser_IdAndStatus(Integer referrerUserId, String status);
}
