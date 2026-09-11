package com.api.klarfinance.dbo.service;

import com.api.klarfinance.dbo.dto.response.BranchTerritoryResponse;
import com.api.klarfinance.dbo.model.Branch;
import com.api.klarfinance.dbo.model.BranchTerritory;
import com.api.klarfinance.dbo.model.Province;
import com.api.klarfinance.dbo.model.Regency;
import com.api.klarfinance.dbo.repository.BranchRepository;
import com.api.klarfinance.dbo.repository.BranchTerritoryRepository;
import com.api.klarfinance.dbo.repository.RegenciesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchTerritoryServiceTest {

    @Mock private BranchTerritoryRepository branchTerritoryRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private RegenciesRepository regenciesRepository;

    private BranchTerritoryService service;

    @BeforeEach
    void setUp() {
        service = new BranchTerritoryService(branchTerritoryRepository, branchRepository, regenciesRepository);
    }

    private Province province(long id, String name) {
        Province p = new Province();
        p.setId(id);
        p.setName(name);
        return p;
    }

    private Regency regency(long id, String name, Province province) {
        Regency r = new Regency();
        r.setId(id);
        r.setName(name);
        r.setProvince(province);
        return r;
    }

    @Test
    void assignRegency_throwsWhenBranchAlreadyCoversRegency() {
        Branch branch = Branch.builder().id(1L).name("Branch A").build();
        Province province = province(35L, "Jawa Timur");
        Regency regency = regency(100L, "Surabaya", province);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(regenciesRepository.findById(100L)).thenReturn(Optional.of(regency));
        when(branchTerritoryRepository.existsByBranchIdAndRegencyId(1L, 100L)).thenReturn(true);

        assertThatThrownBy(() -> service.assignRegency(1L, 100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sudah nge-cover");
    }

    @Test
    void assignRegency_nonJakartaAllowsOnlyOneBranch() {
        Branch branch = Branch.builder().id(1L).name("Branch A").build();
        Province province = province(35L, "Jawa Timur");
        Regency regency = regency(100L, "Surabaya", province);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(regenciesRepository.findById(100L)).thenReturn(Optional.of(regency));
        when(branchTerritoryRepository.existsByBranchIdAndRegencyId(1L, 100L)).thenReturn(false);
        when(branchTerritoryRepository.countByRegencyId(100L)).thenReturn(1L);

        assertThatThrownBy(() -> service.assignRegency(1L, 100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maks 1");
    }

    @Test
    void assignRegency_jakartaAllowsUpToTwoBranches() {
        Branch branch = Branch.builder().id(2L).name("Branch B").build();
        Province jakarta = province(31L, "DKI Jakarta");
        Regency regency = regency(200L, "Jakarta Selatan", jakarta);
        when(branchRepository.findById(2L)).thenReturn(Optional.of(branch));
        when(regenciesRepository.findById(200L)).thenReturn(Optional.of(regency));
        when(branchTerritoryRepository.existsByBranchIdAndRegencyId(2L, 200L)).thenReturn(false);
        when(branchTerritoryRepository.countByRegencyId(200L)).thenReturn(1L); // already 1, still < 2
        when(branchTerritoryRepository.save(any(BranchTerritory.class))).thenAnswer(inv -> {
            BranchTerritory bt = inv.getArgument(0);
            bt.setId(999L);
            return bt;
        });

        BranchTerritoryResponse response = service.assignRegency(2L, 200L);

        assertThat(response.getRegencyName()).isEqualTo("Jakarta Selatan");
        assertThat(response.getBranchName()).isEqualTo("Branch B");
    }

    @Test
    void assignRegency_jakartaRejectsThirdBranch() {
        Branch branch = Branch.builder().id(2L).name("Branch B").build();
        Province jakarta = province(31L, "DKI Jakarta");
        Regency regency = regency(200L, "Jakarta Selatan", jakarta);
        when(branchRepository.findById(2L)).thenReturn(Optional.of(branch));
        when(regenciesRepository.findById(200L)).thenReturn(Optional.of(regency));
        when(branchTerritoryRepository.existsByBranchIdAndRegencyId(2L, 200L)).thenReturn(false);
        when(branchTerritoryRepository.countByRegencyId(200L)).thenReturn(2L);

        assertThatThrownBy(() -> service.assignRegency(2L, 200L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maks 2");
    }

    @Test
    void assignProvince_rejectsJavaProvince() {
        assertThatThrownBy(() -> service.assignProvince(1L, 35L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pulau Jawa");
    }

    @Test
    void assignProvince_rejectsProvinceWithNoRegencies() {
        Branch branch = Branch.builder().id(1L).name("Branch A").build();
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(regenciesRepository.findAllByProvinceId(11L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.assignProvince(1L, 11L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gak punya data regency");
    }

    @Test
    void assignProvince_expandsToAllRegenciesSkippingAlreadyAssigned() {
        Branch branch = Branch.builder().id(1L).name("Branch A").build();
        Province sumut = province(11L, "Sumatera Utara");
        Regency r1 = regency(1001L, "Medan", sumut);
        Regency r2 = regency(1002L, "Deli Serdang", sumut);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(regenciesRepository.findAllByProvinceId(11L)).thenReturn(List.of(r1, r2));
        when(branchTerritoryRepository.existsByBranchIdAndRegencyId(1L, 1001L)).thenReturn(true); // already assigned
        when(branchTerritoryRepository.existsByBranchIdAndRegencyId(1L, 1002L)).thenReturn(false);
        when(branchTerritoryRepository.countByRegencyId(1002L)).thenReturn(0L);
        when(branchTerritoryRepository.save(any(BranchTerritory.class))).thenAnswer(inv -> {
            BranchTerritory bt = inv.getArgument(0);
            bt.setId(1L);
            return bt;
        });

        List<BranchTerritoryResponse> results = service.assignProvince(1L, 11L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRegencyName()).isEqualTo("Deli Serdang");
    }

    @Test
    void unassign_throwsWhenTerritoryNotFound() {
        when(branchTerritoryRepository.existsById(5L)).thenReturn(false);

        assertThatThrownBy(() -> service.unassign(5L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unassign_deletesExistingTerritory() {
        when(branchTerritoryRepository.existsById(5L)).thenReturn(true);

        service.unassign(5L);

        org.mockito.Mockito.verify(branchTerritoryRepository).deleteById(5L);
    }
}
