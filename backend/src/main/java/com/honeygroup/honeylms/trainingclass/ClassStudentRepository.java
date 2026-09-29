package com.honeygroup.honeylms.trainingclass;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassStudentRepository extends JpaRepository<ClassStudent, ClassStudentId> {

    boolean existsByTrainingClass_IdAndStudent_Id(Long classId, Long studentId);

    Optional<ClassStudent> findByTrainingClass_IdAndStudent_Id(Long classId, Long studentId);

    List<ClassStudent> findByTrainingClass_Id(Long classId);
}
