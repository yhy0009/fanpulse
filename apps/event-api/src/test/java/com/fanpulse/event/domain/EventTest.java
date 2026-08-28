package com.fanpulse.event.domain;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventTest {

    private static final OffsetDateTime START_AT = OffsetDateTime.parse("2026-01-01T10:00:00Z");
    private static final OffsetDateTime END_AT = OffsetDateTime.parse("2026-01-01T11:00:00Z");

    @Test
    void calculatesScheduledBeforeStartAt() {
        Event event = createEvent(START_AT, END_AT);

        assertThat(event.status(fixedClock("2026-01-01T09:59:59Z")))
                .isEqualTo(EventStatus.SCHEDULED);
    }

    @Test
    void calculatesOpenAtStartAtAndBeforeEndAt() {
        Event event = createEvent(START_AT, END_AT);

        assertThat(event.status(fixedClock("2026-01-01T10:00:00Z")))
                .isEqualTo(EventStatus.OPEN);
        assertThat(event.status(fixedClock("2026-01-01T10:59:59Z")))
                .isEqualTo(EventStatus.OPEN);
    }

    @Test
    void calculatesClosedAtEndAt() {
        Event event = createEvent(START_AT, END_AT);

        assertThat(event.status(fixedClock("2026-01-01T11:00:00Z")))
                .isEqualTo(EventStatus.CLOSED);
    }

    @Test
    void rejectsEqualStartAtAndEndAt() {
        assertThatThrownBy(() -> createEvent(START_AT, START_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("startAt must be before endAt");
    }

    @Test
    void rejectsStartAtAfterEndAt() {
        assertThatThrownBy(() -> createEvent(END_AT, START_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("startAt must be before endAt");
    }

    private Event createEvent(OffsetDateTime startAt, OffsetDateTime endAt) {
        return Event.create(
                "Test Event",
                "Test description",
                Category.GAME,
                startAt,
                endAt,
                fixedClock("2026-01-01T00:00:00Z")
        );
    }

    private Clock fixedClock(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }
}
