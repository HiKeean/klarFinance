package com.api.klarfinance.dbo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.api.klarfinance.dbo.dto.response.DistrictResponse;
import com.api.klarfinance.dbo.dto.response.ProvinceResponse;
import com.api.klarfinance.dbo.dto.response.RegenciesResponse;
import com.api.klarfinance.dbo.dto.response.VillagesResponse;
import com.api.klarfinance.dbo.repository.DistrictRepository;
import com.api.klarfinance.dbo.repository.ProvinceRepository;
import com.api.klarfinance.dbo.repository.RegenciesRepository;
import com.api.klarfinance.dbo.repository.VillageRepository;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LocationService {
    private final ProvinceRepository provinceRepository;
    private final RegenciesRepository regenciesRepository;
    private final DistrictRepository districtRepository;
    private final VillageRepository villageRepository;

    public List<ProvinceResponse> getAllProvinces(){
        return getAllProvinces(null);
    }

    public List<ProvinceResponse> getAllProvinces(String name){
        return provinceRepository.findAll().stream()
                .filter(province -> matches(province.getName(), name))
                .map(province -> ProvinceResponse.builder()
                        .id(province.getId())
                        .name(province.getName())
                        .build())
                .toList();
    }

    public List<RegenciesResponse> getAllRegencies(long provinceId){
        return getAllRegencies(provinceId, null);
    }

    public List<RegenciesResponse> getAllRegencies(long provinceId, String name){
        return regenciesRepository.findAllByProvinceId(provinceId).stream()
                .filter(regency -> matches(regency.getName(), name))
                .map(regency -> RegenciesResponse.builder()
                        .id(regency.getId())
                        .name(regency.getName())
                        .province(regency.getProvince() == null ? null : ProvinceResponse.builder()
                                .id(regency.getProvince().getId())
                                .name(regency.getProvince().getName())
                                .build())
                        .build())
                .toList();
    }

    public List<DistrictResponse> getAllDistrict(long regenciesId){
        return getAllDistrict(regenciesId, null);
    }

    public List<DistrictResponse> getAllDistrict(long regenciesId, String name){
        return districtRepository.findAllByRegencyId(regenciesId).stream()
                .filter(district -> matches(district.getName(), name))
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
                .toList();
    }

    public List<VillagesResponse> getAllVillages(long districtId){
        return getAllVillages(districtId, null);
    }

    public List<VillagesResponse> getAllVillages(long districtId, String name){
        return villageRepository.findAllByDistrictId(districtId).stream()
                .filter(village -> matches(village.getName(), name))
                .map(village -> VillagesResponse.builder()
                        .id(village.getId())
                        .name(village.getName())
                        .build())
                .toList();
    }

    private boolean matches(String value, String search) {
        return search == null || search.trim().isEmpty()
                || value != null && value.toLowerCase(Locale.ROOT)
                .contains(search.trim().toLowerCase(Locale.ROOT));
    }
}
