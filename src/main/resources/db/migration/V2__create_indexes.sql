CREATE INDEX idx_submissions_user    ON submissions(user_id);
CREATE INDEX idx_submissions_problem ON submissions(problem_id);
CREATE INDEX idx_submissions_status  ON submissions(status);
CREATE INDEX idx_outbox_unpublished  ON outbox_events(published) WHERE published = false;
CREATE INDEX idx_problems_slug       ON problems(slug);
CREATE INDEX idx_problems_difficulty ON problems(difficulty);