package com.quadient.wfdxml.issue_with_generic;

// written by java.util.stream.BaseStream
public interface BaseInterface<S extends BaseInterface<S>> {
    S setBase(String base);
}
