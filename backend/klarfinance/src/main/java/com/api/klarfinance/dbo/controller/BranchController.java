package com.api.klarfinance.dbo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.api.klarfinance.annotation.AdminAnnotation;
import com.api.klarfinance.dbo.dto.request.InsertBranchRequest;
import com.api.klarfinance.dbo.dto.response.GetAllBranchResponse;
import com.api.klarfinance.dbo.service.BranchService;
import com.api.klarfinance.global.ApiResponse;
import com.api.klarfinance.global.ApiResponsePagination;

@AdminAnnotation
@RequiredArgsConstructor
@RequestMapping("/branch")
public class BranchController {
    private final BranchService branchService;

    @GetMapping
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

    @PostMapping
    public ResponseEntity<ApiResponse<Object>> insertBranch(@RequestBody InsertBranchRequest entity){
        branchService.saveBranch(entity);
        return ResponseEntity.ok(ApiResponse.success("Successfully to insert branch",null));
    }
}
