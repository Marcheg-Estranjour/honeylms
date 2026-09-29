package com.honeygroup.honeylms.submission;

public class AlreadySubmittedException extends RuntimeException {

    public AlreadySubmittedException(Long assignmentId) {
        super("You already submitted assignment " + assignmentId + " - use PUT to replace it");
    }
}
