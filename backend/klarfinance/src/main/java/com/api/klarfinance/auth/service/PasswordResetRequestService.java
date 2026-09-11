package com.api.klarfinance.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.api.klarfinance.auth.dto.response.PasswordResetRequestResponse;
import com.api.klarfinance.auth.model.DetailUserInternal;
import com.api.klarfinance.auth.model.PasswordResetRequest;
import com.api.klarfinance.auth.model.User;
import com.api.klarfinance.auth.repository.DetailUserInternalRepository;
import com.api.klarfinance.auth.repository.PasswordResetRequestRepository;
import com.api.klarfinance.auth.repository.UserRepository;

import java.security.Principal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Alur "Lupa Password" - dipicu dari tombol reset password di halaman login (frontend
 * Checker&BM, muncul setelah 3x salah password, lihat project knowledge
 * "password-reset-request"). Nasabah/staff submit identity -> masuk antrean PENDING di webadmin
 * -> Superadmin approve/reject. Approve = generate password baru random, jadi password akun yang
 * baru, dikirim via WhatsApp (KirimiWhatsappService) - user login pakai password baru itu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetRequestService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789"; // no 0/O/1/l/I - avoid look-alikes
    private static final int PASSWORD_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final UserRepository userRepository;
    private final DetailUserInternalRepository detailUserInternalRepository;
    private final PasswordEncoder passwordEncoder;
    private final KirimiWhatsappService kirimiWhatsappService;

    @Transactional
    public void submitRequest(String identity) {
        User user = userRepository.findByIdentity(identity.trim())
                .orElseThrow(() -> new IllegalArgumentException("Identity tidak ditemukan"));

        passwordResetRequestRepository.findByUserAndStatus(user, PasswordResetRequest.STATUS_PENDING)
                .ifPresent(existing -> {
                    throw new IllegalStateException("Sudah ada permintaan reset password yang masih diproses untuk identity ini");
                });

        passwordResetRequestRepository.save(PasswordResetRequest.builder().user(user).build());
        log.info("Password reset request submitted for identity {}", identity);
    }

    @Transactional(readOnly = true)
    public List<PasswordResetRequestResponse> listQueue(String status) {
        String normalizedStatus = StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : null;
        return passwordResetRequestRepository.findAllByOptionalStatus(normalizedStatus).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void decide(Long id, String action, String reason, Principal caller) {
        PasswordResetRequest request = passwordResetRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Password reset request not found"));

        if (!PasswordResetRequest.STATUS_PENDING.equals(request.getStatus())) {
            throw new IllegalStateException("Request ini sudah diproses sebelumnya");
        }

        String normalizedAction = action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
        User decider = userRepository.findByIdentity(caller.getName())
                .orElseThrow(() -> new IllegalStateException("Caller not found"));

        if ("REJECT".equals(normalizedAction)) {
            if (!StringUtils.hasText(reason)) {
                throw new IllegalArgumentException("Alasan penolakan wajib diisi");
            }
            request.setStatus(PasswordResetRequest.STATUS_REJECTED);
            request.setReason(reason);
            request.setDecidedAt(LocalDateTime.now());
            request.setDecidedBy(decider);
            passwordResetRequestRepository.save(request);
            log.info("Password reset request {} rejected by {}", id, caller.getName());
            return;
        }

        if (!"APPROVE".equals(normalizedAction)) {
            throw new IllegalArgumentException("action must be APPROVE or REJECT");
        }

        User targetUser = request.getUser();
        DetailUserInternal detail = detailUserInternalRepository.findByUserId(targetUser.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Nomor HP staff tidak ditemukan (belum ada DetailUserInternal) - tidak bisa kirim WhatsApp"));
        if (!StringUtils.hasText(detail.getNoHp())) {
            throw new IllegalStateException("Nomor HP staff kosong - tidak bisa kirim WhatsApp");
        }

        String newPassword = generateRandomPassword();
        targetUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(targetUser);

        kirimiWhatsappService.sendPasswordResetMessage(detail.getNoHp(), targetUser.getIdentity(), newPassword);

        request.setStatus(PasswordResetRequest.STATUS_APPROVED);
        request.setReason(reason);
        request.setDecidedAt(LocalDateTime.now());
        request.setDecidedBy(decider);
        passwordResetRequestRepository.save(request);
        log.info("Password reset request {} approved by {}, new password sent via WhatsApp to identity {}",
                id, caller.getName(), targetUser.getIdentity());
    }

    private PasswordResetRequestResponse toResponse(PasswordResetRequest request) {
        User user = request.getUser();
        String name = detailUserInternalRepository.findByUserId(user.getId())
                .map(DetailUserInternal::getName)
                .orElse(null);
        return PasswordResetRequestResponse.builder()
                .id(request.getId())
                .identity(user.getIdentity())
                .name(name)
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .requestedAt(request.getRequestedAt())
                .status(request.getStatus())
                .decidedAt(request.getDecidedAt())
                .decidedBy(request.getDecidedBy() != null ? request.getDecidedBy().getIdentity() : null)
                .reason(request.getReason())
                .build();
    }

    private String generateRandomPassword() {
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
