package com.quadient.examples;

import com.quadient.examples.data.ExampleContent;
import com.quadient.wfdxml.WfdXmlBuilder;
import com.quadient.wfdxml.api.layoutnodes.DataMatrixBarcode;
import com.quadient.wfdxml.api.layoutnodes.Element;
import com.quadient.wfdxml.api.layoutnodes.FillStyle;
import com.quadient.wfdxml.api.layoutnodes.Flow;
import com.quadient.wfdxml.api.layoutnodes.FlowArea;
import com.quadient.wfdxml.api.layoutnodes.Image;
import com.quadient.wfdxml.api.layoutnodes.ImageArea;
import com.quadient.wfdxml.api.layoutnodes.LineStyle;
import com.quadient.wfdxml.api.layoutnodes.Page;
import com.quadient.wfdxml.api.layoutnodes.PathObject;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.api.module.Layout;

import static com.quadient.wfdxml.api.layoutnodes.LineStyle.Endings.SIMPLE_WIDE;
import static com.quadient.wfdxml.api.layoutnodes.LineStyle.Endings.SOLID_CIRCLE;

public class ImageAndLineUsageExample extends WorkflowUsageExample {

    private static final String DEFAULT_MOCK_IMAGE_PATH = "C://tmp/image.jpg";

    private final String imagePath;

    public ImageAndLineUsageExample() {
       this(DEFAULT_MOCK_IMAGE_PATH);
    }

    public ImageAndLineUsageExample(String imagePath) {
        this.imagePath = imagePath;
    }

    @Override
    protected ExampleContent prepareContent(WfdXmlBuilder builder) {
        Layout layout = builder.addLayout()
                .setName("Layout");
        Page overflowPage = layout.addPage()
                .setName("FirstPage");
        return new ExampleContent(layout, overflowPage);
    }

    @Override
    public void setContent(ExampleContent exampleContent) {
        createImagesAndLine(exampleContent.layout(), exampleContent.page());
    }

    private void createImagesAndLine(Layout layout, Page page) {
        Image image = layout.addImage()
                .setImageDiskLocation(imagePath)
                .setResizeHeight(0.024)
                .setName("Hamster");

        Flow imageFlow = layout.addFlow()
                .addParagraph().addText()
                .appendImage(image)
                .appendText("Image inserted into Flow")
                .back().back()
                .setName("ImageFlow");

        FlowArea middleRightArea = page.addFlowArea()
                .setPositionAndSize(0.110, 0.135, 0.095, 0.025)
                .setFlow(imageFlow)
                .setName("BottomRightArea");

        FlowArea textFlowArea = page.addFlowArea()
                .setPositionAndSize(0.110, 0.165, 0.025, 0.04)
                .setFlow(layout.addFlow()
                        .addParagraph()
                        .addText()
                        .appendText("Next to this FlowArea there is Image in ImageArea with properties: Stretch, " +
                                "Horizontal align right, and with setup Offset")
                        .back()
                        .back()
                );

        FillStyle lineFillStyle = layout.addFillStyle()
                .setColor(layout.addColor()
                        .setRGB(13, 84, 17))
                .setName("LineColor");

        LineStyle endingsLineStyle = layout.addLineStyle()
                .setStartEnding(SOLID_CIRCLE)
                .setEndEnding(SIMPLE_WIDE)
                .setName("EndingsLineStyle");

        LineStyle lineStyle = layout.addLineStyle()
                .addLineGapValues(0.005)
                .addLineGapValues(0.002)
                .addStripe(0.0, 0.5)
                .addStripe(0.5, 0.2)
                .setName("StripsAndGapLineStyle");

        ImageArea imageArea = page.addImageArea()
                .setImage(image)
                .setPositionAndSize(0.140, 0.165, 0.065, 0.04)
                .setHorizontalAlign(ImageArea.HorizontalAlign.RIGHT)
                .setOffsetX(0.002)
                .setOffsetY(0.003)
                .setRotation(10)
                .setSkew(-10)
                .setName("ImageArea");

        PathObject line = page.addPathObject()
                .setPositionAndSize(0.110, 0.21, 0.025, 0.04)
                .setLineFillStyle(lineFillStyle)
                .setLineWidth(0.001)
                .addLineTo(0.025, 0.04)
                .setName("Line")
                .setLineStyle(lineStyle);

        PathObject verticalLine = page.addLine(0.14, 0.225, 0.14, 0.25)
                .setName("VerticalLine");

        PathObject horizontalLine = page.addLine(0.145, 0.225, 0.19, 0.225)
                .setName("HorizontalLine");

        PathObject diagonal = page.addLine(0.15, 0.23, 0.190, 0.25)
                .setLineStyle(endingsLineStyle)
                .setName("LineWithEnding");

        Variable variable = layout.getData()
                .addVariable()
                .setValue("https://www.quadient.com/")
                .setName("HyperLinkVariable");

        DataMatrixBarcode dataMatrixBarcode = page.getBarcodeFactory()
                .addDataMatrix()
                .setName("Data Matrix")
                .setPositionAndSize(0.11, 0.255, 0.1, 0.1)
                .setBarcodeFill(layout.addFillStyle())
                .setVariable(variable)
                .setModuleWidth(0.001)
                .setQuitZoneRatio(2);

        Element elementWithLine = layout.addElement()
                .setWidth(0.01);

        elementWithLine.addLine(0, 0, 0.01, 0);

        Element elementWithBarcode = layout.addElement()
                .setHeight(0.0075)
                .setWidth(0.0075);

        elementWithBarcode.getBarcodeFactory()
                .addDataMatrix()
                .setBarcodeFill(layout.addFillStyle())
                .setVariable(variable)
                .setModuleWidth(0.0005);

        FlowArea barcodeAndLineFlowArea = page.addFlowArea()
                .setPositionAndSize(0.140, 0.255, 0.05, 0.02)
                .setFlow(
                        layout.addFlow()
                                .addParagraph()
                                .addText()
                                .appendText("Line ")
                                .appendElement(elementWithLine)
                                .appendText(" in flow")
                                .back()
                                .back()
                                .addParagraph()
                                .addText()
                                .appendText("Barcode in flow: ")
                                .appendElement(elementWithBarcode)
                                .back()
                                .back()
                );
    }
}
