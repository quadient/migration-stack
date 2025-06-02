package com.quadient.wfdxml.internal.layoutnodes;

import com.quadient.wfdxml.api.layoutnodes.FillStyle;
import com.quadient.wfdxml.api.layoutnodes.LineStyle;
import com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.layoutnodes.support.PathEnums;
import com.quadient.wfdxml.internal.layoutnodes.support.RPoint;
import com.quadient.wfdxml.internal.layoutnodes.support.RRect;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.CapType.BUTT;
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.ALL_BORDERS;
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.ALL_CORNERS;
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.ALL_LINES;
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.BOTTOM_LINE;
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.LEFT_LINE;
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.LOWER_LEFT_CORNER;
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.LOWER_RIGHT_CORNER;
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.RIGHT_LINE;
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.TOP_LINE;
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.UPPER_LEFT_CORNER;
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.UPPER_RIGHT_CORNER;
import static com.quadient.wfdxml.internal.layoutnodes.BorderStyleImpl.ElementType.CUT_OUT;
import static com.quadient.wfdxml.internal.layoutnodes.BorderStyleImpl.ElementType.LINE_CORNER;
import static com.quadient.wfdxml.internal.layoutnodes.BorderStyleImpl.ElementType.ROUND_CORNER;
import static com.quadient.wfdxml.internal.layoutnodes.BorderStyleImpl.ElementType.ROUND_OUT;
import static com.quadient.wfdxml.internal.layoutnodes.BorderStyleImpl.ElementType.STANDARD_CORNER;
import static com.quadient.wfdxml.internal.layoutnodes.support.PathEnums.JoinType.BEVEL;
import static com.quadient.wfdxml.internal.layoutnodes.support.PathEnums.JoinType.MITER;
import static com.quadient.wfdxml.internal.layoutnodes.support.PathEnums.JoinType.ROUND;
import static java.util.Arrays.asList;

public class BorderStyleImpl extends NodeImpl<BorderStyle> implements BorderStyle {

    private FillStyle fillStyle;
    private FillStyle shadowStyle;
    private RRect margin = new RRect();
    private RRect offset = new RRect();
    private RPoint shadowOffset = new RPoint();
    private PathEnums.JoinType joinType = MITER;
    private double miterLimit = 10;
    private List<FillStyleImpl> outlineStyle;
    private List<Double> lineWidth;
    private List<CapType> capType;
    private List<LineStyleImpl> lineStyle;

    private List<ElementType> elementType;
    private List<RPoint> radius;

    private Type type = Type.SIMPLE;

    private boolean treatDefaultAsError = false;

    public BorderStyleImpl() {
        outlineStyle = asList(new FillStyleImpl[10]);
        lineWidth = asList(new Double[10]);
        capType = asList(new CapType[10]);
        lineStyle = asList(new LineStyleImpl[10]);
        for (int i = 0; i < 10; i++) {
            outlineStyle.set(i, null);
            lineWidth.set(i, 0.0002);
            capType.set(i, BUTT);
            lineStyle.set(i, null);
        }

        elementType = asList(new ElementType[4]);
        radius = asList(new RPoint[4]);
        for (int i = 0; i < 4; i++) {
            elementType.set(i, STANDARD_CORNER);
            radius.set(i, new RPoint());
        }
    }

    public static String convertJoinTypeToXmlValue(PathEnums.JoinType joinType) {
        switch (joinType) {
            case MITER:
                return "Miter";
            case ROUND:
                return "Round";
            case BEVEL:
                return "Bevel";
            default:
                throw new IllegalArgumentException(joinType.toString());
        }
    }

    public static String convertCapTypeToXmlValue(CapType capType) {
        switch (capType) {
            case BUTT:
                return "Butt";
            case ROUND:
                return "Round";
            case SQUARE:
                return "Square";
            default:
                throw new IllegalArgumentException(capType.toString());
        }
    }

    public static String convertElementTypeToXmlValue(ElementType elementType) {
        switch (elementType) {
            case STANDARD_CORNER:
                return "StandardCorner";
            case ROUND_CORNER:
                return "RoundCorner";
            case ROUND_OUT:
                return "RoundOut";
            case CUT_OUT:
                return "CutOut";
            case LINE_CORNER:
                return "LineCorner";
            case LINE:
                return "Line";
            default:
                throw new IllegalArgumentException(elementType.toString());
        }
    }

