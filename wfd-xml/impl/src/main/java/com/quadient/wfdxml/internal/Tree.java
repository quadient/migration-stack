package com.quadient.wfdxml.internal;

import com.quadient.wfdxml.api.Node;

import java.util.ArrayList;
import java.util.List;

public abstract class Tree<S extends Node<S>> extends NodeImpl<S> {
    public List<NodeImpl> children = new ArrayList<>();
}
