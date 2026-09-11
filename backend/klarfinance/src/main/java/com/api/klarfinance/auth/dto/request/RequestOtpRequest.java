package com.api.klarfinance.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequestOtpRequest {
    @NotBlank(message = "phone is required")
    private String phone;
}
