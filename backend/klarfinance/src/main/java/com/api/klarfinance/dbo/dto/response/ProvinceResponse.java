package com.api.klarfinance.dbo.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProvinceResponse {
    private Long id;
    private String name;
}
