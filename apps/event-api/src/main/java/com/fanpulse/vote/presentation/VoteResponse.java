package com.fanpulse.vote.presentation;

import com.fanpulse.vote.application.VoteResult;

import java.time.OffsetDateTime;

public record VoteResponse(
        Long id,
        Long eventId,
        Long candidateId,
        OffsetDateTime createdAt
) {
    static VoteResponse from(VoteResult result) {
        return new VoteResponse(
                result.id(),
                result.eventId(),
                result.candidateId(),
                result.createdAt()
        );
    }
}
