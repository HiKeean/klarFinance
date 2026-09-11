package com.api.klarfinance.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.api.klarfinance.auth.dto.AuthenticationRequest;
import com.api.klarfinance.auth.dto.RegisterRequest;
import com.api.klarfinance.auth.dto.request.ChangePasswordRequest;
import com.api.klarfinance.auth.dto.request.EditProfileRequest;
import com.api.klarfinance.auth.dto.request.FcmTokenRequest;
import com.api.klarfinance.auth.dto.request.VerifyPasswordRequest;
import com.api.klarfinance.auth.dto.response.LoginResponse;
import com.api.klarfinance.auth.dto.response.ProfileResponse;
import com.api.klarfinance.auth.model.*;
import com.api.klarfinance.auth.repository.*;
import com.api.klarfinance.config.JwtService;
import com.api.klarfinance.dbo.model.*;
import com.api.klarfinance.dbo.repository.BranchRepository;
import com.api.klarfinance.dbo.repository.VillageRepository;
import com.api.klarfinance.global.AppConstant;
import com.api.klarfinance.los.repository.ActiveLimitRepository;
import com.api.klarfinance.token.*;

import java.security.Principal;
import java.security.SecureRandom;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthenticationInternalService {
    private final UserRepository userRepository;
    private final DetailUserInternalRepository detailRepository;
    private final RoleRepository roleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenRepository tokenRepository;
    private final BranchRepository branchRepository;
    private final VillageRepository villageRepository;
    private final StringRedisTemplate redisTemplate;
    private final KirimiWhatsappService kirimiWhatsappService;
    private final ActiveLimitRepository activeLimitRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public LoginResponse login(AuthenticationRequest request) {
        require(request.getIdentity(), "identity"); require(request.getPassword(), "password");
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getIdentity(), request.getPassword()));
        User user = userRepository.findByIdentity(request.getIdentity()).orElseThrow(() -> new IllegalStateException("User not found"));
        return issueTokens(user);
    }

    /** Redeems a refresh token for a fresh access+refresh pair - used by the Kotlin app's
     * fingerprint-gated session restore (stores the refresh token behind a biometric prompt,
     * see kotlin-nasabah-app knowledge). Access tokens are short-lived (24h,
     * application.security.jwt.expiration) - this is how a device re-establishes a session
     * without asking for the password again once the access token expires. */
    @Transactional
    public LoginResponse refresh(String refreshToken) {
        require(refreshToken, "refreshToken");
        Token storedToken = tokenRepository.findByToken(refreshToken)
                .filter(t -> t.getTokenType() == TokenType.REFRESH && !t.isRevoked() && !t.isExpired())
                .orElseThrow(() -> new IllegalArgumentException("Refresh token is invalid or expired"));
        User user = storedToken.getUser();
        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new IllegalArgumentException("Refresh token is invalid or expired");
        }
        return issueTokens(user);
    }

    private LoginResponse issueTokens(User user) {
        revokeTokens(user);
        String access = jwtService.generateToken(user), refresh = jwtService.generateRefreshToken(user);
        saveToken(user, access, TokenType.ACCESS); saveToken(user, refresh, TokenType.REFRESH);
        DetailUserInternal detail = detailRepository.findByUserId(user.getId()).orElse(null);
        List<LoginResponse.MenuResponse> menus = roleMenuRepository.findByRoleId(user.getRole().getId()).stream()
                .map(RoleMenu::getMenu).filter(java.util.Objects::nonNull)
                .map(m -> LoginResponse.MenuResponse.builder().url(m.getUrl()).name(m.getName()).logo(m.getLogo()).build()).toList();
        String accountStatus = AppConstant.ROLE_NASABAH.equalsIgnoreCase(user.getRole().getName())
                ? (activeLimitRepository.findByUserIdAndIsActiveTrue(user.getId()).isPresent() ? "ACTIVE" : "PENDING_APPLICATION")
                : null;
        return LoginResponse.builder().accessToken(access).refreshToken(refresh)
                .userProfile(LoginResponse.UserProfile.builder().identity(user.getIdentity()).role(user.getRole().getName()).name(detail == null ? null : detail.getName()).build())
                .menu(menus).accountStatus(accountStatus).build();
    }

    @Transactional
    public void register(RegisterRequest request) {
        require(request.getPassword(), "password"); require(request.getRole(), "role"); require(request.getName(), "name");
        Role role = roleRepository.findByName(request.getRole().trim().toUpperCase(Locale.ROOT)).orElseThrow(() -> new IllegalArgumentException("Role not found"));
        Branch branch = request.getBranchId() == null ? null : branchRepository.findById(request.getBranchId()).orElseThrow(() -> new IllegalArgumentException("Branch not found"));
        Village village = request.getVillageId() == null ? null : villageRepository.findById(request.getVillageId()).orElseThrow(() -> new IllegalArgumentException("Village not found"));
        User user = userRepository.save(User.builder().identity(generateIdentity()).password(passwordEncoder.encode(request.getPassword())).role(role).build());
        detailRepository.save(DetailUserInternal.builder().user(user).dob(request.getDob()).name(request.getName()).noHp(request.getNoHp()).branch(branch).village(village).address(request.getAddress()).build());
        kirimiWhatsappService.sendRegistrationMessage(request.getNoHp(), user.getIdentity(), request.getPassword(), request.getName());
    }

    @Transactional
    public void logout(Principal principal, HttpServletRequest request) {
        User user = currentUser(principal);
        tokenRepository.deleteAll(tokenRepository.findByUserId(user.getId()));
        String identity = user.getIdentity();
        redisTemplate.delete(List.of(identity, "internal:" + identity, "internal_login:" + identity));
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) redisTemplate.delete(header.substring(7));
    }

    @Transactional public void changePassword(ChangePasswordRequest request, Principal principal) {
        User user = currentUser(principal);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) throw new IllegalArgumentException("Old password is incorrect");
        require(request.getNewPassword(), "newPassword"); user.setPassword(passwordEncoder.encode(request.getNewPassword())); userRepository.save(user);
    }

    /** Step-up auth generik buat konfirmasi transaksi (konfirmasi user 2026-09-07: kalau
     * fingerprint nasabah gak aktif, transaksi apapun - pinjaman/QRIS - wajib masukin password
     * dulu sebelum diproses). Tidak menerbitkan token baru sama sekali (beda dari /login) - cuma
     * cocok/tidaknya password terhadap user yang SUDAH login (Principal dari JWT existing). */
    public void verifyPassword(VerifyPasswordRequest request, Principal principal) {
        User user = currentUser(principal);
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Password salah");
        }
    }

    /** Nasabah-only (push notification/FCM cuma buat nasabah, lihat PushNotificationService) -
     * no-op kalau caller-nya staff, biar Kotlin app bisa manggil ini tanpa perlu tau/cek role
     * dulu sendiri. */
    @Transactional
    public void updateFcmToken(FcmTokenRequest request, Principal principal) {
        User user = currentUser(principal);
        if (!AppConstant.ROLE_NASABAH.equalsIgnoreCase(user.getRole().getName())) return;
        customerDetailsRepository.findByUserId(user.getId())
                .ifPresent(details -> {
                    details.setFcmToken(request.getFcmToken());
                    customerDetailsRepository.save(details);
                });
    }

    @Transactional public ProfileResponse profile(Principal principal) { return toProfile(currentUser(principal)); }

    @Transactional public ProfileResponse edit(EditProfileRequest request, Principal principal) {
        User user = currentUser(principal); DetailUserInternal d = detailRepository.findByUserId(user.getId()).orElseThrow(() -> new IllegalStateException("User detail not found"));
        if (request.getDob() != null) d.setDob(request.getDob()); if (request.getName() != null) d.setName(request.getName()); if (request.getNoHp() != null) d.setNoHp(request.getNoHp());
        if (request.getBranchId() != null) d.setBranch(branchRepository.findById(request.getBranchId()).orElseThrow(() -> new IllegalArgumentException("Branch not found")));
        if (request.getVillageId() != null) d.setVillage(villageRepository.findById(request.getVillageId()).orElseThrow(() -> new IllegalArgumentException("Village not found")));
        return toProfile(userRepository.save(user));
    }










    /** Nasabah has no DetailUserInternal row (their data lives in CustomerDetails instead) -
     * branch by role so GET /profile doesn't 500 for nasabah callers. */
    private ProfileResponse toProfile(User u) {
        if (AppConstant.ROLE_NASABAH.equalsIgnoreCase(u.getRole().getName())) {
            CustomerDetails c = customerDetailsRepository.findByUserId(u.getId())
                    .orElseThrow(() -> new IllegalStateException("Customer detail not found"));
            Village v = c.getVillage(); District di = v == null ? null : v.getDistrict();
            Regency r = di == null ? null : di.getRegency(); Province p = r == null ? null : r.getProvince();
            String accountStatus = activeLimitRepository.findByUserIdAndIsActiveTrue(u.getId()).isPresent()
                    ? "ACTIVE" : "PENDING_APPLICATION";
            return ProfileResponse.builder().name(c.getName()).dob(c.getDob()).noHp(u.getIdentity())
                    .email(c.getEmail()).emailVerified(c.getEmailVerifiedAt() != null)
                    .role(u.getRole().getName()).branchId(null).accountStatus(accountStatus)
                    .villageId(v == null ? null : v.getId()).districtId(di == null ? null : di.getId())
                    .regencyId(r == null ? null : r.getId()).provinceId(p == null ? null : p.getId()).build();
        }
        DetailUserInternal d = detailRepository.findByUserId(u.getId()).orElseThrow(() -> new IllegalStateException("User detail not found")); Village v=d.getVillage(); District di=v==null?null:v.getDistrict(); Regency r=di==null?null:di.getRegency(); Province p=r==null?null:r.getProvince(); return ProfileResponse.builder().name(d.getName()).dob(d.getDob()).noHp(d.getNoHp()).role(u.getRole().getName()).branchId(d.getBranch()==null?null:d.getBranch().getId()).villageId(v==null?null:v.getId()).districtId(di==null?null:di.getId()).regencyId(r==null?null:r.getId()).provinceId(p==null?null:p.getId()).build();
    }
    private User currentUser(Principal p) { if (p == null) throw new IllegalStateException("Authentication required"); return userRepository.findByIdentity(p.getName()).orElseThrow(() -> new IllegalStateException("User not found")); }
    private void revokeTokens(User u) { tokenRepository.findAllValidTokenByUser(u.getId()).forEach(t -> {t.setExpired(true); t.setRevoked(true);}); tokenRepository.deleteAll(tokenRepository.findByUserId(u.getId())); }
    private void saveToken(User u,String value,TokenType type) { tokenRepository.save(Token.builder().user(u).token(value).tokenType(type).build()); }
    private String generateIdentity() { String id; do { id=YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM"))+String.format("%04d",RANDOM.nextInt(10000)); } while(userRepository.findByIdentity(id).isPresent()); return id; }
    private void require(String value,String field) { if(!StringUtils.hasText(value)) throw new IllegalArgumentException(field+" is required"); }
}
