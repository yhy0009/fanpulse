package com.fanpulse.event.presentation;

import com.fanpulse.event.application.CandidateSummaryResult;

public record CandidateSummaryResponse(
        Long id,
        String name,
        int displayOrder
) {
    static CandidateSummaryResponse from(CandidateSummaryResult result) {
        return new CandidateSummaryResponse(result.id(), result.name(), result.displayOrder());
    }
}
