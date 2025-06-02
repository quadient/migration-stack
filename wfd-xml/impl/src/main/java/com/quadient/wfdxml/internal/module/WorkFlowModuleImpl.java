package com.quadient.wfdxml.internal.module;

import com.quadient.wfdxml.api.Node;
import com.quadient.wfdxml.api.module.WorkFlowModule;
import com.quadient.wfdxml.internal.Tree;

public abstract class WorkFlowModuleImpl<S extends Node<S>> extends Tree<S> implements WorkFlowModule<S> {
    private int posX = 0;
    private int posY = 0;

    @Override
    @SuppressWarnings("unchecked")
    public S setPosX(int posX) {
        this.posX = posX;
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setPosY(int posY) {
        this.posY = posY;
        return (S) this;
    }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }
}
