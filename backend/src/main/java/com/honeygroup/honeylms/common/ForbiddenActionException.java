package com.honeygroup.honeylms.common;

/**
 * Thrown whenever an authenticated user is not allowed to perform an action
 * on a specific resource because it is outside their perimeter (e.g. a Trainer
 * trying to modify a Course they are not assigned to). Reusable across modules -
 * every future ownership/perimeter check (Module, Lesson, Assignment...) should
 * throw this rather than inventing a new exception per module.
 */
public class ForbiddenActionException extends RuntimeException {

    public ForbiddenActionException(String message) {
        super(message);
    }
}