    public static String convertTypeToXmlValue(Type type) {
        switch (type) {
            case SIMPLE:
                return "Simple";
            case SWITCH_INT:
                return "Integer";
            case SWITCH_RANGE:
                return "Interval";
            case SWITCH_COND:
                return "Condition";
            case STRING:
                return "String";
            case INL_COND:
                return "InlCond";
            case CONTENT:
                return "Content";
            case MEDIA_QUERIES:
                return "MediaQueries";
            default:
                throw new IllegalArgumentException(type.toString());
        }
    }

    public FillStyle getFillStyle() {
        return fillStyle;
    }

    public BorderStyleImpl setFillStyle(FillStyle fillStyle) {
        this.fillStyle = fillStyle;
        return this;
    }

    public FillStyle getShadowStyle() {
        return shadowStyle;
    }

    public BorderStyleImpl setShadowStyle(FillStyle shadowStyle) {
        this.shadowStyle = shadowStyle;
        return this;
    }

    public RRect getMargin() {
        return margin;
    }

    public BorderStyleImpl setMargin(RRect margin) {
        this.margin = margin;
        return this;
    }

    public RRect getOffset() {
        return offset;
    }

    public BorderStyleImpl setOffset(RRect offset) {
        this.offset = offset;
        return this;
    }

    public RPoint getShadowOffset() {
        return shadowOffset;
    }

    public BorderStyleImpl setShadowOffset(RPoint shadowOffset) {
        this.shadowOffset = shadowOffset;
        return this;
    }

    public PathEnums.JoinType getJoinType() {
        return joinType;
    }

    public BorderStyleImpl setJoinType(PathEnums.JoinType joinType) {
        this.joinType = joinType;
        return this;
    }

    public double getMiterLimit() {
        return miterLimit;
    }

    public BorderStyleImpl setMiterLimit(double miterLimit) {
        this.miterLimit = miterLimit;
        return this;
    }

    public List<FillStyleImpl> getOutlineStyle() {
        return outlineStyle;
    }

    public BorderStyleImpl setOutlineStyle(List<FillStyleImpl> outlineStyle) {
        this.outlineStyle = outlineStyle;
        return this;
    }

    public List<Double> getLineWidth() {
        return lineWidth;
    }

    public BorderStyleImpl setLineWidth(List<Double> lineWidth) {
        this.lineWidth = lineWidth;
        return this;
    }

    public List<CapType> getCapType() {
        return capType;
    }

    public BorderStyleImpl setCapType(List<CapType> capType) {
        this.capType = capType;
        return this;
    }

    public List<LineStyleImpl> getLineStyle() {
        return lineStyle;
    }

    public BorderStyleImpl setLineStyle(List<LineStyleImpl> lineStyle) {
        this.lineStyle = lineStyle;
        return this;
    }

    public List<ElementType> getElementType() {
        return elementType;
    }

    public BorderStyleImpl setElementType(List<ElementType> elementType) {
        this.elementType = elementType;
        return this;
    }

    public List<RPoint> getRadius() {
        return radius;
    }

    public BorderStyleImpl setRadius(List<RPoint> radius) {
        this.radius = radius;
        return this;
    }

    public Type getType() {
        return type;
    }

    public BorderStyleImpl setType(Type type) {
        this.type = type;
        return this;
    }

    public boolean isTreatDefaultAsError() {
        return treatDefaultAsError;
    }

    public BorderStyleImpl setTreatDefaultAsError(boolean treatDefaultAsError) {
        this.treatDefaultAsError = treatDefaultAsError;
        return this;
    }

    @Override
    public SelectedLinesAndCorners select(LinesAndCorners... linesAndCorners) {
        if (linesAndCorners == null || linesAndCorners.length < 1) {
            throw new IllegalArgumentException("At least one Line or Corner have to be selected.");
        }
        return new SelectedLinesAndCornersImpl(linesAndCorners);
    }

