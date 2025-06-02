package com.quadient.wfdxml.generator.connection.response;

public class ResultResponse extends IpsResponse {

    private static final String RESPONSE_OK = "ok";

    private static final int STATE_INDEX = 0;
    private static final int JOB_ID_INDEX = 1;

    public ResultResponse(String xml2wfdResponseMsg) {
        super(xml2wfdResponseMsg);
    }

    public boolean isResultStateOk() {
        return getResultState().equals(RESPONSE_OK);
    }

    public String getResultState() {
        return responseFrames[STATE_INDEX];
    }

    public String getJobId() {
        return responseFrames[JOB_ID_INDEX];
    }
}
