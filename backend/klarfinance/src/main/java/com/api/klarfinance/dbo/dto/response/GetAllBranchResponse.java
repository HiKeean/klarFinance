package com.api.klarfinance.dbo.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetAllBranchResponse {
    private Long branchCode;
    private String name;
    private String address;
//    private Province province;
//    private District district;
//    private Regency regency;
    private VillagesResponse village;
}
