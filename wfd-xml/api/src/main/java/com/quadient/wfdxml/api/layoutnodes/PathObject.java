package com.quadient.wfdxml.api.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.data.Variable;

public interface PathObject extends LayoutObject<PathObject> {

    PathObject setLineWidth(double lineWidth);

    PathObject setMiter(double miter);

    PathObject setCap(CapType cap);

    PathObject setJoin(JoinType join);

    PathObject setFillStyle(FillStyle fillStyle);

    PathObject setLineFillStyle(FillStyle fillStyle);

    PathObject setLineStyle(LineStyle lineStyle);

    PathObject setOverlap(boolean overlap);

    PathObject setRunaroundFillOnly(boolean runaroundFillOnly);

    PathObject setUrlTarget(Variable variable);

    PathObject addMoveTo(double x, double y);

    PathObject addLineTo(double x, double y);

    PathObject addBezierTo(double x, double y, double x1, double y1, double x2, double y2);

    PathObject addConicTo(double x, double y, double x1, double y1);

    enum CapType {
        BUTT,
        ROUND,
        SQUARE
    }

    enum JoinType {
        MITER,
        ROUND,
        BEVEL,
    }
}