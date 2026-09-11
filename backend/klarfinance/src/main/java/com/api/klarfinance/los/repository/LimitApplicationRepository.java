package com.api.klarfinance.los.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.api.klarfinance.los.model.LimitApplication;

import java.util.List;

public interface LimitApplicationRepository extends JpaRepository<LimitApplication, Integer> {
    long countByStatus(String status);
    List<LimitApplication> findByStatusOrderByCreatedAtAsc(String status);
    boolean existsByApplicationCode(String applicationCode);

    /**
     * Inquiry (konfirmasi user): cari berdasarkan App ID, nama, atau nomor HP. Aplikasi yang
     * belum masuk proses pengecekan (RETAKE_PHOTO - Vida belum lolos, belum pernah di-assign ke
     * siapapun) sengaja gak ikut ke-search.
     */
    @Query(value = "SELECT la.* FROM los.dbh_limit_applications la "
            + "JOIN auth.dbh_user u ON u.id = la.user_id AND u.deleted_at IS NULL "
            + "LEFT JOIN auth.dbh_customer_details cd ON cd.user_id = u.id "
            + "WHERE la.status <> 'RETAKE_PHOTO' "
            + "AND (la.application_code LIKE CONCAT('%', :q, '%') "
            + "     OR u.user_identity LIKE CONCAT('%', :q, '%') "
            + "     OR cd.name LIKE CONCAT('%', :q, '%')) "
            + "ORDER BY la.created_at DESC", nativeQuery = true)
    List<LimitApplication> search(@Param("q") String q);
}
