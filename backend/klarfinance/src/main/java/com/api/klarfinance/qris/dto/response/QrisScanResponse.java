package com.api.klarfinance.qris.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class QrisScanResponse {
    private String token;
    private String merchantName;
    private LocalDateTime expiresAt;
}
