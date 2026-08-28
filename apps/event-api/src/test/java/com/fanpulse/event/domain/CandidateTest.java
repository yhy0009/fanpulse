package com.fanpulse.event.domain;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CandidateTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-28T00:00:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void createsCandidateAndTrimsName() {
        Candidate candidate = Candidate.create(1L, "  Nova  ", 2, CLOCK);

        assertThat(candidate.getEventId()).isEqualTo(1L);
        assertThat(candidate.getName()).isEqualTo("Nova");
        assertThat(candidate.getDisplayOrder()).isEqualTo(2);
        assertThat(candidate.getCreatedAt().toInstant()).isEqualTo(CLOCK.instant());
    }

    @Test
    void rejectsInvalidValues() {
        assertThatThrownBy(() -> Candidate.create(0L, "Nova", 1, CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("eventId must be positive");
        assertThatThrownBy(() -> Candidate.create(1L, " ", 1, CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
        assertThatThrownBy(() -> Candidate.create(1L, "Nova", 0, CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("displayOrder must be positive");
    }
}
