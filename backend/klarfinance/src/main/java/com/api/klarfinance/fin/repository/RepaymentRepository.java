package com.api.klarfinance.fin.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.klarfinance.fin.model.Repayment;

public interface RepaymentRepository extends JpaRepository<Repayment, Integer> {
}
