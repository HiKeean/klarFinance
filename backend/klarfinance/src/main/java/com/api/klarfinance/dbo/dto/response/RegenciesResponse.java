package com.api.klarfinance.dbo.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegenciesResponse {
    private Long id;
    private String name;
    private ProvinceResponse province;
}
