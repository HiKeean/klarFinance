package com.api.klarfinance.fin.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.klarfinance.auth.model.User;
import com.api.klarfinance.fin.model.LoanReviewRequest;

import java.util.List;

public interface LoanReviewRequestRepository extends JpaRepository<LoanReviewRequest, Integer> {
    List<LoanReviewRequest> findByStatusOrderByCreatedAtAsc(String status);

    /** Dipakai LoanService.requestLoan() - selama nasabah masih punya satu pengajuan tarik tunai
     * yang PENDING_BM, dia tidak boleh mengajukan tarik tunai baru sama sekali (berapapun
     * nominalnya, tidak cuma yang di atas ambang review) sampai itu diputuskan BM. */
    boolean existsByUserAndStatus(User user, String status);
}
