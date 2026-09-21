package com.stevenmoriasi.insurance.cases.api;

import com.stevenmoriasi.insurance.cases.domain.CaseOperationException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CaseExceptionHandler {

    @ExceptionHandler(CaseOperationException.class)
    ProblemDetail handleCaseOperation(CaseOperationException exception) {
        HttpStatus status =
                switch (exception.getReason()) {
                    case NOT_FOUND -> HttpStatus.NOT_FOUND;
                    case FORBIDDEN, AUTHORITY_EXCEEDED -> HttpStatus.FORBIDDEN;
                    case CONFLICT -> HttpStatus.CONFLICT;
                    case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
                };
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        detail.setType(URI.create("urn:insurance:case-error:" + exception.getReason().name()));
        detail.setTitle(exception.getReason().name().replace('_', ' '));
        return detail;
    }
}
