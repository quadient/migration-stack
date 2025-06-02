package com.quadient.wfdxml.internal.layoutnodes.tables

import com.quadient.wfdxml.api.layoutnodes.tables.Cell
import com.quadient.wfdxml.internal.layoutnodes.BorderStyleImpl
import com.quadient.wfdxml.internal.layoutnodes.FlowImpl
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.api.layoutnodes.tables.Cell.CellType.CUSTOM
import static com.quadient.wfdxml.api.layoutnodes.tables.Cell.CellType.FIXED_HEIGHT
import static com.quadient.wfdxml.api.layoutnodes.tables.Cell.CellVerticalAlignment.CENTER
import static com.quadient.wfdxml.api.layoutnodes.tables.Cell.FittingType.VERTICAL
import static com.quadient.wfdxml.api.layoutnodes.tables.Cell.HTMLWidthType.LENGTH
import static com.quadient.wfdxml.api.layoutnodes.tables.Cell.HTMLWidthType.PERCENT
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class CellImplTest extends Specification {

    XmlExporter exporter = new XmlExporter()

    def "export elementName"() {
        when:
        String xmlElementName = new CellImpl().xmlElementName

        then:
        assert xmlElementName == "Cell"
    }

    def "export empty cell"() {
        given:
        Cell cell = new CellImpl()

        when:
        cell.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
            <FlowId/>            
            <BorderId/>
            <WithFixedHeight>False</WithFixedHeight>
            <AlwaysProcess>True</AlwaysProcess>
            <MinWidth>0.001</MinWidth>
            <MaxWidth>300.0</MaxWidth>
            <RatioWidth>1.0</RatioWidth>
            <MinHeight>0.0</MinHeight>
            <MaxHeight>300.0</MaxHeight>
            <FlowToNextPage>False</FlowToNextPage>
            <SpanLeft>False</SpanLeft>
            <SpanUp>False</SpanUp>
            <CellVerticalAlignment>Top</CellVerticalAlignment>
            <FittingType>None</FittingType>
            <RelativeFill>True</RelativeFill>
            <HtmlWidthType>Auto</HtmlWidthType>
            <HtmlWidthValue>100.0</HtmlWidthValue>
          """)
    }

    def "export cell with all values set"() {
        given:
        Cell cell = new CellImpl().setFlow(new FlowImpl())
                .setBorderStyle(new BorderStyleImpl())
                .setAlignment(CENTER)
                .setFitting(VERTICAL)
                .setType(FIXED_HEIGHT)
                .setFixedHeight(0.0008)
                .setHtmlWidth(LENGTH)
                .setFixedWidth(2.22200)
                .setSpanLeft(true)
                .setSpanUp(true)
                .setFlowToNextPage(true)
                .setAlwaysProcess(false)
                .setFillRelativeToCell(false)

        when:
        (cell as CellImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
            <FlowId>SR_1</FlowId>
            <BorderId>SR_2</BorderId>
            <WithFixedHeight>True</WithFixedHeight>
            <AlwaysProcess>False</AlwaysProcess>
            <MinWidth>0.001</MinWidth>
            <MaxWidth>300.0</MaxWidth>
            <RatioWidth>1.0</RatioWidth>
            <MinHeight>8.0E-4</MinHeight>
            <MaxHeight>300.0</MaxHeight>
            <FlowToNextPage>True</FlowToNextPage>
            <SpanLeft>True</SpanLeft>
            <SpanUp>True</SpanUp>
            <CellVerticalAlignment>Center</CellVerticalAlignment>
            <FittingType>Vertical</FittingType>
            <RelativeFill>False</RelativeFill>
            <HtmlWidthType>Length</HtmlWidthType>
            <HtmlWidthValue>2.222</HtmlWidthValue>
          """)
    }


    def "export cell with CUSTOM type and html PERCENT"() {
        given:
        Cell cell = new CellImpl().setType(CUSTOM)
                .setMinHeight(0.02)
                .setMaxHeight(1.1)
                .setHtmlWidth(PERCENT)
                .setWidthInPercent(88.8)

        when:
        (cell as CellImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
            <FlowId/>    
            <BorderId/>
            <WithFixedHeight>False</WithFixedHeight>
            <AlwaysProcess>True</AlwaysProcess>
            <MinWidth>0.001</MinWidth>
            <MaxWidth>300.0</MaxWidth>
            <RatioWidth>1.0</RatioWidth>
            <MinHeight>0.02</MinHeight>
            <MaxHeight>1.1</MaxHeight>
            <FlowToNextPage>False</FlowToNextPage>
            <SpanLeft>False</SpanLeft>
            <SpanUp>False</SpanUp>
            <CellVerticalAlignment>Top</CellVerticalAlignment>
            <FittingType>None</FittingType>
            <RelativeFill>True</RelativeFill>
            <HtmlWidthType>Percent</HtmlWidthType>
            <HtmlWidthValue>88.8</HtmlWidthValue>           
          """)
    }

    def "export cell with htmlWidth LENGTH"() {
        given:
        Cell cell = new CellImpl().setHtmlWidth(LENGTH)
                .setFixedWidth(11.5)

        when:
        (cell as CellImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
                <FlowId/>
                <BorderId/>
                <WithFixedHeight>False</WithFixedHeight>
                <AlwaysProcess>True</AlwaysProcess>
                <MinWidth>0.001</MinWidth>
                <MaxWidth>300.0</MaxWidth>
                <RatioWidth>1.0</RatioWidth>
                <MinHeight>0.0</MinHeight>
                <MaxHeight>300.0</MaxHeight>
                <FlowToNextPage>False</FlowToNextPage>
                <SpanLeft>False</SpanLeft>
                <SpanUp>False</SpanUp>
                <CellVerticalAlignment>Top</CellVerticalAlignment>
                <FittingType>None</FittingType>
                <RelativeFill>True</RelativeFill>
                <HtmlWidthType>Length</HtmlWidthType>
                <HtmlWidthValue>11.5</HtmlWidthValue>               
              """)
    }
}