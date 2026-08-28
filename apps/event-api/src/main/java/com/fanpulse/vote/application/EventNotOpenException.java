package com.fanpulse.vote.application;

public class EventNotOpenException extends RuntimeException {

    private final Long eventId;

    public EventNotOpenException(Long eventId) {
        super("Event is not open");
        this.eventId = eventId;
    }

    public Long getEventId() {
        return eventId;
    }
}
