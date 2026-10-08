package com.honeygroup.honeylms.course;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByLesson_IdOrderByCreatedAtAsc(Long lessonId);

    /** All assignments (DRAFT included) of the given courses, with lesson, module and course fetched. */
    @EntityGraph(attributePaths = {"lesson", "lesson.courseModule", "lesson.courseModule.course"})
    List<Assignment> findByLesson_CourseModule_Course_IdIn(Collection<Long> courseIds);
}
