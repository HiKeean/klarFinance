package com.api.klarfinance.geo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.api.klarfinance.auth.model.User;
import com.api.klarfinance.auth.repository.UserRepository;
import com.api.klarfinance.geo.dto.response.RoutineSummaryResponse;

import java.security.Principal;
import java.util.Set;

/** Thin role-gate in front of RoutineInferenceService for the internal-facing endpoint - only
 * staff who actually review credit applications get to see a nasabah's inferred routine.
 * Kept as its own class (rather than folding the check into RoutineInferenceService) so the
 * inference engine itself stays a pure "pings in, summary out" service with no auth concerns. */
@Service
@RequiredArgsConstructor
public class RoutineAccessService {
    private static final Set<String> ALLOWED_ROLES = Set.of("CHECKER", "BM", "SUPERADMIN");

    private final UserRepository userRepository;
    private final RoutineInferenceService routineInferenceService;

    public RoutineSummaryResponse getRoutine(Integer targetUserId, Principal principal) {
        requireAllowedRole(principal);
        return routineInferenceService.inferRoutine(targetUserId);
    }

    private void requireAllowedRole(Principal principal) {
        if (principal == null) throw new IllegalStateException("Authentication required");
        User caller = userRepository.findByIdentity(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        String role = caller.getRole() == null ? null : caller.getRole().getName().toUpperCase();
        if (role == null || !ALLOWED_ROLES.contains(role)) {
            throw new IllegalStateException("Only Checker, BM, or Superadmin can view location routines");
        }
    }
}
