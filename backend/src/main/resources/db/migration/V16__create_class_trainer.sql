CREATE TABLE class_trainer (
    trainer_user_account_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,

    CONSTRAINT pk_class_trainer
        PRIMARY KEY (trainer_user_account_id, class_id),

    CONSTRAINT fk_class_trainer_trainer
        FOREIGN KEY (trainer_user_account_id)
        REFERENCES user_account(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_class_trainer_class
        FOREIGN KEY (class_id)
        REFERENCES training_class(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_class_trainer_class
    ON class_trainer(class_id);
