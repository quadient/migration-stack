package com.quadient.wfdxml.api.module;

import com.quadient.wfdxml.api.Node;

public interface WorkFlowModule<S extends Node<S>> extends Node<S> {

    S setPosX(int x);

    S setPosY(int y);
}
