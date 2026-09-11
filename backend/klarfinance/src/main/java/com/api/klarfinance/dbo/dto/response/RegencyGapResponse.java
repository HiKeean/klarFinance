package com.api.klarfinance.dbo.dto.response;

import lombok.Builder;
import lombok.Data;

/** Regency di Pulau Jawa yang belum punya branch sama sekali - lihat BranchTerritoryService. */
@Data
@Builder
public class RegencyGapResponse {
    private Long regencyId;
    private String regencyName;
    private String provinceName;
}
