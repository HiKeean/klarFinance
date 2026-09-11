package com.api.klarfinance.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.api.klarfinance.auth.model.PasswordResetRequest;
import com.api.klarfinance.auth.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Long> {
    Optional<PasswordResetRequest> findByUserAndStatus(User user, String status);

    @Query("SELECT r FROM PasswordResetRequest r JOIN FETCH r.user u "
            + "WHERE (:status IS NULL OR r.status = :status) ORDER BY r.requestedAt DESC")
    List<PasswordResetRequest> findAllByOptionalStatus(@Param("status") String status);
}
