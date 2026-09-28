package com.honeygroup.honeylms.enrollment;

public class AlreadyEnrolledException extends RuntimeException {

    public AlreadyEnrolledException(Long courseId) {
        super("You are already enrolled in course: " + courseId);
    }
}
