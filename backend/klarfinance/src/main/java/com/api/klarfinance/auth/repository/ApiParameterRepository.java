package com.api.klarfinance.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.api.klarfinance.dbo.model.ApiParameter;

import java.util.Optional;

@Repository
public interface ApiParameterRepository extends JpaRepository<ApiParameter, Integer> {
    Optional<ApiParameter> findByClientType(String clientType);
}
