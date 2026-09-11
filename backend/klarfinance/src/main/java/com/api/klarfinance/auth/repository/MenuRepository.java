package com.api.klarfinance.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.klarfinance.auth.model.Menu;

import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, Integer> {
    Optional<Menu> findByName(String name);
    Optional<Menu> findByUrl(String url);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByUrlIgnoreCase(String url);
    Optional<Menu> findByNameAndUrl(String name, String url);
}
