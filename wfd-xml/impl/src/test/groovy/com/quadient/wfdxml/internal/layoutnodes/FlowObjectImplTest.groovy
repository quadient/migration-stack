package com.quadient.wfdxml.internal.layoutnodes

import com.quadient.wfdxml.api.layoutnodes.DataMatrixBarcode
import com.quadient.wfdxml.api.layoutnodes.PathObject
import com.quadient.wfdxml.internal.module.layout.LayoutImpl
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.api.layoutnodes.Element.Formatting
import static com.quadient.wfdxml.internal.layoutnodes.FlowObjectImpl.convertFormattingKindToXmlName
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class FlowObjectImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "empty FlowObject serialization "() {
        given:
        FlowObjectImpl flowObject = new FlowObjectImpl()

        when:
        flowObject.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
            <Size X="0.0" Y="0.0"/>
            <Offset>0.0</Offset>
            <VerticalObjectPosition>BaseLine</VerticalObjectPosition>
            <RunaroundByChildren>False</RunaroundByChildren>
            <DynamicSize>False</DynamicSize>
            """)
    }

    def "all set FlowObject serialization "() {
        given:
        FlowObjectImpl flowObject = new FlowObjectImpl()
        flowObject
                .setWidth(0.05)
                .setHeight(0.04)
                .setOffset(0.001)
                .setFormatting(Formatting.CENTER)
                .setRunaroundChildrenOnly(true)
                .setDynamicSizeByRunaround(true)

        when:
        flowObject.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
            <Size X="0.05" Y="0.04"/>
            <Offset>0.001</Offset>
            <VerticalObjectPosition>Center</VerticalObjectPosition>
            <RunaroundByChildren>True</RunaroundByChildren>
            <DynamicSize>True</DynamicSize>
            """)
    }

    def "addPathObject insert newly created PathObject into Tree.children"() {
        given:
        FlowObjectImpl flowObject = new FlowObjectImpl()

        when:
        PathObject pathObject = flowObject.addPathObject()

        then:
        flowObject.children == [pathObject]
    }

    def "addBarcode insert newly created Barcode into Tree.children"() {
        given:
        FlowObjectImpl flowObject = new FlowObjectImpl(Mock(LayoutImpl))

        when:
        DataMatrixBarcode barcode = flowObject.getBarcodeFactory().addDataMatrix()

        then:
        flowObject.children == [barcode]
    }


    def "export of conversion of Formatting to Xml name"() {

        expect:
        convertFormattingKindToXmlName(type) == expectedResult

        where:
        expectedResult | _
        "BaseLine"     | _
        "Top"          | _
        "Center"       | _
        "Bottom"       | _

        type << Formatting.values().toList()
    }
}
