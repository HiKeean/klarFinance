package com.api.klarfinance.dbo.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VillagesResponse {
    private Long id;
    private String name;
    private DistrictResponse district;
}
