package com.fanpulse.vote.application;

import com.fanpulse.event.application.CandidateNotFoundException;
import com.fanpulse.event.application.EventNotFoundException;
import com.fanpulse.event.domain.Candidate;
import com.fanpulse.event.domain.Category;
import com.fanpulse.event.domain.Event;
import com.fanpulse.support.StubCandidateRepository;
import com.fanpulse.support.StubEventRepository;
import com.fanpulse.support.StubVoteRepository;
import com.fanpulse.vote.domain.DuplicateVoteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VoteServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-28T00:00:00Z"),
            ZoneOffset.UTC
    );

    private StubEventRepository eventRepository;
    private StubCandidateRepository candidateRepository;
    private StubVoteRepository voteRepository;
    private VoteService voteService;

    @BeforeEach
    void setUp() {
        eventRepository = new StubEventRepository();
        candidateRepository = new StubCandidateRepository();
        voteRepository = new StubVoteRepository();
        voteService = new VoteService(eventRepository, candidateRepository, voteRepository, CLOCK);
    }

    @Test
    void castsVoteForCandidateInOpenEvent() {
        eventRepository.put(1L, event(1L, "2026-08-27T00:00:00Z", "2026-08-29T00:00:00Z"));
        candidateRepository.put(3L, candidate(3L, 1L));

        VoteResult result = voteService.castVote(1L, 1001L, 3L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.eventId()).isEqualTo(1L);
        assertThat(result.candidateId()).isEqualTo(3L);
        assertThat(voteRepository.values()).singleElement()
                .satisfies(vote -> assertThat(vote.getUserId()).isEqualTo(1001L));
    }

    @Test
    void rejectsMissingEvent() {
        assertThatThrownBy(() -> voteService.castVote(999L, 1001L, 3L))
                .isInstanceOf(EventNotFoundException.class);
    }

    @Test
    void rejectsClosedEvent() {
        eventRepository.put(1L, event(1L, "2026-08-26T00:00:00Z", "2026-08-27T00:00:00Z"));

        assertThatThrownBy(() -> voteService.castVote(1L, 1001L, 3L))
                .isInstanceOf(EventNotOpenException.class)
                .hasMessage("Event is not open");
    }

    @Test
    void hidesCandidateThatBelongsToAnotherEvent() {
        eventRepository.put(1L, event(1L, "2026-08-27T00:00:00Z", "2026-08-29T00:00:00Z"));
        candidateRepository.put(3L, candidate(3L, 2L));

        assertThatThrownBy(() -> voteService.castVote(1L, 1001L, 3L))
                .isInstanceOf(CandidateNotFoundException.class);
    }

    @Test
    void propagatesDuplicateVoteDetectedByRepositoryConstraint() {
        eventRepository.put(1L, event(1L, "2026-08-27T00:00:00Z", "2026-08-29T00:00:00Z"));
        candidateRepository.put(3L, candidate(3L, 1L));
        voteRepository.setDuplicate(true);

        assertThatThrownBy(() -> voteService.castVote(1L, 1001L, 3L))
                .isInstanceOf(DuplicateVoteException.class)
                .hasMessage("Vote already exists");
    }

    private Event event(Long id, String startAt, String endAt) {
        Event event = Event.create(
                "Vote Event",
                "Description",
                Category.GAME,
                OffsetDateTime.parse(startAt),
                OffsetDateTime.parse(endAt),
                CLOCK
        );
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }

    private Candidate candidate(Long id, Long eventId) {
        Candidate candidate = Candidate.create(eventId, "Candidate " + id, 1, CLOCK);
        ReflectionTestUtils.setField(candidate, "id", id);
        return candidate;
    }
}
