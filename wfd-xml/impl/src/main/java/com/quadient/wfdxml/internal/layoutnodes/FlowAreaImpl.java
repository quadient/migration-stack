package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.Flow;
import com.quadient.wfdxml.api.layoutnodes.FlowArea;
import com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import static com.quadient.wfdxml.api.layoutnodes.FlowArea.BorderType.CONTENT_ONLY;

public class FlowAreaImpl extends LayoutObjectImpl<FlowArea> implements FlowArea {
    private Flow flow = null;
    private BorderStyle borderStyle;
    private BorderType borderType = CONTENT_ONLY;

    private boolean flowingToNextPage = false;

    public FlowAreaImpl() {
    }

    public static String convertBorderTypeToXmlName(BorderType borderType) {
        switch (borderType) {
            case CONTENT_ONLY:
                return "Content";
            case CONTENT_WITH_LINE_GAB:
                return "ContentWithLineGap";
            case FULL_AREA:
                return "FullArea";
            default:
                throw new IllegalStateException(borderType.toString());
        }
    }

    public Flow getFlow() {
        return flow;
    }

    @Override
    public FlowAreaImpl setFlow(Flow flow) {
        this.flow = flow;
        return this;
    }

    public boolean isFlowingToNextPage() {
        return flowingToNextPage;
    }

    public FlowAreaImpl setFlowingToNextPage(boolean flowingToNextPage) {
        this.flowingToNextPage = flowingToNextPage;
        return this;
    }

    public BorderStyle getBorderStyle() {
        return borderStyle;
    }

    @Override
    public FlowAreaImpl setBorderStyle(BorderStyle borderStyle) {
        this.borderStyle = borderStyle;
        return this;
    }

    @Override
    public FlowArea setFlowToNextPage(boolean flowToNextPage) {
        return setFlowingToNextPage(flowToNextPage);
    }

    public BorderType getBorderType() {
        return borderType;
    }

    @Override
    public FlowArea setBorderType(BorderType borderType) {
        this.borderType = borderType;
        return this;
    }

    @Override
    public String getXmlElementName() {
        return "FlowArea";
    }

    @Override
    public void export(XmlExporter exporter) {
        exportLayoutObjectProperties(exporter);
        exporter.addElementWithIface("FlowId", flow)
                .addElementWithIface("BorderStyleId", borderStyle)
                .addElementWithBoolData("FlowingToNextPage", flowingToNextPage)
                .addElementWithStringData("BorderType", convertBorderTypeToXmlName(borderType));
    }
}