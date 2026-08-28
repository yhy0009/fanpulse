package com.fanpulse.vote.application;

import com.fanpulse.event.application.CandidateNotFoundException;
import com.fanpulse.event.application.EventNotFoundException;
import com.fanpulse.event.domain.CandidateRepository;
import com.fanpulse.event.domain.EventRepository;
import com.fanpulse.event.domain.EventStatus;
import com.fanpulse.vote.domain.Vote;
import com.fanpulse.vote.domain.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZoneOffset;

@Service
public class VoteService {

    private final EventRepository eventRepository;
    private final CandidateRepository candidateRepository;
    private final VoteRepository voteRepository;
    private final Clock clock;

    public VoteService(
            EventRepository eventRepository,
            CandidateRepository candidateRepository,
            VoteRepository voteRepository,
            Clock clock
    ) {
        this.eventRepository = eventRepository;
        this.candidateRepository = candidateRepository;
        this.voteRepository = voteRepository;
        this.clock = clock;
    }

    @Transactional
    public VoteResult castVote(Long eventId, Long userId, Long candidateId) {
        var operationClock = Clock.fixed(clock.instant(), ZoneOffset.UTC);
        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        if (event.status(operationClock) != EventStatus.OPEN) {
            throw new EventNotOpenException(eventId);
        }

        var candidate = candidateRepository.findById(candidateId)
                .filter(found -> found.getEventId().equals(eventId))
                .orElseThrow(() -> new CandidateNotFoundException(candidateId));

        var vote = Vote.create(eventId, userId, candidate.getId(), operationClock);
        return VoteResult.from(voteRepository.save(vote));
    }
}
