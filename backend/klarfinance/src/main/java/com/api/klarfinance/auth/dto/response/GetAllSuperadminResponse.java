package com.api.klarfinance.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GetAllSuperadminResponse {
    private String identity;
    private String name;
    private String role;
    private String noHp;
    private BranchDto branch;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @Data
    @Builder
    public static class BranchDto{
        private Long branchCode;
        private String name;
    }
}
