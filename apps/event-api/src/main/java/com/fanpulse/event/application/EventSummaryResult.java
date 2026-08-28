package com.fanpulse.event.application;

import com.fanpulse.event.domain.Category;
import com.fanpulse.event.domain.Event;
import com.fanpulse.event.domain.EventStatus;

import java.time.Clock;
import java.time.OffsetDateTime;

public record EventSummaryResult(
        Long id,
        String title,
        Category category,
        EventStatus status,
        OffsetDateTime startAt,
        OffsetDateTime endAt
) {
    static EventSummaryResult from(Event event, Clock clock) {
        return new EventSummaryResult(
                event.getId(),
                event.getTitle(),
                event.getCategory(),
                event.status(clock),
                event.getStartAt(),
                event.getEndAt()
        );
    }
}
