package com.honeygroup.honeylms.trainingclass;

/**
 * Thrown when a user cannot be assigned to a Class because they don't meet a
 * structural requirement - wrong role, or (for a Trainer) not already a
 * CourseTrainer of the Class's Course. Distinct from ForbiddenActionException:
 * this is about the target user's eligibility, not the requester's permission.
 */
public class InvalidClassAssignmentException extends RuntimeException {

    public InvalidClassAssignmentException(String message) {
        super(message);
    }
}
