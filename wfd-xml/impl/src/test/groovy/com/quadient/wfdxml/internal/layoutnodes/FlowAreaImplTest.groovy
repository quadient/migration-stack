package com.quadient.wfdxml.internal.layoutnodes

import com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.api.layoutnodes.FlowArea.BorderType
import static com.quadient.wfdxml.api.layoutnodes.FlowArea.BorderType.CONTENT_WITH_LINE_GAB
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class FlowAreaImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "create empty FlowArea"() {
        given:
        FlowAreaImpl flowArea = new FlowAreaImpl()

        when:
        flowArea.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                <Pos X="0.0" Y="0.0"/>
                <Size X="0.1" Y="0.1"/>
                <Rotation>0.0</Rotation>
                <Skew>0.0</Skew>
                <FlipX>False</FlipX>
                <Scale X="1.0" Y="1.0"/>
                <RotationPointX>0.0</RotationPointX>
                <RotationPointY>0.0</RotationPointY>
                <RotationRound>0.0</RotationRound>
                <FlowId/>
                <BorderStyleId></BorderStyleId>
                <FlowingToNextPage>False</FlowingToNextPage>
                <BorderType>Content</BorderType>
         """)
    }

    def "flowArea allSet IntegrationTest"() {
        given:
        FlowImpl flow = new FlowImpl()
        FlowAreaImpl flowArea = new FlowAreaImpl()
        BorderStyle borderStyle = new BorderStyleImpl()
        flowArea.setFlow(flow)
                .setPosX(0.15d)
                .setPosY(0.25d)
                .setWidth(0.4d)
                .setHeight(0.2d)
                .setBorderStyle(borderStyle)
                .setBorderType(CONTENT_WITH_LINE_GAB)
                .setFlowToNextPage(true)
                .setRotation(180d)
                .setSkew(15.5d)
                .setFlip(true)
                .setScaleX(3d)
                .setScaleY(4d)
                .setRotationPointX(0.008d)
                .setRotationPointY(0.007d)
                .setRotationRound(50d)

        when:
        flowArea.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
                <Pos X="0.15" Y="0.25"/>
                <Size X="0.4" Y="0.2"/>
                <Rotation>180.0</Rotation>
                <Skew>15.5</Skew>
                <FlipX>True</FlipX>
                <Scale X="3.0" Y="4.0"/>
                <RotationPointX>0.008</RotationPointX>
                <RotationPointY>0.007</RotationPointY>
                <RotationRound>50.0</RotationRound>
                <FlowId>SR_1</FlowId>
                <BorderStyleId>SR_2</BorderStyleId>
                <FlowingToNextPage>True</FlowingToNextPage>
                <BorderType>ContentWithLineGap</BorderType>
         """)
    }

    def "flowArea setPositionAndSize"() {
        when:
        FlowAreaImpl flowArea =
                new FlowAreaImpl().setPositionAndSize(0.15d, 0.20d, 0.25d, 0.30d) as FlowAreaImpl

        then:
        flowArea.getPosX() == 0.15d
        flowArea.getPosY() == 0.20d
        flowArea.getWidth() == 0.25d
        flowArea.getHeight() == 0.30d

    }

    def "convert BorderType to xml name"() {
        expect:
        FlowAreaImpl.convertBorderTypeToXmlName(borderTypes) == expectedResult

        where:
        expectedResult       | _
        "Content"            | _
        "ContentWithLineGap" | _
        "FullArea"           | _

        borderTypes << BorderType.values().toList()
    }
}