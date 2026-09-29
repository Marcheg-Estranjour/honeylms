CREATE TABLE lesson_completion (
    student_user_account_id BIGINT NOT NULL,
    lesson_id BIGINT NOT NULL,
    completed_at TIMESTAMPTZ,
    last_viewed_at TIMESTAMPTZ,

    CONSTRAINT pk_lesson_completion
        PRIMARY KEY (student_user_account_id, lesson_id),

    CONSTRAINT fk_lesson_completion_student
        FOREIGN KEY (student_user_account_id)
        REFERENCES user_account(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_lesson_completion_lesson
        FOREIGN KEY (lesson_id)
        REFERENCES lesson(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_lesson_completion_lesson
    ON lesson_completion(lesson_id);
