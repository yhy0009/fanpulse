package com.fanpulse.vote;

import com.fanpulse.support.PostgresIntegrationTestSupport;
import com.fanpulse.vote.application.VoteService;
import com.fanpulse.vote.domain.DuplicateVoteException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class VoteConcurrencyIntegrationTest extends PostgresIntegrationTestSupport {

    private static final int CONCURRENT_REQUESTS = 12;

    @Autowired
    private VoteService voteService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE votes, candidates, events RESTART IDENTITY CASCADE");
        jdbcTemplate.update("""
                INSERT INTO events (
                    id, title, description, category, start_at, end_at, created_at, updated_at
                ) VALUES (
                    100, 'Concurrent Vote', 'Concurrency test event', 'GAME',
                    CURRENT_TIMESTAMP - INTERVAL '1 hour',
                    CURRENT_TIMESTAMP + INTERVAL '1 hour',
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO candidates (id, event_id, name, display_order, created_at)
                VALUES (101, 100, 'Candidate', 1, CURRENT_TIMESTAMP)
                """);
        executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void storesOnlyOneVoteWhenSameUserVotesConcurrently() throws Exception {
        CountDownLatch ready = new CountDownLatch(CONCURRENT_REQUESTS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int request = 0; request < CONCURRENT_REQUESTS; request++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out while waiting to start");
                }
                try {
                    voteService.castVote(100L, 1001L, 101L);
                    return true;
                } catch (DuplicateVoteException exception) {
                    return false;
                }
            }));
        }

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        long successfulRequests = 0;
        for (Future<Boolean> future : futures) {
            if (future.get(20, TimeUnit.SECONDS)) {
                successfulRequests++;
            }
        }

        Integer storedVotes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM votes WHERE event_id = 100 AND user_id = 1001",
                Integer.class
        );
        assertThat(successfulRequests).isOne();
        assertThat(storedVotes).isOne();
    }
}
