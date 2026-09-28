package com.honeygroup.honeylms.course;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourseModule_IdOrderByDisplayOrderAsc(Long moduleId);

    long countByCourseModule_Id(Long moduleId);
}
