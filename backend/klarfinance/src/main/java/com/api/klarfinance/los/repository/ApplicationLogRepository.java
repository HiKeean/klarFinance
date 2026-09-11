package com.api.klarfinance.los.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.klarfinance.los.model.ApplicationLog;

public interface ApplicationLogRepository extends JpaRepository<ApplicationLog, Integer> {
}
