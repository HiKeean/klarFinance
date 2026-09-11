package com.api.klarfinance.dbo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.api.klarfinance.dbo.model.BranchTerritory;
import com.api.klarfinance.dbo.model.Regency;

import java.util.List;

@Repository
public interface BranchTerritoryRepository extends JpaRepository<BranchTerritory, Long> {
    long countByRegencyId(Long regencyId);
    boolean existsByBranchIdAndRegencyId(Long branchId, Long regencyId);
    List<BranchTerritory> findByBranchIdOrderByRegency_NameAsc(Long branchId);
    List<BranchTerritory> findAllByOrderByRegency_NameAsc();

    @Query("""
            select r from Regency r
            where r.province.id in :javaProvinceIds
              and r.id not in (select bt.regency.id from BranchTerritory bt)
            order by r.name
            """)
    List<Regency> findUncoveredRegencies(@Param("javaProvinceIds") List<Long> javaProvinceIds);
}
