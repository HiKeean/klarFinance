package com.api.klarfinance.auth.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.api.klarfinance.auth.model.CustomerDetails;

import java.util.List;
import java.util.Optional;

public interface CustomerDetailsRepository extends JpaRepository<CustomerDetails, Integer> {
    Optional<CustomerDetails> findByUserId(Integer userId);

    List<CustomerDetails> findByUserIdIn(List<Integer> userIds);

    Optional<CustomerDetails> findByReferralCode(String referralCode);

    @Query("SELECT c FROM CustomerDetails c " +
            "JOIN FETCH c.user u " +
            "LEFT JOIN FETCH u.role r " +
            "WHERE (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:role IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :role, '%'))) " +
            "ORDER BY CASE WHEN u.deletedAt IS NULL THEN 0 ELSE 1 END ASC, c.createdAt DESC NULLS LAST")
    List<CustomerDetails> searchForAdminListing(@Param("search") String search,
                                                 @Param("role") String role,
                                                 Pageable pageable);

    @Query("SELECT COUNT(c) FROM CustomerDetails c " +
            "JOIN c.user u " +
            "LEFT JOIN u.role r " +
            "WHERE (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:role IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :role, '%')))")
    long countForAdminListing(@Param("search") String search, @Param("role") String role);
}
