package com.api.klarfinance.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetAllRole {
    private Long id;
    private String role;
}
