package com.quadient.e2e

import com.quadient.containers.DesignerTestEnvironment
import com.quadient.examples.ConditionUsageExample
import com.quadient.examples.ImageAndLineUsageExample
import com.quadient.examples.ListsUsageExample
import com.quadient.examples.OverflowUsageExample
import com.quadient.examples.ParagraphUsageExample
import com.quadient.examples.VariableUsageExample
import com.quadient.examples.data.XmlDataInputSettings
import com.quadient.wfdxml.generator.WfdGenerator
import spock.lang.Shared

import java.nio.file.Path

class ExamplesE2ETest extends DesignerTestEnvironment {

    private static final String IMAGE_FILE_NAME = "image.jpg"
    private static final String OUTPUT_DATA_NAME = "Data.xml"
    private static final String WINGDING_FONT_FILE_NAME = "wingding.ttf"
    private static final String CENTAUR_FONT_FILE_NAME = "centaur.ttf"

    private static final String XML_INPUT_FILE_CONDITION = "ConditionUsageExample.xml"
    private static final String WFD_OUTPUT_FILE_CONDITION = "ConditionUsageExample.wfd"

    private static final String XML_INPUT_FILE_IMAGE_LINE = "ImageAndLineUsageExample.xml"
    private static final String WFD_OUTPUT_FILE_IMAGE_LINE = "ImageAndLineUsageExample.wfd"

    private static final String XML_INPUT_FILE_LISTS = "ListsUsageExample.xml"
    private static final String WFD_OUTPUT_FILE_LISTS = "ListsUsageExample.wfd"

    private static final String XML_INPUT_FILE_OVERFLOW = "OverflowUsageExample.xml"
    private static final String WFD_OUTPUT_FILE_OVERFLOW = "OverflowUsageExample.wfd"

    private static final String XML_INPUT_FILE_TEXT_PARAGRAPH = "TextAndParagraph.xml"
    private static final String WFD_OUTPUT_FILE_TEXT_PARAGRAPH = "TextAndParagraph.wfd"

    private static final String XML_INPUT_FILE_TABLE = "TableUsageExample.xml"
    private static final String WFD_OUTPUT_FILE_TABLE = "TableUsageExample.wfd"

    private static final String XML_INPUT_FILE_VARIABLE = "VariableUsageExample.xml"
    private static final String WFD_OUTPUT_FILE_VARIABLE = "VariableUsageExample.wfd"

    @Shared
    private WfdGenerator wfdGenerator

    def setupSpec() {
        wfdGenerator = new WfdGenerator(ipsDockerCompose.geIpsHost(), ipsDockerCompose.getIpsPort())

        Path imagePath = ipsDockerCompose.getFileResourcePath(IMAGE_FILE_NAME)
        ipsDockerCompose.copFileToIpsDataFolder(imagePath, IMAGE_FILE_NAME)

        Path fontWindingPath = ipsDockerCompose.getFileResourcePath(WINGDING_FONT_FILE_NAME)
        ipsDockerCompose.copFileToIpsDataFolder(fontWindingPath, WINGDING_FONT_FILE_NAME)

        Path fontCentaurPath = ipsDockerCompose.getFileResourcePath(CENTAUR_FONT_FILE_NAME)
        ipsDockerCompose.copFileToIpsDataFolder(fontCentaurPath, CENTAUR_FONT_FILE_NAME)

        String xmlData = XmlDataInputSettings.getXmlData()
        ipsDockerCompose.copyDataToIpsDataFolder(xmlData, OUTPUT_DATA_NAME)
    }

    def cleanupSpec() {
        wfdGenerator.close()
    }

    def "Condition usage example e2e test"() {
        given:
        String xmlWorkflow = new ConditionUsageExample().buildXmlWorkflow()
        ipsDockerCompose.copyDataToIpsDataFolder(xmlWorkflow, XML_INPUT_FILE_CONDITION)

        when:
        wfdIpsGenerator.generateWfdFile(XML_INPUT_FILE_CONDITION, WFD_OUTPUT_FILE_CONDITION)
        String wfdFile = ipsDockerCompose.copyDataFromIpsDataFolder(WFD_OUTPUT_FILE_CONDITION)

        then:
        noExceptionThrown()
        assert wfdFile != null
    }

