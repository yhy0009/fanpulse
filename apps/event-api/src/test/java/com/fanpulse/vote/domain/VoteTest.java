package com.fanpulse.vote.domain;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VoteTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-28T00:00:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void createsVoteWithCurrentTime() {
        Vote vote = Vote.create(1L, 1001L, 3L, CLOCK);

        assertThat(vote.getEventId()).isEqualTo(1L);
        assertThat(vote.getUserId()).isEqualTo(1001L);
        assertThat(vote.getCandidateId()).isEqualTo(3L);
        assertThat(vote.getCreatedAt().toInstant()).isEqualTo(CLOCK.instant());
    }

    @Test
    void rejectsNonPositiveIdentifiers() {
        assertThatThrownBy(() -> Vote.create(1L, 0L, 3L, CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId must be positive");
        assertThatThrownBy(() -> Vote.create(1L, 1001L, -1L, CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("candidateId must be positive");
    }
}
