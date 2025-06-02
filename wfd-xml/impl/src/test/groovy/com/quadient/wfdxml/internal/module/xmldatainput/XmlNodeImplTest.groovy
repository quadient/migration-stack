package com.quadient.wfdxml.internal.module.xmldatainput

import com.quadient.wfdxml.api.module.xmldatainput.XmlNode
import com.quadient.wfdxml.api.module.xmldatainput.XmlNodeChange
import com.quadient.wfdxml.api.module.xmldatainput.XmlNodeOptionality
import com.quadient.wfdxml.api.module.xmldatainput.XmlNodeType
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.api.data.converters.Converters.boolConverter
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeChange.FLATTEN
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeChange.IGNORE
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeChange.NONE
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeOptionality.ONE
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeOptionality.ONE_OR_MORE
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeOptionality.ZERO_OR_MORE
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeType.ATTRIBUTE
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeType.ELEMENT
import static com.quadient.wfdxml.api.module.xmldatainput.XmlNodeType.PC_DATA
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEquals
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class XmlNodeImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "test export empty XmlNode"() {
        given:
        def xmlNode = new XmlNodeImpl()

        when:
        xmlNode.export(exporter)

        then:
        assertXmlEquals(exporter.buildString(), """
           <XMLNode Type="Element" Level="1" Optionality="One" Change="None" Name="NewNode" XMLName="NewNode" />
        """)
    }

    def "test export XmlNode with all values"() {
        given:
        def xmlNode = new XmlNodeImpl()
                .setName("TestName")
                .setType(PC_DATA)
                .setOptionality(ZERO_OR_MORE)
                .setChange(IGNORE)
                .setXmlName("TestXmlName")


        when:
        xmlNode.export(exporter)

        then:
        assertXmlEquals(exporter.buildString(), """
           <XMLNode Type="PCData" Level="1" Optionality="ZeroOrMore" Change="Ignore" Name="TestName" XMLName="TestXmlName" />
        """)
    }

    def "test export XmlNode with Bool Field Converter"() {
        given:
        def xmlNode = new XmlNodeImpl().setFieldConverter(boolConverter())

        when:
        xmlNode.export(exporter)

        then:
        assertXmlEquals(exporter.buildString(), """
           <XMLNode Type="Element" Level="1" Optionality="One" Change="None" Name="NewNode" XMLName="NewNode">
               <FCV FCVClassName="BT2BTFCV">
                   <FCVProps>
                       <InputType>String</InputType>
                       <OutputType>Bool</OutputType>
                   </FCVProps>
               </FCV>
           </XMLNode>
        """)
    }


    def "Test complex recursive inner structure"() {
        given:
        def r1 = new XmlNodeImpl().setName("Root").setChange(FLATTEN).setXmlName("Root")
        def r2 = r1.addSubNode().setName("Attr1").setType(ATTRIBUTE).setOptionality(ONE).setChange(NONE).setXmlName("XmlAttr1")
        def r3 = r1.addSubNode().setName("Node2").setType(ELEMENT).setOptionality(ONE).setChange(NONE).setXmlName("Xml2")
        def r4 = r3.addSubNode().setName("InnerAttr1").setType(ATTRIBUTE).setOptionality(ONE).setChange(NONE).setXmlName("XmlAttr11")
        def r5 = r3.addSubNode().setName("Inner1").setType(ELEMENT).setOptionality(ONE_OR_MORE).setChange(FLATTEN).setXmlName("Xml11")
        def r6 = r5.addSubNode().setName("Inner2").setType(ELEMENT).setOptionality(ONE_OR_MORE).setChange(NONE).setXmlName("Xml12")
        def r7 = r1.addSubNode().setName("Node3").setType(ELEMENT).setOptionality(ONE).setChange(NONE).setXmlName("Xml3")

        when:
        r1.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
           <XMLNode Type="Element" Level="1" Optionality="One" Change="Flatten" Name="Root" XMLName="Root" />
           <XMLNode Type="Attribute" Level="2" Optionality="One" Change="None" Name="Attr1" XMLName="XmlAttr1" />
           <XMLNode Type="Element" Level="2" Optionality="One" Change="None" Name="Node2" XMLName="Xml2" />
           <XMLNode Type="Attribute" Level="3" Optionality="One" Change="None" Name="InnerAttr1" XMLName="XmlAttr11" />
           <XMLNode Type="Element" Level="3" Optionality="OneOrMore" Change="Flatten" Name="Inner1" XMLName="Xml11" />
           <XMLNode Type="Element" Level="4" Optionality="OneOrMore" Change="None" Name="Inner2" XMLName="Xml12" />
           <XMLNode Type="Element" Level="2" Optionality="One" Change="None" Name="Node3" XMLName="Xml3" />
         """)
    }

    def "convert XmlNodeType to xmlName"() {
        when:
        List<String> xmlNames = XmlNodeType.values().collect {
            XmlNodeImpl node = new XmlNodeImpl().setType(it)
            node.convertXmlNodeTypeToXmlName()
        }

        then:
        assert xmlNames == ["Element",
                            "Attribute",
                            "PCData",
                            "OneOf",
                            "Sequence"]
    }

    def "convert XmlNodeChange to xmlName"() {
        when:
        List<String> xmlNames = XmlNodeChange.values().collect {
            XmlNodeImpl node = new XmlNodeImpl().setChange(it)
            node.convertXmlNodeChangeToXmlName()
        }

        then:
        assert xmlNames == ["None",
                            "Ignore",
                            "Flatten",
                            "ErrorRecover"]
    }

    def "convert XmlNodeOptionality"() {
        when:
        List<String> xmlNames = XmlNodeOptionality.values().collect {
            XmlNodeImpl node = new XmlNodeImpl().setOptionality(it)
            node.convertXmlNodeOptionalityToXmlName()
        }

        then:
        assert xmlNames == ["One",
                            "ZeroOrOne",
                            "ZeroOrMore",
                            "OneOrMore"]
    }

    def "parent is correctly set"() {
        when:
        def r1 = new XmlNodeImpl()
        def r2 = r1.addSubNode()

        then:
        r1.parent == null
        r2.parent == r1
    }

    def "set attributes in fluent way works"() {
        given:
        XmlNode x = new XmlNodeImpl() as XmlNode

        when:
        x = x
                .setName("TestNode")
                .setType(ELEMENT)
                .setOptionality(ZERO_OR_MORE)
                .setChange(IGNORE)
                .setXmlName("XmlNode")
                .setFieldConverter(boolConverter())

        then:
        (x as XmlNodeImpl).export(exporter)
        assertXmlEquals(exporter.buildString(), """               
               <XMLNode Type="Element" Level="1" Optionality="ZeroOrMore" Change="Ignore" Name="TestNode" XMLName="XmlNode">
                   <FCV FCVClassName="BT2BTFCV">
                       <FCVProps>
                           <InputType>String</InputType>
                           <OutputType>Bool</OutputType>
                       </FCVProps>
                   </FCV>
               </XMLNode>
             """)
    }

    def "set attributes by add method works"() {
        given:
        XmlNode x = new XmlNodeImpl() as XmlNode

        when:
        x = x.addSubNode("TestNode", ELEMENT, ZERO_OR_MORE, IGNORE, "XmlNode", boolConverter())

        then:
        (x as XmlNodeImpl).export(exporter)
        assertXmlEquals(exporter.buildString(), """               
               <XMLNode Type="Element" Level="1" Optionality="ZeroOrMore" Change="Ignore" Name="TestNode" XMLName="XmlNode">
                   <FCV FCVClassName="BT2BTFCV">
                       <FCVProps>
                           <InputType>String</InputType>
                           <OutputType>Bool</OutputType>
                       </FCVProps>
                   </FCV>
               </XMLNode>
             """)
    }


}
