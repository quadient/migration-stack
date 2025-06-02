package com.quadient.wfdxml.api.layoutnodes;

import com.quadient.wfdxml.api.Node;

public interface Color extends Node<Color> {
    /**
     * Each value is defined in interval &lt;0;1&gt;
     */
    Color setRGB(double red, double green, double blue);

    /**
     * Each value is defined in interval &lt;0;255&gt;
     */
    Color setRGB(int red, int green, int blue);

    Color setRGB(java.awt.Color awtColor);
}
