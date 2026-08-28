package com.fanpulse.event.application;

import com.fanpulse.event.domain.Category;
import com.fanpulse.event.domain.Event;
import com.fanpulse.event.domain.EventStatus;
import com.fanpulse.support.StubEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private StubEventRepository eventRepository;
    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventRepository = new StubEventRepository();
        eventService = new EventService(eventRepository, FIXED_CLOCK);
    }

    @Test
    void searchesWithFiltersAndDefaultSort() {
        Event event = event(10L);
        eventRepository.setPage(new PageImpl<>(
                List.of(event),
                PageRequest.of(0, 20),
                1
        ));

        EventPageResult result = eventService.findEvents(0, 20, Category.GAME, EventStatus.OPEN);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().status()).isEqualTo(EventStatus.OPEN);
        assertThat(eventRepository.getCategory()).isEqualTo(Category.GAME);
        assertThat(eventRepository.getStatus()).isEqualTo(EventStatus.OPEN);
        assertThat(eventRepository.getNow()).isEqualTo(NOW);
        assertThat(eventRepository.getPageable().getPageNumber()).isZero();
        assertThat(eventRepository.getPageable().getPageSize()).isEqualTo(20);
        assertThat(eventRepository.getPageable().getSort().getOrderFor("startAt").isAscending()).isTrue();
        assertThat(eventRepository.getPageable().getSort().getOrderFor("id").isAscending()).isTrue();
    }

    @Test
    void returnsEventDetail() {
        eventRepository.put(10L, event(10L));

        EventDetailResult result = eventService.findEvent(10L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.description()).isEqualTo("Description");
        assertThat(result.status()).isEqualTo(EventStatus.OPEN);
    }

    @Test
    void throwsWhenEventDoesNotExist() {
        assertThatThrownBy(() -> eventService.findEvent(999L))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessage("Event not found");
    }

    private Event event(Long id) {
        Event event = Event.create(
                "Title",
                "Description",
                Category.GAME,
                OffsetDateTime.parse("2025-12-31T00:00:00Z"),
                OffsetDateTime.parse("2026-01-02T00:00:00Z"),
                FIXED_CLOCK
        );
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }
}
