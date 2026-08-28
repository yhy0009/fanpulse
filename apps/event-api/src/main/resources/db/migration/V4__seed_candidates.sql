INSERT INTO candidates (id, event_id, name, display_order, created_at)
VALUES (1, 1, 'Nova', 1, CURRENT_TIMESTAMP),
       (2, 1, 'Blaze', 2, CURRENT_TIMESTAMP),
       (3, 1, 'Echo', 3, CURRENT_TIMESTAMP),
       (4, 2, 'Moonlight Avenue', 1, CURRENT_TIMESTAMP),
       (5, 2, 'Last Summer Frame', 2, CURRENT_TIMESTAMP),
       (6, 2, 'Beyond the Screen', 3, CURRENT_TIMESTAMP),
       (7, 3, 'Ari', 1, CURRENT_TIMESTAMP),
       (8, 3, 'Ren', 2, CURRENT_TIMESTAMP),
       (9, 3, 'Mina', 3, CURRENT_TIMESTAMP),
       (10, 4, 'Aurora', 1, CURRENT_TIMESTAMP),
       (11, 4, 'Pulse Unit', 2, CURRENT_TIMESTAMP),
       (12, 4, 'Daybreak', 3, CURRENT_TIMESTAMP);

SELECT setval(pg_get_serial_sequence('candidates', 'id'), (SELECT MAX(id) FROM candidates), true);