    @Override
    public BorderStyle setOffsets(double top, double right, double bottom, double left) {
        setOffsetTop(top);
        setOffsetRight(right);
        setOffsetBottom(bottom);
        setOffsetLeft(left);
        return this;
    }

    @Override
    public BorderStyle setOffsetTop(double top) {
        offset.getUpperLeft().setY(top);
        return this;
    }

    @Override
    public BorderStyle setOffsetRight(double right) {
        offset.getLowerRight().setX(right);
        return this;
    }

    @Override
    public BorderStyle setOffsetBottom(double bottom) {
        offset.getLowerRight().setY(bottom);
        return this;
    }

    @Override
    public BorderStyle setOffsetLeft(double left) {
        offset.getUpperLeft().setX(left);
        return this;
    }

    @Override
    public BorderStyle setMargins(double top, double right, double bottom, double left) {
        setMarginTop(top);
        setMarginRight(right);
        setMarginBottom(bottom);
        setMarginLeft(left);
        return this;
    }

    @Override
    public BorderStyle setMarginTop(double top) {
        margin.getUpperLeft().setY(top);
        return this;
    }

    @Override
    public BorderStyle setMarginRight(double right) {
        margin.getLowerRight().setX(right);
        return this;
    }

    @Override
    public BorderStyle setMarginBottom(double bottom) {
        margin.getLowerRight().setY(bottom);
        return this;
    }

    @Override
    public BorderStyle setMarginLeft(double left) {
        margin.getUpperLeft().setX(left);
        return this;
    }

    @Override
    public BorderStyle setJoin(JoinType join) {
        switch (join) {
            case MITER:
                setJoinType(MITER);
                break;
            case ROUND:
                setJoinType(ROUND);
                break;
            case BEVEL:
                setJoinType(BEVEL);
                break;
            default:
                throw new IllegalArgumentException(join.toString());
        }
        return this;
    }

    @Override
    public BorderStyle setMiter(double miter) {
        return setMiterLimit(miter);
    }

    @Override
    public BorderStyle setFill(FillStyle fill) {
        return setFillStyle(fill);
    }

    @Override
    public BorderStyle setShadowFillStyle(FillStyle shadowFillStyle) {
        return setShadowStyle(shadowFillStyle);
    }

    @Override
    public BorderStyle setShadowOffsetX(double shadowOffsetX) {
        shadowOffset.setX(shadowOffsetX);
        return this;
    }

    @Override
    public BorderStyle setShadowOffsetY(double shadowOffsetY) {
        shadowOffset.setY(shadowOffsetY);
        return this;
    }

    @Override
    public String getXmlElementName() {
        return "BorderStyle";
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.addElementWithIface("FillStyleId", fillStyle);
        exporter.addElementWithIface("ShadowStyleId", shadowStyle);


        exporter.beginElement("Margin");
        margin.getUpperLeft().export(exporter, "UpperLeft");
        margin.getLowerRight().export(exporter, "LowerRight");
        exporter.endElement();

        exporter.beginElement("Offset");
        offset.getUpperLeft().export(exporter, "UpperLeft");
        offset.getLowerRight().export(exporter, "LowerRight");
        exporter.endElement();

        exporter.beginElement("ShadowOffset");
        exporter.addDoubleAttribute("X", shadowOffset.getX());
        exporter.addDoubleAttribute("Y", shadowOffset.getY());
        exporter.endElement();

        exporter.addElementWithStringData("JoinType", convertJoinTypeToXmlValue(joinType));
        exporter.addElementWithDoubleData("Miter", miterLimit);

        exportElement(exporter, "LeftLine", 0);
        exportElement(exporter, "UpperLeftCorner", 1);
        exportElement(exporter, "TopLine", 2);
        exportElement(exporter, "RightTopCorner", 3);
        exportElement(exporter, "RightLine", 4);
        exportElement(exporter, "LowerRightCorner", 5);
        exportElement(exporter, "BottomLine", 6);
        exportElement(exporter, "LowerLeftCorner", 7);
        exportElement(exporter, "LeftRightLine", 8);
        exportElement(exporter, "RightLeftLine", 9);

        exportElementCorner(exporter, "UpperLeftCornerType", 0);
        exportElementCorner(exporter, "UpperRightCornerType", 1);
        exportElementCorner(exporter, "LowerRightCornerType", 2);
        exportElementCorner(exporter, "LowerLeftCornerType", 3);

        exporter.addElementWithStringData("Type", convertTypeToXmlValue(type));

        switch (type) {
            case SIMPLE:
                break;
            case SWITCH_INT:
            default:
                throw new IllegalArgumentException("BorderStyle Type '" + type + "' is not supported.");
        }

        switch (type) {
            case SWITCH_INT:
            case SWITCH_RANGE:
            case SWITCH_COND:
            case STRING:
            case INL_COND:
            case CONTENT:
                exporter.addElementWithBoolData("TreatDefaultAsError", treatDefaultAsError);
                break;
            default:
                //do nothing
        }
    }

