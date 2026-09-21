package com.stevenmoriasi.insurance.cases.domain;

public class CaseOperationException extends RuntimeException {

    private final Reason reason;

    public CaseOperationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        NOT_FOUND,
        FORBIDDEN,
        CONFLICT,
        INVALID_REQUEST,
        AUTHORITY_EXCEEDED
    }
}
