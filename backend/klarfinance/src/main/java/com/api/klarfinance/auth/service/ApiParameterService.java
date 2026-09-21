package com.api.klarfinance.auth.service;

import com.api.klarfinance.auth.repository.ApiParameterRepository;
import com.api.klarfinance.config.CacheConfig;
import com.api.klarfinance.dbo.model.ApiParameter;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * HmacSignatureFilter needs the client's API key on EVERY request, so this lookup is cached
 * (short TTL, see CacheConfig - there's no write path for dbh_api_param in the app, so a key rotated
 * directly in the DB takes effect once the entry expires).
 *
 * Only the key String is cached, not the ApiParameter entity (its EAGER createdBy/updatedBy User graph
 * shouldn't be serialized into Redis). Unknown client types return null and are NOT cached, so a caller
 * sending random X-Client-Type values can't fill the cache.
 */
@Service
@RequiredArgsConstructor
public class ApiParameterService {
    private final ApiParameterRepository apiParameterRepository;

    @Cacheable(value = CacheConfig.API_KEYS, key = "#clientType", unless = "#result == null")
    public String getApiKey(String clientType) {
        return apiParameterRepository.findByClientType(clientType)
                .map(ApiParameter::getApiKey)
                .orElse(null);
    }
}
