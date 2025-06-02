package com.quadient.examples;

import com.quadient.examples.data.ExampleContent;
import com.quadient.examples.data.XmlDataInputSettings;
import com.quadient.wfdxml.WfdXmlBuilder;
import com.quadient.wfdxml.api.layoutnodes.Page;
import com.quadient.wfdxml.api.module.Layout;
import com.quadient.wfdxml.api.module.TextReplacer;
import com.quadient.wfdxml.api.module.xmldatainput.XmlDataInput;

public class AllInOneUsageExample extends WorkflowUsageExample {

    private final String dataFilePath;

    private static final String DEFAULT_MOCK_DATA_FILE_PATH = "C://tmp/Data.xml";

    public AllInOneUsageExample() {
        this(DEFAULT_MOCK_DATA_FILE_PATH);
    }

    public AllInOneUsageExample(String dataFilePath) {
        this.dataFilePath = dataFilePath;
    }

    @Override
    protected ExampleContent prepareContent(WfdXmlBuilder builder) {
        XmlDataInputSettings.createScriptXmlDataInput(builder);
        XmlDataInput xmlDataInput = XmlDataInputSettings.createXmlDataInput(builder, dataFilePath);
        TextReplacer textReplacer = builder.addTextReplacer();

        Layout layout = builder.addLayout().setName("MyLayout").setPosY(5);
        builder.connectModules(xmlDataInput, 0, textReplacer, 0);
        builder.connectModules(textReplacer, 0, layout, 0);

        XmlDataInputSettings.importDataStructureIntoLayout(xmlDataInput, layout);

        Page page = layout.addPage().setName("FirstPage");
        return new ExampleContent(layout, page);
    }

    @Override
    public void setContent(ExampleContent exampleContent) {
        new VariableUsageExample().setContent(exampleContent);
        new ParagraphUsageExample().setContent(exampleContent);
        new ListsUsageExample().setContent(exampleContent);
        new TableUsageExample().setContent(exampleContent);
        new OverflowUsageExample().setContent(exampleContent);
        new ConditionUsageExample().setContent(exampleContent);
        new ImageAndLineUsageExample().setContent(exampleContent);
    }
}
