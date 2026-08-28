package com.fanpulse.vote.domain;

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
@Table(name = "votes")
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    protected Vote() {
    }

    private Vote(Long eventId, Long userId, Long candidateId, OffsetDateTime createdAt) {
        requirePositive(eventId, "eventId");
        requirePositive(userId, "userId");
        requirePositive(candidateId, "candidateId");
        this.eventId = eventId;
        this.userId = userId;
        this.candidateId = candidateId;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static Vote create(Long eventId, Long userId, Long candidateId, Clock clock) {
        return new Vote(
                eventId,
                userId,
                candidateId,
                OffsetDateTime.now(Objects.requireNonNull(clock, "clock must not be null"))
        );
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getEventId() {
        return eventId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
