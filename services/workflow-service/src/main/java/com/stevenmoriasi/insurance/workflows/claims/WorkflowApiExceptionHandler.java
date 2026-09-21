package com.stevenmoriasi.insurance.workflows.claims;

import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class WorkflowApiExceptionHandler {

    @ExceptionHandler(WorkflowExecutionAlreadyStarted.class)
    ProblemDetail alreadyStarted(WorkflowExecutionAlreadyStarted exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "A workflow already exists for this claim");
    }

    @ExceptionHandler(WorkflowNotFoundException.class)
    ProblemDetail notFound(WorkflowNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, "No workflow exists for this claim");
    }
}
