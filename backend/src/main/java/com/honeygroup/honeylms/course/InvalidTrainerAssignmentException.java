package com.honeygroup.honeylms.course;

/**
 * The target user cannot be assigned to a Course (e.g. not a TRAINER).
 * About the target's eligibility, not the requester's permission -> 422.
 */
public class InvalidTrainerAssignmentException extends RuntimeException {

    public InvalidTrainerAssignmentException(String message) {
        super(message);
    }
}
