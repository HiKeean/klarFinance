package com.api.klarfinance.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    private java.time.LocalDate dob;
    private String name;
    private String noHp;
    private Long branchId;
    private Long villageId;
    private String password;
    private String role;
    private String address;
}
