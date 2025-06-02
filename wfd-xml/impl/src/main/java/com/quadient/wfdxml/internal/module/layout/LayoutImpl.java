package com.quadient.wfdxml.internal.module.layout;

import com.quadient.wfdxml.api.layoutnodes.Element;
import com.quadient.wfdxml.api.layoutnodes.Root;
import com.quadient.wfdxml.api.layoutnodes.TextStyle;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;
import com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle;
import com.quadient.wfdxml.api.layoutnodes.tables.HeaderFooterRowSet;
import com.quadient.wfdxml.api.module.Layout;
import com.quadient.wfdxml.internal.DefaultNodeType;
import com.quadient.wfdxml.internal.Group;
import com.quadient.wfdxml.internal.NodeImpl;
import com.quadient.wfdxml.internal.Tree;
import com.quadient.wfdxml.internal.layoutnodes.BorderStyleImpl;
import com.quadient.wfdxml.internal.layoutnodes.ColorImpl;
import com.quadient.wfdxml.internal.layoutnodes.FillStyleImpl;
import com.quadient.wfdxml.internal.layoutnodes.FlowImpl;
import com.quadient.wfdxml.internal.layoutnodes.FlowObjectImpl;
import com.quadient.wfdxml.internal.layoutnodes.FontImpl;
import com.quadient.wfdxml.internal.layoutnodes.ImageImpl;
import com.quadient.wfdxml.internal.layoutnodes.LineStyleImpl;
import com.quadient.wfdxml.internal.layoutnodes.NumberedListImpl;
import com.quadient.wfdxml.internal.layoutnodes.PageImpl;
import com.quadient.wfdxml.internal.layoutnodes.PagesImpl;
import com.quadient.wfdxml.internal.layoutnodes.ParagraphStyleImpl;
import com.quadient.wfdxml.internal.layoutnodes.RootImpl;
import com.quadient.wfdxml.internal.layoutnodes.TextStyleImpl;
import com.quadient.wfdxml.internal.layoutnodes.data.DataImpl;
import com.quadient.wfdxml.internal.layoutnodes.tables.CellImpl;
import com.quadient.wfdxml.internal.layoutnodes.tables.RowSetImpl;
import com.quadient.wfdxml.internal.layoutnodes.tables.TableImpl;
import com.quadient.wfdxml.internal.module.WorkFlowModuleImpl;
import com.quadient.wfdxml.internal.xml.export.XmlExporter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.quadient.wfdxml.internal.DefaultNodeType.DN_ANCHORS_GROUP;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_BLACK_FIll;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_BORDERSTYLE;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_BORDERSTYLE_GROUP;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_CELL_GROUP;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_COLOR;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_COLOR_GROUP;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_DATA;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_FILLSTYLE_GROUP;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_FLOW_GROUP;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_FLOW_OBJECT_GROUP;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_FONT;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_FONT_GROUP;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_FORMCONTROL_GROUP;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_FRAMESTYLES_GROUP;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_IMAGES_GROUP;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_LINESTYLE_GROUP;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_OTHERS_GROUP;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_PAGES;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_PARASTYLE;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_PARASTYLE_GROUP;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_ROWSET_GROUP;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_SYSTEM_VARIABLE;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_TABLE_GROUP;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_TEXTSTYLE;
import static com.quadient.wfdxml.internal.DefaultNodeType.DN_TEXTSTYLE_GROUP;

public class LayoutImpl extends WorkFlowModuleImpl<Layout> implements Layout {
    private final Map<DefaultNodeType, DefNode> defNodes = new HashMap<>();
    private final Map<String, NodeImpl> defObject = new HashMap<>();
    private RootImpl root;
    private DataImpl data;
    private PagesImpl pages;

    private final List<String> layoutDeltaAllowedGroups = List.of("Flows", "Tables", "RowSets", "Cells", "Data", "Images");

    public LayoutImpl() {
        initializeDefaultNodes();
    }

