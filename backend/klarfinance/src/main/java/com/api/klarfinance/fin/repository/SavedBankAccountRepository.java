package com.api.klarfinance.fin.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.klarfinance.auth.model.User;
import com.api.klarfinance.fin.model.SavedBankAccount;

import java.util.List;

public interface SavedBankAccountRepository extends JpaRepository<SavedBankAccount, Integer> {
    List<SavedBankAccount> findByUserOrderByCreatedAtDesc(User user);

    boolean existsByUserAndBankCodeAndBankAccountNumber(User user, String bankCode, String bankAccountNumber);
}
