package com.honeygroup.honeylms.course;

/** The requested file is not attached to this assignment (gap G4 -> 404, also blocks IDOR). */
public class AssignmentFileNotFoundException extends RuntimeException {

    public AssignmentFileNotFoundException(Long assignmentId, Long storedFileId) {
        super("File " + storedFileId + " is not attached to assignment " + assignmentId);
    }
}