    private void initializeDefaultNodes() {
        Group defColorGroup = createObjectGroup("Colors", "Def.ColorGroup", DN_COLOR_GROUP);
        ColorImpl defColor = new ColorImpl();
        addDefaultNode(defColor, "Black", "Def.Color", DN_COLOR, defColorGroup);

        Group defFillGroup = createObjectGroup("FillStyles", "Def.FillStyleGroup", DN_FILLSTYLE_GROUP);
        FillStyleImpl defBlackFill = new FillStyleImpl();
        defBlackFill.setColor(defColor);
        addDefaultNode(defBlackFill, "BlackFill", "Def.BlackFill", DN_BLACK_FIll, defFillGroup);

        Group defFontGroup = createObjectGroup("Fonts", "Def.FontGroup", DN_FONT_GROUP);
        FontImpl defFont = new FontImpl();
        addDefaultNode(defFont, "Arial", "Def.Font", DN_FONT, defFontGroup);

        Group defTextStyleGroup = createObjectGroup("TextStyles", "Def.TextStyleGroup", DN_TEXTSTYLE_GROUP);
        TextStyleImpl defTextStyle = new TextStyleImpl();
        defTextStyle.setFillStyle(defBlackFill).setFont(defFont);
        defTextStyle.setAncestorId(null);
        addDefaultNode(defTextStyle, "Normal", "Def.TextStyle", DN_TEXTSTYLE, defTextStyleGroup);

        Group defParaStyleGroup = createObjectGroup("ParagraphStyles", "Def.ParaStyleGroup", DN_PARASTYLE_GROUP);
        ParagraphStyleImpl defParaStyle = new ParagraphStyleImpl();
        defParaStyle.setTextStyle(defTextStyle);
        defParaStyle.setAncestorId(null);
        addDefaultNode(defParaStyle, "Normal", "Def.ParaStyle", DN_PARASTYLE, defParaStyleGroup);

        Group defFlowGroup = createObjectGroup("Flows", "Def.FlowGroup", DN_FLOW_GROUP);
        Group defImagesGroup = createObjectGroup("Images", "Def.ImagesGroup", DN_IMAGES_GROUP);

        Group defBorderGroup = createObjectGroup("BorderStyles", "Def.BorderStyleGroup", DN_BORDERSTYLE_GROUP);
        BorderStyleImpl defBorderStyle = new BorderStyleImpl();
        defBorderStyle.setFillStyle(defBlackFill);
        addDefaultNode(defBorderStyle, "BorderStyle", "Def.BorderStyle", DN_BORDERSTYLE, defBorderGroup);

        Group defLineGroup = createObjectGroup("LineStyles", "Def.LineStyleGroup", DN_LINESTYLE_GROUP);
        Group defFlowObjectGroup = createObjectGroup("FlowObjects", "Def.FlowObjectGroup", DN_FLOW_OBJECT_GROUP);
        Group defTablesGroup = createObjectGroup("Tables", "Def.TableGroup", DN_TABLE_GROUP);
        Group defRowSet = createObjectGroup("RowSets", "Def.RowSetGroup", DN_ROWSET_GROUP);
        Group defCellGroup = createObjectGroup("Cells", "Def.CellGroup", DN_CELL_GROUP);
        Group defAnchor = createObjectGroup("Anchors", "Def.AnchorsGroup", DN_ANCHORS_GROUP);
        Group defFormControl = createObjectGroup("FormControls", "Def.FormControlGroup", DN_FORMCONTROL_GROUP);
        Group defFrameStyles = createObjectGroup("FrameStyles", "Def.FrameStyleGroup", DN_FRAMESTYLES_GROUP);
        Group defOthers = createObjectGroup("Others", "Def.OthersGroup", DN_OTHERS_GROUP);

        createPages();
        createData();
    }

    private Group createObjectGroup(String groupName, String xmlId, DefaultNodeType groupType) {
        Group group = new Group();
        group.setName(groupName);
        defNodes.put(groupType, new DefNode(group, xmlId));
        children.add(group);
        return group;
    }

