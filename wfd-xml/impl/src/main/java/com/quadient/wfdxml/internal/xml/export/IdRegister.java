package com.quadient.wfdxml.internal.xml.export;

import com.quadient.wfdxml.utils.SequenceIdGenerator;

import java.util.HashMap;
import java.util.Map;

public class IdRegister {
    private final Map<Integer, String> register = new HashMap<>();
    private final SequenceIdGenerator idGenerator = new SequenceIdGenerator("SR_");

    public void setObjectId(Object obj, String id) {
        int objIdentifier = getObjectIdentifier(obj);
        register.put(objIdentifier, id);
    }

    public String getOrCreateId(Object obj) {
        if (obj == null) {
            throw new NullPointerException("Its not possible to get Id from null object.");
        }
        int identity = getObjectIdentifier(obj);
        String id = register.get(identity);
        if (id == null) {
            id = idGenerator.generateId();
            register.put(identity, id);
        }
        return id;
    }

    private int getObjectIdentifier(Object obj) {
        return System.identityHashCode(obj);
    }
}