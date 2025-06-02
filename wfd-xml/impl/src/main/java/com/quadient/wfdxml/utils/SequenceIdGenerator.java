package com.quadient.wfdxml.utils;

public class SequenceIdGenerator {

    private int i;

    private String prefix = "SeqIG_";

    public SequenceIdGenerator() {
        reset();
    }

    public SequenceIdGenerator(String prefix) {
        this();
        this.prefix = prefix;
    }

    synchronized public String generateId() {
        return prefix + i++;
    }

    synchronized public void reset() {
        i = 1;
    }

}
