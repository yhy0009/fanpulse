package com.fanpulse.event.infrastructure;

import com.fanpulse.event.domain.Category;
import com.fanpulse.event.domain.Event;
import com.fanpulse.event.domain.EventRepository;
import com.fanpulse.event.domain.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Repository
public class EventRepositoryAdapter implements EventRepository {

    private final SpringDataEventRepository repository;

    public EventRepositoryAdapter(SpringDataEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<Event> search(
            Category category,
            EventStatus status,
            Instant now,
            Pageable pageable
    ) {
        Specification<Event> specification = Specification.unrestricted();

        if (category != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("category"), category)
            );
        }
        if (status != null) {
            specification = specification.and(statusSpecification(status, now));
        }

        return repository.findAll(specification, pageable);
    }

    @Override
    public Optional<Event> findById(Long id) {
        return repository.findById(id);
    }

    private Specification<Event> statusSpecification(EventStatus status, Instant now) {
        OffsetDateTime nowUtc = OffsetDateTime.ofInstant(now, ZoneOffset.UTC);
        return switch (status) {
            case SCHEDULED -> (root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThan(root.get("startAt"), nowUtc);
            case OPEN -> (root, query, criteriaBuilder) -> criteriaBuilder.and(
                    criteriaBuilder.lessThanOrEqualTo(root.get("startAt"), nowUtc),
                    criteriaBuilder.greaterThan(root.get("endAt"), nowUtc)
            );
            case CLOSED -> (root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("endAt"), nowUtc);
        };
    }
}
