package com.honeygroup.honeylms.course;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByStatus(PublicationStatus status);

    List<Course> findByStatusAndCategory(PublicationStatus status, CourseCategory category);
}
