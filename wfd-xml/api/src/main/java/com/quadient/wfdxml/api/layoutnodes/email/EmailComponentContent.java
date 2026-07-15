package com.quadient.wfdxml.api.layoutnodes.email;

import com.quadient.wfdxml.api.Node;
import com.quadient.wfdxml.api.layoutnodes.FillStyle;
import com.quadient.wfdxml.api.layoutnodes.data.Variable;

public interface EmailComponentContent extends Node<EmailComponentContent> {

    EmailComponentContent setType(ContentType type);

    EmailComponentContent setContent(Node node);

    EmailComponentContent setPaddingLeft(double paddingLeft);

    EmailComponentContent setPaddingTop(double paddingTop);

    EmailComponentContent setPaddingRight(double paddingRight);

    EmailComponentContent setPaddingBottom(double paddingBottom);

    EmailComponentContent setFillStyle(FillStyle fillStyle);

    EmailComponentContent setAlternateTextVariable(Variable variable);

    EmailComponentContent setLinkUrl(Variable variable);

    EmailComponentContent setHtmlWidthValue(double htmlWidthValue);

    EmailComponentContent setHorizontalAlignment(HorizontalAlignment alignment);

    enum ContentType {
        TEXT,
        IMAGE,
        EXTERNAL_IMAGE,
    }

    enum HorizontalAlignment {
        LEFT,
        CENTER,
        RIGHT,
    }
}
