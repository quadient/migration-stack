package com.quadient.wfdxml.generator.connection.response;

public class QjResponse extends IpsResponse {
    private static final String ZERO_IPS_ERRORS = "E0";
    private static final String ZERO_IPS_WARNINGS = "W0";

    private static final int STATE_INDEX = 5;
    private static final int ERRORS_INDEX = 6;
    private static final int WARNINGS_INDEX = 7;

    public QjResponse(String qjMessageResponse) {
        super(qjMessageResponse);
    }

    public boolean hasErrors() {
        return !getErrors().equals(ZERO_IPS_ERRORS);
    }

    public String getState() {
        return responseFrames[STATE_INDEX];
    }

    public String getErrors() {
        return responseFrames[ERRORS_INDEX];
    }

    public String getWarnings() {
        return responseFrames[WARNINGS_INDEX];
    }
}