package com.fanpulse.event.infrastructure;

import com.fanpulse.event.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface SpringDataEventRepository extends
        JpaRepository<Event, Long>,
        JpaSpecificationExecutor<Event> {
}
