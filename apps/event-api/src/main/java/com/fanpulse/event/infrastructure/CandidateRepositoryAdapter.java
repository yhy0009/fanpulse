package com.fanpulse.event.infrastructure;

import com.fanpulse.event.domain.Candidate;
import com.fanpulse.event.domain.CandidateRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CandidateRepositoryAdapter implements CandidateRepository {

    private final SpringDataCandidateRepository repository;

    public CandidateRepositoryAdapter(SpringDataCandidateRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Candidate> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Candidate> findAllByEventIdOrderByDisplayOrderAscIdAsc(Long eventId) {
        return repository.findAllByEventIdOrderByDisplayOrderAscIdAsc(eventId);
    }
}
