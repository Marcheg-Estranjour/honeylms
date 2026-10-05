package com.honeygroup.honeylms.course;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseTrainerRepository extends JpaRepository<CourseTrainer, CourseTrainerId> {

    boolean existsByCourse_IdAndTrainer_Id(Long courseId, Long trainerId);

    List<CourseTrainer> findByCourse_Id(Long courseId);
}
