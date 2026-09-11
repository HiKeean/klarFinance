package com.api.klarfinance.fin.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.klarfinance.fin.model.Drawdown;

public interface DrawdownRepository extends JpaRepository<Drawdown, Integer> {
}
