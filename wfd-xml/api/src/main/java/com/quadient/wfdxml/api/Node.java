package com.quadient.wfdxml.api;

public interface Node<S extends Node<S>> {
    String getName();

    S setName(String name);

    String getComment();

    S setComment(String comment);

    String getId();

    S setId(String id);
}
