package com.honeygroup.honeylms.trainingclass;

public class ClassMembershipNotFoundException extends RuntimeException {

    public ClassMembershipNotFoundException(Long classId, Long userId) {
        super("User " + userId + " is not a member of class " + classId);
    }
}
