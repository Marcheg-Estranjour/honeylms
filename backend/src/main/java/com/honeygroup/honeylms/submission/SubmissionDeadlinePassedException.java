package com.honeygroup.honeylms.submission;

public class SubmissionDeadlinePassedException extends RuntimeException {

    public SubmissionDeadlinePassedException() {
        super("The deadline for this assignment has passed");
    }
}
