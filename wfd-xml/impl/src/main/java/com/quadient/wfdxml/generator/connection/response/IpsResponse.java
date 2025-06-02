package com.quadient.wfdxml.generator.connection.response;

public abstract class IpsResponse {

    public static final String SPLIT_FRAME_CHAR = ";";

    protected String[] responseFrames;

    IpsResponse(String responseMessage) {
        responseFrames = responseMessage.split(SPLIT_FRAME_CHAR);
    }
}