    private void exportElement(XmlExporter exporter, String elementName, int elementIndex) {
        exporter.beginElement(elementName);
        exporter.addElementWithIface("FillStyle", outlineStyle.get(elementIndex));
        exporter.addElementWithDoubleData("LineWidth", lineWidth.get(elementIndex));
        exporter.addElementWithStringData("CapType", convertCapTypeToXmlValue(capType.get(elementIndex)));
        exporter.addElementWithIface("LineStyle", lineStyle.get(elementIndex));
        exporter.endElement();
    }

    private void exportElementCorner(XmlExporter exporter, String elementName, int elementIndex) {
        exporter.beginElement(elementName);
        exporter.addElementWithStringData("CornerType", convertElementTypeToXmlValue(elementType.get(elementIndex)));
        radius.get(elementIndex).export(exporter, "CornerRadius");
        exporter.endElement();
    }

    private ElementType guiToImpl(CornerType cornerType) {
        switch (cornerType) {
            case STANDARD:
                return STANDARD_CORNER;
            case ROUND:
                return ROUND_CORNER;
            case ROUND_OUT:
                return ROUND_OUT;
            case CUT_OUT:
                return CUT_OUT;
            case LINE_CORNER:
                return LINE_CORNER;
            default:
                throw new IllegalArgumentException(cornerType.toString());
        }
    }

    public enum ElementType {
        STANDARD_CORNER,
        ROUND_CORNER,
        LINE,
        ROUND_OUT,
        CUT_OUT,
        LINE_CORNER,
    }


    enum Type {
        SIMPLE,
        SWITCH_INT,
        SWITCH_RANGE,
        SWITCH_COND,
        STRING,
        INL_COND,
        CONTENT,
        MEDIA_QUERIES,
    }

    public class SelectedLinesAndCornersImpl implements SelectedLinesAndCorners {
        private final Set<LinesAndCorners> linesAndCorners;

        public SelectedLinesAndCornersImpl(LinesAndCorners... linesAndCorners) {
            if (linesAndCorners == null || linesAndCorners.length < 1) {
                throw new IllegalArgumentException(Arrays.toString(linesAndCorners));
            }
            this.linesAndCorners = new HashSet<>(asList(linesAndCorners));
            substituteSpecialLinesAndCornersTypes();
        }

        @SuppressWarnings("Duplicates")
        private void substituteSpecialLinesAndCornersTypes() {
            if (linesAndCorners.contains(ALL_BORDERS)) {
                linesAndCorners.remove(ALL_BORDERS);
                linesAndCorners.add(ALL_LINES);
                linesAndCorners.add(ALL_CORNERS);
            }
            if (linesAndCorners.contains(ALL_LINES)) {
                linesAndCorners.remove(ALL_LINES);
                linesAndCorners.add(TOP_LINE);
                linesAndCorners.add(RIGHT_LINE);
                linesAndCorners.add(BOTTOM_LINE);
                linesAndCorners.add(LEFT_LINE);
            }
            if (linesAndCorners.contains(ALL_CORNERS)) {
                linesAndCorners.remove(ALL_CORNERS);
                linesAndCorners.add(UPPER_LEFT_CORNER);
                linesAndCorners.add(UPPER_RIGHT_CORNER);
                linesAndCorners.add(LOWER_RIGHT_CORNER);
                linesAndCorners.add(LOWER_LEFT_CORNER);
            }
        }

