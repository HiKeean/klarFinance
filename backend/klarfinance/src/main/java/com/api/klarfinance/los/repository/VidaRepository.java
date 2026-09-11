package com.api.klarfinance.los.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.klarfinance.los.model.Vida;

import java.util.Optional;

public interface VidaRepository extends JpaRepository<Vida, Integer> {
    Optional<Vida> findFirstByUserIdOrderByCreatedAtDesc(Integer userId);
}
