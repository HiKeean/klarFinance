package com.api.klarfinance.dbo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.klarfinance.dbo.dto.response.*;
import com.api.klarfinance.dbo.service.BranchService;
import com.api.klarfinance.dbo.service.LocationService;
import com.api.klarfinance.global.ApiResponse;
import com.api.klarfinance.global.ApiResponsePagination;

import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("dbo")
public class LocationController {
    private final LocationService service;
    private final BranchService branchService;

    @GetMapping("/location/provinces")
    public ResponseEntity<ApiResponse<List<ProvinceResponse>>> getAllProvinces(
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Provinces fetched successfully", service.getAllProvinces(search)));
    }

    @GetMapping("/location/regencies")
    public ResponseEntity<ApiResponse<List<RegenciesResponse>>> getAllRegencies(
            @RequestParam long provinceId,
            @RequestParam(required = false) String name) {
        return ResponseEntity.ok(ApiResponse.success(
                "Regencies fetched successfully", service.getAllRegencies(provinceId, name)));
    }

    @GetMapping("/location/districts")
    public ResponseEntity<ApiResponse<List<DistrictResponse>>> getAllDistricts(
            @RequestParam long regenciesId,
            @RequestParam(required = false) String name) {
        return ResponseEntity.ok(ApiResponse.success(
                "Districts fetched successfully", service.getAllDistrict(regenciesId, name)));
    }

    @GetMapping("/location/villages")
    public ResponseEntity<ApiResponse<List<VillagesResponse>>> getAllVillages(
            @RequestParam long districtsId,
            @RequestParam(required = false) String name) {
        return ResponseEntity.ok(ApiResponse.success(
                "Villages fetched successfully", service.getAllVillages(districtsId, name)));
    }

    @GetMapping("/branch")
    public ResponseEntity<ApiResponse<ApiResponsePagination<GetAllBranchResponse>>> getAllBranch(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long villageId
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Successfully to get all branch",
                    ApiResponsePagination.from(branchService.getAllBranch(page, size, name, villageId))
            ));
        }
        catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Internal Server Error"));
        }
    }
}
