package com.api.klarfinance.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequestResponse {
    private Long id;
    private String identity;
    private String name;
    private String role;
    private LocalDateTime requestedAt;
    private String status;
    private LocalDateTime decidedAt;
    private String decidedBy;
    private String reason;
}
