package com.api.klarfinance.dbo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.klarfinance.dbo.dto.response.BranchTerritoryResponse;
import com.api.klarfinance.dbo.dto.response.RegencyGapResponse;
import com.api.klarfinance.dbo.model.Branch;
import com.api.klarfinance.dbo.model.BranchTerritory;
import com.api.klarfinance.dbo.model.Regency;
import com.api.klarfinance.dbo.repository.BranchRepository;
import com.api.klarfinance.dbo.repository.BranchTerritoryRepository;
import com.api.klarfinance.dbo.repository.RegenciesRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Aturan wilayah BM per Branch (konfirmasi user 2026-09-01, lihat knowledge base
 * projects/klarfinance/branch-territory.md buat konteks lengkap):
 * - Jawa (non-Jakarta): 1 regency maks 1 branch, di-assign satu-satu ({@link #assignRegency}).
 * - Luar Jawa: assign per-provinsi ({@link #assignProvince}), auto-expand ke semua regency-nya,
 *   tetap maks 1 branch per regency (semuanya nunjuk branch yang sama).
 * - Jakarta (province id 31): pengecualian, 1 regency boleh maks 2 branch.
 * - Endpoint controller-nya (@AdminAnnotation) udah otomatis SUPERADMIN-only lewat
 *   SecurityConfiguration, gak perlu role-check manual lagi di sini.
 */
@Service
@RequiredArgsConstructor
public class BranchTerritoryService {
    private static final Long JAKARTA_PROVINCE_ID = 31L;
    private static final Set<Long> JAVA_PROVINCE_IDS = Set.of(31L, 32L, 33L, 34L, 35L, 36L);

    private final BranchTerritoryRepository branchTerritoryRepository;
    private final BranchRepository branchRepository;
    private final RegenciesRepository regenciesRepository;

    public List<BranchTerritoryResponse> listAll() {
        return branchTerritoryRepository.findAllByOrderByRegency_NameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BranchTerritoryResponse> listByBranch(Long branchId) {
        return branchTerritoryRepository.findByBranchIdOrderByRegency_NameAsc(branchId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BranchTerritoryResponse assignRegency(Long branchId, Long regencyId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
        Regency regency = regenciesRepository.findById(regencyId)
                .orElseThrow(() -> new IllegalArgumentException("Regency not found"));

        if (branchTerritoryRepository.existsByBranchIdAndRegencyId(branchId, regencyId)) {
            throw new IllegalArgumentException("Branch ini sudah nge-cover regency " + regency.getName());
        }
        enforceRegencyLimit(regency);

        BranchTerritory saved = branchTerritoryRepository.save(
                BranchTerritory.builder().branch(branch).regency(regency).build());
        return toResponse(saved);
    }

    /** Luar Jawa aja - Jawa wajib assign per regency lewat {@link #assignRegency}. */
    @Transactional
    public List<BranchTerritoryResponse> assignProvince(Long branchId, Long provinceId) {
        if (JAVA_PROVINCE_IDS.contains(provinceId)) {
            throw new IllegalArgumentException(
                    "Provinsi ini di Pulau Jawa - assign per regency satu-satu, bukan per provinsi");
        }

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
        List<Regency> regencies = regenciesRepository.findAllByProvinceId(provinceId);
        if (regencies.isEmpty()) {
            throw new IllegalArgumentException("Provinsi ini gak punya data regency");
        }

        List<BranchTerritoryResponse> results = new ArrayList<>();
        for (Regency regency : regencies) {
            if (branchTerritoryRepository.existsByBranchIdAndRegencyId(branchId, regency.getId())) {
                continue; // udah pernah di-assign ke branch yang sama, skip (idempotent)
            }
            enforceRegencyLimit(regency);
            results.add(toResponse(branchTerritoryRepository.save(
                    BranchTerritory.builder().branch(branch).regency(regency).build())));
        }
        return results;
    }

    @Transactional
    public void unassign(Long territoryId) {
        if (!branchTerritoryRepository.existsById(territoryId)) {
            throw new IllegalArgumentException("Branch territory not found");
        }
        branchTerritoryRepository.deleteById(territoryId);
    }

    /** Regency di Pulau Jawa yang belum ada branch sama sekali - buat mastiin "minimal 1 per regency" beneran kepenuhi. */
    public List<RegencyGapResponse> findCoverageGaps() {
        return branchTerritoryRepository.findUncoveredRegencies(List.copyOf(JAVA_PROVINCE_IDS)).stream()
                .map(r -> RegencyGapResponse.builder()
                        .regencyId(r.getId())
                        .regencyName(r.getName())
                        .provinceName(r.getProvince().getName())
                        .build())
                .toList();
    }

    private void enforceRegencyLimit(Regency regency) {
        long existing = branchTerritoryRepository.countByRegencyId(regency.getId());
        long maxAllowed = JAKARTA_PROVINCE_ID.equals(regency.getProvince().getId()) ? 2 : 1;
        if (existing >= maxAllowed) {
            throw new IllegalArgumentException(
                    "Regency " + regency.getName() + " sudah dipegang " + existing + " branch (maks " + maxAllowed + ")");
        }
    }

    private BranchTerritoryResponse toResponse(BranchTerritory bt) {
        return BranchTerritoryResponse.builder()
                .id(bt.getId())
                .branchId(bt.getBranch().getId())
                .branchName(bt.getBranch().getName())
                .regencyId(bt.getRegency().getId())
                .regencyName(bt.getRegency().getName())
                .provinceName(bt.getRegency().getProvince().getName())
                .build();
    }
}
