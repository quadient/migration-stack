package com.quadient.wfdxml.generator.connection;

import com.quadient.wfdxml.generator.connection.response.IpsResponse;

public class IpsClient {

    public static final String IPS_LOCAL_HOST = "localhost";
    public static final int IPS_LOCAL_PORT = 30354;

    private final IpsSocketConnector ipsSocketConnector;

    public IpsClient(String ipsHost, int ipsPort) {
        ipsSocketConnector = new IpsSocketConnector(ipsHost, ipsPort);
    }

    public void sendXmlToWfdRequest(String input, String output) {
        ipsSocketConnector.write(String.format(" xml2wfd %s;%s;  \n", input, output));
    }

    public void sendWfjRequest(String jobId) {
        ipsSocketConnector.write(String.format("wfj %s 30 \n", jobId));
    }

    public void sendQjRequest(String jobId) {
        ipsSocketConnector.write(String.format("qj %s \n", jobId));
    }

    public void sendQjmRequest(String jobId) {
        ipsSocketConnector.write(String.format("qjm %s \n", jobId));
    }

    public String readJobMessages() {
        String intToRead = readMessageResponse().split(IpsResponse.SPLIT_FRAME_CHAR)[1];
        int countOfJobMsgChars = Integer.parseInt(intToRead);
        return ipsSocketConnector.readCharacters(countOfJobMsgChars);
    }

    public String readMessageResponse() {
        return ipsSocketConnector.readOneLine();
    }

    public void close() {
        ipsSocketConnector.close();
    }
}
