package com.fanpulse.event.application;

import com.fanpulse.event.domain.Category;
import com.fanpulse.event.domain.Event;
import com.fanpulse.event.domain.EventStatus;

import java.time.Clock;
import java.time.OffsetDateTime;

public record EventDetailResult(
        Long id,
        String title,
        String description,
        Category category,
        EventStatus status,
        OffsetDateTime startAt,
        OffsetDateTime endAt
) {
    static EventDetailResult from(Event event, Clock clock) {
        return new EventDetailResult(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCategory(),
                event.status(clock),
                event.getStartAt(),
                event.getEndAt()
        );
    }
}
