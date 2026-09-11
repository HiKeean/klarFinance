package com.api.klarfinance.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.klarfinance.auth.model.User;

public interface UserRepository extends JpaRepository<User, Integer> {
    // Optional<User> findByEmail(String email); 
    Optional<User> findByIdentity(String identity);
}

