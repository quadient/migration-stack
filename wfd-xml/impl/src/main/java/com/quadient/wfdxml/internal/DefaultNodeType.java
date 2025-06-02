package com.quadient.wfdxml.internal;

public enum DefaultNodeType {
    DN_FONT_GROUP(0),
    DN_COLOR_GROUP(1),
    DN_PARASTYLE_GROUP(2),
    DN_TEXTSTYLE_GROUP(3),

    DN_FONT(4),
    DN_COLOR(5),
    DN_PARASTYLE(6),
    DN_TEXTSTYLE(7),

    DN_IMAGES_GROUP(8),

    DN_FILLSTYLE_GROUP(9),
    DN_BLACK_FIll(10),

    DN_BORDERSTYLE_GROUP(11),
    DN_BORDERSTYLE(12),  ///

    DN_FLOW_GROUP(13),
    DN_FLOW_OBJECT_GROUP(14),

    DN_LAST_PRE_TABLE(15), ///
    DN_TABLE_GROUP(15),
    DN_ROWSET_GROUP(16),
    DN_CELL_GROUP(17),

    DN_LINESTYLE_GROUP(20),
    DN_OTHERS_GROUP(22),
    DN_ANCHORS_GROUP(23),

    DN_FORMCONTROL_GROUP(29),
    DN_FRAMESTYLES_GROUP(33),

    DN_PAGES(36),
    DN_DATA(37),
    DN_SYSTEM_VARIABLE(38);

    private final int designerOrder;

    DefaultNodeType(int designerOrder) {
        this.designerOrder = designerOrder;
    }
}