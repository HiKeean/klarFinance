package com.api.klarfinance.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.klarfinance.auth.model.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(String name);
}