        private int lineOrCornerToIndex(LinesAndCorners lineOrCorner) {
            switch (lineOrCorner) {
                case LEFT_LINE:
                    return 0;
                case UPPER_LEFT_CORNER:
                    return 1;
                case TOP_LINE:
                    return 2;
                case UPPER_RIGHT_CORNER:
                    return 3;
                case RIGHT_LINE:
                    return 4;
                case LOWER_RIGHT_CORNER:
                    return 5;
                case BOTTOM_LINE:
                    return 6;
                case LOWER_LEFT_CORNER:
                    return 7;
                case MAIN_DIAGONAL_LINE:
                    return 8;
                case SECONDARY_DIAGONAL_LINE:
                    return 9;
                default:
                    throw new IllegalArgumentException(lineOrCorner.toString());
            }
        }

        private int cornerToIndex(LinesAndCorners corner) {
            switch (corner) {
                case UPPER_LEFT_CORNER:
                    return 0;
                case UPPER_RIGHT_CORNER:
                    return 1;
                case LOWER_RIGHT_CORNER:
                    return 2;
                case LOWER_LEFT_CORNER:
                    return 3;
                default:
                    throw new IllegalArgumentException(corner.toString());
            }
        }

        public Set<LinesAndCorners> getLinesAndCorners() {
            return linesAndCorners;
        }

        @Override
        public BorderStyle backToBorderStyle() {
            return BorderStyleImpl.this;
        }

        @Override
        public SelectedLinesAndCorners setLineFillStyle(FillStyle lineFillStyle) {
            for (LinesAndCorners linesOrCorner : linesAndCorners) {
                BorderStyleImpl.this.outlineStyle.set(lineOrCornerToIndex(linesOrCorner), (FillStyleImpl) lineFillStyle);
            }
            return this;
        }

        @Override
        public SelectedLinesAndCorners setLineWidth(double lineWidth) {
            for (LinesAndCorners linesOrCorner : linesAndCorners) {
                BorderStyleImpl.this.lineWidth.set(lineOrCornerToIndex(linesOrCorner), lineWidth);
            }
            return this;
        }

        @Override
        public SelectedLinesAndCorners setCap(CapType cap) {
            for (LinesAndCorners linesOrCorner : linesAndCorners) {
                BorderStyleImpl.this.capType.set(lineOrCornerToIndex(linesOrCorner), cap);
            }
            return this;
        }

        @Override
        public SelectedLinesAndCorners setLineStyle(LineStyle lineStyle) {
            for (LinesAndCorners linesOrCorner : linesAndCorners) {
                BorderStyleImpl.this.lineStyle.set(lineOrCornerToIndex(linesOrCorner), (LineStyleImpl) lineStyle);
            }
            return this;
        }

        @Override
        public SelectedLinesAndCorners setCorner(CornerType corner) {
            for (LinesAndCorners linesOrCorner : linesAndCorners) {
                if (isCorner(linesOrCorner)) {
                    BorderStyleImpl.this.elementType.set(cornerToIndex(linesOrCorner), guiToImpl(corner));
                }
            }
            return this;
        }

        @Override
        public SelectedLinesAndCorners setRadiusX(double radiusX) {
            for (LinesAndCorners linesOrCorner : linesAndCorners) {
                if (isCorner(linesOrCorner)) {
                    BorderStyleImpl.this.radius.get(cornerToIndex(linesOrCorner)).setX(radiusX);
                }
            }
            return this;
        }

        @Override
        public SelectedLinesAndCorners setRadiusY(double radiusY) {
            for (LinesAndCorners linesOrCorner : linesAndCorners) {
                if (isCorner(linesOrCorner)) {
                    BorderStyleImpl.this.radius.get(cornerToIndex(linesOrCorner)).setY(radiusY);
                }
            }
            return this;
        }

        private boolean isCorner(LinesAndCorners linesOrCorner) {
            switch (linesOrCorner) {
                case UPPER_LEFT_CORNER:
                case UPPER_RIGHT_CORNER:
                case LOWER_RIGHT_CORNER:
                case LOWER_LEFT_CORNER:
                    return true;
                default:
                    return false;
            }
        }
    }
}
