package com.honeygroup.honeylms.course;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findByLesson_IdOrderByDisplayOrderAsc(Long lessonId);

    long countByLesson_Id(Long lessonId);
}
