package com.fanpulse.event.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;

public interface EventRepository {

    Page<Event> search(
            Category category,
            EventStatus status,
            Instant now,
            Pageable pageable
    );

    Optional<Event> findById(Long id);
}
