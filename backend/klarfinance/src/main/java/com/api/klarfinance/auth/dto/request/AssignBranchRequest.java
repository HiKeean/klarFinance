package com.api.klarfinance.auth.dto.request;

import lombok.Data;

@Data
public class AssignBranchRequest {
    /** Null = unassign (lepas BM/staff ini dari branch manapun). */
    private Long branchId;
}
