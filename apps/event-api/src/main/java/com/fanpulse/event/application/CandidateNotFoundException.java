package com.fanpulse.event.application;

public class CandidateNotFoundException extends RuntimeException {

    private final Long candidateId;

    public CandidateNotFoundException(Long candidateId) {
        super("Candidate not found");
        this.candidateId = candidateId;
    }

    public Long getCandidateId() {
        return candidateId;
    }
}
