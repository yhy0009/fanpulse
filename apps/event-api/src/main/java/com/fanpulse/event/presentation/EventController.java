package com.fanpulse.event.presentation;

import com.fanpulse.event.application.EventService;
import com.fanpulse.event.domain.Category;
import com.fanpulse.event.domain.EventStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public EventPageResponse findEvents(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) EventStatus status
    ) {
        return EventPageResponse.from(eventService.findEvents(page, size, category, status));
    }

    @GetMapping("/{id}")
    public EventDetailResponse findEvent(@PathVariable @Min(1) Long id) {
        return EventDetailResponse.from(eventService.findEvent(id));
    }
}
