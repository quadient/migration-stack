package com.quadient.wfdxml.internal.module

import com.quadient.wfdxml.api.Node
import com.quadient.wfdxml.api.layoutnodes.FillStyle
import com.quadient.wfdxml.api.layoutnodes.Flow
import com.quadient.wfdxml.api.layoutnodes.LocationType
import com.quadient.wfdxml.api.layoutnodes.TextStyle
import com.quadient.wfdxml.api.layoutnodes.data.DataType
import com.quadient.wfdxml.api.layoutnodes.data.VariableKind
import com.quadient.wfdxml.api.layoutnodes.tables.Cell
import com.quadient.wfdxml.api.layoutnodes.tables.RowSet
import com.quadient.wfdxml.api.layoutnodes.tables.Table
import com.quadient.wfdxml.api.module.Layout
import com.quadient.wfdxml.internal.Group
import com.quadient.wfdxml.internal.layoutnodes.TextStyleImpl
import com.quadient.wfdxml.internal.module.layout.LayoutImpl
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlFileEquals

class LayoutImplTest extends Specification {
    private XmlExporter exporter = new XmlExporter()

    def "empty layout serialization"() {
        given:
        def emptyLayout = new LayoutImpl()

        when:
        emptyLayout.export(exporter)

        then:
        assertXmlFileEquals('com/quadient/wfdxml/workflow/EmptyLayout.xml', exporter.buildString())
    }

    def "addTable"() {
        given:
        def layout = new LayoutImpl()

        when:
        Table table = layout.addTable()

        then:
        assert table instanceof Table
        assertAdd(layout, table, "Tables")
    }

    def "addRowSet"() {
        given:
        def layout = new LayoutImpl()

        when:
        RowSet rowSet = layout.addRowSet()

        then:
        assert rowSet instanceof RowSet
        assertAdd(layout, rowSet, "RowSets")
    }

    def "addCell"() {
        given:
        def layout = new LayoutImpl()

        when:
        Cell cell = layout.addCell()

        then:
        assert cell instanceof Cell
        assertAdd(layout, cell, "Cells")
    }

    void assertAdd(LayoutImpl layout, Node newNode, String expectedGroupName) {
        Group tables = layout.children.find { it.name == expectedGroupName } as Group
        assert tables.children.size() == 1
        assert tables.children[0].is(newNode)
    }


    def "export addBulletFlow"() {
        given:
        TextStyle textStyle = new TextStyleImpl()
        Layout layout = new LayoutImpl()
        layout.addBulletParagraph(textStyle, "l\t").setName("BullettingFlow")

        when:
        layout.export(exporter)

        then:
        String result = exporter.buildString()

        assert result.contains("BullettingFlow")
        result.contains("""l<Tab></Tab>""")

    }

    def "export addBulletParagraph"() {
        given:
        TextStyle textStyle = new TextStyleImpl()
        Layout layout = new LayoutImpl()
        layout.addBulletParagraph(textStyle, "m\t").setFirstLineLeftIndent(0.005d)

        when:
        layout.export(exporter)

        then:
        String result = exporter.buildString()

        assert result.contains("""m<Tab></Tab>""")
        result.contains("<FirstLineLeftIndent>0.005</FirstLineLeftIndent>")
    }

    def "addFillStyle without color set def.Color Black"() {
        when:
        Layout layout = new LayoutImpl()
        FillStyle fillStyle = layout.addFillStyle()

        then:
        fillStyle.getColor().getName() == "Black"
    }

    def "exportLayoutDelta exports single layout tag containing only relevant nodes"() {
        given:
        Layout layout = new LayoutImpl()
        layout.addPage().setName("ignoredPage")
        layout.addFlow().setName("includedFlow").setType(Flow.Type.SIMPLE)

        when:
        layout.exportLayoutDelta(exporter)

        then:
        assertXmlFileEquals("com/quadient/wfdxml/workflow/SimpleDeltaLayout.xml", exporter.buildString())
    }

    def "exportLayoutDelta exports image with ICM location and flow containing it"() {
        given:
        Layout layout = new LayoutImpl()
        def image = layout.addImage().setImageLocation("icm://testImage.jpg", LocationType.ICM)
        layout.addFlow().setName("ImageFlow").addParagraph().addText().appendImage(image)

        when:
        layout.exportLayoutDelta(exporter)

        then:
        assertXmlFileEquals("com/quadient/wfdxml/workflow/SimpleDeltaLayoutWithImage.xml", exporter.buildString())
    }

    def "node with id use it instead of generated id and is omitted from forward reference export"() {
        given:
        Layout layout = new LayoutImpl()
        layout.addFlow().setName("flowWithGeneratedId1").setType(Flow.Type.SIMPLE)
        layout.addFlow().setName("MainFlow").setType(Flow.Type.SIMPLE).setId("AnyId")
        layout.addFlow().setName("flowWithGeneratedId2").setType(Flow.Type.SIMPLE)

        when:
        layout.exportLayoutDelta(exporter)

        then:
        assertXmlFileEquals("com/quadient/wfdxml/workflow/SimpleDeltaLayoutWithCustomId.xml", exporter.buildString())
    }

