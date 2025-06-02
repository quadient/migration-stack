package com.quadient.wfdxml.internal.layoutnodes.support;

public class ColorRgb {
    private double red = 0;
    private double green = 0;
    private double blue = 0;

    public ColorRgb() {
    }

    public ColorRgb setRgb(double red, double green, double blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        return this;
    }

    public double getRed() {
        return red;
    }

    public ColorRgb setRed(double red) {
        this.red = red;
        return this;
    }

    public double getGreen() {
        return green;
    }

    public ColorRgb setGreen(double green) {
        this.green = green;
        return this;
    }

    public double getBlue() {
        return blue;
    }

    public ColorRgb setBlue(double blue) {
        this.blue = blue;
        return this;

    }
}