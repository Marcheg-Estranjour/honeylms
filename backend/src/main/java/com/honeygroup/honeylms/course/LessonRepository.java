package com.honeygroup.honeylms.course;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourseModule_IdOrderByDisplayOrderAsc(Long moduleId);

    long countByCourseModule_Id(Long moduleId);

    /**
     * "Accessible" here means PUBLISHED lesson in a PUBLISHED module of the given
     * Course - the same chain LearningAccessService checks, minus Enrollment
     * (verified once by the caller, not per lesson). Used for progress calculation.
     */
    @Query("""
            SELECT l FROM Lesson l
            WHERE l.courseModule.course.id = :courseId
              AND l.status = com.honeygroup.honeylms.course.PublicationStatus.PUBLISHED
              AND l.courseModule.status = com.honeygroup.honeylms.course.PublicationStatus.PUBLISHED
            """)
    List<Lesson> findAccessibleLessonsByCourse(@Param("courseId") Long courseId);

    @Query("""
            SELECT l FROM Lesson l
            WHERE l.courseModule.id = :moduleId
              AND l.status = com.honeygroup.honeylms.course.PublicationStatus.PUBLISHED
              AND l.courseModule.status = com.honeygroup.honeylms.course.PublicationStatus.PUBLISHED
            """)
    List<Lesson> findAccessibleLessonsByModule(@Param("moduleId") Long moduleId);
}