    def "variable with existing parent id uses in its forward reference export"() {
        given:
        Layout layout = new LayoutImpl()
        layout.data.addVariable().setName("MyVar").setDataType(DataType.INT).setKind(VariableKind.DISCONNECTED).setExistingParentId("Data.Clients.Value")

        when:
        layout.exportLayoutDelta(exporter)
        String result = exporter.buildString()

        then:
        assert result.contains("<Variable><Id>SR_1</Id><Name>MyVar</Name><ParentId>Data.Clients.Value</ParentId><Forward useExisting=\"True\"></Forward></Variable>")
        assert result.contains("<Variable><Id>SR_1</Id><Type>Disconnected</Type><VarType>Int</VarType><Content>0</Content></Variable>")
    }

    def "node with display name"() {
        given:
        Layout layout = new LayoutImpl()
        layout.addFlow().setType(Flow.Type.SIMPLE).setDisplayName("Custom flow name")

        when:
        layout.exportLayoutDelta(exporter)
        String result = exporter.buildString()

        then:
        assert result.contains("<Flow><Id>SR_1</Id><ParentId>Def.FlowGroup</ParentId><CustomProperty>{&quot;DisplayName&quot;:&quot;Custom flow name&quot;}</CustomProperty><Forward></Forward></Flow>")
    }

    def "node with display name and additional custom property"() {
        given:
        Layout layout = new LayoutImpl()
        layout.addFlow().setType(Flow.Type.SIMPLE).setDisplayName("Flow with multiple properties").addCustomProperty("ValueWrapperVariable", true).addCustomProperty("Version", 2)

        when:
        layout.exportLayoutDelta(exporter)
        String result = exporter.buildString()

        then:
        assert result.contains("<Layout><Flow><Id>SR_1</Id><ParentId>Def.FlowGroup</ParentId><CustomProperty>{&quot;ValueWrapperVariable&quot;:true,&quot;Version&quot;:2,&quot;DisplayName&quot;:&quot;Flow with multiple properties&quot;}</CustomProperty><Forward></Forward></Flow><Color><Id>Def.Color</Id><RGB>0.0,0.0,0.0</RGB></Color><FillStyle><Id>Def.BlackFill</Id><ColorId>Def.Color</ColorId></FillStyle><Flow><Id>SR_1</Id><Type>Simple</Type><FlowContent Width=\"0.2\"></FlowContent><SectionFlow>False</SectionFlow></Flow><BorderStyle><Id>Def.BorderStyle</Id><FillStyleId>Def.BlackFill</FillStyleId><ShadowStyleId></ShadowStyleId><Margin><UpperLeft X=\"0.0\" Y=\"0.0\"></UpperLeft><LowerRight X=\"0.0\" Y=\"0.0\"></LowerRight></Margin><Offset><UpperLeft X=\"0.0\" Y=\"0.0\"></UpperLeft><LowerRight X=\"0.0\" Y=\"0.0\"></LowerRight></Offset><ShadowOffset X=\"0.0\" Y=\"0.0\"></ShadowOffset><JoinType>Miter</JoinType><Miter>10.0</Miter><LeftLine><FillStyle></FillStyle><LineWidth>2.0E-4</LineWidth><CapType>Butt</CapType><LineStyle></LineStyle></LeftLine><UpperLeftCorner><FillStyle></FillStyle><LineWidth>2.0E-4</LineWidth><CapType>Butt</CapType><LineStyle></LineStyle></UpperLeftCorner><TopLine><FillStyle></FillStyle><LineWidth>2.0E-4</LineWidth><CapType>Butt</CapType><LineStyle></LineStyle></TopLine><RightTopCorner><FillStyle></FillStyle><LineWidth>2.0E-4</LineWidth><CapType>Butt</CapType><LineStyle></LineStyle></RightTopCorner><RightLine><FillStyle></FillStyle><LineWidth>2.0E-4</LineWidth><CapType>Butt</CapType><LineStyle></LineStyle></RightLine><LowerRightCorner><FillStyle></FillStyle><LineWidth>2.0E-4</LineWidth><CapType>Butt</CapType><LineStyle></LineStyle></LowerRightCorner><BottomLine><FillStyle></FillStyle><LineWidth>2.0E-4</LineWidth><CapType>Butt</CapType><LineStyle></LineStyle></BottomLine><LowerLeftCorner><FillStyle></FillStyle><LineWidth>2.0E-4</LineWidth><CapType>Butt</CapType><LineStyle></LineStyle></LowerLeftCorner><LeftRightLine><FillStyle></FillStyle><LineWidth>2.0E-4</LineWidth><CapType>Butt</CapType><LineStyle></LineStyle></LeftRightLine><RightLeftLine><FillStyle></FillStyle><LineWidth>2.0E-4</LineWidth><CapType>Butt</CapType><LineStyle></LineStyle></RightLeftLine><UpperLeftCornerType><CornerType>StandardCorner</CornerType><CornerRadius X=\"0.0\" Y=\"0.0\"></CornerRadius></UpperLeftCornerType><UpperRightCornerType><CornerType>StandardCorner</CornerType><CornerRadius X=\"0.0\" Y=\"0.0\"></CornerRadius></UpperRightCornerType><LowerRightCornerType><CornerType>StandardCorner</CornerType><CornerRadius X=\"0.0\" Y=\"0.0\"></CornerRadius></LowerRightCornerType><LowerLeftCornerType><CornerType>StandardCorner</CornerType><CornerRadius X=\"0.0\" Y=\"0.0\"></CornerRadius></LowerLeftCornerType><Type>Simple</Type></BorderStyle><Data><Id>Def.Data</Id></Data></Layout>")
    }
}