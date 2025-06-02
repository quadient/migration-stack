package com.quadient.wfdxml.internal.layoutnodes.support;

public class Path {
    private double x;
    private double y;
    private PathType type;

    public Path(PathType type, double x, double y) {
        this.type = type;
        this.x = x;
        this.y = y;
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
    }
}