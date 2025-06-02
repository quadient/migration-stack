package com.quadient.wfdxml.internal.layoutnodes

import com.quadient.wfdxml.api.layoutnodes.LineStyle
import com.quadient.wfdxml.api.layoutnodes.TextStyle
import com.quadient.wfdxml.api.layoutnodes.data.Variable
import com.quadient.wfdxml.internal.layoutnodes.data.VariableImpl
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class TextStyleImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "def TextStyle serialization"() {
        given:
        TextStyle textStyle = new TextStyleImpl()

        when:
        textStyle.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                   <AncestorId>Def.TextStyle</AncestorId>
                   <FontSize>0.003527777777777778</FontSize>
                   <BaselineShift>0.0</BaselineShift>
                   <InterCharacterSpacing>0.0</InterCharacterSpacing>
                   <Kerning>False</Kerning>
                   <LineWidth>1.0E-4</LineWidth>
                   <BorderStyleId></BorderStyleId>
                   <IsVisible>True</IsVisible>
                   <ConnectBorders>False</ConnectBorders>
                   <WithLineGap>False</WithLineGap>
                   <Bold>False</Bold>
                   <Italic>False</Italic>
                   <Underline>False</Underline>
                   <Strikethrough>False</Strikethrough>
                   <Language>en</Language>
                   <SmallCap>False</SmallCap>
                   <SuperScript>False</SuperScript>
                   <SubScript>False</SubScript>
                   <URLLink></URLLink>
                   <HorizontalScale>100.0</HorizontalScale>
                   <Type>Simple</Type>
                   <StrikethroughLineStyleId></StrikethroughLineStyleId>
                   <UnderlineLineStyleId></UnderlineLineStyleId>
                   <IsFixedWidth>False</IsFixedWidth>
                   <FixedWidth>0.003</FixedWidth>""")
    }

    def "textStyle allSet serialization IntegrationTest"() {
        given:
        FontImpl font = new FontImpl()
        FillStyleImpl fillStyle = new FillStyleImpl()
        BorderStyleImpl borderStyle = new BorderStyleImpl()
        Variable urlVariable = new VariableImpl()
        LineStyle strikeThroughStyle = Mock()
        LineStyle underLineStyle = Mock()

        TextStyle textStyle = new TextStyleImpl()
                .setFillStyle(fillStyle)
                .setFont(font)
                .seItalic(true)
                .setBold(true)
                .setStrikeThrough(true)
                .setUnderline(true)
                .setFontSize(20)
                .setLanguage("de")
                .setLineWidth(0.0002d)
                .setSmallCap(true)
                .setSubScript(true)
                .setSuperScript(true)
                .setBorderStyle(borderStyle)
                .setWithLineGap(true)
                .setConnectBorders(true)
                .setUrlTarget(urlVariable)
                .setBaselineShift(0.0004d)
                .setInterSpacing(0.0008d)
                .setKerning(true)
                .setUseFixedWidth(true)
                .setFixedWidth(0.007d)
                .setHorizontalScale(8)
                .setUnderlineStyle(underLineStyle)
                .setStrikeThroughStyle(strikeThroughStyle) as TextStyle

        when:
        (textStyle as TextStyleImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                    <AncestorId>Def.TextStyle</AncestorId>
                    <FontSize>0.007055555555555556</FontSize>
                    <BaselineShift>4.0E-4</BaselineShift>
                    <InterCharacterSpacing>8.0E-4</InterCharacterSpacing>
                    <Kerning>True</Kerning>
                    <LineWidth>2.0E-4</LineWidth>
                    <BorderStyleId>SR_1</BorderStyleId>
                    <IsVisible>True</IsVisible>
                    <ConnectBorders>True</ConnectBorders>
                    <WithLineGap>True</WithLineGap>
                    <FillStyleId>SR_2</FillStyleId>
                    <FontId>SR_3</FontId>
                    <Bold>True</Bold>
                    <Italic>True</Italic>
                    <Underline>True</Underline>
                    <Strikethrough>True</Strikethrough>
                    <Language>de</Language>
                    <SmallCap>True</SmallCap>
                    <SuperScript>True</SuperScript>
                    <SubScript>True</SubScript>
                    <URLLink>SR_4</URLLink>
                    <HorizontalScale>8.0</HorizontalScale>
                    <Type>Simple</Type>
                    <StrikethroughLineStyleId>SR_5</StrikethroughLineStyleId>
                    <UnderlineLineStyleId>SR_6</UnderlineLineStyleId>
                    <IsFixedWidth>True</IsFixedWidth>
                    <FixedWidth>0.007</FixedWidth>""")
    }

    def "def italic TextStyle"() {
        when:
        TextStyleImpl textStyle = new TextStyleImpl().seItalic(true)

        then:
        assert textStyle.isItalic()
    }

    def "textStyle setFontSize transfer"() {
        when:
        TextStyleImpl textStyle = new TextStyleImpl().setFontSize(15)

        then:
        def valueWrittenByDesigner = 5.2916666666666667e-003d
        assert textStyle.getFontSize() == valueWrittenByDesigner
    }

    def "textStyle setLineWidth"() {
        when:
        TextStyleImpl textStyle = new TextStyleImpl().setLineWidth(0.005d)

        then:
        assert textStyle.getLineWidth() == 0.005d
    }

    def "no font"() {
        when:
        TextStyleImpl textStyle = new TextStyleImpl()

        then:
        assert textStyle.getFont() == null
    }

    def "no fillStyle()"() {
        when:
        TextStyleImpl textStyle = new TextStyleImpl()

        then:
        assert textStyle.getFillStyle() == null
    }

    def "def setLanguage"() {
        when:
        TextStyleImpl textStyle = new TextStyleImpl()

        then:
        assert textStyle.getLanguage() == "en"
    }
}
