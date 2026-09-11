package com.api.klarfinance.los.service;

public record VidaResult(String status, boolean employed, String verifiedMonthlyIncome) {
    public static final String UNCLEAR = "UNCLEAR";
    public static final String REJECTED = "REJECTED";
    public static final String APPROVED = "APPROVED";

    public boolean isApproved() {
        return APPROVED.equals(status);
    }
}
