package com.honeygroup.honeylms.course;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseTrainerRepository extends JpaRepository<CourseTrainer, CourseTrainerId> {

    boolean existsByCourse_IdAndTrainer_Id(Long courseId, Long trainerId);
}
