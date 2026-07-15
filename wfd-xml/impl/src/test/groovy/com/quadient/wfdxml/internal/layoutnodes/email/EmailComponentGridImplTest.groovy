package com.quadient.wfdxml.internal.layoutnodes.email

import com.quadient.wfdxml.api.layoutnodes.FillStyle
import com.quadient.wfdxml.api.layoutnodes.email.EmailComponentContent
import com.quadient.wfdxml.api.layoutnodes.email.EmailComponentGrid
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.api.layoutnodes.email.EmailComponentGrid.ColumnDistribution
import static com.quadient.wfdxml.api.layoutnodes.email.EmailComponentGrid.OnMobile
import static com.quadient.wfdxml.api.layoutnodes.email.EmailComponentGrid.VerticalAlignment
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class EmailComponentGridImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "export empty EmailComponentGrid"() {
        given:
        EmailComponentGrid grid = new EmailComponentGridImpl()

        when:
        grid.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                <Columns/>
                <ColumnsCount>0</ColumnsCount>
                <FullWidthBackground>False</FullWidthBackground>
                <Distribution>None</Distribution>
                <VerticalAlignment>Top</VerticalAlignment>
                <OnMobile>FromLeft</OnMobile>
                <Padding>
                    <Left>0px</Left>
                    <Top>0px</Top>
                    <Right>0px</Right>
                    <Bottom>0px</Bottom>
                </Padding>
                <FillStyleId/>
                """)
    }

    def "export allSet EmailComponentGrid"() {
        given:
        EmailComponentContent col1 = Mock()
        EmailComponentContent col2 = Mock()
        EmailComponentContent col3 = Mock()
        FillStyle fillStyle = Mock()

        EmailComponentGrid grid = new EmailComponentGridImpl() as EmailComponentGrid
        grid.addColumn().addContent(col1)
        grid.addColumn().addContent(col2)
        grid.addColumn().addContent(col3)
        grid
                .setFullWidthBackground(true)
                .setDistribution(ColumnDistribution.THREE_COLUMNS_25_25_50)
                .setVerticalAlignment(VerticalAlignment.CENTER)
                .setOnMobile(OnMobile.NO_STACKING)
                .setPaddingLeft(1)
                .setPaddingTop(2)
                .setPaddingRight(3)
                .setPaddingBottom(4)
                .setFillStyle(fillStyle)

        String idCol1 = exporter.idRegister.getOrCreateId(col1)
        String idCol2 = exporter.idRegister.getOrCreateId(col2)
        String idCol3 = exporter.idRegister.getOrCreateId(col3)
        String idFillStyle = exporter.idRegister.getOrCreateId(fillStyle)

        when:
        (grid as EmailComponentGridImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                <Columns>
                    <Column>
                        <Component>$idCol1</Component>
                    </Column>
                    <Column>
                        <Component>$idCol2</Component>
                    </Column>
                    <Column>
                        <Component>$idCol3</Component>
                    </Column>
                </Columns>
                <ColumnsCount>3</ColumnsCount>
                <FullWidthBackground>True</FullWidthBackground>
                <Distribution>25-25-50</Distribution>
                <VerticalAlignment>Center</VerticalAlignment>
                <OnMobile>NoStacking</OnMobile>
                <Padding>
                    <Left>1px</Left>
                    <Top>2px</Top>
                    <Right>3px</Right>
                    <Bottom>4px</Bottom>
                </Padding>
                <FillStyleId>$idFillStyle</FillStyleId>
                """)
    }

    def "xml element name is ECGrid"() {
        expect:
        new EmailComponentGridImpl().getXmlElementName() == "ECGrid"
    }

    def "ColumnDistribution #distribution maps to XML #expected"() {
        given:
        EmailComponentGrid grid = new EmailComponentGridImpl() as EmailComponentGrid
        grid.setDistribution(distribution)

        when:
        (grid as EmailComponentGridImpl).export(exporter)

        then:
        exporter.buildString().contains("<Distribution>$expected</Distribution>")

        where:
        distribution                                | expected
        ColumnDistribution.EVEN_WIDTH               | "None"
        ColumnDistribution.TWO_COLUMNS_25_75        | "25-75"
        ColumnDistribution.TWO_COLUMNS_33_66        | "33-66"
        ColumnDistribution.TWO_COLUMNS_66_33        | "66-33"
        ColumnDistribution.TWO_COLUMNS_75_25        | "75-25"
        ColumnDistribution.THREE_COLUMNS_25_25_50   | "25-25-50"
        ColumnDistribution.THREE_COLUMNS_25_50_25   | "25-50-25"
        ColumnDistribution.THREE_COLUMNS_50_25_25   | "50-25-25"
    }

    def "VerticalAlignment #alignment maps to XML #expected"() {
        given:
        EmailComponentGrid grid = new EmailComponentGridImpl() as EmailComponentGrid
        grid.setVerticalAlignment(alignment)

        when:
        (grid as EmailComponentGridImpl).export(exporter)

        then:
        exporter.buildString().contains("<VerticalAlignment>$expected</VerticalAlignment>")

        where:
        alignment                  | expected
        VerticalAlignment.TOP      | "Top"
        VerticalAlignment.CENTER   | "Center"
        VerticalAlignment.BOTTOM   | "Bottom"
    }

    def "OnMobile #onMobile maps to XML #expected"() {
        given:
        EmailComponentGrid grid = new EmailComponentGridImpl() as EmailComponentGrid
        grid.setOnMobile(onMobile)

        when:
        (grid as EmailComponentGridImpl).export(exporter)

        then:
        exporter.buildString().contains("<OnMobile>$expected</OnMobile>")

        where:
        onMobile               | expected
        OnMobile.FROM_LEFT     | "FromLeft"
        OnMobile.FROM_RIGHT    | "FromRight"
        OnMobile.NO_STACKING   | "NoStacking"
    }
}
