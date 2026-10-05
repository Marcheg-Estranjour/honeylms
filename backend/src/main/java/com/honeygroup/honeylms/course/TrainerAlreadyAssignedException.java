package com.honeygroup.honeylms.course;

public class TrainerAlreadyAssignedException extends RuntimeException {

    public TrainerAlreadyAssignedException(Long courseId, Long trainerId) {
        super("Trainer " + trainerId + " is already assigned to course " + courseId);
    }
}
