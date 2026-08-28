package com.fanpulse.event.infrastructure;

import com.fanpulse.event.domain.Category;
import com.fanpulse.event.domain.Candidate;
import com.fanpulse.event.domain.Event;
import com.fanpulse.event.domain.EventStatus;
import com.fanpulse.support.PostgresIntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({EventRepositoryAdapter.class, CandidateRepositoryAdapter.class})
class EventRepositoryIntegrationTest extends PostgresIntegrationTestSupport {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Sort SORT = Sort.by("startAt").ascending().and(Sort.by("id").ascending());

    @Autowired
    private EventRepositoryAdapter eventRepository;

    @Autowired
    private CandidateRepositoryAdapter candidateRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void clearSeedData() {
        entityManager.createNativeQuery(
                "TRUNCATE TABLE votes, candidates, events RESTART IDENTITY CASCADE"
        ).executeUpdate();
    }

    @Test
    void filtersByCategoryAndStatusUsingPostgreSql() {
        entityManager.persist(event("Open Game", Category.GAME, "2025-12-31T00:00:00Z", "2026-01-02T00:00:00Z"));
        entityManager.persist(event("Scheduled Game", Category.GAME, "2026-01-02T00:00:00Z", "2026-01-03T00:00:00Z"));
        entityManager.persist(event("Open Music", Category.MUSIC, "2025-12-31T00:00:00Z", "2026-01-02T00:00:00Z"));
        entityManager.flush();

        var result = eventRepository.search(
                Category.GAME,
                EventStatus.OPEN,
                NOW,
                PageRequest.of(0, 20, SORT)
        );

        assertThat(result.getContent())
                .extracting(Event::getTitle)
                .containsExactly("Open Game");
    }

    @Test
    void sortsByStartAtThenId() {
        entityManager.persist(event("Later", Category.ANIME, "2026-01-03T00:00:00Z", "2026-01-04T00:00:00Z"));
        entityManager.persist(event("Earlier First", Category.ANIME, "2026-01-01T00:00:00Z", "2026-01-02T00:00:00Z"));
        entityManager.persist(event("Earlier Second", Category.ANIME, "2026-01-01T00:00:00Z", "2026-01-02T00:00:00Z"));
        entityManager.flush();

        var result = eventRepository.search(null, null, NOW, PageRequest.of(0, 20, SORT));

        assertThat(result.getContent())
                .extracting(Event::getTitle)
                .containsExactly("Earlier First", "Earlier Second", "Later");
    }

    @Test
    void findsCandidatesInDisplayOrderThenIdOrder() {
        Event event = event("Candidate Event", Category.GAME, "2025-12-31T00:00:00Z", "2026-01-02T00:00:00Z");
        entityManager.persist(event);
        entityManager.flush();
        entityManager.persist(Candidate.create(event.getId(), "Second", 2, CLOCK));
        entityManager.persist(Candidate.create(event.getId(), "First A", 1, CLOCK));
        entityManager.persist(Candidate.create(event.getId(), "First B", 1, CLOCK));
        entityManager.flush();

        var result = candidateRepository.findAllByEventIdOrderByDisplayOrderAscIdAsc(event.getId());

        assertThat(result)
                .extracting(Candidate::getName)
                .containsExactly("First A", "First B", "Second");
    }

    private Event event(String title, Category category, String startAt, String endAt) {
        return Event.create(
                title,
                title + " description",
                category,
                OffsetDateTime.parse(startAt),
                OffsetDateTime.parse(endAt),
                CLOCK
        );
    }
}
