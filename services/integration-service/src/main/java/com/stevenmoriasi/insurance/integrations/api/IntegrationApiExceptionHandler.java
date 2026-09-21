package com.stevenmoriasi.insurance.integrations.api;

import com.stevenmoriasi.insurance.integrations.automation.InvalidWorkLeaseException;
import com.stevenmoriasi.insurance.integrations.legacy.IntegrationConflictException;
import com.stevenmoriasi.insurance.integrations.legacy.IntegrationNotFoundException;
import com.stevenmoriasi.insurance.integrations.payments.InvalidCallbackSignatureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class IntegrationApiExceptionHandler {

    @ExceptionHandler(IntegrationConflictException.class)
    ProblemDetail conflict(IntegrationConflictException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(IntegrationNotFoundException.class)
    ProblemDetail notFound(IntegrationNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(InvalidCallbackSignatureException.class)
    ProblemDetail invalidSignature(InvalidCallbackSignatureException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(InvalidWorkLeaseException.class)
    ProblemDetail invalidLease(InvalidWorkLeaseException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }
}
