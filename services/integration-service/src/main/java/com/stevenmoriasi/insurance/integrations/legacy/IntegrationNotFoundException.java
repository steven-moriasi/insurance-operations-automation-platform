package com.stevenmoriasi.insurance.integrations.legacy;

public class IntegrationNotFoundException extends RuntimeException {

    public IntegrationNotFoundException(String message) {
        super(message);
    }
}
