package com.quadient.wfdxml.internal.layoutnodes

import com.quadient.wfdxml.api.layoutnodes.Section.ApplyTo
import com.quadient.wfdxml.api.layoutnodes.Section.BalancingType
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class SectionImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "default section serialization"() {
        given:
        SectionImpl section = new SectionImpl()

        when:
        section.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
            <ColumnType>AutomaticColumns</ColumnType>
            <BorderStyleId/>
            <EqualColumns>True</EqualColumns>
            <Column>
                <ColumnWidth>0.0</ColumnWidth>
                <GutterWidth>0.0</GutterWidth>
                <BorderStyleId/>
            </Column>
            <Column>
                <ColumnWidth>0.0</ColumnWidth>
                <GutterWidth>0.0</GutterWidth>
                <BorderStyleId/>
            </Column>
            <ColumnLineWidth>0.0</ColumnLineWidth>
            <ColumnFillStyleId/>
            <BalancingType>FirstColumnBiggest</BalancingType>
            <FirstHeader/>
            <Header/>
            <Footer/>
            <FirstFooter/>
            <ColumnType>AutomaticColumns</ColumnType>
            <SectionPageBreak>None</SectionPageBreak>
            <AutoFinish>True</AutoFinish>
            """)
    }

    def "section with three columns serializes correct number of Column elements"() {
        given:
        SectionImpl section = new SectionImpl()
                .setNumberOfColumns(3)

        when:
        section.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
            <ColumnType>AutomaticColumns</ColumnType>
            <BorderStyleId/>
            <EqualColumns>True</EqualColumns>
            <Column>
                <ColumnWidth>0.0</ColumnWidth>
                <GutterWidth>0.0</GutterWidth>
                <BorderStyleId/>
            </Column>
            <Column>
                <ColumnWidth>0.0</ColumnWidth>
                <GutterWidth>0.0</GutterWidth>
                <BorderStyleId/>
            </Column>
            <Column>
                <ColumnWidth>0.0</ColumnWidth>
                <GutterWidth>0.0</GutterWidth>
                <BorderStyleId/>
            </Column>
            <ColumnLineWidth>0.0</ColumnLineWidth>
            <ColumnFillStyleId/>
            <BalancingType>FirstColumnBiggest</BalancingType>
            <FirstHeader/>
            <Header/>
            <Footer/>
            <FirstFooter/>
            <ColumnType>AutomaticColumns</ColumnType>
            <SectionPageBreak>None</SectionPageBreak>
            <AutoFinish>True</AutoFinish>
            """)
    }

    def "section with custom gutter width serializes gutter in each column"() {
        given:
        SectionImpl section = new SectionImpl()
                .setGutterWidth(0.005)

        when:
        section.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
            <ColumnType>AutomaticColumns</ColumnType>
            <BorderStyleId/>
            <EqualColumns>True</EqualColumns>
            <Column>
                <ColumnWidth>0.0</ColumnWidth>
                <GutterWidth>0.005</GutterWidth>
                <BorderStyleId/>
            </Column>
            <Column>
                <ColumnWidth>0.0</ColumnWidth>
                <GutterWidth>0.005</GutterWidth>
                <BorderStyleId/>
            </Column>
            <ColumnLineWidth>0.0</ColumnLineWidth>
            <ColumnFillStyleId/>
            <BalancingType>FirstColumnBiggest</BalancingType>
            <FirstHeader/>
            <Header/>
            <Footer/>
            <FirstFooter/>
            <ColumnType>AutomaticColumns</ColumnType>
            <SectionPageBreak>None</SectionPageBreak>
            <AutoFinish>True</AutoFinish>
            """)
    }

    def "section with applyTo WholeTemplate serializes AutoFinish False"() {
        given:
        SectionImpl section = new SectionImpl()
                .setApplyTo(ApplyTo.WHOLE_TEMPLATE)

        when:
        section.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
            <ColumnType>AutomaticColumns</ColumnType>
            <BorderStyleId/>
            <EqualColumns>True</EqualColumns>
            <Column>
                <ColumnWidth>0.0</ColumnWidth>
                <GutterWidth>0.0</GutterWidth>
                <BorderStyleId/>
            </Column>
            <Column>
                <ColumnWidth>0.0</ColumnWidth>
                <GutterWidth>0.0</GutterWidth>
                <BorderStyleId/>
            </Column>
            <ColumnLineWidth>0.0</ColumnLineWidth>
            <ColumnFillStyleId/>
            <BalancingType>FirstColumnBiggest</BalancingType>
            <FirstHeader/>
            <Header/>
            <Footer/>
            <FirstFooter/>
            <ColumnType>AutomaticColumns</ColumnType>
            <SectionPageBreak>None</SectionPageBreak>
            <AutoFinish>False</AutoFinish>
            """)
    }

    def "section with applyTo ThisBlockOnly serializes AutoFinish True"() {
        given:
        SectionImpl section = new SectionImpl()
                .setApplyTo(ApplyTo.THIS_BLOCK_ONLY)

        when:
        section.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
            <ColumnType>AutomaticColumns</ColumnType>
            <BorderStyleId/>
            <EqualColumns>True</EqualColumns>
            <Column><ColumnWidth>0.0</ColumnWidth><GutterWidth>0.0</GutterWidth><BorderStyleId/></Column>
            <Column><ColumnWidth>0.0</ColumnWidth><GutterWidth>0.0</GutterWidth><BorderStyleId/></Column>
            <ColumnLineWidth>0.0</ColumnLineWidth>
            <ColumnFillStyleId/>
            <BalancingType>FirstColumnBiggest</BalancingType>
            <FirstHeader/>
            <Header/>
            <Footer/>
            <FirstFooter/>
            <ColumnType>AutomaticColumns</ColumnType>
            <SectionPageBreak>None</SectionPageBreak>
            <AutoFinish>True</AutoFinish>
            """)
    }

    def "section with Balanced balancing type serializes Balanced"() {
        given:
        SectionImpl section = new SectionImpl()
                .setBalancingType(BalancingType.BALANCED)

        when:
        section.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
            <ColumnType>AutomaticColumns</ColumnType>
            <BorderStyleId/>
            <EqualColumns>True</EqualColumns>
            <Column><ColumnWidth>0.0</ColumnWidth><GutterWidth>0.0</GutterWidth><BorderStyleId/></Column>
            <Column><ColumnWidth>0.0</ColumnWidth><GutterWidth>0.0</GutterWidth><BorderStyleId/></Column>
            <ColumnLineWidth>0.0</ColumnLineWidth>
            <ColumnFillStyleId/>
            <BalancingType>Balanced</BalancingType>
            <FirstHeader/>
            <Header/>
            <Footer/>
            <FirstFooter/>
            <ColumnType>AutomaticColumns</ColumnType>
            <SectionPageBreak>None</SectionPageBreak>
            <AutoFinish>True</AutoFinish>
            """)
    }

    def "section with Unbalanced balancing type serializes Unbalanced"() {
        given:
        SectionImpl section = new SectionImpl()
                .setBalancingType(BalancingType.UNBALANCED)

        when:
        section.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
            <ColumnType>AutomaticColumns</ColumnType>
            <BorderStyleId/>
            <EqualColumns>True</EqualColumns>
            <Column><ColumnWidth>0.0</ColumnWidth><GutterWidth>0.0</GutterWidth><BorderStyleId/></Column>
            <Column><ColumnWidth>0.0</ColumnWidth><GutterWidth>0.0</GutterWidth><BorderStyleId/></Column>
            <ColumnLineWidth>0.0</ColumnLineWidth>
            <ColumnFillStyleId/>
            <BalancingType>Unbalanced</BalancingType>
            <FirstHeader/>
            <Header/>
            <Footer/>
            <FirstFooter/>
            <ColumnType>AutomaticColumns</ColumnType>
            <SectionPageBreak>None</SectionPageBreak>
            <AutoFinish>True</AutoFinish>
            """)
    }

    def "setting numberOfColumns to zero throws exception"() {
        when:
        new SectionImpl().setNumberOfColumns(0)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message.contains("numberOfColumns must be >= 1")
    }

    def "section with all values set serializes correctly"() {
        given:
        SectionImpl section = new SectionImpl()
                .setNumberOfColumns(3)
                .setGutterWidth(0.01)
                .setBalancingType(BalancingType.BALANCED)
                .setApplyTo(ApplyTo.WHOLE_TEMPLATE)

        when:
        section.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
            <ColumnType>AutomaticColumns</ColumnType>
            <BorderStyleId/>
            <EqualColumns>True</EqualColumns>
            <Column>
                <ColumnWidth>0.0</ColumnWidth>
                <GutterWidth>0.01</GutterWidth>
                <BorderStyleId/>
            </Column>
            <Column>
                <ColumnWidth>0.0</ColumnWidth>
                <GutterWidth>0.01</GutterWidth>
                <BorderStyleId/>
            </Column>
            <Column>
                <ColumnWidth>0.0</ColumnWidth>
                <GutterWidth>0.01</GutterWidth>
                <BorderStyleId/>
            </Column>
            <ColumnLineWidth>0.0</ColumnLineWidth>
            <ColumnFillStyleId/>
            <BalancingType>Balanced</BalancingType>
            <FirstHeader/>
            <Header/>
            <Footer/>
            <FirstFooter/>
            <ColumnType>AutomaticColumns</ColumnType>
            <SectionPageBreak>None</SectionPageBreak>
            <AutoFinish>False</AutoFinish>
            """)
    }
}
