package com.honeygroup.honeylms.course;

public class LessonNotFoundException extends RuntimeException {

    public LessonNotFoundException(Long lessonId) {
        super("No lesson found with id: " + lessonId);
    }
}
