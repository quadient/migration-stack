package com.quadient.wfdxml.api.layoutnodes;

import com.quadient.wfdxml.api.Node;

import java.util.List;

public interface Root {
    Root setAllowRuntimeModifications(boolean allowRuntimeModifications);
    Root setExternalStylesLayout(String vcsLocation);
    Root setSubject(String subject);

    Root addLockedWebNode(Node<?> node);
    List<Node<?>> getLockedWebNodes();
}