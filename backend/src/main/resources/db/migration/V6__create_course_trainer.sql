CREATE TABLE course_trainer (
    course_id BIGINT NOT NULL,
    trainer_user_account_id BIGINT NOT NULL,

    CONSTRAINT pk_course_trainer
        PRIMARY KEY (course_id, trainer_user_account_id),

    CONSTRAINT fk_course_trainer_course
        FOREIGN KEY (course_id)
        REFERENCES course(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_course_trainer_user
        FOREIGN KEY (trainer_user_account_id)
        REFERENCES user_account(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_course_trainer_trainer
    ON course_trainer(trainer_user_account_id);
