package com.api.klarfinance.qris.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.klarfinance.qris.model.Merchant;

import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Integer> {
    Optional<Merchant> findByMerchantCode(String merchantCode);
}
