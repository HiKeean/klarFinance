package com.api.klarfinance.dbo.dto.request;

import lombok.Data;

@Data
public class AssignRegencyTerritoryRequest {
    private Long branchId;
    private Long regencyId;
}
