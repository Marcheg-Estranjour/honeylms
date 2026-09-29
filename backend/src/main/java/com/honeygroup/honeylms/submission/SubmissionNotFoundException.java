package com.honeygroup.honeylms.submission;

public class SubmissionNotFoundException extends RuntimeException {

    public SubmissionNotFoundException(Long submissionId) {
        super("No submission found with id: " + submissionId);
    }
}
