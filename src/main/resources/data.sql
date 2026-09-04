INSERT INTO rate_limit_config (id, path_pattern, algorithm, max_requests, window_seconds, enabled) VALUES (1, '/api/**', 'TOKEN_BUCKET', 10, 60, true);
INSERT INTO rate_limit_config (id, path_pattern, algorithm, max_requests, window_seconds, enabled) VALUES (2, '/api/premium/**', 'SLIDING_WINDOW', 100, 60, true);
