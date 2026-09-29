package com.honeygroup.honeylms.trainingclass;

import com.honeygroup.honeylms.user.UserAccount;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Supervision of a Class by a Trainer - distinct from CourseTrainer (which
 * grants content-editing rights on the whole Course). A Trainer must already
 * be a CourseTrainer of the Class's Course to be added here (validated rule,
 * Dossier de Conception §6 - intégrité CourseTrainer/ClassTrainer).
 */
@Entity
@Table(name = "class_trainer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassTrainer {

    @EmbeddedId
    private ClassTrainerId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("trainerUserAccountId")
    @JoinColumn(name = "trainer_user_account_id")
    private UserAccount trainer;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("classId")
    @JoinColumn(name = "class_id")
    private TrainingClass trainingClass;

    public static ClassTrainer of(TrainingClass trainingClass, UserAccount trainer) {
        ClassTrainer ct = new ClassTrainer();
        ct.setId(new ClassTrainerId(trainer.getId(), trainingClass.getId()));
        ct.setTrainer(trainer);
        ct.setTrainingClass(trainingClass);
        return ct;
    }
}
