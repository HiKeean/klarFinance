package com.api.klarfinance.auth.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class EditProfileRequest {
    private LocalDate dob;
    private String name;
    private String noHp;
    private Long branchId;
    private Long villageId;
}