    def "Image and line usage example e2e test"() {
        given:
        String xmlWorkflow = new ImageAndLineUsageExample(ipsDockerCompose.getIpsDataPathOfFile(IMAGE_FILE_NAME))
                .buildXmlWorkflow()
        ipsDockerCompose.copyDataToIpsDataFolder(xmlWorkflow, XML_INPUT_FILE_IMAGE_LINE)

        when:
        wfdIpsGenerator.generateWfdFile(XML_INPUT_FILE_IMAGE_LINE, WFD_OUTPUT_FILE_IMAGE_LINE)
        String wfdFile = ipsDockerCompose.copyDataFromIpsDataFolder(WFD_OUTPUT_FILE_IMAGE_LINE)

        then:
        noExceptionThrown()
        assert wfdFile != null
    }

    def "Lists usage example e2e test"() {
        given:
        String xmlWorkflow = new ListsUsageExample(ipsDockerCompose.getIpsDataPathOfFile(WINGDING_FONT_FILE_NAME))
                .buildXmlWorkflow()
        ipsDockerCompose.copyDataToIpsDataFolder(xmlWorkflow, XML_INPUT_FILE_LISTS)

        when:
        wfdIpsGenerator.generateWfdFile(XML_INPUT_FILE_LISTS, WFD_OUTPUT_FILE_LISTS)
        String wfdFile = ipsDockerCompose.copyDataFromIpsDataFolder(WFD_OUTPUT_FILE_LISTS)

        then:
        noExceptionThrown()
        assert wfdFile != null
    }

    def "Overflow usage example usage e2e test"() {
        given:
        String xmlWorkflow = new OverflowUsageExample(ipsDockerCompose.getIpsDataPathOfFile(OUTPUT_DATA_NAME))
                .buildXmlWorkflow()
        ipsDockerCompose.copyDataToIpsDataFolder(xmlWorkflow, XML_INPUT_FILE_OVERFLOW)

        when:
        wfdIpsGenerator.generateWfdFile(XML_INPUT_FILE_OVERFLOW, WFD_OUTPUT_FILE_OVERFLOW)
        String wfdFile = ipsDockerCompose.copyDataFromIpsDataFolder(WFD_OUTPUT_FILE_OVERFLOW)

        then:
        noExceptionThrown()
        assert wfdFile != null
    }

    def "Paragraph usage example e2e test"() {
        given:
        String xmlWorkflow = new ParagraphUsageExample(ipsDockerCompose.getIpsDataPathOfFile(CENTAUR_FONT_FILE_NAME))
                .buildXmlWorkflow()
        ipsDockerCompose.copyDataToIpsDataFolder(xmlWorkflow, XML_INPUT_FILE_TEXT_PARAGRAPH)

        when:
        wfdIpsGenerator.generateWfdFile(XML_INPUT_FILE_TEXT_PARAGRAPH, WFD_OUTPUT_FILE_TEXT_PARAGRAPH)
        String wfdFile = ipsDockerCompose.copyDataFromIpsDataFolder(WFD_OUTPUT_FILE_TEXT_PARAGRAPH)

        then:
        noExceptionThrown()
        assert wfdFile != null
    }

    def "Table usage example e2e test"() {
        given:
        String xmlWorkflow = new VariableUsageExample(ipsDockerCompose.getIpsDataPathOfFile(CENTAUR_FONT_FILE_NAME))
                .buildXmlWorkflow()
        ipsDockerCompose.copyDataToIpsDataFolder(xmlWorkflow, XML_INPUT_FILE_TABLE)

        when:
        wfdIpsGenerator.generateWfdFile(XML_INPUT_FILE_TABLE, WFD_OUTPUT_FILE_TABLE)
        String wfdFile = ipsDockerCompose.copyDataFromIpsDataFolder(WFD_OUTPUT_FILE_TABLE)

        then:
        noExceptionThrown()
        assert wfdFile != null
    }

    def "Variable usage example e2e test"() {
        given:
        String xmlWorkflow = new VariableUsageExample(ipsDockerCompose.getIpsDataPathOfFile(OUTPUT_DATA_NAME))
                .buildXmlWorkflow()
        ipsDockerCompose.copyDataToIpsDataFolder(xmlWorkflow, XML_INPUT_FILE_VARIABLE)

        when:
        wfdIpsGenerator.generateWfdFile(XML_INPUT_FILE_VARIABLE, WFD_OUTPUT_FILE_VARIABLE)
        String wfdFile = ipsDockerCompose.copyDataFromIpsDataFolder(WFD_OUTPUT_FILE_VARIABLE)

        then:
        noExceptionThrown()
        assert wfdFile != null
    }
}
