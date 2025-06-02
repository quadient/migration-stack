package com.quadient.wfdxml.api.layoutnodes;

import com.quadient.wfdxml.api.Node;

public interface LayoutObject<S extends Node<S>> extends Node<S> {

    S setPositionAndSize(double posX, double posY, double sizeX, double sizeY);

    S setPosX(double posX);

    S setPosY(double posY);

    S setWidth(double sizeX);

    S setHeight(double sizeY);

    S setRotation(double rotation);

    S setRotationPointX(double rotation);

    S setRotationPointY(double rotation);

    S setRotationRound(double rotation);

    S setSkew(double skew);

    S setScaleX(double scaleX);

    S setScaleY(double scaleY);

    S setFlip(boolean flipsX);
}
