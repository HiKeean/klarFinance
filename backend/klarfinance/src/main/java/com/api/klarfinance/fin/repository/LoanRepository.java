package com.api.klarfinance.fin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.api.klarfinance.fin.model.Loan;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Integer> {
    List<Loan> findByLimit_Branch_Id(Long branchId);

    /** Drill-down NPL Report per branch (webadmin) - search by nasabah name lewat CustomerDetails,
     * yang TIDAK punya relasi JPA langsung ke User (cuma FK user_id di kolom DB) makanya di-join
     * pakai ON eksplisit, bukan lewat path relasi biasa. JOIN FETCH lim/user/loanDetails biar gak
     * N+1 pas dipetakan ke BranchLoanDetailResponse (lihat FinDashboardService#getBranchLoanDetails). */
    @Query(value = "SELECT l FROM Loan l " +
            "JOIN FETCH l.limit lim " +
            "JOIN FETCH lim.user u " +
            "JOIN FETCH l.loanDetails " +
            "LEFT JOIN com.api.klarfinance.auth.model.CustomerDetails cd ON cd.user.id = u.id " +
            "WHERE lim.branch.id = :branchId " +
            "AND (:search IS NULL OR :search = '' OR LOWER(cd.name) LIKE LOWER(CONCAT('%', :search, '%')))",
            countQuery = "SELECT COUNT(l) FROM Loan l " +
            "JOIN l.limit lim " +
            "JOIN lim.user u " +
            "LEFT JOIN com.api.klarfinance.auth.model.CustomerDetails cd ON cd.user.id = u.id " +
            "WHERE lim.branch.id = :branchId " +
            "AND (:search IS NULL OR :search = '' OR LOWER(cd.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Loan> findByBranchIdAndNasabahNameContaining(@Param("branchId") Long branchId,
                                                        @Param("search") String search,
                                                        Pageable pageable);

    @Query("SELECT l FROM Loan l " +
            "JOIN FETCH l.limit lim " +
            "LEFT JOIN FETCH lim.branch " +
            "JOIN FETCH lim.user " +
            "JOIN FETCH l.loanDetails")
    List<Loan> findAllWithBranchAndDetails();

    /** Akumulasi drawdown per channel ("QRIS"/"BANK_TRANSFER") untuk satu ActiveLimit - dipakai
     * QrisPolicy/QrisService buat hitung kuota QRIS terpakai. Derive-on-read, gak ada kolom
     * terpisah yang disimpan - konsisten sama cara project ini ngitung status Loan/NPL. */
    @Query("SELECT COALESCE(SUM(d.drawdownAmount), 0) FROM Loan l JOIN l.drawdown d " +
            "WHERE l.limit.id = :activeLimitId AND d.channel = :channel")
    BigDecimal sumAmountByLimitIdAndChannel(@Param("activeLimitId") Integer activeLimitId, @Param("channel") String channel);

    /** Semua Loan (tarik tunai maupun QRIS - dibedakan lewat Drawdown.channel) milik satu
     * nasabah, terbaru duluan - dipakai halaman "History" Android. JOIN FETCH eksplisit
     * (drawdown+merchant+loanDetails) biar gak N+1 pas dipetakan ke response satu-satu. */
    @Query("SELECT l FROM Loan l " +
            "JOIN FETCH l.drawdown d " +
            "LEFT JOIN FETCH d.merchant " +
            "JOIN FETCH l.loanDetails " +
            "WHERE l.limit.user.id = :userId " +
            "ORDER BY l.createdAt DESC")
    List<Loan> findByLimit_User_IdOrderByCreatedAtDesc(@Param("userId") Integer userId);

    /** Tagihan "Transportasi" (channel TRANSJAKARTA) bulan berjalan buat satu ActiveLimit, kalau
     * sudah ada - lihat LoanService#purchaseTransjakartaTicket (tiket baru numpuk ke Loan ini
     * kalau ada, bikin Loan baru kalau belum). */
    @Query("SELECT l FROM Loan l JOIN l.drawdown d " +
            "WHERE l.limit.id = :activeLimitId AND d.channel = 'TRANSJAKARTA' AND l.billingCycle = :billingCycle")
    Optional<Loan> findOpenTransjakartaBill(@Param("activeLimitId") Integer activeLimitId, @Param("billingCycle") String billingCycle);
}
