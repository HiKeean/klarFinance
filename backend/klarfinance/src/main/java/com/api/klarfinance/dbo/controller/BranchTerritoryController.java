package com.api.klarfinance.dbo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.api.klarfinance.annotation.AdminAnnotation;
import com.api.klarfinance.dbo.dto.request.AssignProvinceTerritoryRequest;
import com.api.klarfinance.dbo.dto.request.AssignRegencyTerritoryRequest;
import com.api.klarfinance.dbo.dto.response.BranchTerritoryResponse;
import com.api.klarfinance.dbo.dto.response.RegencyGapResponse;
import com.api.klarfinance.dbo.service.BranchTerritoryService;
import com.api.klarfinance.global.ApiResponse;

import java.util.List;

/** Superadmin-only (lihat SecurityConfiguration - @AdminAnnotation -> /api/v1/admin/** -> hasAuthority("SUPERADMIN")). */
@AdminAnnotation
@RequiredArgsConstructor
@RequestMapping("/dbo/branch-territory")
public class BranchTerritoryController {
    private final BranchTerritoryService branchTerritoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchTerritoryResponse>>> list(
            @RequestParam(required = false) Long branchId) {
        List<BranchTerritoryResponse> data = branchId == null
                ? branchTerritoryService.listAll()
                : branchTerritoryService.listByBranch(branchId);
        return ResponseEntity.ok(ApiResponse.success("Branch territory fetched successfully", data));
    }

    @GetMapping("/gaps")
    public ResponseEntity<ApiResponse<List<RegencyGapResponse>>> gaps() {
        return ResponseEntity.ok(ApiResponse.success("Coverage gaps fetched successfully", branchTerritoryService.findCoverageGaps()));
    }

    @PostMapping("/regency")
    public ResponseEntity<ApiResponse<BranchTerritoryResponse>> assignRegency(@RequestBody AssignRegencyTerritoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Regency assigned successfully",
                branchTerritoryService.assignRegency(request.getBranchId(), request.getRegencyId())));
    }

    @PostMapping("/province")
    public ResponseEntity<ApiResponse<List<BranchTerritoryResponse>>> assignProvince(@RequestBody AssignProvinceTerritoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Province assigned successfully",
                branchTerritoryService.assignProvince(request.getBranchId(), request.getProvinceId())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> unassign(@PathVariable Long id) {
        branchTerritoryService.unassign(id);
        return ResponseEntity.ok(ApiResponse.success("Branch territory removed successfully", null));
    }
}
