package com.quadient.wfdxml.api.layoutnodes;

import com.quadient.wfdxml.api.Node;
import com.quadient.wfdxml.api.layoutnodes.font.SubFont;

import java.util.List;

public interface Font extends Node<Font> {
    Font setFontName(String fontName);

    SubFont addSubfont();

    List<SubFont> getSubFonts();
}
