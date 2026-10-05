package com.honeygroup.honeylms.course;

public class TrainerAssignmentNotFoundException extends RuntimeException {

    public TrainerAssignmentNotFoundException(Long courseId, Long trainerId) {
        super("Trainer " + trainerId + " is not assigned to course " + courseId);
    }
}
