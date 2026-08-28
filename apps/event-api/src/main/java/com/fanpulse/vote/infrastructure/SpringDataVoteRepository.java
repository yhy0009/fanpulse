package com.fanpulse.vote.infrastructure;

import com.fanpulse.vote.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataVoteRepository extends JpaRepository<Vote, Long> {
}
