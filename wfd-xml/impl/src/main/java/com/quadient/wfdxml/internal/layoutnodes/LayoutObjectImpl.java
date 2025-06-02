package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.Node;
import com.quadient.wfdxml.api.layoutnodes.LayoutObject;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

/**
 * Parent of Objects, that can be placed directly into Page
 */
public abstract class LayoutObjectImpl<S extends Node<S>> extends NodeImpl<S> implements LayoutObject<S> {
    private double posX = 0.0;
    private double posY = 0.0;
    private double width = 0.1;
    private double height = 0.1;

    private double rotation = 0.0;
    private double rotationPointX = 0.0;
    private double rotationPointY = 0.0;
    private double rotationRound = 0.0;
    private double skew = 0.0;
    private double scaleX = 1.0;
    private double scaleY = 1.0;

    private boolean flipX = false;


    @Override
    @SuppressWarnings("unchecked")
    public S setPositionAndSize(double posX, double posY, double sizeX, double sizeY) {
        this.posX = posX;
        this.posY = posY;
        this.width = sizeX;
        this.height = sizeY;
        return (S) this;
    }

    public double getPosX() {
        return posX;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setPosX(double posX) {
        this.posX = posX;
        return (S) this;
    }

    public double getPosY() {
        return posY;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setPosY(double posY) {
        this.posY = posY;
        return (S) this;
    }

    public double getWidth() {
        return width;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setWidth(double width) {
        this.width = width;
        return (S) this;
    }

    public double getHeight() {
        return height;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setHeight(double height) {
        this.height = height;
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setRotation(double rotation) {
        this.rotation = rotation;
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setRotationPointX(double rotation) {
        this.rotationPointX = rotation;
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setRotationPointY(double rotation) {
        this.rotationPointY = rotation;
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setRotationRound(double rotation) {
        this.rotationRound = rotation;
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setSkew(double skew) {
        this.skew = skew;
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setScaleX(double scaleX) {
        this.scaleX = scaleX;
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setScaleY(double scaleY) {
        this.scaleY = scaleY;
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setFlip(boolean flip) {
        this.flipX = flip;
        return (S) this;
    }


    public void exportLayoutObjectProperties(XmlExporter exporter) {
        exporter.beginElement("Pos")
                .addDoubleAttribute("X", posX)
                .addDoubleAttribute("Y", posY)
                .endElement();

        exporter.beginElement("Size")
                .addDoubleAttribute("X", width)
                .addDoubleAttribute("Y", height)
                .endElement()
                .addElementWithDoubleData("Rotation", rotation)
                .addElementWithDoubleData("Skew", skew)
                .addElementWithBoolData("FlipX", flipX)
                .beginElement("Scale")
                .addDoubleAttribute("X", scaleX)
                .addDoubleAttribute("Y", scaleY)
                .endElement()
                .addElementWithDoubleData("RotationPointX", rotationPointX)
                .addElementWithDoubleData("RotationPointY", rotationPointY)
                .addElementWithDoubleData("RotationRound", rotationRound);
    }
}
