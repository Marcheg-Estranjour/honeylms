package com.honeygroup.honeylms.progress;

import com.honeygroup.honeylms.course.Lesson;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonCompletionRepository extends JpaRepository<LessonCompletion, LessonCompletionId> {

    Optional<LessonCompletion> findByStudent_IdAndLesson_Id(Long studentId, Long lessonId);

    long countByStudent_IdAndLesson_InAndCompletedAtIsNotNull(Long studentId, List<Lesson> lessons);

    List<LessonCompletion> findByStudent_IdAndLesson_CourseModule_Course_IdOrderByLastViewedAtDesc(
            Long studentId, Long courseId, Pageable pageable);
}
