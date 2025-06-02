package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.FillStyle;
import com.quadient.wfdxml.api.layoutnodes.LineStyle;
import com.quadient.wfdxml.api.layoutnodes.PathObject;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.internal.layoutnodes.support.Path;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import java.util.ArrayList;
import java.util.List;

import static com.quadient.wfdxml.api.layoutnodes.PathObject.CapType.BUTT;
import static com.quadient.wfdxml.api.layoutnodes.PathObject.JoinType.MITER;
import static com.quadient.wfdxml.internal.layoutnodes.support.Path.PathType;
import static com.quadient.wfdxml.internal.layoutnodes.support.Path.PathType.LINE_TO;
import static com.quadient.wfdxml.internal.layoutnodes.support.Path.PathType.MOVE_TO;

public class PathObjectImpl extends LayoutObjectImpl<PathObject> implements PathObject {
    private final List<Path> paths = new ArrayList<>();
    private boolean ruleOddEven = true;
    private FillStyle fillStyleId;          // Normal fillStyle
    private FillStyle outLineStyleId;       // line fillStyle
    private double lineWidth = 0.0002;
    private double miterLimit = 10.0;
    private CapType capType = BUTT;
    private JoinType joinType = MITER;
    private LineStyle lineStyleId;
    private boolean affectRunaroundByFillOnly = true;
    private Variable urlLink;

    public static String convertCapTypToXmlName(CapType type) {
        switch (type) {
            case BUTT:
                return "Butt";
            case ROUND:
                return "Round";
            case SQUARE:
                return "Square";
            default:
                throw new IllegalStateException(type.toString());
        }
    }

    public static String convertJoinTypToXmlName(JoinType type) {
        switch (type) {
            case MITER:
                return "Miter";
            case ROUND:
                return "Round";
            case BEVEL:
                return "Bevel";
            default:
                throw new IllegalStateException(type.toString());
        }
    }

    public static String convertPathTypeToXmlName(PathType type) {
        switch (type) {
            case LINE_TO:
                return "LineTo";
            case MOVE_TO:
                return "MoveTo";
            default:
                throw new IllegalStateException(type.toString());
        }
    }

    public boolean isRuleOddEven() {
        return ruleOddEven;
    }

    public PathObjectImpl setRuleOddEven(boolean ruleOddEven) {
        this.ruleOddEven = ruleOddEven;
        return this;
    }

    public FillStyle getFillStyleId() {
        return fillStyleId;
    }

    public PathObjectImpl setFillStyleId(FillStyle fillStyleId) {
        this.fillStyleId = fillStyleId;
        return this;
    }

    public FillStyle getOutLineStyleId() {
        return outLineStyleId;
    }

    public PathObjectImpl setOutLineStyleId(FillStyle outLineStyleId) {
        this.outLineStyleId = outLineStyleId;
        return this;
    }

    public double getLineWidth() {
        return lineWidth;
    }

    @Override
    public PathObjectImpl setLineWidth(double lineWidth) {
        this.lineWidth = lineWidth;
        return this;
    }

    public double getMiterLimit() {
        return miterLimit;
    }

    public PathObjectImpl setMiterLimit(double miterLimit) {
        this.miterLimit = miterLimit;
        return this;
    }

    public CapType getCapType() {
        return capType;
    }

    public PathObjectImpl setCapType(CapType capType) {
        this.capType = capType;
        return this;
    }

    public JoinType getJoinType() {
        return joinType;
    }

    public PathObjectImpl setJoinType(JoinType joinType) {
        this.joinType = joinType;
        return this;
    }

    public LineStyle getLineStyleId() {
        return lineStyleId;
    }

    public PathObjectImpl setLineStyleId(LineStyle lineStyleId) {
        this.lineStyleId = lineStyleId;
        return this;
    }

    public boolean isAffectRunaroundByFillOnly() {
        return affectRunaroundByFillOnly;
    }

    public PathObjectImpl setAffectRunaroundByFillOnly(boolean affectRunaroundByFillOnly) {
        this.affectRunaroundByFillOnly = affectRunaroundByFillOnly;
        return this;
    }

    public Variable getUrlLink() {
        return urlLink;
    }

    public PathObjectImpl setUrlLink(Variable urlLink) {
        this.urlLink = urlLink;
        return this;
    }

    public List<Path> getPaths() {
        return paths;
    }

    @Override
    public PathObjectImpl setMiter(double miter) {
        return setMiterLimit(miter);
    }

    @Override
    public PathObjectImpl setCap(CapType cap) {
        return setCapType(cap);
    }

    @Override
    public PathObjectImpl setJoin(JoinType join) {
        return setJoinType(join);
    }

    @Override
    public PathObjectImpl setFillStyle(FillStyle fillStyle) {
        return setFillStyleId(fillStyle);
    }

    @Override
    public PathObjectImpl setLineFillStyle(FillStyle fillStyle) {
        return setOutLineStyleId(fillStyle);
    }

    @Override
    public PathObjectImpl setLineStyle(LineStyle lineStyle) {
        return setLineStyleId(lineStyle);
    }

    @Override
    public PathObject setOverlap(boolean overlap) {
        return setRuleOddEven(overlap);
    }

    @Override
    public PathObjectImpl setRunaroundFillOnly(boolean runaroundFillOnly) {
        return setAffectRunaroundByFillOnly(runaroundFillOnly);
    }

    @Override
    public PathObjectImpl setUrlTarget(Variable variable) {
        setUrlLink(variable);
        return this;
    }

    @Override
    public PathObjectImpl addMoveTo(double x, double y) {
        paths.add(new Path(MOVE_TO, x, y));
        return this;
    }

    @Override
    public PathObject addLineTo(double x, double y) {
        if (paths.isEmpty()) {
            paths.add(new Path(MOVE_TO, 0.0, 0.0));
        }
        paths.add(new Path(LINE_TO, x, y));
        return this;
    }

    @Override
    public String getXmlElementName() {
        return "PathObject";
    }

    @Override
    public void export(XmlExporter exporter) {
        exportLayoutObjectProperties(exporter);

        exporter.beginElement("Path")
                .addElementWithBoolData("RuleOddEven", ruleOddEven);

        for (Path path : paths) {
            exporter.beginElement(convertPathTypeToXmlName(path.getType()))
                    .addDoubleAttribute("X", path.getX())
                    .addDoubleAttribute("Y", path.getY())
                    .endElement();
        }

        exporter.endElement()
                .addElementWithIface("FillStyleId", fillStyleId)
                .addElementWithIface("OutlineStyleId", outLineStyleId)
                .addElementWithStringData("CapType", convertCapTypToXmlName(capType))
                .addElementWithStringData("JoinType", convertJoinTypToXmlName(joinType))
                .addElementWithDoubleData("MiterLimit", miterLimit)
                .addElementWithDoubleData("LineWidth", lineWidth)
                .addElementWithIface("LineStyleId", lineStyleId)
                .addElementWithBoolData("AffectRunaroundByFillOnly", affectRunaroundByFillOnly)
                .addElementWithIface("URLLink", urlLink);
    }
}