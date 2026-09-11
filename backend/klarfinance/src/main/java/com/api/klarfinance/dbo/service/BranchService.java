package com.api.klarfinance.dbo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.klarfinance.dbo.dto.request.InsertBranchRequest;
import com.api.klarfinance.dbo.dto.response.DistrictResponse;
import com.api.klarfinance.dbo.dto.response.GetAllBranchResponse;
import com.api.klarfinance.dbo.dto.response.ProvinceResponse;
import com.api.klarfinance.dbo.dto.response.RegenciesResponse;
import com.api.klarfinance.dbo.dto.response.VillagesResponse;
import com.api.klarfinance.dbo.model.Branch;
import com.api.klarfinance.dbo.model.Village;
import com.api.klarfinance.dbo.repository.BranchRepository;
import com.api.klarfinance.dbo.repository.VillageRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final VillageRepository villageRepository;

    @Transactional(readOnly = true)
    public Page<GetAllBranchResponse> getAllBranch(int page, int size) {
        return getAllBranch(page, size, null, null);
    }

    @Transactional(readOnly = true)
    public Page<GetAllBranchResponse> getAllBranch(
            int page, int size, String name, Long villageId) {

        Pageable pageable = PageRequest.of(page, size);
        String normalizedName = name == null || name.trim().isEmpty() ? null : name.trim();
        Page<Branch> branchPage = branchRepository.findAllByFilters(normalizedName, villageId, pageable);
        return branchPage.map(branch -> new GetAllBranchResponse(
                branch.getId(), branch.getName(), branch.getAddress(), toVillageResponse(branch.getVillage())));
    }

    private VillagesResponse toVillageResponse(Village village) {
        if (village == null) return null;
        var district = village.getDistrict();
        var regency = district == null ? null : district.getRegency();
        var province = regency == null ? null : regency.getProvince();

        return VillagesResponse.builder()
                .id(village.getId())
                .name(village.getName())
                .district(district == null ? null : DistrictResponse.builder()
                        .id(district.getId())
                        .name(district.getName())
                        .regencies(regency == null ? null : RegenciesResponse.builder()
                                .id(regency.getId())
                                .name(regency.getName())
                                .province(province == null ? null : ProvinceResponse.builder()
                                        .id(province.getId())
                                        .name(province.getName())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    public void saveBranch(InsertBranchRequest request) {
        Village village = villageRepository.findById(request.getVillageId()).orElseThrow(()->new RuntimeException("Village not found"));
        Branch branch = Branch.builder()
                .name(request.getName())
                .address(request.getAddress())
                .village(village)
                .build();
        branchRepository.save(branch);
    }
}
