package com.quadient.wfdxml.api.layoutnodes;

import com.quadient.wfdxml.api.Node;

public interface Font extends Node<Font> {
    Font setFont(String font);

    Font setFontFromDiskLocation(String fontPath);
}
