package com.api.klarfinance.auth.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Nasabah self-register form. Deliberately separate from auth.dto.RegisterRequest,
 * which is for internal staff (creates DetailUserInternal, not CustomerDetails).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerRegisterRequest {

    private String nik;
    private String npwp;
    private String name;
    private String email;
    private String address;
    private String address2;
    private LocalDate dob;
    private Long villageId;
    private String password;

    /** Kode referral punya orang lain, opsional - lihat ReferralService.onCustomerRegistered. */
    private String referralCode;

    private BigDecimal claimedIncome;
    private List<String> pinjolApps;
    private List<String> bankApps;

    private MultipartFile fotoKtp;
    private MultipartFile fotoKyc;
}
