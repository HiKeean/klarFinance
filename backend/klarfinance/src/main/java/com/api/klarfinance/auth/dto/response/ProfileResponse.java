package com.api.klarfinance.auth.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data @Builder
public class ProfileResponse {
    private String name;
    private LocalDate dob;
    private String noHp;
    private String role;
    /** Nasabah-only - null/false for staff (DetailUserInternal has no email column). */
    private String email;
    private boolean emailVerified;
    /** Nasabah-only - "ACTIVE"/"PENDING_APPLICATION", same values/logic as
     * LoginResponse#accountStatus (see AuthenticationInternalService#login). Null for staff.
     * Lets the app re-derive AccountState on demand (e.g. after an FCM approval push) instead
     * of only at login/register time. */
    private String accountStatus;
    private Long branchId;
    private Long provinceId;
    private Long districtId;
    private Long regencyId;
    private Long villageId;
}
