package com.quadient.wfdxml.api.layoutnodes.email;

import com.quadient.wfdxml.api.Node;
import com.quadient.wfdxml.api.layoutnodes.FillStyle;

import java.util.ArrayList;
import java.util.List;

public interface EmailComponentGrid extends Node<EmailComponentGrid> {

    Column addColumn();

    EmailComponentGrid setFullWidthBackground(boolean fullWidthBackground);

    EmailComponentGrid setDistribution(ColumnDistribution distribution);

    EmailComponentGrid setVerticalAlignment(VerticalAlignment verticalAlignment);

    EmailComponentGrid setOnMobile(OnMobile onMobile);

    EmailComponentGrid setPaddingLeft(double paddingLeft);

    EmailComponentGrid setPaddingTop(double paddingTop);

    EmailComponentGrid setPaddingRight(double paddingRight);

    EmailComponentGrid setPaddingBottom(double paddingBottom);

    EmailComponentGrid setFillStyle(FillStyle fillStyle);

    class Column {
        private final List<EmailComponentContent> content = new ArrayList<>();

        public Column addContent(EmailComponentContent component) {
            content.add(component);
            return this;
        }

        public List<EmailComponentContent> getContent() {
            return content;
        }
    }

    enum VerticalAlignment {
        TOP,
        BOTTOM,
        CENTER,
    }

    enum OnMobile {
        FROM_LEFT,
        FROM_RIGHT,
        NO_STACKING,
    }

    enum ColumnDistribution {
        EVEN_WIDTH,
        TWO_COLUMNS_25_75,
        TWO_COLUMNS_33_66,
        TWO_COLUMNS_66_33,
        TWO_COLUMNS_75_25,
        THREE_COLUMNS_25_25_50,
        THREE_COLUMNS_25_50_25,
        THREE_COLUMNS_50_25_25,
    }
}
