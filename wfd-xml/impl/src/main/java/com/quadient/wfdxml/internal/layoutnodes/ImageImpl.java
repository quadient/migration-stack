package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.Image;
import com.quadient.wfdxml.api.layoutnodes.LocationType;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

public class ImageImpl extends NodeImpl<Image> implements Image {
    private String imageType = "Simple";
    private String imageLocation;
    private boolean useResizeWidth = false;
    private boolean useResizeHeight = false;
    private boolean makeTransparent = false;

    private double resizeImageWidth = 0.0;
    private double resizeImageHeight = 0.0;
    private double dpiX = 0.0;
    private double dpiY = 0.0;

    private int transparencyRX = 255;
    private int transparencyRY = 255;
    private int transparencyGX = 255;
    private int transparencyGY = 255;
    private int transparencyBX = 255;
    private int transparencyBY = 255;

    private boolean useDifferentImageSizeForHtml = false;
    private String htmlImageWidthValue;
    private String htmlImageHeightValue;

    public String getImageType() {
        return imageType;
    }

    public ImageImpl setImageType(String imageType) {
        this.imageType = imageType;
        return this;
    }

    public String getImageLocation() {
        return imageLocation;
    }

    public boolean getUseResizeWidth() {
        return useResizeWidth;
    }

    public ImageImpl setUseResizeWidth(boolean useResizeWidth) {
        this.useResizeWidth = useResizeWidth;
        return this;
    }

    public boolean getUseResizeHeight() {
        return useResizeHeight;
    }

    public ImageImpl setUseResizeHeight(boolean useResizeHeight) {
        this.useResizeHeight = useResizeHeight;
        return this;
    }

    public double getResizeImageWidth() {
        return resizeImageWidth;
    }

    public ImageImpl setResizeImageWidth(double resizeImageWidth) {
        this.resizeImageWidth = resizeImageWidth;
        return this;
    }

    public double getResizeImageHeight() {
        return resizeImageHeight;
    }

    public ImageImpl setResizeImageHeight(double resizeImageHeight) {
        this.resizeImageHeight = resizeImageHeight;
        return this;
    }

    public double getDpiX() {
        return dpiX;
    }

    public ImageImpl setDpiX(double dpiX) {
        this.dpiX = dpiX;
        return this;
    }

    public double getDpiY() {
        return dpiY;
    }

    public ImageImpl setDpiY(double dpiY) {
        this.dpiY = dpiY;
        return this;
    }

    public boolean isMakeTransparent() {
        return makeTransparent;
    }

    public ImageImpl setMakeTransparent(boolean transparent) {
        this.makeTransparent = transparent;
        return this;
    }

    public int getTransparencyRX() {
        return transparencyRX;
    }

    public ImageImpl setTransparencyRX(int transparencyRX) {
        this.transparencyRX = transparencyRX;
        return this;
    }

    public int getTransparencyRY() {
        return transparencyRY;
    }

    public ImageImpl setTransparencyRY(int transparencyRY) {
        this.transparencyRY = transparencyRY;
        return this;
    }

    public int getTransparencyGX() {
        return transparencyGX;
    }

    public ImageImpl setTransparencyGX(int transparencyGX) {
        this.transparencyGX = transparencyGX;
        return this;
    }

    public int getTransparencyGY() {
        return transparencyGY;
    }

    public ImageImpl setTransparencyGY(int transparencyGY) {
        this.transparencyGY = transparencyGY;
        return this;
    }

    public int getTransparencyBX() {
        return transparencyBX;
    }

    public ImageImpl setTransparencyBX(int transparencyBX) {
        this.transparencyBX = transparencyBX;
        return this;
    }

    public int getTransparencyBY() {
        return transparencyBY;
    }

    public ImageImpl setTransparencyBY(int transparencyBY) {
        this.transparencyBY = transparencyBY;
        return this;
    }

    public boolean isUseDifferentImageSizeForHtml() {
        return useDifferentImageSizeForHtml;
    }

