package com.api.klarfinance.fin.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.klarfinance.fin.model.LoanDetails;

public interface LoanDetailsRepository extends JpaRepository<LoanDetails, Integer> {
}
