package com.api.klarfinance.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.api.klarfinance.auth.model.DetailUserInternal;

import java.util.List;
import java.util.Optional;

@Repository
public interface DetailUserInternalRepository extends JpaRepository<DetailUserInternal, Integer> {
    Optional<DetailUserInternal> findByUserId(Integer userId);
    Page<DetailUserInternal> findByUserRoleNameIgnoreCase(String roleName, Pageable pageable);

    @Query("SELECT d FROM DetailUserInternal d " +
            "JOIN FETCH d.user u " +
            "LEFT JOIN FETCH u.role r " +
            "LEFT JOIN FETCH d.branch b " +
            "WHERE (:search IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:role IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :role, '%'))) " +
            "AND (:branchId IS NULL OR b.id = :branchId) " +
            "ORDER BY CASE WHEN u.deletedAt IS NULL THEN 0 ELSE 1 END ASC, d.createdAt DESC NULLS LAST")
    List<DetailUserInternal> searchForAdminListing(@Param("search") String search,
                                                    @Param("role") String role,
                                                    @Param("branchId") Long branchId,
                                                    Pageable pageable);

    @Query("SELECT COUNT(d) FROM DetailUserInternal d " +
            "JOIN d.user u " +
            "LEFT JOIN u.role r " +
            "LEFT JOIN d.branch b " +
            "WHERE (:search IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:role IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :role, '%'))) " +
            "AND (:branchId IS NULL OR b.id = :branchId)")
    long countForAdminListing(@Param("search") String search,
                               @Param("role") String role,
                               @Param("branchId") Long branchId);
}
