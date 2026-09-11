package com.api.klarfinance.dbo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InsertBranchRequest {
    private String name;
    private String address;
    private Long villageId;
}
