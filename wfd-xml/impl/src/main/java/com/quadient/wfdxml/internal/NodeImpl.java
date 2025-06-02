package com.quadient.wfdxml.internal;

import com.quadient.wfdxml.api.Node;
import com.quadient.wfdxml.internal.xml.export.XmlExportable;

public abstract class NodeImpl<S extends Node<S>> implements Node<S>, XmlExportable {

    private String name;
    private String comment;
    private String id;

    @Override
    public String getName() {
        return name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setName(String name) {
        this.name = name;
        return (S) this;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setComment(String comment) {
        this.comment = comment;
        return (S) this;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setId(String id) {
        this.id = id;
        return (S) this;
    }

    public String getXmlElementName() {
        throw new UnsupportedOperationException("Not supported operation on class '" + this.getClass().getName() + "'");
    }

}
