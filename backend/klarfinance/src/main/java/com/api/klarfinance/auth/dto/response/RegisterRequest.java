package com.api.klarfinance.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterRequest {
    private String identity;
    private String role;
}
