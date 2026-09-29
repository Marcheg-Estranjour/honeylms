package com.honeygroup.honeylms.course;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(Long resourceId) {
        super("No resource found with id: " + resourceId);
    }
}
