package com.quadient.examples;

import com.quadient.examples.data.ExampleContent;
import com.quadient.wfdxml.WfdXmlBuilder;

public abstract class WorkflowUsageExample {

    private WfdXmlBuilder builder = new WfdXmlBuilder();

    public String buildXmlWorkflow() {
        WfdXmlBuilder builder = new WfdXmlBuilder();

        ExampleContent content = prepareContent(builder);
        setContent(content);

        return builder.build();
    }

    protected abstract ExampleContent prepareContent(WfdXmlBuilder builder);

    public abstract void setContent(ExampleContent exampleContent);
}
