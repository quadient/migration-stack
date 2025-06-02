package com.quadient.wfdxml.internal.layoutnodes.tables;

import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle;
import com.quadient.wfdxml.api.layoutnodes.tables.RowSet;
import com.quadient.wfdxml.api.layoutnodes.tables.Table;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import java.util.ArrayList;
import java.util.List;

import static com.quadient.wfdxml.api.layoutnodes.tables.Table.BordersType.MERGE_BORDERS;
import static com.quadient.wfdxml.api.layoutnodes.tables.Table.EditabilityType.LABEL_AND_LOCK;
import static com.quadient.wfdxml.api.layoutnodes.tables.Table.TableAlignment.LEFT;

public class TableImpl extends NodeImpl<Table> implements Table {

    private final List<Column> columns = new ArrayList<>();
    private RowSet rowSet = null;
    private BorderStyle border = null;
    private EditabilityType editabilityType = LABEL_AND_LOCK;
    private double horizontalCellSpacing = 0;
    private double verticalCellSpacing = 0;
    private double minWidth = 0.001;
    private double maxWidth = 300.0;
    private double percentWidth = 100;
    private double spaceLeft = 0;
    private double spaceTop = 0;
    private double spaceRight = 0;
    private double spaceBottom = 0;
    private TableAlignment tableAlignment = LEFT;
    private boolean useColumnWidths = true;
    private BordersType bordersType = MERGE_BORDERS;
    private boolean includeLineGap = false;
    private boolean displayAsImage = true;
    private boolean htmlFormatting = false;
    private boolean responsiveHtml = false;

    public RowSet getRowSet() {
        return rowSet;
    }

    @Override
    public TableImpl setRowSet(RowSet rowSet) {
        this.rowSet = rowSet;
        return this;
    }

    public BorderStyle getBorder() {
        return border;
    }

    public TableImpl setBorder(BorderStyle border) {
        this.border = border;
        return this;
    }

    @Override
    public TableImpl setBorderStyle(BorderStyle borderStyle) {
        return setBorder(borderStyle);
    }

    @Override
    public TableImpl addColumn() {
        return addColumn(0.001, 0.1, null, false);
    }

    @Override
    public TableImpl addColumn(double minWidth, double sizeRatio) {
        return addColumn(minWidth, sizeRatio, null, false);
    }

