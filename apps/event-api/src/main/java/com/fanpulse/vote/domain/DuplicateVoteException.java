package com.fanpulse.vote.domain;

public class DuplicateVoteException extends RuntimeException {

    public DuplicateVoteException(Throwable cause) {
        super("Vote already exists", cause);
    }
}
