package com.honeygroup.honeylms.submission;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Optional<Submission> findByAssignment_IdAndStudent_Id(Long assignmentId, Long studentId);

    List<Submission> findByAssignment_IdOrderBySubmittedAtDesc(Long assignmentId);
}
