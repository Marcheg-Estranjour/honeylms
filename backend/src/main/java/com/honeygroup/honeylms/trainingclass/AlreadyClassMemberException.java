package com.honeygroup.honeylms.trainingclass;

public class AlreadyClassMemberException extends RuntimeException {

    public AlreadyClassMemberException(Long classId, Long userId) {
        super("User " + userId + " is already a member of class " + classId);
    }
}
