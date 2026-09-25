package com.api.klarfinance.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyFirebasePhoneRequest {
    @NotBlank(message = "phone is required")
    private String phone;

    /** Firebase ID token hasil Firebase Phone Auth (SMS) di app - klaim phone_number-nya dicocokkan ke [phone]. */
    @NotBlank(message = "idToken is required")
    private String idToken;
}
