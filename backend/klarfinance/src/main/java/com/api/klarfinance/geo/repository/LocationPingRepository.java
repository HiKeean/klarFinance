package com.api.klarfinance.geo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.klarfinance.geo.model.LocationPing;

import java.time.LocalDateTime;
import java.util.List;

public interface LocationPingRepository extends JpaRepository<LocationPing, Integer> {
    List<LocationPing> findByUserIdAndCapturedAtAfterOrderByCapturedAtAsc(Integer userId, LocalDateTime after);
}
