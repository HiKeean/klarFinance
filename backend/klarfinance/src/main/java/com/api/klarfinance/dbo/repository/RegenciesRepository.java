
package com.api.klarfinance.dbo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.api.klarfinance.dbo.model.Regency;

import java.util.List;

@Repository
public interface RegenciesRepository extends JpaRepository<Regency, Long> {
    List<Regency> findAllByProvinceId(Long provinceId);
}
