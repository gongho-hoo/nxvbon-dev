CREATE TABLE IF NOT EXISTS code_change_proposals (
    id VARCHAR(80) PRIMARY KEY,
    project_id VARCHAR(80) NOT NULL,
    session_id VARCHAR(120),
    target_file_path TEXT NOT NULL,
    instruction TEXT NOT NULL,
    original_code TEXT NOT NULL,
    proposed_code TEXT NOT NULL,
    diff_text TEXT,
    original_hash VARCHAR(128) NOT NULL,
    proposed_hash VARCHAR(128) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    applied_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_code_change_proposals_project_file
ON code_change_proposals(project_id, target_file_path);

CREATE TABLE IF NOT EXISTS code_change_histories (
    id VARCHAR(80) PRIMARY KEY,
    project_id VARCHAR(80) NOT NULL,
    proposal_id VARCHAR(80),
    target_file_path TEXT NOT NULL,
    instruction TEXT,
    before_code TEXT NOT NULL,
    after_code TEXT NOT NULL,
    before_hash VARCHAR(128) NOT NULL,
    after_hash VARCHAR(128) NOT NULL,
    action_type VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_code_change_histories_project_file_created
ON code_change_histories(project_id, target_file_path, created_at DESC);
