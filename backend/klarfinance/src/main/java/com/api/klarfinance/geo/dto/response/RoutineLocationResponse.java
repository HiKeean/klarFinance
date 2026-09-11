package com.api.klarfinance.geo.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoutineLocationResponse {
    private Double latitude;
    private Double longitude;
    /** How many of the analyzed pings fell into this same spot (grid-rounded, see
     * RoutineInferenceService). */
    private Integer sampleCount;
    /** sampleCount / total pings in this time-of-day bucket - a rough confidence signal, not a
     * statistically rigorous score. */
    private Double confidence;
}
