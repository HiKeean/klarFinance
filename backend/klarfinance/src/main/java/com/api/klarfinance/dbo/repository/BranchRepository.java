package com.api.klarfinance.dbo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.api.klarfinance.dbo.model.Branch;

import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    Optional<Branch> findByAddress(String address);

    @Query("""
            select b from Branch b
            where (:name is null or lower(b.name) like lower(concat('%', :name, '%')))
              and (:villageId is null or b.village.id = :villageId)
            """)
    Page<Branch> findAllByFilters(
            @Param("name") String name,
            @Param("villageId") Long villageId,
            Pageable pageable);

}
