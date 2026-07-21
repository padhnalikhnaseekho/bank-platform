package com.bankplatform.common.error;

public class ValidationException extends DomainException {

    public ValidationException(String message) {
        super("VALIDATION_FAILED", message);
    }
}
