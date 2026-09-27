package com.honeygroup.honeylms.course;

public class CourseNotFoundException extends RuntimeException {

    public CourseNotFoundException(Long courseId) {
        super("No course found with id: " + courseId);
    }
}
