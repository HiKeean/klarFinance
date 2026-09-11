package com.api.klarfinance.los.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.klarfinance.los.model.ActiveLimit;

import java.util.Optional;

public interface ActiveLimitRepository extends JpaRepository<ActiveLimit, Integer> {
    Optional<ActiveLimit> findByUserIdAndIsActiveTrue(Integer userId);
}
