package com.quadient.wfdxml.internal.layoutnodes.email;

import com.quadient.wfdxml.api.Node;
import com.quadient.wfdxml.api.layoutnodes.FillStyle;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.api.layoutnodes.email.EmailComponentContent;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import static com.quadient.wfdxml.utils.FormatUtils.formatPx;

public class EmailComponentContentImpl extends NodeImpl<EmailComponentContent> implements EmailComponentContent {
    private ContentType type = ContentType.TEXT;
    private Node content;
    private Variable alternateTextVariable;
    private Variable linkUrl;
    private String htmlWidthValue;
    private HorizontalAlignment horizontalAlignment;
    private double paddingLeft = 0;
    private double paddingTop = 0;
    private double paddingRight = 0;
    private double paddingBottom = 0;
    private FillStyle fillStyle;

    @Override
    public EmailComponentContent setType(ContentType type) {
        this.type = type;
        return this;
    }

    @Override
    public EmailComponentContent setContent(Node flow) {
        this.content = flow;
        return this;
    }

    @Override
    public EmailComponentContent setPaddingLeft(double paddingLeft) {
        this.paddingLeft = paddingLeft;
        return this;
    }

    @Override
    public EmailComponentContent setPaddingTop(double paddingTop) {
        this.paddingTop = paddingTop;
        return this;
    }

    @Override
    public EmailComponentContent setPaddingRight(double paddingRight) {
        this.paddingRight = paddingRight;
        return this;
    }

    @Override
    public EmailComponentContent setPaddingBottom(double paddingBottom) {
        this.paddingBottom = paddingBottom;
        return this;
    }

    @Override
    public EmailComponentContent setFillStyle(FillStyle fillStyle) {
        this.fillStyle = fillStyle;
        return this;
    }

    @Override
    public EmailComponentContent setAlternateTextVariable(Variable variable) {
        this.alternateTextVariable = variable;
        return this;
    }

    @Override
    public EmailComponentContent setLinkUrl(Variable variable) {
        this.linkUrl = variable;
        return this;
    }

    @Override
    public EmailComponentContent setHtmlWidthValue(double htmlWidthValue) {
        this.htmlWidthValue = formatPx(htmlWidthValue);
        return this;
    }

    @Override
    public EmailComponentContent setHorizontalAlignment(HorizontalAlignment alignment) {
        this.horizontalAlignment = alignment;
        return this;
    }

    @Override
    public String getXmlElementName() {
        return "ECContent";
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.addElementWithStringData("Type", contentTypeToXml(type));

        if (content != null) {
            exporter.addElementWithIface("ContentId", content);
        } else {
            exporter.addElement("ContentId");
        }

        if (alternateTextVariable != null) {
            exporter.addElementWithIface("AlternateTextVarId", alternateTextVariable);
        } else {
            exporter.addElement("AlternateTextVarId");
        }

        if (horizontalAlignment != null) {
            exporter.addElementWithStringData("HorizontalAlignment", horizontalAlignmentToXml(horizontalAlignment));
        }

        if (htmlWidthValue != null) {
            exporter.addElementWithStringData("HtmlWidthValue", htmlWidthValue);
        }

        if (linkUrl != null) {
            exporter.addElementWithIface("UrlLinkVarId", linkUrl);
        }

        exporter.beginElement("Padding");
        exporter.addElementWithStringData("Left", formatPx(paddingLeft));
        exporter.addElementWithStringData("Top", formatPx(paddingTop));
        exporter.addElementWithStringData("Right", formatPx(paddingRight));
        exporter.addElementWithStringData("Bottom", formatPx(paddingBottom));
        exporter.endElement();

        if (fillStyle != null) {
            exporter.addElementWithIface("FillStyleId", fillStyle);
        } else {
            exporter.addElement("FillStyleId");
        }
    }

    private String contentTypeToXml(ContentType type) {
        switch (type) {
            case TEXT:
                return "Text";
            case IMAGE:
                return "Image";
            case EXTERNAL_IMAGE:
                return "ExternalImage";
            default:
                throw new IllegalStateException(type.toString());
        }
    }

    private String horizontalAlignmentToXml(HorizontalAlignment alignment) {
        switch (alignment) {
            case LEFT:
                return "Left";
            case CENTER:
                return "Center";
            case RIGHT:
                return "Right";
            default:
                throw new IllegalStateException(alignment.toString());
        }
    }
}
