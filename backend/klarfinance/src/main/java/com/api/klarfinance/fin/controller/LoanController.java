package com.api.klarfinance.fin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.api.klarfinance.annotation.NasabahAnnotation;
import com.api.klarfinance.fin.dto.request.LoanRequest;
import com.api.klarfinance.fin.dto.request.RepaymentRequest;
import com.api.klarfinance.fin.dto.response.BankAccountResponse;
import com.api.klarfinance.fin.dto.response.LimitSummaryResponse;
import com.api.klarfinance.fin.dto.response.LoanHistoryItemResponse;
import com.api.klarfinance.fin.dto.response.LoanResponse;
import com.api.klarfinance.fin.service.LoanService;
import com.api.klarfinance.global.ApiResponse;

import java.security.Principal;
import java.util.List;

@NasabahAnnotation
@RequestMapping("/loan")
@RequiredArgsConstructor
public class LoanController {
    private final LoanService loanService;

    @GetMapping("/limit")
    public ResponseEntity<ApiResponse<LimitSummaryResponse>> myLimit(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Limit summary retrieved", loanService.getMyLimitSummary(principal)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LoanResponse>> requestLoan(@RequestBody LoanRequest request, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Loan requested successfully", loanService.requestLoan(request, principal)));
    }

    /** Rekening tujuan pencairan yang pernah dipakai (konfirmasi user 2026-09-07) - isi dropdown
     * "Rekening Tujuan" di Android, lihat LoanService#getMyBankAccounts. */
    @GetMapping("/bank-accounts")
    public ResponseEntity<ApiResponse<List<BankAccountResponse>>> myBankAccounts(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Bank accounts retrieved", loanService.getMyBankAccounts(principal)));
    }

    /** Halaman "History" Android - semua Loan nasabah ini, tarik tunai maupun bayar QRIS,
     * terbaru duluan (lihat LoanService#getMyLoanHistory). */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<LoanHistoryItemResponse>>> myLoanHistory(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Loan history retrieved", loanService.getMyLoanHistory(principal)));
    }

    /** Fitur "Bayar" - lihat LoanService#repay buat aturan minimum/alokasi lengkap. Balikin
     * LoanHistoryItemResponse yang sudah ter-update (status/paidInstallments/nextDueDate baru)
     * biar Android bisa langsung refresh baris ini tanpa perlu GET /history ulang. */
    @PostMapping("/{loanId}/repayment")
    public ResponseEntity<ApiResponse<LoanHistoryItemResponse>> repay(
            @PathVariable Integer loanId,
            @RequestBody RepaymentRequest request,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Repayment recorded", loanService.repay(loanId, request, principal)));
    }
}
