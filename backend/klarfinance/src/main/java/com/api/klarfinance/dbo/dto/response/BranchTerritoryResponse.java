package com.api.klarfinance.dbo.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BranchTerritoryResponse {
    private Long id;
    private Long branchId;
    private String branchName;
    private Long regencyId;
    private String regencyName;
    private String provinceName;
}
