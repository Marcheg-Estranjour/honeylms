package com.honeygroup.honeylms.course;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByLesson_IdOrderByCreatedAtAsc(Long lessonId);
}
