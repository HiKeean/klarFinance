package com.api.klarfinance.dbo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegenciesResponse {
    private Long id;
    private String name;
    private ProvinceResponse province;
}
