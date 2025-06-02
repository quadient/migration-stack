package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.Image;
import com.quadient.wfdxml.api.layoutnodes.ImageArea;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

public class ImageObjectImpl extends LayoutObjectImpl<ImageArea> implements ImageArea {
    private Image image;

    private boolean preserveAspectRatio = true;
    private boolean stretch = true;
    private boolean shrink = true;
    private boolean clipImage = false;

    private HorizontalAlign horizontalAlign = HorizontalAlign.CENTER;
    private VerticalAlign verticalAlign = VerticalAlign.CENTER;

    private double offsetX = 0.0;
    private double offsetY = 0.0;

    private Variable urlLink;

    public static String convertHorizontalAlignToXmlName(HorizontalAlign align) {
        switch (align) {
            case LEFT:
                return "Left";
            case RIGHT:
                return "Right";
            case CENTER:
                return "Center";
            default:
                throw new IllegalStateException(align.toString());
        }
    }

    public static String convertVerticalAlignToXmlName(VerticalAlign align) {
        switch (align) {
            case TOP:
                return "Top";
            case CENTER:
                return "Center";
            case BOTTOM:
                return "Bottom";
            default:
                throw new IllegalStateException(align.toString());
        }
    }

    public Image getImage() {
        return image;
    }

    @Override
    public ImageObjectImpl setImage(Image image) {
        this.image = image;
        return this;
    }

    public boolean isPreserveAspectRatio() {
        return preserveAspectRatio;
    }

    @Override
    public ImageObjectImpl setPreserveAspectRatio(boolean preserveAspectRatio) {
        this.preserveAspectRatio = preserveAspectRatio;
        return this;
    }

    public boolean isStretch() {
        return stretch;
    }

    @Override
    public ImageObjectImpl setStretch(boolean stretch) {
        this.stretch = stretch;
        return this;
    }

    public boolean isShrink() {
        return shrink;
    }

    @Override
    public ImageObjectImpl setShrink(boolean shrink) {
        this.shrink = shrink;
        return this;
    }

    public boolean isClipImage() {
        return clipImage;
    }

    @Override
    public ImageObjectImpl setClipImage(boolean clipImage) {
        this.clipImage = clipImage;
        return this;
    }

    public HorizontalAlign getHorizontalAlign() {
        return horizontalAlign;
    }

    @Override
    public ImageObjectImpl setHorizontalAlign(HorizontalAlign horizontalAlign) {
        this.horizontalAlign = horizontalAlign;
        return this;
    }

    public VerticalAlign getVerticalAlign() {
        return verticalAlign;
    }

    @Override
    public ImageObjectImpl setVerticalAlign(VerticalAlign verticalAlign) {
        this.verticalAlign = verticalAlign;
        return this;
    }

    public double getOffsetX() {
        return offsetX;
    }

    @Override
    public ImageObjectImpl setOffsetX(double offsetX) {
        this.offsetX = offsetX;
        return this;
    }

    public double getOffsetY() {
        return offsetY;
    }

    @Override
    public ImageObjectImpl setOffsetY(double offsetY) {
        this.offsetY = offsetY;
        return this;
    }

    public Variable getUrlLink() {
        return urlLink;
    }

    @Override
    public ImageObjectImpl setUrlLink(Variable variable) {
        this.urlLink = variable;
        return this;
    }

    @Override
    public String getXmlElementName() {
        return "ImageObject";
    }

    @Override
    public void export(XmlExporter exporter) {
        exportLayoutObjectProperties(exporter);

        exporter.addElementWithIface("ImageId", image)
                .addElementWithBoolData("PreserveAspectRatio", preserveAspectRatio)
                .addElementWithBoolData("Stretch", stretch)
                .addElementWithBoolData("Shrink", shrink)
                .addElementWithStringData("HorizontalAlign", convertHorizontalAlignToXmlName(horizontalAlign))
                .addElementWithStringData("VerticalAlign", convertVerticalAlignToXmlName(verticalAlign))
                .addElementWithIface("URLLink", urlLink)
                .addElementWithBoolData("ClippingImage", clipImage)
                .beginElement("Offset")
                .addDoubleAttribute("X", offsetX)
                .addDoubleAttribute("Y", offsetY)
                .endElement();
    }


}