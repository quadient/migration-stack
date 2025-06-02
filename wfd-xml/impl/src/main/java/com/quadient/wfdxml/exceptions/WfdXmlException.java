package com.quadient.wfdxml.exceptions;

public class WfdXmlException extends RuntimeException {
    public WfdXmlException(String message) {
        super(message);
    }

    public WfdXmlException(Throwable cause) {
        super(cause);
    }

    public WfdXmlException(String message, Throwable cause) {
        super(message, cause);
    }
}
