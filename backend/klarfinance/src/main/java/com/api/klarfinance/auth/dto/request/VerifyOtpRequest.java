package com.api.klarfinance.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank(message = "phone is required")
    private String phone;

    @NotBlank(message = "otp is required")
    private String otp;
}