    @Override
    public TableImpl addColumn(double minWidth, double sizeRatio, Variable enableBy, boolean header) {
        columns.add(new Column(minWidth, sizeRatio, enableBy, header));
        return this;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public EditabilityType getEditabilityType() {
        return editabilityType;
    }

    public TableImpl setEditabilityType(EditabilityType editabilityType) {
        this.editabilityType = editabilityType;
        return this;
    }

    @Override
    public Table setEditability(EditabilityType editability) {
        return setEditabilityType(editability);
    }

    public double getHorizontalCellSpacing() {
        return horizontalCellSpacing;
    }

    public TableImpl setHorizontalCellSpacing(double horizontalCellSpacing) {
        this.horizontalCellSpacing = horizontalCellSpacing;
        return this;
    }

    @Override
    public TableImpl setHorizontalSpacing(double horizontalSpacing) {
        return setHorizontalCellSpacing(horizontalSpacing);
    }

    public double getVerticalCellSpacing() {
        return verticalCellSpacing;
    }

    public TableImpl setVerticalCellSpacing(double verticalCellSpacing) {
        this.verticalCellSpacing = verticalCellSpacing;
        return this;
    }

    @Override
    public TableImpl setVerticalSpacing(double verticalSpacing) {
        return setVerticalCellSpacing(verticalSpacing);
    }

    public double getMinWidth() {
        return minWidth;
    }

    @Override
    public TableImpl setMinWidth(double minWidth) {
        this.minWidth = minWidth;
        return this;
    }

    @Override
    public TableImpl setMinWidthDisable() {
        return setMinWidth(0);
    }

    public double getMaxWidth() {
        return maxWidth;
    }

    @Override
    public TableImpl setMaxWidth(double maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    @Override
    public TableImpl setMaxWidthDisable() {
        return setMaxWidth(0);
    }

    public double getPercentWidth() {
        return percentWidth;
    }

    @Override
    public TableImpl setPercentWidth(double percentWidth) {
        this.percentWidth = percentWidth;
        return this;
    }

    @Override
    public TableImpl setPercentWidthDisable() {
        return setPercentWidth(-1);
    }

    public double getSpaceLeft() {
        return spaceLeft;
    }

    @Override
    public TableImpl setSpaceLeft(double spaceLeft) {
        this.spaceLeft = spaceLeft;
        return this;
    }

    public double getSpaceTop() {
        return spaceTop;
    }

    @Override
    public TableImpl setSpaceTop(double spaceTop) {
        this.spaceTop = spaceTop;
        return this;
    }

    public double getSpaceRight() {
        return spaceRight;
    }

    @Override
    public TableImpl setSpaceRight(double spaceRight) {
        this.spaceRight = spaceRight;
        return this;
    }

    public double getSpaceBottom() {
        return spaceBottom;
    }

    @Override
    public TableImpl setSpaceBottom(double spaceBottom) {
        this.spaceBottom = spaceBottom;
        return this;
    }

    public TableAlignment getTableAlignment() {
        return tableAlignment;
    }

    public TableImpl setTableAlignment(TableAlignment tableAlignment) {
        this.tableAlignment = tableAlignment;
        return this;
    }

    @Override
    public TableImpl setAlignment(TableAlignment alignment) {
        return setTableAlignment(alignment);
    }

    public boolean isUseColumnWidths() {
        return useColumnWidths;
    }

    public TableImpl setUseColumnWidths(boolean useColumnWidths) {
        this.useColumnWidths = useColumnWidths;
        return this;
    }

    public BordersType getBordersType() {
        return bordersType;
    }

    public TableImpl setBordersType(BordersType bordersType) {
        this.bordersType = bordersType;
        return this;
    }

    @Override
    public TableImpl setBorderType(BordersType borderType) {
        return setBordersType(borderType);
    }

    public boolean isIncludeLineGap() {
        return includeLineGap;
    }

    @Override
    public TableImpl setIncludeLineGap(boolean includeLineGap) {
        this.includeLineGap = includeLineGap;
        return this;
    }

    public boolean isDisplayAsImage() {
        return displayAsImage;
    }

    @Override
    public TableImpl setDisplayAsImage(boolean displayAsImage) {
        this.displayAsImage = displayAsImage;
        return this;
    }

    public boolean isHtmlFormatting() {
        return htmlFormatting;
    }

    public TableImpl setHtmlFormatting(boolean htmlFormatting) {
        this.htmlFormatting = htmlFormatting;
        return this;
    }

    @Override
    public TableImpl setApplyHtmlFormatting(boolean applyHtmlFormatting) {
        return setHtmlFormatting(applyHtmlFormatting);
    }

    public boolean isResponsiveHtml() {
        return responsiveHtml;
    }

    @Override
    public TableImpl setResponsiveHtml(boolean responsiveHtml) {
        this.responsiveHtml = responsiveHtml;
        return this;
    }


    @Override
    public String getXmlElementName() {
        return "Table";
    }

    @Override
    public void export(XmlExporter exporter) {
        exporter.addElementWithIface("RowSetId", rowSet);
        exporter.addElementWithIface("BorderId", border);
        exporter.addElementWithDoubleData("HorizontalCellSpacing", horizontalCellSpacing);
        exporter.addElementWithDoubleData("VerticalCellSpacing", verticalCellSpacing);
        exporter.addElementWithDoubleData("MinWidth", minWidth);
        exporter.addElementWithDoubleData("MaxWidth", maxWidth);
        exporter.addElementWithDoubleData("PercentWidth", percentWidth);
        exporter.addElementWithDoubleData("SpaceLeft", spaceLeft);
        exporter.addElementWithDoubleData("SpaceTop", spaceTop);
        exporter.addElementWithDoubleData("SpaceRight", spaceRight);
        exporter.addElementWithDoubleData("SpaceBottom", spaceBottom);

        switch (tableAlignment) {
            case LEFT:
                exporter.addElementWithStringData("TableAlignment", "Left");
                break;
            case RIGHT:
                exporter.addElementWithStringData("TableAlignment", "Right");
                break;
            case CENTER:
                exporter.addElementWithStringData("TableAlignment", "Center");
                break;
            case INHERIT:
                exporter.addElementWithStringData("TableAlignment", "Inherit");
                break;
            default:
                throw new IllegalStateException(tableAlignment.toString());
        }
        switch (bordersType) {
            case SIMPLE:
                exporter.addElementWithStringData("BordersType", "Simple");
                break;
            case MERGE_BORDERS:
                exporter.addElementWithStringData("BordersType", "MergeBorders");
                break;
            default:
                throw new IllegalStateException(bordersType.toString());
        }
        exporter.addElementWithBoolData("IncludeLineGap", includeLineGap);
        exporter.addElementWithBoolData("UseColumnWidths", useColumnWidths);
        for (Column column : columns) {
            exporter.beginElement("ColumnWidths");
            exporter.addElementWithDoubleData("MinWidth", column.minWidth);
            exporter.addElementWithDoubleData("PercentWidth", column.percentWidth);
            exporter.endElement();
        }

        exporter.addElementWithBoolData("HTMLFormatting", htmlFormatting);
        exporter.addElementWithBoolData("DisplayAsImage", displayAsImage);

        for (Column column : columns) {
            exporter.addElementWithIface("EnableById", column.enableBy);
        }

        exporter.addElementWithBoolData("ResponsiveHtml", responsiveHtml);
        {
            switch (editabilityType) {
                case LABEL_AND_LOCK:
                    exporter.addElementWithStringData("Editability", "LabelAndLock");
                    break;
                case LOCK:
                    exporter.addElementWithStringData("Editability", "Lock");
                    break;
                default:
                    throw new IllegalStateException(editabilityType.toString());
            }
        }

        for (Column column : columns) {
            exporter.addElementWithBoolData("IsHeader", column.header);
        }
    }
}