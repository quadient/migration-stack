package com.quadient.wfdxml.internal.layoutnodes

import com.quadient.wfdxml.api.layoutnodes.Image
import com.quadient.wfdxml.api.layoutnodes.ImageArea
import com.quadient.wfdxml.api.layoutnodes.data.Variable
import com.quadient.wfdxml.internal.layoutnodes.data.VariableImpl
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import com.quadient.wfdxml.utils.AssertXml
import spock.lang.Specification

import static com.quadient.wfdxml.api.layoutnodes.ImageArea.HorizontalAlign
import static com.quadient.wfdxml.api.layoutnodes.ImageArea.VerticalAlign

class ImageObjectImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "export empty ImageObject"() {
        given:
        ImageArea imageObject = new ImageObjectImpl() as ImageArea

        when:
        (imageObject as ImageObjectImpl).export(exporter)

        then:
        AssertXml.assertXmlEqualsWrapRoot(exporter.buildString(), """
                    <Pos X="0.0" Y="0.0"></Pos>
                    <Size X="0.1" Y="0.1"></Size>
                    <Rotation>0.0</Rotation>
                    <Skew>0.0</Skew>
                    <FlipX>False</FlipX>
                    <Scale X="1.0" Y="1.0"/>
                    <RotationPointX>0.0</RotationPointX>
                    <RotationPointY>0.0</RotationPointY>
                    <RotationRound>0.0</RotationRound>
                    <ImageId/>
                    <PreserveAspectRatio>True</PreserveAspectRatio>
                    <Stretch>True</Stretch>
                    <Shrink>True</Shrink>
                    <HorizontalAlign>Center</HorizontalAlign>
                    <VerticalAlign>Center</VerticalAlign>
                    <URLLink/>
                    <ClippingImage>False</ClippingImage>
                    <Offset X="0.0" Y="0.0"/> 
                    """)
    }

    def "export ImageObject allSet"() {
        given:
        Image image = new ImageImpl()
        Variable variable = new VariableImpl()
        ImageArea imageObject = new ImageObjectImpl() as ImageArea

        String imageId = exporter.idRegister.getOrCreateId(image)
        String variableId = exporter.idRegister.getOrCreateId(variable)

        imageObject.setPositionAndSize(0.1d, 0.2d, 0.07d, 0.08d)
                .setImage(image)
                .setPreserveAspectRatio(false)
                .setStretch(false)
                .setShrink(false)
                .setHorizontalAlign(HorizontalAlign.RIGHT)
                .setVerticalAlign(VerticalAlign.TOP)
                .setUrlLink(variable)
                .setClipImage(true)
                .setOffsetX(0.1d)
                .setOffsetY(0.22d)
        when:
        (imageObject as ImageObjectImpl).export(exporter)

        then:
        AssertXml.assertXmlEqualsWrapRoot(exporter.buildString(), """
                    <Pos X="0.1" Y="0.2"></Pos>
                    <Size X="0.07" Y="0.08"></Size>
                    <Rotation>0.0</Rotation>
                    <Skew>0.0</Skew>
                    <FlipX>False</FlipX>
                    <Scale X="1.0" Y="1.0"/>
                    <RotationPointX>0.0</RotationPointX>
                    <RotationPointY>0.0</RotationPointY>
                    <RotationRound>0.0</RotationRound>
                    <ImageId>$imageId</ImageId>
                    <PreserveAspectRatio>False</PreserveAspectRatio>
                    <Stretch>False</Stretch>
                    <Shrink>False</Shrink>
                    <HorizontalAlign>Right</HorizontalAlign>
                    <VerticalAlign>Top</VerticalAlign>
                    <URLLink>$variableId</URLLink>
                    <ClippingImage>True</ClippingImage>
                    <Offset X="0.1" Y="0.22"/> 
                    """)
    }

    def "convert HorizontalAlign to xml name"() {
        expect:
        ImageObjectImpl.convertHorizontalAlignToXmlName(alings) == expectedResult

        where:
        expectedResult | _
        "Left"         | _
        "Center"       | _
        "Right"        | _

        alings << HorizontalAlign.values().toList()

    }

    def "convert VerticalAlign to xml name"() {
        expect:
        ImageObjectImpl.convertVerticalAlignToXmlName(alings) == expectedResult

        where:
        expectedResult | _
        "Top"          | _
        "Center"       | _
        "Bottom"       | _

        alings << VerticalAlign.values().toList()
    }
}