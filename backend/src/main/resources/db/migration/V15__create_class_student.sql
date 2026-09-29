CREATE TABLE class_student (
    student_user_account_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,

    CONSTRAINT pk_class_student
        PRIMARY KEY (student_user_account_id, class_id),

    CONSTRAINT fk_class_student_student
        FOREIGN KEY (student_user_account_id)
        REFERENCES user_account(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_class_student_class
        FOREIGN KEY (class_id)
        REFERENCES training_class(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_class_student_class
    ON class_student(class_id);
