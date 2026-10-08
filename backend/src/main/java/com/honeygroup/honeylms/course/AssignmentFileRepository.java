package com.honeygroup.honeylms.course;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentFileRepository extends JpaRepository<AssignmentFile, AssignmentFileId> {

    List<AssignmentFile> findByAssignment_Id(Long assignmentId);

    Optional<AssignmentFile> findByAssignment_IdAndStoredFile_Id(Long assignmentId, Long storedFileId);
}
