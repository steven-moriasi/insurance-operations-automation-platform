package com.stevenmoriasi.insurance.integrations.automation;

public class InvalidWorkLeaseException extends RuntimeException {

    public InvalidWorkLeaseException() {
        super("Automation work lease is invalid or expired");
    }
}
