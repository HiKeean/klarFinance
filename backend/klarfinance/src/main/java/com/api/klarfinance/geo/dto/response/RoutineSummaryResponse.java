package com.api.klarfinance.geo.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoutineSummaryResponse {
    private Integer totalPingsAnalyzed;
    private Integer daysCovered;
    /** Null if there's not enough repeat night-time samples yet to infer a "home" spot with any
     * confidence (see RoutineInferenceService#dominantCluster) - not an error, just "not enough
     * data yet". */
    private RoutineLocationResponse homeLocation;
    private RoutineLocationResponse daytimeLocation;
}
