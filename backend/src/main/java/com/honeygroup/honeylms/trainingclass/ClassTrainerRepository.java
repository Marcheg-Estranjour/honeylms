package com.honeygroup.honeylms.trainingclass;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassTrainerRepository extends JpaRepository<ClassTrainer, ClassTrainerId> {

    boolean existsByTrainingClass_IdAndTrainer_Id(Long classId, Long trainerId);

    Optional<ClassTrainer> findByTrainingClass_IdAndTrainer_Id(Long classId, Long trainerId);

    List<ClassTrainer> findByTrainingClass_Id(Long classId);
}
