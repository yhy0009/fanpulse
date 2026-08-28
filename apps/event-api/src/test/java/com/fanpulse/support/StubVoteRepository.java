package com.fanpulse.support;

import com.fanpulse.vote.domain.DuplicateVoteException;
import com.fanpulse.vote.domain.Vote;
import com.fanpulse.vote.domain.VoteRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

public class StubVoteRepository implements VoteRepository {

    private final List<Vote> votes = new ArrayList<>();
    private boolean duplicate;
    private long nextId = 1L;

    @Override
    public Vote save(Vote vote) {
        if (duplicate) {
            throw new DuplicateVoteException(new IllegalStateException("duplicate"));
        }
        ReflectionTestUtils.setField(vote, "id", nextId++);
        votes.add(vote);
        return vote;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public List<Vote> values() {
        return List.copyOf(votes);
    }

    public void clear() {
        votes.clear();
        duplicate = false;
        nextId = 1L;
    }
}
