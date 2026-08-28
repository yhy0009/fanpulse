package com.fanpulse.event.application;

import com.fanpulse.event.domain.Category;
import com.fanpulse.event.domain.Event;
import com.fanpulse.event.domain.EventStatus;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

public record EventDetailResult(
        Long id,
        String title,
        String description,
        Category category,
        EventStatus status,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        List<CandidateSummaryResult> candidates
) {
    static EventDetailResult from(
            Event event,
            Clock clock,
            List<CandidateSummaryResult> candidates
    ) {
        return new EventDetailResult(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCategory(),
                event.status(clock),
                event.getStartAt(),
                event.getEndAt(),
                List.copyOf(candidates)
        );
    }
}
