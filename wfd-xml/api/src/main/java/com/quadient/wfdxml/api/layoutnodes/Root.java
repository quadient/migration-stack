package com.quadient.wfdxml.api.layoutnodes;

public interface Root {
    Root setAllowRuntimeModifications(boolean allowRuntimeModifications);
    Root setExternalStylesLayout(String vcsLocation);
    Root setSubject(String subject);
}