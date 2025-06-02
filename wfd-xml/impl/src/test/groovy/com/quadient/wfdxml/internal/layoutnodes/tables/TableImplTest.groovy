package com.quadient.wfdxml.internal.layoutnodes.tables

import com.quadient.wfdxml.api.layoutnodes.tables.Table
import com.quadient.wfdxml.internal.layoutnodes.BorderStyleImpl
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.api.layoutnodes.tables.RowSet.Type.MULTIPLE_ROWS
import static com.quadient.wfdxml.api.layoutnodes.tables.Table.BordersType.SIMPLE
import static com.quadient.wfdxml.api.layoutnodes.tables.Table.EditabilityType.LOCK
import static com.quadient.wfdxml.api.layoutnodes.tables.Table.TableAlignment.CENTER
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class TableImplTest extends Specification {

    XmlExporter exporter = new XmlExporter()

    def "export elementName"() {
        when:
        String xmlElementName = new TableImpl().xmlElementName

        then:
        assert xmlElementName == "Table"
    }

    def "export empty table"() {
        given:
        Table table = new TableImpl()

        when:
        table.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
                <RowSetId/>
                <BorderId/>
                <HorizontalCellSpacing>0.0</HorizontalCellSpacing>
                <VerticalCellSpacing>0.0</VerticalCellSpacing>
                <MinWidth>0.001</MinWidth>
                <MaxWidth>300.0</MaxWidth>
                <PercentWidth>100.0</PercentWidth>
                <SpaceLeft>0.0</SpaceLeft>
                <SpaceTop>0.0</SpaceTop>
                <SpaceRight>0.0</SpaceRight>
                <SpaceBottom>0.0</SpaceBottom>
                <TableAlignment>Left</TableAlignment>
                <BordersType>MergeBorders</BordersType>
                <IncludeLineGap>False</IncludeLineGap>
                <UseColumnWidths>True</UseColumnWidths>
                <HTMLFormatting>False</HTMLFormatting>
                <DisplayAsImage>True</DisplayAsImage>
                <ResponsiveHtml>False</ResponsiveHtml>
                <Editability>LabelAndLock</Editability>            
              """)
    }

    def "export simple table 2x2 created by designer"() {
        given:
        Table table = new TableImpl()
        table.addColumn(0.01, 0.3)
                .addColumn(0.01, 0.3)
                .setRowSet(new RowSetImpl().setType(MULTIPLE_ROWS)
                        .addRowSet(new RowSetImpl().addCell(new CellImpl()).addCell(new CellImpl()))
                        .addRowSet(new RowSetImpl().addCell(new CellImpl()).addCell(new CellImpl()))
                )

        when:
        table.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
                <RowSetId>SR_1</RowSetId>
                <BorderId/>
                <HorizontalCellSpacing>0.0</HorizontalCellSpacing>
                <VerticalCellSpacing>0.0</VerticalCellSpacing>
                <MinWidth>0.001</MinWidth>
                <MaxWidth>300.0</MaxWidth>
                <PercentWidth>100.0</PercentWidth>
                <SpaceLeft>0.0</SpaceLeft>
                <SpaceTop>0.0</SpaceTop>
                <SpaceRight>0.0</SpaceRight>
                <SpaceBottom>0.0</SpaceBottom>
                <TableAlignment>Left</TableAlignment>
                <BordersType>MergeBorders</BordersType>
                <IncludeLineGap>False</IncludeLineGap>
                <UseColumnWidths>True</UseColumnWidths>
                <ColumnWidths>
                  <MinWidth>0.01</MinWidth>
                  <PercentWidth>0.3</PercentWidth>
                </ColumnWidths>
                <ColumnWidths>
                  <MinWidth>0.01</MinWidth>
                  <PercentWidth>0.3</PercentWidth>
                </ColumnWidths>
                <HTMLFormatting>False</HTMLFormatting>
                <DisplayAsImage>True</DisplayAsImage>
                
                <EnableById/>
                <EnableById/>
                <ResponsiveHtml>False</ResponsiveHtml>
                <Editability>LabelAndLock</Editability>
                <IsHeader>False</IsHeader>
                <IsHeader>False</IsHeader>            
              """)
    }

    def "export table with all values"() {
        given:
        Table table = new TableImpl()
        table.setRowSet(new RowSetImpl())
                .setBorderStyle(new BorderStyleImpl())
                .setBorderType(SIMPLE)
                .setAlignment(CENTER)
                .setPercentWidth(88.8)
                .setMinWidth(1.12)
                .setMaxWidth(0.15645)
                .setIncludeLineGap(true)
                .setApplyHtmlFormatting(true)
                .setResponsiveHtml(true)
                .setHorizontalSpacing(0.0001)
                .setVerticalSpacing(0.0002)
                .setSpaceLeft(0.0003)
                .setSpaceTop(0.0004)
                .setSpaceRight(0.0005)
                .setSpaceBottom(0.0006)
                .setDisplayAsImage(false)
                .setEditability(LOCK)

        when:
        table.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
            <RowSetId>SR_1</RowSetId>
            <BorderId>SR_2</BorderId>
            <HorizontalCellSpacing>1.0E-4</HorizontalCellSpacing>
            <VerticalCellSpacing>2.0E-4</VerticalCellSpacing>
            <MinWidth>1.12</MinWidth>
            <MaxWidth>0.15645</MaxWidth>
            <PercentWidth>88.8</PercentWidth>
            <SpaceLeft>3.0E-4</SpaceLeft>
            <SpaceTop>4.0E-4</SpaceTop>
            <SpaceRight>5.0E-4</SpaceRight>
            <SpaceBottom>6.0E-4</SpaceBottom>
            <TableAlignment>Center</TableAlignment>
            <BordersType>Simple</BordersType>
            <IncludeLineGap>True</IncludeLineGap>
            <UseColumnWidths>True</UseColumnWidths>
            <HTMLFormatting>True</HTMLFormatting>
            <DisplayAsImage>False</DisplayAsImage>
            
            <ResponsiveHtml>True</ResponsiveHtml>
            <Editability>Lock</Editability>       
                  """)
    }

    def "export table magic checkboxes for disabling values"() {
        given:
        Table table = new TableImpl()
        table.setPercentWidthDisable()
                .setMinWidthDisable()
                .setMaxWidthDisable()

        when:
        table.export(exporter)

        then:
        String result = exporter.buildString()
        result.contains("<MinWidth>0.0</MinWidth>")
        result.contains("<MaxWidth>0.0</MaxWidth>")
        result.contains("<PercentWidth>-1.0</PercentWidth>")
    }
}