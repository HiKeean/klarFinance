package com.api.klarfinance.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.api.klarfinance.annotation.AdminAnnotation;
import com.api.klarfinance.auth.dto.RegisterRequest;
import com.api.klarfinance.auth.dto.request.AddRoleMenuRequest;
import com.api.klarfinance.auth.dto.request.AddRoleRequest;
import com.api.klarfinance.auth.dto.request.AssignBranchRequest;
import com.api.klarfinance.auth.dto.request.MenuRequest;
import com.api.klarfinance.auth.dto.request.PasswordResetDecisionRequest;
import com.api.klarfinance.auth.dto.response.GetAllMenu;
import com.api.klarfinance.auth.dto.response.GetAllRole;
import com.api.klarfinance.auth.dto.response.GetAllRoleMenu;
import com.api.klarfinance.auth.dto.response.GetAllSuperadminResponse;
import com.api.klarfinance.auth.dto.response.PasswordResetRequestResponse;
import com.api.klarfinance.auth.service.AuthenticationInternalService;
import com.api.klarfinance.auth.service.AuthenticationSuperadminService;
import com.api.klarfinance.auth.service.PasswordResetRequestService;
import com.api.klarfinance.global.ApiResponse;
import com.api.klarfinance.global.ApiResponsePagination;

import java.security.Principal;
import java.util.List;

@AdminAnnotation
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthenticationSuperadminController {
    private final AuthenticationSuperadminService service;
    private final PasswordResetRequestService passwordResetRequestService;
    private final AuthenticationInternalService authService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<ApiResponsePagination<GetAllSuperadminResponse>>> getAllSuperadmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long branchId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Berhasil mengambil user page " + page,ApiResponsePagination.from(
                service.getAllSuperadmin(size, page, search, role, branchId))));
    }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<GetAllRole>>> getAllRole() {
        return ResponseEntity.ok(ApiResponse.success("Roles fetched successfully", service.getAllRole()));
    }

    @GetMapping("/menus")
    public ResponseEntity<ApiResponse<List<GetAllMenu>>> getAllMenu(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String url) {
        return ResponseEntity.ok(ApiResponse.success("Menus fetched successfully", service.getAllMenu(name, url)));
    }

    @GetMapping("/role-menus")
    public ResponseEntity<ApiResponse<List<GetAllRoleMenu>>> getAllRoleMenu(
            @RequestParam(required = false) String menuName,
            @RequestParam(required = false) String menuUrl,
            @RequestParam(required = false) String role) {
        return ResponseEntity.ok(ApiResponse.success("Role menus fetched successfully",
                service.getAllRoleMenu(menuName, menuUrl, role)));
    }

    /** Assign/ganti Branch buat satu user staff internal (dipakai buat pasang BM ke Branch dari
     * halaman Branch webadmin) - branchId null = unassign. Cuma valid buat user yang punya
     * DetailUserInternal (staff), lihat AuthenticationSuperadminService#assignBranch. */
    @PatchMapping("/users/{identity}/branch")
    public ResponseEntity<ApiResponse<Object>> assignBranch(
            @PathVariable String identity, @RequestBody AssignBranchRequest request) {
        service.assignBranch(identity, request.getBranchId());
        return ResponseEntity.ok(ApiResponse.success("Branch assigned successfully", null));
    }

    @DeleteMapping("/")
    public ResponseEntity<ApiResponse<Object>> delete(@RequestParam String identity, Principal principal) {
        String superadminId = principal.getName();
        service.deleteUser(identity, superadminId);
        return ResponseEntity.ok(ApiResponse.success("Superadmin deleted successfully", null));
    }

    @GetMapping("/identity")
    public ResponseEntity<ApiResponse<String>> getIdentity(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Identity fetched successfully", principal.getName()));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Object>> register(@RequestBody RegisterRequest request) {
        authService.register(request); return ResponseEntity.ok(ApiResponse.success("Registration successful", null));
    }

    @PostMapping("/menu")
    public ResponseEntity<ApiResponse<Object>> addMenu(@RequestBody MenuRequest request, Principal principal) {
        service.addMenu(request, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Menu added successfully", null));
    }

    @PostMapping("/role-menu")
    public ResponseEntity<ApiResponse<Object>> addRoleMenu(@RequestBody AddRoleMenuRequest request, Principal principal) {
        service.addRoleMenu(request, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Role Menu assigned successfully", null));
    }

    @PostMapping("/role")
    public ResponseEntity<ApiResponse<Object>> addRole(@RequestBody AddRoleRequest request, Principal principal) {
        service.createNewRole(request.getRole(), principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Role added successfully", null));
    }

    @PutMapping("/roles/{id}")
    public ResponseEntity<ApiResponse<Object>> editRole(
            @PathVariable Integer id, @RequestBody AddRoleRequest request, Principal principal) {
        service.editRole(id, request.getRole(), principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Role updated successfully", null));
    }

    @DeleteMapping("/roles/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteRole(@PathVariable Integer id) {
        service.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success("Role deleted successfully", null));
    }

    @PutMapping("/menus/{id}")
    public ResponseEntity<ApiResponse<Object>> editMenu(
            @PathVariable Integer id, @RequestBody MenuRequest request, Principal principal) {
        service.editMenu(id, request, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Menu updated successfully", null));
    }

    @DeleteMapping("/menus/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteMenu(@PathVariable Integer id) {
        service.deleteMenu(id);
        return ResponseEntity.ok(ApiResponse.success("Menu deleted successfully", null));
    }

    @PutMapping("/role-menus/{id}")
    public ResponseEntity<ApiResponse<Object>> editRoleMenu(
            @PathVariable Integer id, @RequestBody AddRoleMenuRequest request, Principal principal) {
        service.editRoleMenu(id, request, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Role menu updated successfully", null));
    }

    @DeleteMapping("/role-menus/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteRoleMenu(@PathVariable Integer id) {
        service.deleteRoleMenu(id);
        return ResponseEntity.ok(ApiResponse.success("Role menu deleted successfully", null));
    }

    /** Antrean permintaan reset password (dari tombol "Lupa Password" di halaman login
     * Checker&BM, muncul setelah 3x salah password) - default cuma PENDING, override lewat
     * ?status= kalau butuh lihat APPROVED/REJECTED juga. */
    @GetMapping("/password-reset-requests")
    public ResponseEntity<ApiResponse<List<PasswordResetRequestResponse>>> listPasswordResetRequests(
            @RequestParam(defaultValue = "PENDING") String status) {
        return ResponseEntity.ok(ApiResponse.success("Password reset requests fetched successfully",
                passwordResetRequestService.listQueue(status)));
    }

    @PostMapping("/password-reset-requests/{id}/decision")
    public ResponseEntity<ApiResponse<Object>> decidePasswordResetRequest(
            @PathVariable Long id, @RequestBody PasswordResetDecisionRequest request, Principal principal) {
        passwordResetRequestService.decide(id, request.getAction(), request.getReason(), principal);
        return ResponseEntity.ok(ApiResponse.success("Password reset request processed successfully", null));
    }

}
