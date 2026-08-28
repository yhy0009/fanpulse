package com.fanpulse.vote.infrastructure;

import com.fanpulse.vote.domain.DuplicateVoteException;
import com.fanpulse.vote.domain.Vote;
import com.fanpulse.vote.domain.VoteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class VoteRepositoryAdapter implements VoteRepository {

    private static final String DUPLICATE_VOTE_CONSTRAINT = "uk_votes_event_user";

    private final SpringDataVoteRepository repository;

    public VoteRepositoryAdapter(SpringDataVoteRepository repository) {
        this.repository = repository;
    }

    @Override
    public Vote save(Vote vote) {
        try {
            return repository.saveAndFlush(vote);
        } catch (DataIntegrityViolationException exception) {
            if (causedByConstraint(exception, DUPLICATE_VOTE_CONSTRAINT)) {
                throw new DuplicateVoteException(exception);
            }
            throw exception;
        }
    }

    private boolean causedByConstraint(Throwable throwable, String constraintName) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException constraintViolation
                    && constraintName.equalsIgnoreCase(constraintViolation.getConstraintName())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
