package com.quadient.wfdxml.api.layoutnodes.email;

import com.quadient.wfdxml.api.Node;
import com.quadient.wfdxml.api.layoutnodes.Flow;

public interface EmailComponentPlaceHolder extends Node<EmailComponentPlaceHolder> {

    EmailComponentPlaceHolder setContent(Flow flow);

    EmailComponentPlaceHolder setType(Type type);

    enum Type {
        HEADER,
        BODY,
        FOOTER,
    }
}
