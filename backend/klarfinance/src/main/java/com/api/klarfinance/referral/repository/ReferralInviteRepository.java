package com.api.klarfinance.referral.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.klarfinance.referral.model.ReferralInvite;

import java.util.List;
import java.util.Optional;

public interface ReferralInviteRepository extends JpaRepository<ReferralInvite, Integer> {
    Optional<ReferralInvite> findByInviteeUser_Id(Integer inviteeUserId);

    List<ReferralInvite> findByReferrerUser_Id(Integer referrerUserId);

    long countByReferrerUser_Id(Integer referrerUserId);

    long countByReferrerUser_IdAndQualifiedAtIsNotNull(Integer referrerUserId);
}
