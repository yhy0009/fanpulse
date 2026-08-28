package com.fanpulse.vote.presentation;

import com.fanpulse.common.config.TraceIdFilter;
import com.fanpulse.common.exception.GlobalExceptionHandler;
import com.fanpulse.event.domain.Candidate;
import com.fanpulse.event.domain.Category;
import com.fanpulse.event.domain.Event;
import com.fanpulse.support.StubCandidateRepository;
import com.fanpulse.support.StubEventRepository;
import com.fanpulse.support.StubVoteRepository;
import com.fanpulse.vote.application.VoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VoteController.class)
@Import({
        VoteService.class,
        GlobalExceptionHandler.class,
        TraceIdFilter.class,
        VoteControllerTest.TestConfig.class
})
class VoteControllerTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-28T00:00:00Z"),
            ZoneOffset.UTC
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StubEventRepository eventRepository;

    @Autowired
    private StubCandidateRepository candidateRepository;

    @Autowired
    private StubVoteRepository voteRepository;

    @BeforeEach
    void setUp() {
        eventRepository.clear();
        candidateRepository.clear();
        voteRepository.clear();
    }

    @Test
    void createsVote() throws Exception {
        eventRepository.put(1L, openEvent(1L));
        candidateRepository.put(3L, candidate(3L, 1L));

        mockMvc.perform(post("/api/v1/events/1/votes")
                        .header("X-Test-User-Id", "1001")
                        .contentType("application/json")
                        .content("{\"candidateId\":3}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.eventId").value(1))
                .andExpect(jsonPath("$.candidateId").value(3))
                .andExpect(jsonPath("$.createdAt").value("2026-08-28T00:00:00Z"))
                .andExpect(jsonPath("$.userId").doesNotExist());
    }

    @Test
    void rejectsMissingUserHeader() throws Exception {
        mockMvc.perform(post("/api/v1/events/1/votes")
                        .contentType("application/json")
                        .content("{\"candidateId\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void rejectsNonPositiveCandidateId() throws Exception {
        mockMvc.perform(post("/api/v1/events/1/votes")
                        .header("X-Test-User-Id", "1001")
                        .contentType("application/json")
                        .content("{\"candidateId\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void returnsConflictWhenEventIsClosed() throws Exception {
        eventRepository.put(1L, event(
                1L,
                "2026-08-26T00:00:00Z",
                "2026-08-27T00:00:00Z"
        ));

        mockMvc.perform(post("/api/v1/events/1/votes")
                        .header("X-Test-User-Id", "1001")
                        .contentType("application/json")
                        .content("{\"candidateId\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_OPEN"));
    }

    @Test
    void returnsNotFoundWhenCandidateDoesNotBelongToEvent() throws Exception {
        eventRepository.put(1L, openEvent(1L));
        candidateRepository.put(3L, candidate(3L, 2L));

        mockMvc.perform(post("/api/v1/events/1/votes")
                        .header("X-Test-User-Id", "1001")
                        .contentType("application/json")
                        .content("{\"candidateId\":3}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CANDIDATE_NOT_FOUND"));
    }

    @Test
    void returnsConflictForDuplicateVote() throws Exception {
        eventRepository.put(1L, openEvent(1L));
        candidateRepository.put(3L, candidate(3L, 1L));
        voteRepository.setDuplicate(true);

        mockMvc.perform(post("/api/v1/events/1/votes")
                        .header("X-Test-User-Id", "1001")
                        .contentType("application/json")
                        .content("{\"candidateId\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_VOTE"));
    }

    private Event openEvent(Long id) {
        return event(id, "2026-08-27T00:00:00Z", "2026-08-29T00:00:00Z");
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

    @TestConfiguration
    static class TestConfig {

        @Bean
        StubEventRepository eventRepository() {
            return new StubEventRepository();
        }

        @Bean
        StubCandidateRepository candidateRepository() {
            return new StubCandidateRepository();
        }

        @Bean
        StubVoteRepository voteRepository() {
            return new StubVoteRepository();
        }

        @Bean
        Clock clock() {
            return CLOCK;
        }
    }
}
