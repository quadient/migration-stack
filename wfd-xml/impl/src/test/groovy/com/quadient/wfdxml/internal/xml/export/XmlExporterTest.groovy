package com.quadient.wfdxml.internal.xml.export

import com.quadient.wfdxml.internal.NodeImpl
import com.quadient.wfdxml.internal.TestNode
import com.quadient.wfdxml.internal.layoutnodes.ColorImpl
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlEquals
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class XmlExporterTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "addElementWithStringData"() {
        when:
        exporter.addElementWithStringData("ElementName", "Element Data")

        then:
        assertXmlEquals(exporter.buildString(),
                """<ElementName>Element Data</ElementName>""")
    }

    def "special chars are escaped"() {
        when:
        exporter.addElementWithStringData("E", "specialChars<>&\\\"'")

        then:
        //This is a more than xml need to be escaped, but this is exactly how its done by designer
        exporter.buildString() == """<E>specialChars&lt;&gt;&amp;\\&quot;&#39;</E>"""
    }

    def "special chars are escaped new line"() {
        when:
        exporter.addElementWithStringData("E", "one\r\ntwo")

        then:
        //This is a more than xml need to be escaped, but this is exactly how its done by designer
        exporter.buildString() == """<E>one&#xd;&#xa;two</E>"""
    }

    def "special chars are escaped in attribute value"() {
        when:
        exporter.beginElement("E")
                .addStringAttribute("attr", "specialChars<>&\\\"'")
                .endElement()

        then:
        exporter.buildString() == """<E attr="specialChars&lt;&gt;&amp;\\&quot;'"></E>"""
    }

    def "special chars are escaped inside attribute value"() {
        when:
        exporter.beginElement("E")
                .addStringAttribute("Attr", "\"&'<>")
                .endElement()

        then:
        exporter.buildString() == """<E Attr="&quot;&amp;'&lt;&gt;"></E>"""
    }

    def "quot is escaped inside attribute value"() {
        when:
        exporter.beginElement("E")
                .addStringAttribute("Attr", "A\"B")
                .endElement()

        then:
        exporter.buildString() == """<E Attr="A&quot;B"></E>"""
    }

    def "spaces are not escaped inside attribute value"() {
        when:
        exporter.beginElement("E")
                .addStringAttribute("Attr", " A B ")
                .endElement()

        then:
        exporter.buildString() == """<E Attr=" A B "></E>"""
    }

    def "new line is not changed inside attribute value"() {
        //In designer this case can happen in CustCodeFieldConverter in outputString value
        when:
        exporter.beginElement("E")
                .addStringAttribute("Attr", "A\nB")
                .endElement()

        then:
        exporter.buildString() == """<E Attr="A\nB"></E>"""
    }

    def "beginElement with preserve space"() {
        when:
        exporter.beginElement("E", true)

        then:
        assertXmlEquals(exporter.buildString(),
                """<E xml:space="preserve"/>""")
    }

    def "addElementWithIface"() {
        when:
        exporter.addElementWithIface("TetstId", new ColorImpl())

        then:
        assertXmlEquals(exporter.buildString(),
                """<TetstId>SR_1</TetstId>""")
    }

    def "Simple export"() {
        given:
        NodeImpl node = new TestNode().setName("Fake Node")

        when:
        exporter.addElementWithIface("Id", node)
        exporter.addElementWithStringData("RGB", "RGB")

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(),
                """<Id>SR_1</Id><RGB>RGB</RGB>""")
    }

    def "add different type of elements"() {
        NodeImpl node = new TestNode().setName("Node 1")
        when:
        exporter.beginElement("root")
                .addElementWithStringData("eString", "myString")
                .addElementWithDoubleData("eDouble", 0.12d)
                .addElementWithBoolData("eBool", true)
                .addElementWithIntData("eInt", 182)
                .addElementWithInt64Data("eInt64", 184L)
                .addElementWithIface("eObj1", node)
                .addElement("eEmpty1",)
        exporter.endElement()

        then:
        assertXmlEquals(exporter.buildString(),
                """
                <root>
                  <eString>myString</eString>
                  <eDouble>0.12</eDouble>
                  <eBool>True</eBool>
                  <eInt>182</eInt>
                  <eInt64>184</eInt64>
                  <eObj1>SR_1</eObj1>
                  <eEmpty1/>
                </root>""")
    }


    def "addElementWithBoolData"() {
        when:
        exporter.addElementWithBoolData("eTrue", true)
        exporter.addElementWithBoolData("eFalse", false)

        then:
        assertXmlEqualsWrapRoot("${exporter.buildString()}",
                """
                   <eTrue>True</eTrue>
                   <eFalse>False</eFalse>""")
    }

    def "addStringAttribute"() {
        when:
        exporter.beginElement("E")
                .addStringAttribute("attr1", "val1")
                .addStringAttribute("attr2", "val2")
        exporter.endElement()


        then:
        assertXmlEquals(exporter.buildString(),
                '''<E attr1="val1" attr2="val2" />''')
    }

    def "add different type of attributes"() {
        when:
        exporter.beginElement("E")
                .addStringAttribute("aString", "StrVal")
                .addDoubleAttribute("aDouble", 0.8d)
                .addBoolAttribute("aBoolean", true)
                .addIntAttribute("aInt", 188)
                .addInt64Attribute("aLong", 184L)
                .addIfaceAttribute("aObjId", "ObjId")
        exporter.endElement()

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(),
                '''
                          <E
                              aString="StrVal"
                              aDouble="0.8"
                              aBoolean="True"
                              aInt="188"
                              aLong="184"
                              aObjId="SR_1"
                              />
                            ''')
    }

    def "beginElement addPCData endElement"() {
        when:
        exporter.beginElement("ElementName")
        exporter.addPCData("Element Data")
        exporter.endElement()
        exporter.addElement("After")

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(),
                """
              <ElementName>Element Data</ElementName>
              <After/>""")
    }

    def "add different PCData"() {
        given:
        def object = new ArrayList()

        when:
        exporter.beginElement("MyString").addPCData("StringVal").endElement()
        exporter.beginElement("MyDouble").addPCData(0.25d).endElement()
        exporter.beginElement("MyText").addPCData(" My Text ", true).endElement()
        exporter.beginElement("MyId").addPCData(object).endElement()

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(),
                '''
                          <MyString>StringVal</MyString>
                          <MyDouble>0.25</MyDouble>
                          <MyText xml:space="preserve"> My Text </MyText>
                          <MyId>SR_1</MyId>
                            ''')
    }

    def "addIfaceAttribute When add null attribute, then attribute is not in xml"() {
        when:
        exporter.beginElement("E")
                .addIfaceAttribute("nullAttr", null)
        exporter.endElement()

        then:
        assertXmlEquals(exporter.buildString(),
                '''
                              <E />
                                ''')
    }


    def "addElementWithIface When add null element, then element with empty content is in xml"() {
        when:
        exporter.beginElement("Wrap")
                .addElementWithIface("NullElement", null)
                .endElement()

        then:
        assertXmlEquals(exporter.buildString(),
                '''<Wrap><NullElement/></Wrap>''')
    }

    def "addElementWithStringData When value is null, then element is not in xml"() {
        when:
        exporter.beginElement("Wrap")
                .addElementWithStringData("NullElement", null)
                .endElement()

        then:
        assertXmlEquals(exporter.buildString(),
                '''<Wrap/>''')
    }


    def "add text twice will concat it"() {
        when:
        exporter.beginElement("E")
        exporter.addPCData("AAA")
        exporter.addPCData("BBB")
        exporter.endElement()

        then:
        assertXmlEquals(exporter.buildString(),
                """<E>AAABBB</E>""")
    }


    def "add element into middle of the text"() {
        when:
        exporter.beginElement("E")
                .addPCData("AAA")
                .addElement("Flow")
                .addPCData("BBB")
        exporter.endElement()

        then:
        assertXmlEquals(exporter.buildString(),
                """<E>AAA<Flow/>BBB</E>""")
    }


    def "insert raw xml"() {
        when:
        exporter.beginElement("E")
                .addRawXml("<RawTag>content<RawInnerTag/></RawTag>")
                .endElement()

        then:
        assertXmlEquals(exporter.buildString(),
                """<E><RawTag>content<RawInnerTag/></RawTag></E>""")
    }


    def "PCData with null value"() {
        when:
        exporter.beginElement("E")
                .addPCData((String) null)
                .endElement()

        then:
        assertXmlEquals(exporter.buildString(),
                """<E></E>""")
    }

    def "PCData with empty value"() {
        when:
        exporter.beginElement("E")
                .addPCData("")
                .endElement()

        then:
        assertXmlEquals(exporter.buildString(),
                """<E></E>""")
    }

    def "addElementWithStringData add xmlspacePreserve for value with space before"() {
        when:
        exporter.addElementWithStringData("E", " space before")

        then:
        assertXmlEquals(exporter.buildString(),
                """<E xml:space="preserve"> space before</E>""")
    }

    def "addElementWithStringData add xmlspacePreserve for value with space after"() {
        when:
        exporter.addElementWithStringData("E", "space after ")

        then:
        assertXmlEquals(exporter.buildString(),
                """<E xml:space="preserve">space after </E>""")
    }

    def "addElementWithStringData first space is escaped"() {
        when:
        exporter.addElementWithStringData("E", " space before")

        then:
        exporter.buildString() == """<E xml:space="preserve">&#x20;space before</E>"""
    }

    def "addElementWithStringData last space is escaped"() {
        when:
        exporter.addElementWithStringData("E", "space after ")

        then:
        exporter.buildString() == """<E xml:space="preserve">space after&#x20;</E>"""
    }


    def "addElementWithStringData add xmlspacePreserve and escape first and last space for value with multiple spaces"() {
        when:
        exporter.addElementWithStringData("E", "  space  multiple  ")

        then:
        exporter.buildString() == """<E xml:space="preserve">&#x20; space  multiple &#x20;</E>"""
    }

    def "pcData escape space in the beginning and in the end"() {
        when:
        exporter.beginElement("E")
        exporter.addPCData("  one  two  ")
        exporter.endElement()

        then:
        exporter.buildString() == """<E>&#x20; one  two &#x20;</E>"""
    }
}