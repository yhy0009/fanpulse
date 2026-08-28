package com.fanpulse.event.application;

import com.fanpulse.event.domain.Candidate;

public record CandidateSummaryResult(
        Long id,
        String name,
        int displayOrder
) {
    public static CandidateSummaryResult from(Candidate candidate) {
        return new CandidateSummaryResult(
                candidate.getId(),
                candidate.getName(),
                candidate.getDisplayOrder()
        );
    }
}
