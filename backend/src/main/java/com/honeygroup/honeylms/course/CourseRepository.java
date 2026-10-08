package com.honeygroup.honeylms.course;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByStatus(PublicationStatus status);

    List<Course> findByStatusAndCategory(PublicationStatus status, CourseCategory category);

    /** Every course, DRAFT included (Admin back-office, gap G5). */
    List<Course> findAllByOrderByTitleAsc();

    /** Courses a Trainer is assigned to via CourseTrainer, DRAFT included (gap G5). */
    @Query("select ct.course from CourseTrainer ct where ct.trainer.id = :trainerId order by ct.course.title")
    List<Course> findManagedByTrainer(@Param("trainerId") Long trainerId);
}
