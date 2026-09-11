package com.api.klarfinance.dbo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.api.klarfinance.dbo.model.District;
import com.api.klarfinance.dbo.model.Village;

import java.util.List;

@Repository
public interface VillageRepository extends JpaRepository<Village, Long> {
    List<Village> findAllByDistrictId(Long districtId);
}
