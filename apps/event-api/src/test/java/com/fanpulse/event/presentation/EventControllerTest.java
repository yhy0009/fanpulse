package com.fanpulse.event.presentation;

import com.fanpulse.common.config.TraceIdFilter;
import com.fanpulse.common.exception.GlobalExceptionHandler;
import com.fanpulse.event.application.EventService;
import com.fanpulse.event.domain.Category;
import com.fanpulse.event.domain.Event;
import com.fanpulse.event.domain.EventStatus;
import com.fanpulse.support.StubEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import({
        EventService.class,
        GlobalExceptionHandler.class,
        TraceIdFilter.class,
        EventControllerTest.TestConfig.class
})
class EventControllerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-01-15T00:00:00Z"),
            ZoneOffset.UTC
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StubEventRepository eventRepository;

    @BeforeEach
    void setUp() {
        eventRepository.clear();
    }

    @Test
    void returnsPaginatedAndFilteredEventsWithoutExposingEntityFields() throws Exception {
        eventRepository.setPage(new PageImpl<>(
                List.of(event(1L, Category.GAME)),
                PageRequest.of(1, 10),
                21
        ));

        mockMvc.perform(get("/api/v1/events")
                        .param("page", "1")
                        .param("size", "10")
                        .param("category", "GAME")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].category").value("GAME"))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.content[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.content[0].updatedAt").doesNotExist())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(21))
                .andExpect(jsonPath("$.totalPages").value(3));

        assertThat(eventRepository.getCategory()).isEqualTo(Category.GAME);
        assertThat(eventRepository.getStatus()).isEqualTo(EventStatus.OPEN);
    }

    @Test
    void usesDefaultPagination() throws Exception {
        eventRepository.setPage(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));

        assertThat(eventRepository.getPageable().getPageNumber()).isZero();
        assertThat(eventRepository.getPageable().getPageSize()).isEqualTo(20);
    }

    @Test
    void returnsEventDetail() throws Exception {
        eventRepository.put(1L, event(1L, Category.MOVIE));

        mockMvc.perform(get("/api/v1/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Description"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());
    }

    @Test
    void returnsCommonErrorResponseWhenEventDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/events/999").header("X-Trace-Id", "test-trace-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Event not found"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").value("test-trace-id"));
    }

    @Test
    void rejectsPageSizeOverOneHundred() throws Exception {
        mockMvc.perform(get("/api/v1/events").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private Event event(Long id, Category category) {
        Event event = Event.create(
                "Title",
                "Description",
                category,
                OffsetDateTime.parse("2026-01-10T00:00:00Z"),
                OffsetDateTime.parse("2026-02-10T00:00:00Z"),
                FIXED_CLOCK
        );
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        StubEventRepository eventRepository() {
            return new StubEventRepository();
        }

        @Bean
        Clock clock() {
            return FIXED_CLOCK;
        }
    }
}
