package com.quadient.wfdxml.api;

import java.util.Map;

public interface Node<S extends Node<S>> {
    String getName();

    S setName(String name);

    String getComment();

    S setComment(String comment);

    String getId();

    S setId(String id);

    String getDisplayName();

    S setDisplayName(String displayName);

    Map<String, Object> getCustomProperties();

    S addCustomProperty(String key, Object value);
}
