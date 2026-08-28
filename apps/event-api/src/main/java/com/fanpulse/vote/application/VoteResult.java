package com.fanpulse.vote.application;

import com.fanpulse.vote.domain.Vote;

import java.time.OffsetDateTime;

public record VoteResult(
        Long id,
        Long eventId,
        Long candidateId,
        OffsetDateTime createdAt
) {
    static VoteResult from(Vote vote) {
        return new VoteResult(
                vote.getId(),
                vote.getEventId(),
                vote.getCandidateId(),
                vote.getCreatedAt()
        );
    }
}
