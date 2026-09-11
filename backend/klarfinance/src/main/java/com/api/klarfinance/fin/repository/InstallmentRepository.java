package com.api.klarfinance.fin.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.klarfinance.fin.model.Installment;

import java.util.List;

public interface InstallmentRepository extends JpaRepository<Installment, Integer> {
    List<Installment> findByLoan_IdInOrderByDueDateAsc(List<Integer> loanIds);
}
