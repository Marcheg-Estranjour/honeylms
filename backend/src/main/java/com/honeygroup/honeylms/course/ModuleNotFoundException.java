package com.honeygroup.honeylms.course;

public class ModuleNotFoundException extends RuntimeException {

    public ModuleNotFoundException(Long moduleId) {
        super("No module found with id: " + moduleId);
    }
}
