package com.honeygroup.honeylms.submission;

/** The Student has not submitted anything for this assignment yet (gap G2 -> 404). */
public class NoSubmissionYetException extends RuntimeException {

    public NoSubmissionYetException(Long assignmentId) {
        super("No submission yet for assignment: " + assignmentId);
    }
}
