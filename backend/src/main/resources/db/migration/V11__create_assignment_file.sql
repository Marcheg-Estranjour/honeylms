CREATE TABLE assignment_file (
    assignment_id BIGINT NOT NULL,
    stored_file_id BIGINT NOT NULL,

    CONSTRAINT pk_assignment_file
        PRIMARY KEY (assignment_id, stored_file_id),

    CONSTRAINT fk_assignment_file_assignment
        FOREIGN KEY (assignment_id)
        REFERENCES assignment(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_assignment_file_file
        FOREIGN KEY (stored_file_id)
        REFERENCES stored_file(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_assignment_file_file
    ON assignment_file(stored_file_id);
