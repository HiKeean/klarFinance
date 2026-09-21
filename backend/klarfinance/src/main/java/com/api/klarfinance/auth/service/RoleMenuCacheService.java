package com.api.klarfinance.auth.service;

import com.api.klarfinance.auth.dto.response.LoginResponse;
import com.api.klarfinance.auth.model.RoleMenu;
import com.api.klarfinance.auth.repository.RoleMenuRepository;
import com.api.klarfinance.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Menus a role can see, read on every internal login. Evicted (allEntries) by every RBAC mutation in
 * AuthenticationSuperadminService - if you add another write path to Role/Menu/RoleMenu, evict
 * CacheConfig.ROLE_MENUS there too.
 *
 * Plain ArrayList on purpose (see LocationCacheService).
 */
@Service
@RequiredArgsConstructor
public class RoleMenuCacheService {
    private final RoleMenuRepository roleMenuRepository;

    @Cacheable(value = CacheConfig.ROLE_MENUS, key = "#roleId")
    public List<LoginResponse.MenuResponse> getMenusByRole(Integer roleId) {
        return new ArrayList<>(roleMenuRepository.findByRoleId(roleId).stream()
                .map(RoleMenu::getMenu)
                .filter(Objects::nonNull)
                .map(m -> LoginResponse.MenuResponse.builder().url(m.getUrl()).name(m.getName()).logo(m.getLogo()).build())
                .toList());
    }
}
