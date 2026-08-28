package com.fanpulse.support;

import com.fanpulse.event.domain.Candidate;
import com.fanpulse.event.domain.CandidateRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StubCandidateRepository implements CandidateRepository {

    private final Map<Long, Candidate> candidates = new HashMap<>();

    @Override
    public Optional<Candidate> findById(Long id) {
        return Optional.ofNullable(candidates.get(id));
    }

    @Override
    public List<Candidate> findAllByEventIdOrderByDisplayOrderAscIdAsc(Long eventId) {
        return candidates.values().stream()
                .filter(candidate -> candidate.getEventId().equals(eventId))
                .sorted(Comparator.comparingInt(Candidate::getDisplayOrder)
                        .thenComparing(Candidate::getId))
                .toList();
    }

    public void put(Long id, Candidate candidate) {
        candidates.put(id, candidate);
    }

    public List<Candidate> values() {
        return new ArrayList<>(candidates.values());
    }

    public void clear() {
        candidates.clear();
    }
}
