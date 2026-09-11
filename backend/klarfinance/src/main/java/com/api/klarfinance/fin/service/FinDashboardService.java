package com.api.klarfinance.fin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.api.klarfinance.fin.InstallmentStatus;
import com.api.klarfinance.fin.LoanStatusBucket;
import com.api.klarfinance.fin.NplSeverity;
import com.api.klarfinance.fin.dto.response.BranchLoanSummary;
import com.api.klarfinance.fin.dto.response.BranchNplSummary;
import com.api.klarfinance.fin.model.Installment;
import com.api.klarfinance.fin.model.Loan;
import com.api.klarfinance.fin.repository.InstallmentRepository;
import com.api.klarfinance.fin.repository.LoanRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Sumber data dashboard BM — dihitung dari Loan/Installment yang benar-benar tersimpan
 * (bukan mock), tapi cakupannya terbatas ke apa yang benar-benar ada: loan yang dibuat via
 * pengajuan nasabah (LoanService). Belum ada Repayment recording, jadi installment tidak pernah
 * berubah jadi PAID di alur saat ini — lihat catatan di knowledge project.
 */
@Service
@RequiredArgsConstructor
public class FinDashboardService {
    private final LoanRepository loanRepository;
    private final InstallmentRepository installmentRepository;

    public BranchLoanSummary getBranchSummary(Long branchId) {
        List<Loan> loans = loanRepository.findByLimit_Branch_Id(branchId);
        return summarize(loans);
    }

    /** Buat NPL report semua branch (webadmin) - satu query buat semua Loan + satu query buat
     * semua Installment (bukan N+1 per branch), baru dikelompokkan di memory. */
    public List<BranchNplSummary> getAllBranchSummaries() {
        List<Loan> allLoans = loanRepository.findAllWithBranchAndDetails();
        Map<Long, List<Loan>> loansByBranch = allLoans.stream()
                .filter(loan -> loan.getLimit().getBranch() != null)
                .collect(Collectors.groupingBy(loan -> loan.getLimit().getBranch().getId()));

        return loansByBranch.entrySet().stream()
                .map(entry -> {
                    List<Loan> branchLoans = entry.getValue();
                    BranchLoanSummary summary = summarize(branchLoans);
                    return BranchNplSummary.builder()
                            .branchId(entry.getKey())
                            .branchName(branchLoans.get(0).getLimit().getBranch().getName())
                            .activeLoanCount(summary.getActiveLoanCount())
                            .overdueLoanCount(summary.getOverdueLoanCount())
                            .nplPercent(summary.getNplPercent())
                            .nplSeverity(summary.getNplSeverity())
                            .totalOutstandingPenalty(summary.getTotalOutstandingPenalty())
                            .build();
                })
                .sorted(Comparator.comparing(BranchNplSummary::getBranchName))
                .toList();
    }

    private BranchLoanSummary summarize(List<Loan> loans) {
        if (loans.isEmpty()) {
            return BranchLoanSummary.builder()
                    .loansInRegion(BigDecimal.ZERO)
                    .activeBorrowers(0)
                    .delinquentBorrowers(0)
                    .loanStatusDistribution(List.of())
                    .activeLoanCount(0)
                    .overdueLoanCount(0)
                    .nplPercent(BigDecimal.ZERO)
                    .nplSeverity(NplSeverity.GREEN)
                    .totalOutstandingPenalty(BigDecimal.ZERO)
                    .build();
        }

        List<Integer> loanIds = loans.stream().map(Loan::getId).collect(Collectors.toList());
        List<Installment> installments = installmentRepository.findByLoan_IdInOrderByDueDateAsc(loanIds);
        Map<Integer, List<Installment>> installmentsByLoan = installments.stream()
                .collect(Collectors.groupingBy(i -> i.getLoan().getId()));

        LocalDateTime now = LocalDateTime.now();
        BigDecimal loansInRegion = BigDecimal.ZERO;
        BigDecimal totalOutstandingPenalty = BigDecimal.ZERO;
        Map<Integer, String> bucketByUser = new HashMap<>();
        Map<String, Integer> bucketCounts = new LinkedHashMap<>();
        bucketCounts.put(LoanStatusBucket.CURRENT, 0);
        bucketCounts.put(LoanStatusBucket.OVERDUE, 0);

        int activeLoanCount = 0;
        int overdueLoanCount = 0;
        for (Loan loan : loans) {
            Optional<Installment> nextUnpaid = installmentsByLoan.getOrDefault(loan.getId(), List.of()).stream()
                    .filter(i -> InstallmentStatus.UNPAID.equals(i.getStatus()))
                    .min(Comparator.comparing(Installment::getDueDate));
            if (nextUnpaid.isEmpty()) continue; // lunas — tidak dihitung sebagai pinjaman aktif

            activeLoanCount++;
            loansInRegion = loansInRegion.add(loan.getLoanDetails().getRequestedAmount());

            Installment installment = nextUnpaid.get();
            String bucket = resolveBucket(installment.getDueDate(), now);
            bucketCounts.merge(bucket, 1, Integer::sum);
            bucketByUser.put(loan.getLimit().getUser().getId(), bucket);

            if (LoanStatusBucket.OVERDUE.equals(bucket)) {
                overdueLoanCount++;
                long daysOverdue = ChronoUnit.DAYS.between(installment.getDueDate().toLocalDate(), now.toLocalDate());
                totalOutstandingPenalty = totalOutstandingPenalty
                        .add(LoanInterestPolicy.calculateLatePenalty(installment.getAmount(), daysOverdue));
            }
        }

        long activeBorrowers = loans.stream()
                .filter(l -> !installmentsByLoan.getOrDefault(l.getId(), List.of()).stream()
                        .filter(i -> InstallmentStatus.UNPAID.equals(i.getStatus())).toList().isEmpty())
                .map(l -> l.getLimit().getUser().getId())
                .distinct()
                .count();
        long delinquentBorrowers = bucketByUser.values().stream().filter(LoanStatusBucket.OVERDUE::equals).count();

        int totalActive = activeLoanCount;
        List<BranchLoanSummary.StatusSlice> distribution = totalActive == 0
                ? List.of()
                : bucketCounts.entrySet().stream()
                        .map(e -> BranchLoanSummary.StatusSlice.builder()
                                .label(e.getKey())
                                .percent(Math.round(e.getValue() * 100f / totalActive))
                                .build())
                        .collect(Collectors.toList());

        BigDecimal nplPercent = totalActive == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(overdueLoanCount * 100.0 / totalActive).setScale(1, RoundingMode.HALF_UP);

        return BranchLoanSummary.builder()
                .loansInRegion(loansInRegion)
                .activeBorrowers(activeBorrowers)
                .delinquentBorrowers(delinquentBorrowers)
                .loanStatusDistribution(distribution)
                .activeLoanCount(activeLoanCount)
                .overdueLoanCount(overdueLoanCount)
                .nplPercent(nplPercent)
                .nplSeverity(NplSeverity.classify(nplPercent))
                .totalOutstandingPenalty(totalOutstandingPenalty)
                .build();
    }

    private String resolveBucket(LocalDateTime dueDate, LocalDateTime now) {
        return now.isBefore(dueDate) ? LoanStatusBucket.CURRENT : LoanStatusBucket.OVERDUE;
    }
}
