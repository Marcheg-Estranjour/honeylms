package com.honeygroup.honeylms.course;

public class AssignmentNotFoundException extends RuntimeException {

    public AssignmentNotFoundException(Long assignmentId) {
        super("No assignment found with id: " + assignmentId);
    }
}
