package com.api.klarfinance.los.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LimitApplicationSearchResponse {
    private Integer id;
    private String applicationCode;
    private String customerName;
    private String customerIdentity;
    private String status;
}
