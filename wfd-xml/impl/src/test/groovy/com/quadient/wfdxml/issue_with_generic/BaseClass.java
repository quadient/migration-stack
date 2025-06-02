package com.quadient.wfdxml.issue_with_generic;

// written by java.util.stream.AbstractPipeline
public class BaseClass<S extends BaseInterface<S>> implements BaseInterface<S> {

    @Override
    @SuppressWarnings("unchecked")
    public S setBase(String base) {
        return (S) this;
    }
}
