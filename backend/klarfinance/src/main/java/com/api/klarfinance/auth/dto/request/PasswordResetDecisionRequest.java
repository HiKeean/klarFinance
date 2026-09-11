package com.api.klarfinance.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetDecisionRequest {
    /** APPROVE atau REJECT. */
    @NotBlank(message = "action is required")
    private String action;

    /** Wajib diisi kalau action = REJECT (alasan ditolak), opsional buat APPROVE. */
    private String reason;
}
