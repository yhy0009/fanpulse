package com.fanpulse.event.domain;

import java.util.List;
import java.util.Optional;

public interface CandidateRepository {

    Optional<Candidate> findById(Long id);

    List<Candidate> findAllByEventIdOrderByDisplayOrderAscIdAsc(Long eventId);
}
