package com.quadient.wfdxml.internal.layoutnodes

import com.quadient.wfdxml.api.layoutnodes.FillStyle
import com.quadient.wfdxml.api.layoutnodes.LineStyle
import com.quadient.wfdxml.api.layoutnodes.Page
import com.quadient.wfdxml.api.layoutnodes.PathObject
import com.quadient.wfdxml.api.layoutnodes.data.Variable
import com.quadient.wfdxml.internal.layoutnodes.support.Path
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.api.layoutnodes.PathObject.CapType
import static com.quadient.wfdxml.api.layoutnodes.PathObject.CapType.ROUND
import static com.quadient.wfdxml.api.layoutnodes.PathObject.JoinType
import static com.quadient.wfdxml.api.layoutnodes.PathObject.JoinType.BEVEL
import static com.quadient.wfdxml.internal.layoutnodes.PathObjectImpl.convertCapTypToXmlName
import static com.quadient.wfdxml.internal.layoutnodes.PathObjectImpl.convertJoinTypToXmlName
import static com.quadient.wfdxml.internal.layoutnodes.PathObjectImpl.convertPathTypeToXmlName
import static com.quadient.wfdxml.internal.layoutnodes.support.Path.PathType
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class PathObjectImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "export empty PathObject"() {
        given:
        PathObject pathObject = new PathObjectImpl()

        when:
        pathObject.export(exporter)

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
                <Path>
                     <RuleOddEven>True</RuleOddEven>
                </Path>
                <FillStyleId/>
                <OutlineStyleId/>
                <CapType>Butt</CapType>
                <JoinType>Miter</JoinType>
                <MiterLimit>10.0</MiterLimit>
                <LineWidth>2.0E-4</LineWidth>
                <LineStyleId/>
                <AffectRunaroundByFillOnly>True</AffectRunaroundByFillOnly>
                <URLLink/>        
                """)
    }

    def "export allSet PathObject"() {
        given:
        FillStyle fillStyle = Mock()
        FillStyle lineFillStyle = Mock()
        LineStyle lineStyle = Mock()
        Variable urlVariable = Mock()

        PathObject pathObject = new PathObjectImpl() as PathObject
        pathObject
                .setOverlap(false)
                .addMoveTo(0.002d, 0.003d)
                .addLineTo(0.15d, 0.35d)
                .addLineTo(0.24d, 0.07d)
                .addMoveTo(0.016d, 0.33d)
                .setFillStyle(fillStyle)
                .setLineFillStyle(lineFillStyle)
                .setCap(ROUND)
                .setJoin(BEVEL)
                .setMiter(9.8d)
                .setLineWidth(0.0004d)
                .setLineStyle(lineStyle)
                .setRunaroundFillOnly(false)
                .setUrlTarget(urlVariable)

        String idFillStyle = exporter.idRegister.getOrCreateId(fillStyle)
        String idLineFillStyle = exporter.idRegister.getOrCreateId(lineFillStyle)
        String idLineStyle = exporter.idRegister.getOrCreateId(lineStyle)
        String idUrlVariable = exporter.idRegister.getOrCreateId(urlVariable)

        when:
        (pathObject as PathObjectImpl).export(exporter)

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
                <Path>
                    <RuleOddEven>False</RuleOddEven>
                    <MoveTo X="0.002" Y="0.003"/>
                    <LineTo X="0.15" Y="0.35"/>
                    <LineTo X="0.24" Y="0.07"/>
                    <MoveTo X="0.016" Y="0.33"/>
                </Path>
                <FillStyleId>$idFillStyle</FillStyleId>
                <OutlineStyleId>$idLineFillStyle</OutlineStyleId>
                <CapType>Round</CapType>
                <JoinType>Bevel</JoinType>
                <MiterLimit>9.8</MiterLimit>
                <LineWidth>4.0E-4</LineWidth>
                <LineStyleId>$idLineStyle</LineStyleId>
                <AffectRunaroundByFillOnly>False</AffectRunaroundByFillOnly>
                <URLLink>$idUrlVariable</URLLink>        
                """)
    }

    def "test convertCapTypeToXmlName"() {
        expect:
        convertCapTypToXmlName(types) == result

        where:
        result   | _
        "Butt"   | _
        "Round"  | _
        "Square" | _

        types << CapType.values().toList()
    }

    def "test convertJoinTypeToXmlName"() {
        expect:
        convertJoinTypToXmlName(types) == result

        where:
        result  | _
        "Miter" | _
        "Round" | _
        "Bevel" | _

        types << JoinType.values().toList()
    }

    def "test convertPathTypeToXmlName"() {
        expect:
        convertPathTypeToXmlName(types) == result

        where:
        result   | _
        "MoveTo" | _
        "LineTo" | _

        types << PathType.values().toList()
    }

    def "addLineTo add automatically add moveTo, when nothing was added before"() {
        given:
        PathObject pathObject = new PathObjectImpl() as PathObject

        when:
        pathObject.addLineTo(0.05d, 0.4d)

        then:
        Path path = (pathObject as PathObjectImpl).paths[0]
        assert path.getType() == PathType.MOVE_TO
        assert path.getX() == 0.0d
        assert path.getY() == 0.0d
    }

    def "addLine on Page set value correctly"() {
        given:
        Page page = new PageImpl(null)

        when:
        PathObject pathObject = page.addLine(0.02d, 0.03d, 0.07d, 0.08d)

        then:
        //Line position
        pathObject.getPosX() == 0.02d
        pathObject.getPosY() == 0.03d

        //Area
        pathObject.getWidth() == 0.05d
        pathObject.getHeight() == 0.05d

        //MoveTo
        pathObject.getPaths().get(0).getX() == 0.0d
        pathObject.getPaths().get(0).getY() == 0.0d

        //LineTo
        pathObject.getPaths().get(1).getX() == 0.05d
        pathObject.getPaths().get(1).getY() == 0.05d
    }
}