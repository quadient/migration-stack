package com.quadient.wfdxml.internal.layoutnodes

import com.quadient.wfdxml.api.layoutnodes.Flow
import com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle
import com.quadient.wfdxml.internal.layoutnodes.data.VariableImpl
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.api.layoutnodes.ParagraphStyle.AlignType
import static com.quadient.wfdxml.api.layoutnodes.ParagraphStyle.LineSpacingType
import static com.quadient.wfdxml.api.layoutnodes.ParagraphStyle.NumberingType
import static com.quadient.wfdxml.api.layoutnodes.TabulatorType.LEFT
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class ParagraphStyleImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "paraStyle serialization"() {
        given:
        ParagraphStyleImpl paraStyle = new ParagraphStyleImpl()

        when:
        paraStyle.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                    <AncestorId>Def.ParaStyle</AncestorId>
                    <LeftIndent>0.0</LeftIndent>
                    <RightIndent>0.0</RightIndent>
                    <SpaceBefore>0.0</SpaceBefore>
                    <SpaceAfter>0.0</SpaceAfter>
                    <FirstLineLeftIndent>0.0</FirstLineLeftIndent>
                    <HAlign>Left</HAlign>
                    <BorderStyleId></BorderStyleId>
                    <IsVisible>True</IsVisible>
                    <ConnectBorders>False</ConnectBorders>
                    <WithLineGap>False</WithLineGap>
                    <BullettingId/>
                    <IgnoreEmptyLines>False</IgnoreEmptyLines>
                    <TabulatorProperties>
                        <Default>0.0125</Default>
                        <UseOutsideTabs>False</UseOutsideTabs>
                    </TabulatorProperties>
                    <Hyphenation>
                        <Hyphenate>False</Hyphenate>
                    </Hyphenation>
                    <NumberingType>Increment</NumberingType>
                    <NumberingVariableId/>
                    <CalcMaxSpaceBeforeAfter>False</CalcMaxSpaceBeforeAfter>
                    <DistributeLineSpace>False</DistributeLineSpace>
                    <SpaceBeforeFirst>False</SpaceBeforeFirst>
                    <LineSpacing>0.0</LineSpacing>
                    <LineSpacingType>Additional</LineSpacingType>
                    <Type>Simple</Type>""")
    }

    def "paraStyle allSet serialization IntegrationTest"() {
        given:
        Flow mock = Mock()
        VariableImpl mock2 = Mock()
        BorderStyle mock3 = Mock()
        TextStyleImpl textStyle = new TextStyleImpl()
        ParagraphStyleImpl paraStyle = new ParagraphStyleImpl()
                .setTextStyle(textStyle)
                .setLeftIndent(0.01d)
                .setRightIndent(0.02d)
                .setSpaceBefore(0.03d)
                .setSpaceAfter(0.015d)
                .setFirstLineLeftIndent(0.05d)
                .setAlignType(AlignType.JUSTIFY_WITH_MARGIN)
                .setVisible(false)
                .setSpaceBeforeFirstPara(true)
                .setTakeMaxFromSpace(true)
                .setDistributeLineSpacing(true)
                .setLineSpacingValue(0.03d)
                .setLineSpacingType(LineSpacingType.EXACT)
                .setUseOutsideTabs(true)
                .setIgnoreEmptyLines(true)
                .setDefaultTabSize(0.0145d)
                .addTabulator(0.015d, LEFT)
                .setBulletsNumberingFlow(mock)
                .setNumberingType(NumberingType.RESET)
                .setNumberingFrom(12)
                .setNumberingVariable(mock2)
                .addDecimalTabulatorWithPoint(0.035d, ".")
                .setBorderStyle(mock3)
                .setConnectBorders(true)
                .setWithLineGap(true)
                .setHyphenate(true)


        when:
        paraStyle.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                    <AncestorId>Def.ParaStyle</AncestorId>
                    <LeftIndent>0.01</LeftIndent>
                    <RightIndent>0.02</RightIndent>
                    <SpaceBefore>0.03</SpaceBefore>
                    <SpaceAfter>0.015</SpaceAfter>
                    <FirstLineLeftIndent>0.060000000000000005</FirstLineLeftIndent>
                    <HAlign>JustifyBlock2</HAlign>
                    <BorderStyleId>SR_1</BorderStyleId>
                    <IsVisible>False</IsVisible>
                    <ConnectBorders>True</ConnectBorders>
                    <WithLineGap>True</WithLineGap>
                    <BullettingId>SR_2</BullettingId>
                    <IgnoreEmptyLines>True</IgnoreEmptyLines>
                     <TabulatorProperties>
                        <Default>0.0145</Default>
                        <UseOutsideTabs>True</UseOutsideTabs>
                        <Tabulator>
                            <Type>Left</Type>
                            <Pos>0.015</Pos>
                        </Tabulator>
                        <Tabulator>
                            <Type>Decimal</Type>
                            <Separator>.</Separator>
                            <Pos>0.035</Pos>
                        </Tabulator>
                    </TabulatorProperties>
                    <Hyphenation>
                        <Hyphenate>True</Hyphenate>
                    </Hyphenation>
                    <NumberingType>Reset</NumberingType>
                    <NumberingVariableId>SR_3</NumberingVariableId>
                    <NumberingFrom>12</NumberingFrom>
                    <CalcMaxSpaceBeforeAfter>True</CalcMaxSpaceBeforeAfter>
                    <DistributeLineSpace>True</DistributeLineSpace>
                    <DefaultTextStyleId>SR_4</DefaultTextStyleId>
                    <SpaceBeforeFirst>True</SpaceBeforeFirst>
                    <LineSpacing>0.03</LineSpacing>
                    <LineSpacingType>Exact</LineSpacingType>
                    <Type>Simple</Type>""")
    }

    def "textStyle is not set to paraStyle"() {
        when:
        ParagraphStyleImpl paraStyle = new ParagraphStyleImpl()

        then:
        assert paraStyle.getTextStyle() == null
    }

    def "paraStyle setIndent"() {
        when:
        ParagraphStyleImpl paraStyle = new ParagraphStyleImpl().setIndent(0.01d, 0.02d, 0.015d, 0.04d)

        then:
        paraStyle.getLeftIndent() == 0.04d
        paraStyle.getRightIndent() == 0.02d
        paraStyle.getSpaceAfter() == 0.015d
        paraStyle.getSpaceBefore() == 0.01d
    }

    def "paraStyle setAlignType"() {
        when:
        ParagraphStyleImpl paragraphStyle = new ParagraphStyleImpl().setAlignType(AlignType.JUSTIFY_WITH_MARGIN)

        then:
        assert paragraphStyle.convertAlignTypeToXmlName(AlignType.JUSTIFY_WITH_MARGIN) == "JustifyBlock2"
    }

    def "paraStyle setLineSpacingType"() {
        when:
        ParagraphStyleImpl paragraphStyle = new ParagraphStyleImpl().setLineSpacingType(LineSpacingType.MULTIPLE_OF)

        then:
        assert paragraphStyle.convertLineSpacingTypeToXmlName(LineSpacingType.MULTIPLE_OF) == "MultipleOf"
    }

    def "paraStyle setNumberingType"() {
        when:
        ParagraphStyleImpl paragraphStyle = new ParagraphStyleImpl().setNumberingType(NumberingType.INCREMENT)

        then:
        assert paragraphStyle.convertNumberingTypeToXmlName(NumberingType.INCREMENT) == "Increment"
    }

    def "paraStyle setLineSpacingValue"() {
        when:
        ParagraphStyleImpl paragraphStyle = new ParagraphStyleImpl().setLineSpacingValue(0.05d)

        then:
        assert paragraphStyle.getLineSpacingValue() == 0.05d
    }

    def "paraStyle setDistributeLineSpacing"() {
        when:
        ParagraphStyleImpl paragraphStyle = new ParagraphStyleImpl().setDistributeLineSpacing(true)

        then:
        assert paragraphStyle.isDistributeLineSpacing()
    }

    def "paraStyle setSpaceBefore"() {
        when:
        ParagraphStyleImpl paragraphStyle = new ParagraphStyleImpl().setSpaceBefore(0.010d)

        then:
        assert paragraphStyle.getSpaceBefore() == (double) 0.010
    }

    def "paraStyle setSpaceAfter"() {
        when:
        ParagraphStyleImpl paragraphStyle = new ParagraphStyleImpl().setSpaceAfter(0.0233275d)

        then:
        assert paragraphStyle.getSpaceAfter() == (double) 0.0233275
    }

    def "paraStyle  setFirstLineLeftIndent"() {
        when:
        ParagraphStyleImpl paraStyle = new ParagraphStyleImpl().setFirstLineLeftIndent(0.01d)

        then:
        assert paraStyle.getFirstLineLeftIndent() == (double) 0.01
    }

    def "paraStyle setSpaceBeforeFirstPara"() {
        when:
        ParagraphStyleImpl paraStyle = new ParagraphStyleImpl().setSpaceBeforeFirstPara(true)

        then:
        assert paraStyle.isSpaceBeforeFirstPara()
    }

    def "convertNumberingTypeToXmlName"() {
        expect:
        ParagraphStyleImpl.convertNumberingTypeToXmlName(numberingTypes) == expectedResult

        where:
        expectedResult | _
        "Increment"    | _
        "Reset"        | _
        "None"         | _

        numberingTypes << NumberingType.values().toList()
    }

    def "convertAlignTypeToXmlName"() {
        expect:
        ParagraphStyleImpl.convertAlignTypeToXmlName(alignTypes) == expectedResult

        where:
        expectedResult        | _
        "Left"                | _
        "Right"               | _
        "Center"              | _
        "JustifyLeft"         | _
        "JustifyRight"        | _
        "JustifyCenter"       | _
        "JustifyBlock"        | _
        "JustifyBlock2"       | _
        "JustifyBlockUniform" | _

        alignTypes << AlignType.values().toList()
    }

    def "convertLineSpacingTypeToXmlName"() {
        expect:
        ParagraphStyleImpl.convertLineSpacingTypeToXmlName(lineSpacingTypes) == expectedResult

        where:
        expectedResult          | _
        "Additional"            | _
        "Exact"                 | _
        "AtLeast"               | _
        "MultipleOf"            | _
        "ExactFromPrevWithAdj"  | _
        "ExactFromPrev"         | _
        "ExactFromPrevWithAdj2" | _

        lineSpacingTypes << LineSpacingType.values().toList()
    }

    /** In gui is firstLineLeftIndent relative to leftIndent, but in exported file is this value absolute */
    def "firstLineLeftIndent and leftIndent works in the same way as in designer GUI"() {
        given:
        ParagraphStyleImpl paraStyle = new ParagraphStyleImpl()
                .setLeftIndent(0.005d)
                .setFirstLineLeftIndent(0.002d)

        when:
        paraStyle.export(exporter)

        then:
        String result = exporter.buildString()
        result.contains("<LeftIndent>0.005</LeftIndent>")
        result.contains("<FirstLineLeftIndent>0.007</FirstLineLeftIndent>")
    }
}