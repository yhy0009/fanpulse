package com.fanpulse.event.infrastructure;

import com.fanpulse.event.domain.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataCandidateRepository extends JpaRepository<Candidate, Long> {

    List<Candidate> findAllByEventIdOrderByDisplayOrderAscIdAsc(Long eventId);
}