    private void createPages() {
        pages = new PagesImpl();
        pages.setName("Pages");
        defNodes.put(DN_PAGES, new DefNode(pages, "Def.Pages"));
        children.add(pages);
    }

    private void createData() {
        data = new DataImpl();
        data.setName("Data");
        defNodes.put(DN_DATA, new DefNode(data, "Def.Data"));
        children.add(data);
        defNodes.put(DN_SYSTEM_VARIABLE, new DefNode(data.getSystemVariableArray(), "Def.SystemVariables"));
    }

    private void addDefaultNode(NodeImpl node, String objectName, String xmlId, DefaultNodeType type, Group group) {
        node.setName(objectName);
        //noinspection unchecked
        group.children.add(node);
        defNodes.put(type, new DefNode(node, xmlId));
        defObject.put(xmlId, node);
    }

    @Override
    public ColorImpl addColor() {
        ColorImpl color = new ColorImpl();
        addNodeToDefGroup(DN_COLOR_GROUP, color);
        return color;
    }

    @Override
    public FillStyleImpl addFillStyle() {
        FillStyleImpl fillStyle = new FillStyleImpl();
        fillStyle.setColor(getDefColor());
        addNodeToDefGroup(DN_FILLSTYLE_GROUP, fillStyle);
        return fillStyle;
    }

    @Override
    public FontImpl addFont() {
        FontImpl font = new FontImpl();
        addNodeToDefGroup(DN_FONT_GROUP, font);
        return font;
    }

    @Override
    public BorderStyle addBorderStyle() {
        BorderStyleImpl borderStyle = new BorderStyleImpl();
        addNodeToDefGroup(DN_BORDERSTYLE_GROUP, borderStyle);
        return borderStyle;
    }

    @Override
    public TextStyleImpl addTextStyle() {
        TextStyleImpl textStyle = new TextStyleImpl();
        addNodeToDefGroup(DN_TEXTSTYLE_GROUP, textStyle);
        return textStyle;
    }

    @Override
    public ParagraphStyleImpl addParagraphStyle() {
        ParagraphStyleImpl paragraphStyle = new ParagraphStyleImpl();
        addNodeToDefGroup(DN_PARASTYLE_GROUP, paragraphStyle);
        return paragraphStyle;
    }

    @Override
    public FlowImpl addFlow() {
        FlowImpl flow = new FlowImpl();
        addNodeToDefGroup(DN_FLOW_GROUP, flow);
        return flow;
    }

    @Override
    public Element addElement() {
        FlowObjectImpl element = new FlowObjectImpl(this);
        addNodeToDefGroup(DN_FLOW_OBJECT_GROUP, element);
        return element;
    }

    @Override
    public Root addRoot() {
        root = new RootImpl();
        return root;
    }

    @Override
    public ParagraphStyleImpl addBulletParagraph(TextStyle textStyle, String bullet) {
        return addParagraphStyle().setBulletsNumberingFlow(addBulletFlow(textStyle, bullet));
    }

    @Override
    public ParagraphStyleImpl addNumberingParagraph(TextStyle textStyle, String bullet_suffix, Variable variable) {
        return addParagraphStyle().setBulletsNumberingFlow(addBulletFlow(textStyle, bullet_suffix, variable));
    }

    private FlowImpl addBulletFlow(TextStyle textStyle, String str) {
        FlowImpl flow = addFlow();
        flow.addParagraph().addText().appendText(str).setTextStyle(textStyle);
        return flow;
    }

    private FlowImpl addBulletFlow(TextStyle textStyle, String str, Variable variable) {
        FlowImpl flow = addFlow();
        flow.addParagraph().addText().appendVariable(variable).appendText(str).setTextStyle(textStyle);
        return flow;
    }

    @Override
    public TableImpl addTable() {
        TableImpl table = new TableImpl();
        addNodeToDefGroup(DN_TABLE_GROUP, table);
        return table;
    }

    @Override
    public RowSetImpl addRowSet() {
        RowSetImpl rowSet = new RowSetImpl();
        addNodeToDefGroup(DN_ROWSET_GROUP, rowSet);
        return rowSet;
    }

