package com.api.klarfinance.qris.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.klarfinance.qris.model.QrisTransaction;

import java.util.Optional;

public interface QrisTransactionRepository extends JpaRepository<QrisTransaction, Integer> {
    Optional<QrisTransaction> findByToken(String token);
}
