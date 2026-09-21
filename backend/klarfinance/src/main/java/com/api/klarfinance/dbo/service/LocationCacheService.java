package com.api.klarfinance.dbo.service;

import com.api.klarfinance.config.CacheConfig;
import com.api.klarfinance.dbo.dto.response.DistrictResponse;
import com.api.klarfinance.dbo.dto.response.ProvinceResponse;
import com.api.klarfinance.dbo.dto.response.RegenciesResponse;
import com.api.klarfinance.dbo.dto.response.VillagesResponse;
import com.api.klarfinance.dbo.repository.DistrictRepository;
import com.api.klarfinance.dbo.repository.ProvinceRepository;
import com.api.klarfinance.dbo.repository.RegenciesRepository;
import com.api.klarfinance.dbo.repository.VillageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Cached, UNFILTERED region lists (province -> regency -> district -> village is static reference data).
 * Kept in its own bean because @Cacheable doesn't apply on self-invocation from LocationService. The
 * name search stays in LocationService so search terms never become cache keys.
 *
 * Results are plain ArrayLists on purpose: Stream#toList() returns java.util.ImmutableCollections$ListN,
 * which the Redis JSON serializer can write but not read back.
 */
@Service
@RequiredArgsConstructor
public class LocationCacheService {
    private final ProvinceRepository provinceRepository;
    private final RegenciesRepository regenciesRepository;
    private final DistrictRepository districtRepository;
    private final VillageRepository villageRepository;

    @Cacheable(CacheConfig.PROVINCES)
    public List<ProvinceResponse> provinces() {
        return new ArrayList<>(provinceRepository.findAll().stream()
                .map(province -> ProvinceResponse.builder()
                        .id(province.getId())
                        .name(province.getName())
                        .build())
                .toList());
    }

    @Cacheable(value = CacheConfig.REGENCIES, key = "#provinceId")
    public List<RegenciesResponse> regencies(long provinceId) {
        return new ArrayList<>(regenciesRepository.findAllByProvinceId(provinceId).stream()
                .map(regency -> RegenciesResponse.builder()
                        .id(regency.getId())
                        .name(regency.getName())
                        .province(regency.getProvince() == null ? null : ProvinceResponse.builder()
                                .id(regency.getProvince().getId())
                                .name(regency.getProvince().getName())
                                .build())
                        .build())
                .toList());
    }

    @Cacheable(value = CacheConfig.DISTRICTS, key = "#regencyId")
    public List<DistrictResponse> districts(long regencyId) {
        return new ArrayList<>(districtRepository.findAllByRegencyId(regencyId).stream()
                .map(district -> DistrictResponse.builder()
                        .id(district.getId())
                        .name(district.getName())
                        .regencies(district.getRegency() == null ? null : RegenciesResponse.builder()
                                .id(district.getRegency().getId())
                                .name(district.getRegency().getName())
                                .province(district.getRegency().getProvince() == null ? null : ProvinceResponse.builder()
                                        .id(district.getRegency().getProvince().getId())
                                        .name(district.getRegency().getProvince().getName())
                                        .build())
                                .build())
                        .build())
                .toList());
    }

    @Cacheable(value = CacheConfig.VILLAGES, key = "#districtId")
    public List<VillagesResponse> villages(long districtId) {
        return new ArrayList<>(villageRepository.findAllByDistrictId(districtId).stream()
                .map(village -> VillagesResponse.builder()
                        .id(village.getId())
                        .name(village.getName())
                        .build())
                .toList());
    }
}
