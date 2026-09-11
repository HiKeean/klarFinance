package com.api.klarfinance.geo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.api.klarfinance.annotation.InternalAnnotation;
import com.api.klarfinance.geo.dto.response.RoutineSummaryResponse;
import com.api.klarfinance.geo.service.RoutineAccessService;
import com.api.klarfinance.global.ApiResponse;

import java.security.Principal;

/** Checker/BM/Superadmin-facing read side of the location-routine feature - see
 * RoutineInferenceService for how the summary is derived, LocationController for the
 * nasabah-facing opt-in + submission side. */
@InternalAnnotation
@RequestMapping("/geo/routine")
@RequiredArgsConstructor
public class RoutineController {
    private final RoutineAccessService routineAccessService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<RoutineSummaryResponse>> getRoutine(@PathVariable Integer userId, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Routine summary computed",
                routineAccessService.getRoutine(userId, principal)));
    }
}
