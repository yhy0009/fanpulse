package com.fanpulse.event.presentation;

import com.fanpulse.event.application.EventDetailResult;
import com.fanpulse.event.domain.Category;
import com.fanpulse.event.domain.EventStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record EventDetailResponse(
        Long id,
        String title,
        String description,
        Category category,
        EventStatus status,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        List<CandidateSummaryResponse> candidates
) {
    static EventDetailResponse from(EventDetailResult result) {
        return new EventDetailResponse(
                result.id(),
                result.title(),
                result.description(),
                result.category(),
                result.status(),
                result.startAt(),
                result.endAt(),
                result.candidates().stream()
                        .map(CandidateSummaryResponse::from)
                        .toList()
        );
    }
}
