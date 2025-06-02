package com.quadient.wfdxml.internal.layoutnodes

import com.quadient.wfdxml.api.layoutnodes.FillStyle
import com.quadient.wfdxml.api.layoutnodes.LineStyle
import com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.CapType
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.CornerType
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.ALL_CORNERS
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.LOWER_LEFT_CORNER
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.RIGHT_LINE
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.SECONDARY_DIAGONAL_LINE
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.TOP_LINE
import static com.quadient.wfdxml.internal.layoutnodes.BorderStyleImpl.ElementType
import static com.quadient.wfdxml.internal.layoutnodes.BorderStyleImpl.Type
import static com.quadient.wfdxml.internal.layoutnodes.BorderStyleImpl.convertCapTypeToXmlValue
import static com.quadient.wfdxml.internal.layoutnodes.BorderStyleImpl.convertElementTypeToXmlValue
import static com.quadient.wfdxml.internal.layoutnodes.BorderStyleImpl.convertJoinTypeToXmlValue
import static com.quadient.wfdxml.internal.layoutnodes.BorderStyleImpl.convertTypeToXmlValue
import static com.quadient.wfdxml.internal.layoutnodes.support.PathEnums.JoinType
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class BorderStyleImplTest extends Specification {

    XmlExporter exporter = new XmlExporter()

    def "export empty border style"() {
        given:
        BorderStyleImpl borderStyle = new BorderStyleImpl()

        when:
        borderStyle.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                <FillStyleId/>
                <ShadowStyleId/>
                <Margin>
                  <UpperLeft X="0.0" Y="0.0"/>
                  <LowerRight X="0.0" Y="0.0"/>
                </Margin>
                <Offset>
                  <UpperLeft X="0.0" Y="0.0"/>
                  <LowerRight X="0.0" Y="0.0"/>
                </Offset>
                <ShadowOffset X="0.0" Y="0.0"/>
                <JoinType>Miter</JoinType>
                <Miter>10.0</Miter>
                <LeftLine>
                  <FillStyle/>
                  <LineWidth>2.0E-4</LineWidth>
                  <CapType>Butt</CapType>
                  <LineStyle/>
                </LeftLine>
                <UpperLeftCorner>
                  <FillStyle/>
                  <LineWidth>2.0E-4</LineWidth>
                  <CapType>Butt</CapType>
                  <LineStyle/>
                </UpperLeftCorner>
                <TopLine>
                  <FillStyle/>
                  <LineWidth>2.0E-4</LineWidth>
                  <CapType>Butt</CapType>
                  <LineStyle/>
                </TopLine>
                <RightTopCorner>
                  <FillStyle/>
                  <LineWidth>2.0E-4</LineWidth>
                  <CapType>Butt</CapType>
                  <LineStyle/>
                </RightTopCorner>
                <RightLine>
                  <FillStyle/>
                  <LineWidth>2.0E-4</LineWidth>
                  <CapType>Butt</CapType>
                  <LineStyle/>
                </RightLine>
                <LowerRightCorner>
                  <FillStyle/>
                  <LineWidth>2.0E-4</LineWidth>
                  <CapType>Butt</CapType>
                  <LineStyle/>
                </LowerRightCorner>
                <BottomLine>
                  <FillStyle/>
                  <LineWidth>2.0E-4</LineWidth>
                  <CapType>Butt</CapType>
                  <LineStyle/>
                </BottomLine>
                <LowerLeftCorner>
                  <FillStyle/>
                  <LineWidth>2.0E-4</LineWidth>
                  <CapType>Butt</CapType>
                  <LineStyle/>
                </LowerLeftCorner>
                <LeftRightLine>
                  <FillStyle/>
                  <LineWidth>2.0E-4</LineWidth>
                  <CapType>Butt</CapType>
                  <LineStyle/>
                </LeftRightLine>
                <RightLeftLine>
                  <FillStyle/>
                  <LineWidth>2.0E-4</LineWidth>
                  <CapType>Butt</CapType>
                  <LineStyle/>
                </RightLeftLine>
                <UpperLeftCornerType>
                  <CornerType>StandardCorner</CornerType>
                  <CornerRadius X="0.0" Y="0.0"/>
                </UpperLeftCornerType>
                <UpperRightCornerType>
                  <CornerType>StandardCorner</CornerType>
                  <CornerRadius X="0.0" Y="0.0"/>
                </UpperRightCornerType>
                <LowerRightCornerType>
                  <CornerType>StandardCorner</CornerType>
                  <CornerRadius X="0.0" Y="0.0"/>
                </LowerRightCornerType>
                <LowerLeftCornerType>
                  <CornerType>StandardCorner</CornerType>
                  <CornerRadius X="0.0" Y="0.0"/>
                </LowerLeftCornerType>
                <Type>Simple</Type>
        """)
    }

    def "border select and backToBorderStyle methods are fluent"() {
        given:
        BorderStyle borderStyle = new BorderStyleImpl()

        when:
        BorderStyle returnedBorderStyle = borderStyle.select(ALL_CORNERS).backToBorderStyle()

        then:
        borderStyle.is(returnedBorderStyle)
    }

    def "export border style with set all values"() {
        given:
        BorderStyle borderStyle = new BorderStyleImpl() as BorderStyle
        FillStyle fill = new FillStyleImpl() as FillStyle
        FillStyle shadowFillStyle = new FillStyleImpl() as FillStyle
        FillStyle lineFillStyle = new FillStyleImpl() as FillStyle
        LineStyle lineStyle = new LineStyleImpl() as LineStyleImpl
        String fillId = exporter.idRegister.getOrCreateId(fill)
        String shadowFillStyleId = exporter.idRegister.getOrCreateId(shadowFillStyle)
        String lineFillStyleId = exporter.idRegister.getOrCreateId(lineFillStyle)
        String lineStyleId = exporter.idRegister.getOrCreateId(lineStyle)


        borderStyle
                .setOffsets(0.0023d, 0.0022d, 0.0024d, 0.0021d) // top,right,bottom,left   setOffsetTop...
                .setMargins(0.0063d, 0.0062d, 0.0064d, 0.0061d) // top,right,bottom,left   setMarginTop...
                .setJoin(BorderStyle.JoinType.BEVEL)
                .setMiter(10.1d)
                .setFill(fill)
                .setShadowFillStyle(shadowFillStyle)
                .setShadowOffsetX(0.00140d)
                .setShadowOffsetY(0.00141d)
                .select(TOP_LINE, RIGHT_LINE, SECONDARY_DIAGONAL_LINE, LOWER_LEFT_CORNER)
                .setLineFillStyle(lineFillStyle)
                .setLineWidth(0.0012d)
                .setCap(CapType.ROUND)
                .setLineStyle(lineStyle)
                .setCorner(CornerType.ROUND_OUT)
                .setRadiusX(0.0051d)
                .setRadiusY(0.0052d)

        when:
        (borderStyle as BorderStyleImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                  <FillStyleId>$fillId</FillStyleId>
                  <ShadowStyleId>$shadowFillStyleId</ShadowStyleId>
                  <Margin>
                    <UpperLeft X="0.0061" Y="0.0063"/>
                    <LowerRight X="0.0062" Y="0.0064"/>
                  </Margin>
                  <Offset>
                    <UpperLeft X="0.0021" Y="0.0023"/>
                    <LowerRight X="0.0022" Y="0.0024"/>
                  </Offset>
                  <ShadowOffset X="0.0014" Y="0.00141"/>
                  <JoinType>Bevel</JoinType>
                  <Miter>10.1</Miter>
                  <LeftLine>
                    <FillStyle/>
                    <LineWidth>2.0E-4</LineWidth>
                    <CapType>Butt</CapType>
                    <LineStyle/>
                  </LeftLine>
                  <UpperLeftCorner>
                    <FillStyle/>
                    <LineWidth>2.0E-4</LineWidth>
                    <CapType>Butt</CapType>
                    <LineStyle/>
                  </UpperLeftCorner>
                  <TopLine>
                    <FillStyle>$lineFillStyleId</FillStyle>
                    <LineWidth>0.0012</LineWidth>
                    <CapType>Round</CapType>
                    <LineStyle>$lineStyleId</LineStyle>
                  </TopLine>
                  <RightTopCorner>
                    <FillStyle/>
                    <LineWidth>2.0E-4</LineWidth>
                    <CapType>Butt</CapType>
                    <LineStyle/>
                  </RightTopCorner>
                  <RightLine>
                    <FillStyle>$lineFillStyleId</FillStyle>
                    <LineWidth>0.0012</LineWidth>
                    <CapType>Round</CapType>
                    <LineStyle>$lineStyleId</LineStyle>
                  </RightLine>
                  <LowerRightCorner>
                    <FillStyle/>
                    <LineWidth>2.0E-4</LineWidth>
                    <CapType>Butt</CapType>
                    <LineStyle/>
                  </LowerRightCorner>
                  <BottomLine>
                    <FillStyle/>
                    <LineWidth>2.0E-4</LineWidth>
                    <CapType>Butt</CapType>
                    <LineStyle/>
                  </BottomLine>
                  <LowerLeftCorner>
                    <FillStyle>$lineFillStyleId</FillStyle>
                    <LineWidth>0.0012</LineWidth>
                    <CapType>Round</CapType>
                    <LineStyle>$lineStyleId</LineStyle>
                  </LowerLeftCorner>
                  <LeftRightLine>
                    <FillStyle/>
                    <LineWidth>2.0E-4</LineWidth>
                    <CapType>Butt</CapType>
                    <LineStyle/>
                  </LeftRightLine>
                  <RightLeftLine>
                    <FillStyle>$lineFillStyleId</FillStyle>
                    <LineWidth>0.0012</LineWidth>
                    <CapType>Round</CapType>
                    <LineStyle>$lineStyleId</LineStyle>
                  </RightLeftLine>
                  <UpperLeftCornerType>
                    <CornerType>StandardCorner</CornerType>
                    <CornerRadius X="0.0" Y="0.0"/>
                  </UpperLeftCornerType>
                  <UpperRightCornerType>
                    <CornerType>StandardCorner</CornerType>
                    <CornerRadius X="0.0" Y="0.0"/>
                  </UpperRightCornerType>
                  <LowerRightCornerType>
                    <CornerType>StandardCorner</CornerType>
                    <CornerRadius X="0.0" Y="0.0"/>
                  </LowerRightCornerType>
                  <LowerLeftCornerType>
                    <CornerType>RoundOut</CornerType>
                    <CornerRadius X="0.0051" Y="0.0052"/>
                  </LowerLeftCornerType>
                  <Type>Simple</Type>
        """)
    }


    def "conversion of JoinType to XmlValues"() {
        expect:
        convertJoinTypeToXmlValue(joinType as JoinType) == expectedXmlName

        where:
        expectedXmlName | _
        "Miter"         | _
        "Round"         | _
        "Bevel"         | _

        joinType << JoinType.values()
    }

    def "conversion of CapType to XmlValues"() {
        expect:
        convertCapTypeToXmlValue(capType as CapType) == expectedXmlName

        where:
        expectedXmlName | _
        "Butt"          | _
        "Round"         | _
        "Square"        | _

        capType << CapType.values()
    }

    def "conversion of ElementType to XmlValues"() {
        expect:
        convertElementTypeToXmlValue(elementType as ElementType) == expectedXmlName

        where:
        expectedXmlName  | _
        "StandardCorner" | _
        "RoundCorner"    | _
        "Line"           | _
        "RoundOut"       | _
        "CutOut"         | _
        "LineCorner"     | _

        elementType << ElementType.values()
    }

    def "conversion of Type to XmlValues"() {
        expect:
        convertTypeToXmlValue(type as Type) == expectedXmlName

        where:
        expectedXmlName | _
        "Simple"        | _
        "Integer"       | _
        "Interval"      | _
        "Condition"     | _
        "String"        | _
        "InlCond"       | _
        "Content"       | _
        "MediaQueries"  | _

        type << Type.values()
    }


}
