CREATE TABLE enrollment (
    student_user_account_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_enrollment
        PRIMARY KEY (student_user_account_id, course_id),

    CONSTRAINT fk_enrollment_student
        FOREIGN KEY (student_user_account_id)
        REFERENCES user_account(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_enrollment_course
        FOREIGN KEY (course_id)
        REFERENCES course(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_enrollment_course
    ON enrollment(course_id);
