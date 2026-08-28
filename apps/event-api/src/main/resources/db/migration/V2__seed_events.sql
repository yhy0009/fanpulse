INSERT INTO events (id, title, description, category, start_at, end_at, created_at, updated_at)
VALUES (1, 'Championship Finals Vote', 'Choose the MVP of the championship finals.', 'GAME',
        '2026-01-10T00:00:00Z', '2026-02-10T00:00:00Z', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (2, 'Summer Premiere Showcase', 'Explore the featured summer movie premieres.', 'MOVIE',
        '2026-06-01T00:00:00Z', '2026-09-01T00:00:00Z', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (3, 'Animation Character Awards', 'Vote for the animation character awards.', 'ANIME',
        '2027-01-01T00:00:00Z', '2027-02-01T00:00:00Z', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (4, 'Global Music Fan Choice', 'Discover the global music fan choice event.', 'MUSIC',
        '2027-03-01T00:00:00Z', '2027-04-01T00:00:00Z', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

SELECT setval(pg_get_serial_sequence('events', 'id'), (SELECT MAX(id) FROM events), true);
