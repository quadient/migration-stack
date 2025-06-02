package com.quadient.wfdxml.exceptions;

public class IpsConnectionException extends RuntimeException {
    public IpsConnectionException(String message) {
        super(message);
    }

    public IpsConnectionException(Throwable cause) {
        super(cause);
    }

    public IpsConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