    public ImageImpl setUseDifferentImageSizeForHtml(boolean useDifferentImageSizeForHtml) {
        this.useDifferentImageSizeForHtml = useDifferentImageSizeForHtml;
        return this;
    }

    public String getHtmlImageWidthValue() {
        return htmlImageWidthValue;
    }

    public ImageImpl setHtmlImageWidthValue(String htmlImageWidthValue) {
        this.htmlImageWidthValue = htmlImageWidthValue;
        return this;
    }

    public String getHtmlImageHeightValue() {
        return htmlImageHeightValue;
    }

    public ImageImpl setHtmlImageHeightValue(String htmlImageHeightValue) {
        this.htmlImageHeightValue = htmlImageHeightValue;
        return this;
    }

    @Override
    public ImageImpl setImageDiskLocation(String imageLocation) {
        this.imageLocation = LocationType.DISK.getXmlValue() + "," + imageLocation;
        return this;
    }

    @Override
    public ImageImpl setImageLocation(String imageLocation, LocationType locationType) {
        this.imageLocation = locationType.getXmlValue() + "," + imageLocation;
        return this;
    }

    @Override
    public Image setDpiXAndDpiY(double dpiX, double dpiY) {
        setDpiX(dpiX);
        setDpiY(dpiY);
        return this;
    }

    @Override
    public Image setResizeWidthAndHeight(double width, double height) {
        setResizeWidth(width);
        setResizeHeight(height);
        return this;
    }

    @Override
    public ImageImpl setResizeWidth(double width) {
        setUseResizeWidth(true);
        return setResizeImageWidth(width);
    }

    @Override
    public ImageImpl setResizeHeight(double height) {
        setUseResizeHeight(true);
        return setResizeImageHeight(height);
    }


    @Override
    public ImageImpl setTransparentR(int min, int max) {
        setMakeTransparent(true);
        setTransparencyRX(min);
        setTransparencyRY(max);
        return this;
    }

    @Override
    public ImageImpl setTransparentG(int min, int max) {
        setMakeTransparent(true);
        setTransparencyGX(min);
        setTransparencyGY(max);
        return this;
    }

    @Override
    public ImageImpl setTransparentB(int min, int max) {
        setMakeTransparent(true);
        setTransparencyBX(min);
        setTransparencyBY(max);
        return this;
    }

    @Override
    public Image setHtmlWidthAndHeight(String htmlWidth, String htmlHeight) {
        setUseDifferentImageSizeForHtml(true);
        setHtmlImageWidthValue(htmlWidth);
        setHtmlImageHeightValue(htmlHeight);
        return this;
    }

    @Override
    public String getXmlElementName() {
        return "Image";
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.addElementWithStringData("ImageType", imageType)
                .addElementWithStringData("ImageLocation", imageLocation)
                .addElementWithDoubleData("ImageDPIX", dpiX)
                .addElementWithDoubleData("ImageDPIY", dpiY)
                .addElementWithBoolData("UseResizeWidth", useResizeWidth)
                .addElementWithDoubleData("ResizeImageWidth", resizeImageWidth)
                .addElementWithBoolData("UseResizeHeight", useResizeHeight)
                .addElementWithDoubleData("ResizeImageHeight", resizeImageHeight)
                .addElementWithBoolData("MakeTransparent", makeTransparent)
                .beginElement("TransparencyR")
                .addIntAttribute("X", transparencyRX)
                .addIntAttribute("Y", transparencyRY)
                .endElement()
                .beginElement("TransparencyG")
                .addIntAttribute("X", transparencyGX)
                .addIntAttribute("Y", transparencyGY)
                .endElement()
                .beginElement("TransparencyB")
                .addIntAttribute("X", transparencyBX)
                .addIntAttribute("Y", transparencyBY)
                .endElement()
                .addElementWithBoolData("UseDifferentImageSizeForHtml", useDifferentImageSizeForHtml)
                .addElementWithStringData("HtmlImageWidthValue", htmlImageWidthValue)
                .addElementWithStringData("HtmlImageHeightValue", htmlImageHeightValue);
    }
}