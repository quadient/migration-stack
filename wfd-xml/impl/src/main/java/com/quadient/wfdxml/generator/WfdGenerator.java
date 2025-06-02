package com.quadient.wfdxml.generator;

import com.quadient.wfdxml.exceptions.IpsConnectionException;
import com.quadient.wfdxml.exceptions.WfdXmlException;
import com.quadient.wfdxml.generator.connection.IpsClient;
import com.quadient.wfdxml.generator.connection.response.QjResponse;
import com.quadient.wfdxml.generator.connection.response.ResultResponse;
import com.quadient.wfdxml.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static com.quadient.wfdxml.generator.connection.IpsClient.IPS_LOCAL_HOST;
import static com.quadient.wfdxml.generator.connection.IpsClient.IPS_LOCAL_PORT;

public class WfdGenerator {

    private static final Logger logger = LoggerFactory.getLogger(WfdGenerator.class);

    private final IpsClient ipsClient;

    public WfdGenerator() {
        this(IPS_LOCAL_HOST, IPS_LOCAL_PORT);
    }

    public WfdGenerator(int ipsPort) {
        this(IPS_LOCAL_HOST, ipsPort);
    }

    public WfdGenerator(String ipsHost, int ipsPort) {
        ipsClient = new IpsClient(ipsHost, ipsPort);
    }

    /**
     * Method calls local instance of ips, and it processes conversion of xml data into wfd file.
     * During processing, it creates xml and wfd file in the same folder.
     * IPS must run localhost with default port 30354
     *
     * @param xml       xml data
     * @param directory wfd directory
     * @param name      name of the wfd file
     */
    public static void generateWfd(String xml, String directory, String name) {
        File fileDir = new File(directory);

        File xmlFile = new File(fileDir, name + ".xml");
        FileUtils.saveXmlToFile(xml, xmlFile);

        File wfdFile = new File(fileDir, name + ".wfd");
        WfdGenerator wfdGenerator = new WfdGenerator();

        wfdGenerator.generateWfdFile(xmlFile.getAbsolutePath(), wfdFile.getAbsolutePath());
        wfdGenerator.close();

        logger.info("Result xml and wfd generated into: '{}'", fileDir.getAbsolutePath());
        logger.info("xml: '", xmlFile.getAbsoluteFile());
        logger.info("wfd: '", wfdFile.getAbsoluteFile());
    }

    public void generateWfdFile(String xmlFileName, String wfdFilename) {
        try {
            createWFD(xmlFileName, wfdFilename);
        } catch (IpsConnectionException e) {
            throw new WfdXmlException(e);
        } finally {
            close();
        }
    }

    public void close() {
        try {
            ipsClient.close();
        } catch (IpsConnectionException e) {
            throw new WfdXmlException(e);
        }
    }

    private void createWFD(String input, String output) {
        ipsClient.sendXmlToWfdRequest(input, output);
        ResultResponse resultResponse = new ResultResponse(ipsClient.readMessageResponse());

        if (!resultResponse.isResultStateOk()) {
            throw new WfdXmlException(String.format("Call to designer with XML2WFD has failed. '%s'",
                    resultResponse.getResultState()));
        }

        String jobId = resultResponse.getJobId();
        logger.info("jobId {}", jobId);

        ipsClient.sendWfjRequest(jobId);
        String wfjResponse = ipsClient.readMessageResponse();

        ipsClient.sendQjRequest(jobId);
        QjResponse qjResponse = new QjResponse(ipsClient.readMessageResponse());
        logger.info(
                "State: {} Errors: {} Warnings: {}",
                qjResponse.getState(),
                qjResponse.getErrors(),
                qjResponse.getWarnings()
        );

        ipsClient.sendQjmRequest(jobId);

        String jobMessages = ipsClient.readJobMessages();
        logger.info(jobMessages);

        if (qjResponse.hasErrors()) {
            throw new WfdXmlException(String.format("Call to designer with XML2WFD has failed. %n %s", jobMessages));
        }
    }
}