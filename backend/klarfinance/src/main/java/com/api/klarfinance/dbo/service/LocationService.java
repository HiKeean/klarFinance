package com.api.klarfinance.dbo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.api.klarfinance.dbo.dto.response.DistrictResponse;
import com.api.klarfinance.dbo.dto.response.ProvinceResponse;
import com.api.klarfinance.dbo.dto.response.RegenciesResponse;
import com.api.klarfinance.dbo.dto.response.VillagesResponse;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LocationService {
    private final LocationCacheService locationCache;

    public List<ProvinceResponse> getAllProvinces(){
        return getAllProvinces(null);
    }

    public List<ProvinceResponse> getAllProvinces(String name){
        return locationCache.provinces().stream()
                .filter(province -> matches(province.getName(), name))
                .toList();
    }

    public List<RegenciesResponse> getAllRegencies(long provinceId){
        return getAllRegencies(provinceId, null);
    }

    public List<RegenciesResponse> getAllRegencies(long provinceId, String name){
        return locationCache.regencies(provinceId).stream()
                .filter(regency -> matches(regency.getName(), name))
                .toList();
    }

    public List<DistrictResponse> getAllDistrict(long regenciesId){
        return getAllDistrict(regenciesId, null);
    }

    public List<DistrictResponse> getAllDistrict(long regenciesId, String name){
        return locationCache.districts(regenciesId).stream()
                .filter(district -> matches(district.getName(), name))
                .toList();
    }

    public List<VillagesResponse> getAllVillages(long districtId){
        return getAllVillages(districtId, null);
    }

    public List<VillagesResponse> getAllVillages(long districtId, String name){
        return locationCache.villages(districtId).stream()
                .filter(village -> matches(village.getName(), name))
                .toList();
    }

    private boolean matches(String value, String search) {
        return search == null || search.trim().isEmpty()
                || value != null && value.toLowerCase(Locale.ROOT)
                .contains(search.trim().toLowerCase(Locale.ROOT));
    }
}
