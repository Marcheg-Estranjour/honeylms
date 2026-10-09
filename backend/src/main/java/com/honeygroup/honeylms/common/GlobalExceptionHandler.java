package com.honeygroup.honeylms.common;

import com.honeygroup.honeylms.course.AssignmentFileNotFoundException;
import com.honeygroup.honeylms.course.AssignmentNotFoundException;
import com.honeygroup.honeylms.course.CourseNotFoundException;
import com.honeygroup.honeylms.course.InvalidTrainerAssignmentException;
import com.honeygroup.honeylms.course.LessonNotFoundException;
import com.honeygroup.honeylms.course.ModuleNotFoundException;
import com.honeygroup.honeylms.course.ResourceNotFoundException;
import com.honeygroup.honeylms.course.TrainerAlreadyAssignedException;
import com.honeygroup.honeylms.course.TrainerAssignmentNotFoundException;
import com.honeygroup.honeylms.enrollment.AlreadyEnrolledException;
import com.honeygroup.honeylms.file.InvalidFileException;
import com.honeygroup.honeylms.progress.NoResumePointException;
import com.honeygroup.honeylms.submission.AlreadySubmittedException;
import com.honeygroup.honeylms.submission.NoSubmissionYetException;
import com.honeygroup.honeylms.submission.SubmissionDeadlinePassedException;
import com.honeygroup.honeylms.submission.SubmissionNotFoundException;
import com.honeygroup.honeylms.trainingclass.AlreadyClassMemberException;
import com.honeygroup.honeylms.trainingclass.ClassMembershipNotFoundException;
import com.honeygroup.honeylms.trainingclass.InvalidClassAssignmentException;
import com.honeygroup.honeylms.trainingclass.TrainingClassNotFoundException;
import com.honeygroup.honeylms.user.AccountDisabledException;
import com.honeygroup.honeylms.user.EmailAlreadyExistsException;
import com.honeygroup.honeylms.user.InvalidCredentialsException;
import com.honeygroup.honeylms.user.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Centralizes exception -> HTTP response mapping so controllers stay free
 * of try/catch and every error follows the same ApiError envelope.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyExists(EmailAlreadyExistsException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ApiError> handleAccountDisabled(AccountDisabledException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException ex,
                                                         HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<ApiError> handleCourseNotFound(CourseNotFoundException ex,
                                                           HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ModuleNotFoundException.class)
    public ResponseEntity<ApiError> handleModuleNotFound(ModuleNotFoundException ex,
                                                           HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(LessonNotFoundException.class)
    public ResponseEntity<ApiError> handleLessonNotFound(LessonNotFoundException ex,
                                                           HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex,
                                                             HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(AssignmentNotFoundException.class)
    public ResponseEntity<ApiError> handleAssignmentNotFound(AssignmentNotFoundException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(AlreadyEnrolledException.class)
    public ResponseEntity<ApiError> handleAlreadyEnrolled(AlreadyEnrolledException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenActionException.class)
    public ResponseEntity<ApiError> handleForbiddenAction(ForbiddenActionException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(SubmissionNotFoundException.class)
    public ResponseEntity<ApiError> handleSubmissionNotFound(SubmissionNotFoundException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(NoSubmissionYetException.class)
    public ResponseEntity<ApiError> handleNoSubmissionYet(NoSubmissionYetException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(AssignmentFileNotFoundException.class)
    public ResponseEntity<ApiError> handleAssignmentFileNotFound(AssignmentFileNotFoundException ex,
                                                                   HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(AlreadySubmittedException.class)
    public ResponseEntity<ApiError> handleAlreadySubmitted(AlreadySubmittedException ex,
                                                             HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(SubmissionDeadlinePassedException.class)
    public ResponseEntity<ApiError> handleDeadlinePassed(SubmissionDeadlinePassedException ex,
                                                           HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(NoResumePointException.class)
    public ResponseEntity<ApiError> handleNoResumePoint(NoResumePointException ex,
                                                          HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(TrainingClassNotFoundException.class)
    public ResponseEntity<ApiError> handleTrainingClassNotFound(TrainingClassNotFoundException ex,
                                                                  HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ClassMembershipNotFoundException.class)
    public ResponseEntity<ApiError> handleClassMembershipNotFound(ClassMembershipNotFoundException ex,
                                                                    HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(AlreadyClassMemberException.class)
    public ResponseEntity<ApiError> handleAlreadyClassMember(AlreadyClassMemberException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidClassAssignmentException.class)
    public ResponseEntity<ApiError> handleInvalidClassAssignment(InvalidClassAssignmentException ex,
                                                                   HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    @ExceptionHandler(TrainerAlreadyAssignedException.class)
    public ResponseEntity<ApiError> handleTrainerAlreadyAssigned(TrainerAlreadyAssignedException ex,
                                                                   HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(TrainerAssignmentNotFoundException.class)
    public ResponseEntity<ApiError> handleTrainerAssignmentNotFound(TrainerAssignmentNotFoundException ex,
                                                                      HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidTrainerAssignmentException.class)
    public ResponseEntity<ApiError> handleInvalidTrainerAssignment(InvalidTrainerAssignmentException ex,
                                                                     HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ApiError> handleInvalidFile(InvalidFileException ex,
                                                        HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex,
                                                                  HttpServletRequest request) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file is too large", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                         HttpServletRequest request) {
        String message = "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue();
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                       HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Unmapped URL. Without this handler Spring forwards to /error, which used to answer an
     * empty 403 (the error dispatch was not permitted by SecurityConfig). Now: a real 404 ApiError.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND,
                "No endpoint " + request.getMethod() + " " + request.getRequestURI(), request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request) {
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
