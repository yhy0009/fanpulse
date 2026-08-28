package com.fanpulse.event.presentation;

import com.fanpulse.event.application.EventSummaryResult;
import com.fanpulse.event.domain.Category;
import com.fanpulse.event.domain.EventStatus;

import java.time.OffsetDateTime;

public record EventSummaryResponse(
        Long id,
        String title,
        Category category,
        EventStatus status,
        OffsetDateTime startAt,
        OffsetDateTime endAt
) {
    static EventSummaryResponse from(EventSummaryResult result) {
        return new EventSummaryResponse(
                result.id(),
                result.title(),
                result.category(),
                result.status(),
                result.startAt(),
                result.endAt()
        );
    }
}
