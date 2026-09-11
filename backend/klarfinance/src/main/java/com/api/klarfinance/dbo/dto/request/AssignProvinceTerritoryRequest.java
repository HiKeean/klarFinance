package com.api.klarfinance.dbo.dto.request;

import lombok.Data;

@Data
public class AssignProvinceTerritoryRequest {
    private Long branchId;
    private Long provinceId;
}
