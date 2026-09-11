package com.api.klarfinance.geo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.klarfinance.geo.model.LocationConsent;

import java.util.Optional;

public interface LocationConsentRepository extends JpaRepository<LocationConsent, Integer> {
    Optional<LocationConsent> findByUserId(Integer userId);
}
