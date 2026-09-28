package com.honeygroup.honeylms.course;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseModuleRepository extends JpaRepository<CourseModule, Long> {

    List<CourseModule> findByCourse_IdOrderByDisplayOrderAsc(Long courseId);

    long countByCourse_Id(Long courseId);
}
