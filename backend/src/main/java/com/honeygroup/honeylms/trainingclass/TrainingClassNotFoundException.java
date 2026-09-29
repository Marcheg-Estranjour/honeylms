package com.honeygroup.honeylms.trainingclass;

public class TrainingClassNotFoundException extends RuntimeException {

    public TrainingClassNotFoundException(Long classId) {
        super("No class found with id: " + classId);
    }
}
