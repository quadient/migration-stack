package com.quadient.wfdxml.api.layoutnodes;

import com.quadient.wfdxml.api.Node;

public interface Image extends Node<Image> {

    Image setImageDiskLocation(String location);

    Image setImageLocation(String location, LocationType locationType);

    Image setDpiXAndDpiY(double dpiX, double dpiY);

    Image setResizeWidthAndHeight(double width, double height);

    Image setResizeWidth(double width);

    Image setResizeHeight(double height);

    Image setHtmlWidthAndHeight(String htmlWidth, String htmlHeight);

    Image setTransparentR(int min, int max);

    Image setTransparentG(int min, int max);

    Image setTransparentB(int min, int max);
}