package com.api.klarfinance.fin;

/**
 * Bucket status pinjaman aktif buat "Loan Status Distribution" di dashboard BM, diturunkan dari
 * due date installment terdekat yang belum lunas — bukan kolom tersimpan.
 * Gak ada grace period (konfirmasi user 2026-09-01) - begitu lewat due date langsung Overdue,
 * kena denda keterlambatan LoanInterestPolicy.calculateLatePenalty().
 */
public class LoanStatusBucket {
    private LoanStatusBucket() {}

    public static final String CURRENT = "Current";
    public static final String OVERDUE = "Overdue";
}
