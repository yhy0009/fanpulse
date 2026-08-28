package com.fanpulse.support;

import com.fanpulse.event.domain.Category;
import com.fanpulse.event.domain.Event;
import com.fanpulse.event.domain.EventRepository;
import com.fanpulse.event.domain.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class StubEventRepository implements EventRepository {

    private final Map<Long, Event> events = new HashMap<>();
    private Page<Event> page = Page.empty();
    private Category category;
    private EventStatus status;
    private Instant now;
    private Pageable pageable;

    @Override
    public Page<Event> search(Category category, EventStatus status, Instant now, Pageable pageable) {
        this.category = category;
        this.status = status;
        this.now = now;
        this.pageable = pageable;
        return page;
    }

    @Override
    public Optional<Event> findById(Long id) {
        return Optional.ofNullable(events.get(id));
    }

    public void setPage(Page<Event> page) {
        this.page = page;
    }

    public void put(Long id, Event event) {
        events.put(id, event);
    }

    public void clear() {
        events.clear();
        page = Page.empty();
        category = null;
        status = null;
        now = null;
        pageable = null;
    }

    public Category getCategory() {
        return category;
    }

    public EventStatus getStatus() {
        return status;
    }

    public Instant getNow() {
        return now;
    }

    public Pageable getPageable() {
        return pageable;
    }
}
