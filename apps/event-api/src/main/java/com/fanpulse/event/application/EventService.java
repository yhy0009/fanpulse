package com.fanpulse.event.application;

import com.fanpulse.event.domain.Category;
import com.fanpulse.event.domain.CandidateRepository;
import com.fanpulse.event.domain.EventRepository;
import com.fanpulse.event.domain.EventStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZoneOffset;

@Service
@Transactional(readOnly = true)
public class EventService {

    private static final Sort DEFAULT_SORT = Sort.by(
            Sort.Order.asc("startAt"),
            Sort.Order.asc("id")
    );

    private final EventRepository eventRepository;
    private final CandidateRepository candidateRepository;
    private final Clock clock;

    public EventService(
            EventRepository eventRepository,
            CandidateRepository candidateRepository,
            Clock clock
    ) {
        this.eventRepository = eventRepository;
        this.candidateRepository = candidateRepository;
        this.clock = clock;
    }

    public EventPageResult findEvents(
            int page,
            int size,
            Category category,
            EventStatus status
    ) {
        var queryTime = clock.instant();
        var queryClock = Clock.fixed(queryTime, ZoneOffset.UTC);
        var events = eventRepository.search(
                category,
                status,
                queryTime,
                PageRequest.of(page, size, DEFAULT_SORT)
        );

        var content = events.getContent().stream()
                .map(event -> EventSummaryResult.from(event, queryClock))
                .toList();

        return new EventPageResult(
                content,
                events.getNumber(),
                events.getSize(),
                events.getTotalElements(),
                events.getTotalPages()
        );
    }

    public EventDetailResult findEvent(Long id) {
        var event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
        var candidates = candidateRepository.findAllByEventIdOrderByDisplayOrderAscIdAsc(id).stream()
                .map(CandidateSummaryResult::from)
                .toList();
        return EventDetailResult.from(event, clock, candidates);
    }
}