    @Override
    public HeaderFooterRowSet addRowSetHeaderFooter() {
        return addRowSet().initAsHeaderFooterRowSet();
    }

    @Override
    public CellImpl addCell() {
        CellImpl cell = new CellImpl();
        addNodeToDefGroup(DN_CELL_GROUP, cell);
        return cell;
    }

    @Override
    public PageImpl addPage() {
        PageImpl page = new PageImpl(this);
        addNodeToDefGroup(DN_PAGES, page);
        return page;
    }

    @Override
    public ImageImpl addImage() {
        ImageImpl image = new ImageImpl();
        addNodeToDefGroup(DN_IMAGES_GROUP, image);
        return image;
    }

    @Override
    public LineStyleImpl addLineStyle() {
        LineStyleImpl lineStyle = new LineStyleImpl();
        addNodeToDefGroup(DN_LINESTYLE_GROUP, lineStyle);
        return lineStyle;
    }

    private void addNodeToDefGroup(DefaultNodeType nodeType, NodeImpl c) {
        //noinspection unchecked
        ((Tree) (defNodes.get(nodeType).node)).children.add(c);
    }


    @Override
    public void export(XmlExporter exporter) {
        for (DefNode defNode : defNodes.values()) {
            exporter.getIdRegister().setObjectId(defNode.node, defNode.xmlId);
        }

        exporter.beginElement("Layout")
                .addElementWithIface("Id", this)
                .addElementWithStringData("Name", getName())
                .beginElement("ModulePos")
                .addIntAttribute("X", getPosX())
                .addIntAttribute("Y", getPosY())
                .endElement()
                .beginElement("Layout");

        if (root != null) {
            exporter.beginElement("Root");
            root.export(exporter);
            exporter.endElement();
        }

        new ForwardReferencesExporter(this, defNodes, exporter).exportForwardReferences();
        exportNodes(exporter);

        exporter.endElement();
        exporter.endElement();
    }

    public void exportLayoutDelta(XmlExporter exporter) {
        for (DefNode defNode : defNodes.values()) {
            exporter.getIdRegister().setObjectId(defNode.node, defNode.xmlId);
        }

        exporter.beginElement("Layout");

        children = children.stream()
                .filter(child -> layoutDeltaAllowedGroups.contains(child.getName()))
                .map(child -> {
                    if (child.getName().equals("Data") && child instanceof Tree<?> dataNode) {
                        dataNode.children = dataNode.children.stream().filter(dataChild -> dataChild.getName() == null || !dataChild.getName().equals("SystemVariable")).toList();
                        return dataNode;
                    }
                    return child;
                }).collect(Collectors.toList());

        new ForwardReferencesExporter(this, defNodes, exporter).exportForwardReferences(true);
        exportNodes(exporter);

        exporter.endElement();
    }

    private void exportNodes(XmlExporter exporter) {
        exportTree(this, exporter);
    }

    private void exportTree(Tree tree, XmlExporter exporter) {
        for (Object c : tree.children) {
            NodeImpl child = (NodeImpl) c;
            if (!(child instanceof Group)) {
                exporter.beginElement(child.getXmlElementName());
                exporter.addElementWithIface("Id", child);
                child.export(exporter);
                exporter.endElement();
            }
            if (child instanceof Tree) {
                exportTree((Tree) child, exporter);
            }

        }
    }

    @Override
    public DataImpl getData() {
        return data;
    }

    @Override
    public PagesImpl getPages() {
        return pages;
    }

    @Override
    public NumberedListImpl addNumberedList() {
        return new NumberedListImpl(this);
    }

    public ColorImpl getDefColor() {
        return (ColorImpl) defObject.get("Def.Color");
    }

    public FillStyleImpl getDefFillStyle() {
        return (FillStyleImpl) defObject.get("Def.BlackFill");
    }

    public TextStyleImpl getDefTextStyle() {
        return (TextStyleImpl) defObject.get("Def.TextStyle");
    }
}