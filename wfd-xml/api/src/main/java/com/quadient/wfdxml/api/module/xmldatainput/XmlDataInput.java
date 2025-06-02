package com.quadient.wfdxml.api.module.xmldatainput;

import com.quadient.wfdxml.api.data.DataDefinition;
import com.quadient.wfdxml.api.module.WorkFlowModule;

public interface XmlDataInput extends WorkFlowModule<XmlDataInput> {

    XmlDataInput setDiskLocation(String location);

    XmlDataInput setFileLocation(LocationType type, String filePath);

    XmlNode addRootXmlNode();

    XmlDataInput setCreateByScript(boolean createByScript);

    /**
     * Note that line ends have to be in "Windows style" = "\r\n"
     */
    XmlDataInput setScript(String script);

    XmlDataInput allowAnyOrderOfElements(boolean allowAnyOrderOfElements);

    DataDefinition generateDataDefinition();
}