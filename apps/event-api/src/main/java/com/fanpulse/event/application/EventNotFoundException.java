package com.fanpulse.event.application;

public class EventNotFoundException extends RuntimeException {

    private final Long eventId;

    public EventNotFoundException(Long eventId) {
        super("Event not found");
        this.eventId = eventId;
    }

    public Long getEventId() {
        return eventId;
    }
}
