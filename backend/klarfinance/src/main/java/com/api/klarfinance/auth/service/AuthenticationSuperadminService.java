package com.api.klarfinance.auth.service;

import lombok.RequiredArgsConstructor;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.api.klarfinance.auth.dto.request.AddRoleMenuRequest;
import com.api.klarfinance.auth.dto.request.MenuRequest;
import com.api.klarfinance.auth.dto.response.GetAllMenu;
import com.api.klarfinance.auth.dto.response.GetAllRole;
import com.api.klarfinance.auth.dto.response.GetAllRoleMenu;
import com.api.klarfinance.auth.dto.response.GetAllSuperadminResponse;
import com.api.klarfinance.auth.model.*;
import com.api.klarfinance.auth.repository.*;
import com.api.klarfinance.config.JwtService;
import com.api.klarfinance.dbo.model.Branch;
import com.api.klarfinance.dbo.repository.BranchRepository;
import com.api.klarfinance.dbo.repository.VillageRepository;
import com.api.klarfinance.token.TokenRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AuthenticationSuperadminService {
    private final UserRepository userRepository;
    private final DetailUserInternalRepository detailRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final RoleRepository roleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenRepository tokenRepository;
    private final BranchRepository branchRepository;
    private final VillageRepository villageRepository;
    private final StringRedisTemplate redisTemplate;
    private final MenuRepository menuRepository;
    private static final SecureRandom RANDOM = new SecureRandom();

    public Page<GetAllSuperadminResponse> getAllSuperadmin(int size, int page){
        return getAllSuperadmin(size, page, null, null, null);
    }

    private static final Comparator<GetAllSuperadminResponse> SUPERADMIN_LISTING_ORDER = Comparator
            .comparing((GetAllSuperadminResponse user) -> user.getDeletedAt() != null)
            .thenComparing(GetAllSuperadminResponse::getCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder()));

    public Page<GetAllSuperadminResponse> getAllSuperadmin(
            int size, int page, String search, String role, Long branchId) {
        if (size < 1) {
            throw new IllegalArgumentException("size must be greater than zero");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }

        String normalizedSearch = hasText(search) ? search.trim() : null;
        String normalizedRole = hasText(role) ? role.trim() : null;

        int offset = page * size;
        Pageable topN = PageRequest.of(0, offset + size);

        List<GetAllSuperadminResponse> internal = detailRepository
                .searchForAdminListing(normalizedSearch, normalizedRole, branchId, topN)
                .stream().map(this::toSuperadminResponse).toList();

        // customers have no branch, so a branch filter can never match a customer row
        List<GetAllSuperadminResponse> customers = branchId != null
                ? List.of()
                : customerDetailsRepository
                        .searchForAdminListing(normalizedSearch, normalizedRole, topN)
                        .stream().map(this::toSuperadminResponse).toList();

        long total = detailRepository.countForAdminListing(normalizedSearch, normalizedRole, branchId)
                + (branchId != null ? 0 : customerDetailsRepository.countForAdminListing(normalizedSearch, normalizedRole));

        List<GetAllSuperadminResponse> merged = Stream.concat(internal.stream(), customers.stream())
                .sorted(SUPERADMIN_LISTING_ORDER)
                .skip(offset)
                .limit(size)
                .toList();

        return new PageImpl<>(merged, PageRequest.of(page, size), total);
    }

    /** Assign/ganti Branch buat satu staff internal (dipakai halaman Branch webadmin buat pasang
     * BM) - branchId null = unassign. Cuma valid buat user yang punya DetailUserInternal; nasabah
     * (CustomerDetails) gak punya konsep branch sama sekali. Kalau staff ini masih attached ke
     * branch lain, assign ke branch baru ditolak - harus di-unassign (branchId=null) dulu baru
     * bisa dipilih jadi BM/staff di branch tersebut. */
    @Transactional
    public void assignBranch(String identity, Long branchId) {
        User user = findUser(identity);
        DetailUserInternal detail = detailRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("User ini bukan staff internal (gak punya detail/branch)"));
        if (branchId == null) {
            detail.setBranch(null);
        } else {
            Branch currentBranch = detail.getBranch();
            if (currentBranch != null && !currentBranch.getId().equals(branchId)) {
                throw new IllegalStateException("User ini masih bertugas di branch \"" + currentBranch.getName()
                        + "\" - unassign dulu dari branch tersebut sebelum di-assign ke branch baru");
            }
            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
            detail.setBranch(branch);
        }
        detailRepository.save(detail);
    }

    @Transactional
    public void deleteUser(String id, String identity) {
        User admin = userRepository.findByIdentity(identity).orElseThrow(()->new RuntimeException("Admin not found"));
        User user = userRepository.findByIdentity(id).orElseThrow(()->new RuntimeException("User not found"));
        user.setDeletedAt(LocalDateTime.now());
        user.setDeletedBy(admin);
        userRepository.save(user);
        tokenRepository.deleteAll(tokenRepository.findByUserId(user.getId()));
        redisTemplate.delete(java.util.List.of(id, "internal:" + id, "internal_login:" + id));
    }

    @Transactional
    public void addMenu(MenuRequest request, String identity) {
        User user = userRepository.findByIdentity(identity).orElseThrow(()->new RuntimeException("User not found"));
        Menu menu = Menu.builder().name(request.getName()).url(request.getUrl()).logo(request.getLogo()).createdBy(user).build();
        menuRepository.save(menu);
    }

    @Transactional
    public void addRoleMenu(AddRoleMenuRequest request, String identity) {
        User user = userRepository.findByIdentity(identity).orElseThrow(()->new RuntimeException("User not found"));
        Role role = roleRepository.findByName(request.getRole()).orElseThrow(()->new RuntimeException("Role not found"));
        Menu menu = menuRepository.findByName(request.getMenu()).orElseThrow(()->new RuntimeException("Menu not found"));
        RoleMenu roleMenu = RoleMenu.builder().role(role).menu(menu).assignedBy(user).build();
        roleMenuRepository.save(roleMenu);
    }

    @Transactional
    public void createNewRole(String roleName, String identity) {
        User user = userRepository.findByIdentity(identity).orElseThrow(()->new RuntimeException("User not found"));
        Role role = Role.builder().name(roleName).createdBy(user).build();
        roleRepository.save(role);
    }

    @Transactional
    public void editRole(Integer roleId, String roleName, String identity) {
        User user = findUser(identity);
        requireText(roleName, "role");
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        role.setName(roleName.trim());
        role.setUpdatedBy(user);
        roleRepository.save(role);
    }

    @Transactional
    public void deleteRole(Integer roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        roleMenuRepository.deleteAll(roleMenuRepository.findByRoleId(roleId));
        roleRepository.delete(role);
    }

    @Transactional
    public void editMenu(Integer menuId, MenuRequest request, String identity) {
        User user = findUser(identity);
        requireText(request.getName(), "name");
        requireText(request.getUrl(), "url");
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new RuntimeException("Menu not found"));
        menu.setName(request.getName().trim());
        menu.setUrl(request.getUrl().trim());
        menu.setLogo(request.getLogo());
        menu.setUpdatedBy(user);
        menuRepository.save(menu);
    }

    @Transactional
    public void deleteMenu(Integer menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new RuntimeException("Menu not found"));
        roleMenuRepository.deleteAll(roleMenuRepository.findByMenuId(menuId));
        menuRepository.delete(menu);
    }

    @Transactional
    public void editRoleMenu(Integer roleMenuId, AddRoleMenuRequest request, String identity) {
        User user = findUser(identity);
        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new RuntimeException("Role not found"));
        Menu menu = menuRepository.findByName(request.getMenu())
                .orElseThrow(() -> new RuntimeException("Menu not found"));
        RoleMenu roleMenu = roleMenuRepository.findById(roleMenuId)
                .orElseThrow(() -> new RuntimeException("Role menu not found"));
        roleMenu.setRole(role);
        roleMenu.setMenu(menu);
        roleMenu.setAssignedBy(user);
        roleMenuRepository.save(roleMenu);
    }

    @Transactional
    public void deleteRoleMenu(Integer roleMenuId) {
        RoleMenu roleMenu = roleMenuRepository.findById(roleMenuId)
                .orElseThrow(() -> new RuntimeException("Role menu not found"));
        roleMenuRepository.delete(roleMenu);
    }

    private User findUser(String identity) {
        return userRepository.findByIdentity(identity)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    public List<GetAllRole> getAllRole(){
        return roleRepository.findAll().stream()
                .map(role -> GetAllRole.builder()
                        .id(role.getId() == null ? null : role.getId().longValue())
                        .role(role.getName())
                        .build())
                .toList();
    }

    public List<GetAllMenu> getAllMenu() {
        return getAllMenu(null, null);
    }

    public List<GetAllMenu> getAllMenu(String name, String url) {
        return menuRepository.findAll().stream()
                .filter(menu -> !hasText(name) || containsIgnoreCase(menu.getName(), name))
                .filter(menu -> !hasText(url) || containsIgnoreCase(menu.getUrl(), url))
                .map(menu -> GetAllMenu.builder()
                        .id(menu.getId() == null ? null : menu.getId().longValue())
                        .url(menu.getUrl())
                        .name(menu.getName())
                        .logo(menu.getLogo())
                        .createdBy(menu.getCreatedBy() == null ? null : menu.getCreatedBy().getIdentity())
                        .createdAt(menu.getCreatedAt())
                        .updatedAt(menu.getUpdatedAt())
                        .build())
                .toList();
    }

    public List<GetAllRoleMenu> getAllRoleMenu() {
        return getAllRoleMenu(null, null, null);
    }

    public List<GetAllRoleMenu> getAllRoleMenu(String menuName, String menuUrl, String role) {
        return roleMenuRepository.findAll().stream()
                .filter(roleMenu -> roleMenu.getMenu() != null
                        && (!hasText(menuName) || containsIgnoreCase(roleMenu.getMenu().getName(), menuName))
                        && (!hasText(menuUrl) || containsIgnoreCase(roleMenu.getMenu().getUrl(), menuUrl)))
                .filter(roleMenu -> !hasText(role)
                        || roleMenu.getRole() != null && containsIgnoreCase(roleMenu.getRole().getName(), role))
                .map(roleMenu -> {
                    Role roleEntity = roleMenu.getRole();
                    Menu menu = roleMenu.getMenu();
                    User assignedBy = roleMenu.getAssignedBy();
                    return GetAllRoleMenu.builder()
                            .id(roleMenu.getId() == null ? null : roleMenu.getId().longValue())
                            .roleId(roleEntity == null || roleEntity.getId() == null ? null : roleEntity.getId().longValue())
                            .role(roleEntity == null ? null : roleEntity.getName())
                            .menuId(menu == null || menu.getId() == null ? null : menu.getId().longValue())
                            .menu(menu == null ? null : menu.getName())
                            .url(menu == null ? null : menu.getUrl())
                            .logo(menu == null ? null : menu.getLogo())
                            .assignedBy(assignedBy == null ? null : assignedBy.getIdentity())
                            .assignedAt(roleMenu.getAssignedAt())
                            .build();
                })
                .toList();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean containsIgnoreCase(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT)
                .contains(search.trim().toLowerCase(Locale.ROOT));
    }

    private GetAllSuperadminResponse toSuperadminResponse(DetailUserInternal detail) {
        User user = detail.getUser();
        Branch branch = detail.getBranch();

        return GetAllSuperadminResponse.builder()
                .identity(user == null ? null : user.getIdentity())
                .name(detail.getName())
                .role(user == null || user.getRole() == null ? null : user.getRole().getName())
                .noHp(detail.getNoHp())
                .branch(branch == null ? null : GetAllSuperadminResponse.BranchDto.builder()
                        .branchCode(branch.getId())
                        .name(branch.getName())
                        .build())
                .createdAt(detail.getCreatedAt())
                .updatedAt(detail.getUpdatedAt())
                .deletedAt(user == null ? null : user.getDeletedAt())
                .build();
    }

    private GetAllSuperadminResponse toSuperadminResponse(CustomerDetails detail) {
        User user = detail.getUser();

        return GetAllSuperadminResponse.builder()
                .identity(user == null ? null : user.getIdentity())
                .name(detail.getName())
                .role(user == null || user.getRole() == null ? null : user.getRole().getName())
                .noHp(null)
                .branch(null)
                .createdAt(detail.getCreatedAt())
                .updatedAt(detail.getUpdatedAt())
                .deletedAt(user == null ? null : user.getDeletedAt())
                .build();
    }

}
