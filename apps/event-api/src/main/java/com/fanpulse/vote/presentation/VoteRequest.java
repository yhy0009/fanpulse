package com.fanpulse.vote.presentation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record VoteRequest(
        @NotNull @Positive Long candidateId
) {
}
