package com.quadient.wfdxml.internal.layoutnodes.support;

public class Path {
    private double x;
    private double y;
    private double x1;
    private double y1;
    private double x2;
    private double y2;
    private PathType type;

    public Path(PathType type, double x, double y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public Path(double x, double y, double x1, double y1, double x2, double y2) {
        this.type = PathType.BEZIER_TO;
        this.x = x;
        this.y = y;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public Path(double x, double y, double x1, double y1) {
        this.type = PathType.CONIC_TO;
        this.x = x;
        this.y = y;
        this.x1 = x1;
        this.y1 = y1;
    }

    public double getX() {
        return x;
    }

    public Path setX(double x) {
        this.x = x;
        return this;
    }

    public double getY() {
        return y;
    }

    public Path setY(double y) {
        this.y = y;
        return this;
    }

    public double getX1() {
        return x1;
    }

    public void setX1(double x1) {
        this.x1 = x1;
    }

    public double getY1() {
        return y1;
    }

    public void setY1(double y1) {
        this.y1 = y1;
    }

    public double getX2() {
        return x2;
    }

    public void setX2(double x2) {
        this.x2 = x2;
    }

    public double getY2() {
        return y2;
    }

    public void setY2(double y2) {
        this.y2 = y2;
    }

    public PathType getType() {
        return type;
    }

    public Path setType(PathType type) {
        this.type = type;
        return this;
    }

    public enum PathType {
        MOVE_TO,
        LINE_TO,
        CONIC_TO,
        BEZIER_TO,
    }
}