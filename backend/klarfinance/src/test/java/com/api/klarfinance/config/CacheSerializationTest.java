package com.api.klarfinance.config;

import com.api.klarfinance.auth.dto.response.LoginResponse;
import com.api.klarfinance.dbo.dto.response.DistrictResponse;
import com.api.klarfinance.dbo.dto.response.ProvinceResponse;
import com.api.klarfinance.dbo.dto.response.RegenciesResponse;
import com.api.klarfinance.dbo.dto.response.VillagesResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Everything put behind @Cacheable goes through the same serializer RedisConfig uses. These types must
 * survive a write/read round trip - it fails if a cached DTO loses its no-args constructor, or if a cached
 * method returns Stream#toList() (ImmutableCollections$ListN can't be read back).
 */
class CacheSerializationTest {

    private final GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

    private Object roundTrip(Object value) {
        return serializer.deserialize(serializer.serialize(value));
    }

    @Test
    void locationLists_roundTrip() {
        ProvinceResponse province = ProvinceResponse.builder().id(31L).name("DKI JAKARTA").build();
        RegenciesResponse regency = RegenciesResponse.builder().id(3171L).name("JAKARTA SELATAN").province(province).build();
        DistrictResponse district = DistrictResponse.builder().id(317101L).name("TEBET").regencies(regency).build();
        VillagesResponse village = VillagesResponse.builder().id(3171011L).name("TEBET BARAT").build();

        assertEquals(List.of(province), roundTrip(new ArrayList<>(List.of(province))));
        assertEquals(List.of(regency), roundTrip(new ArrayList<>(List.of(regency))));
        assertEquals(List.of(district), roundTrip(new ArrayList<>(List.of(district))));
        assertEquals(List.of(village), roundTrip(new ArrayList<>(List.of(village))));
    }

    @Test
    void roleMenus_roundTrip() {
        LoginResponse.MenuResponse menu = LoginResponse.MenuResponse.builder().url("/loans").name("Loans").logo("loan.svg").build();

        assertEquals(List.of(menu), roundTrip(new ArrayList<>(List.of(menu))));
    }

    @Test
    void apiKey_roundTrip() {
        assertEquals("secret-key", roundTrip("secret-key"));
    }
}
