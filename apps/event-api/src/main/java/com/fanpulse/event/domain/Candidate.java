package com.fanpulse.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    protected Candidate() {
    }

    private Candidate(Long eventId, String name, int displayOrder, OffsetDateTime createdAt) {
        if (eventId == null || eventId <= 0) {
            throw new IllegalArgumentException("eventId must be positive");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (displayOrder <= 0) {
            throw new IllegalArgumentException("displayOrder must be positive");
        }
        this.eventId = eventId;
        this.name = name.strip();
        this.displayOrder = displayOrder;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static Candidate create(Long eventId, String name, int displayOrder, Clock clock) {
        return new Candidate(
                eventId,
                name,
                displayOrder,
                OffsetDateTime.now(Objects.requireNonNull(clock, "clock must not be null"))
        );
    }

    public Long getId() {
        return id;
    }

    public Long getEventId() {
        return eventId;
    }

    public String getName() {
        return name;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
