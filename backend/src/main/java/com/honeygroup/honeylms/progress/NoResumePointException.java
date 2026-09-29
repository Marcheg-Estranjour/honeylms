package com.honeygroup.honeylms.progress;

public class NoResumePointException extends RuntimeException {

    public NoResumePointException(Long courseId) {
        super("No lesson has been viewed yet in course: " + courseId);
    }
}
