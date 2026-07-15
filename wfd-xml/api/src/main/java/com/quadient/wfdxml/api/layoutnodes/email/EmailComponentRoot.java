package com.quadient.wfdxml.api.layoutnodes.email;

import com.quadient.wfdxml.api.Node;
import com.quadient.wfdxml.api.layoutnodes.FillStyle;

public interface EmailComponentRoot extends Node<EmailComponentRoot> {
    EmailComponentRoot setWidth(double width);

    EmailComponentRoot setEmailComponentsText(TMText flow);

    EmailComponentRoot setFill(FillStyle SFillStyle);
}