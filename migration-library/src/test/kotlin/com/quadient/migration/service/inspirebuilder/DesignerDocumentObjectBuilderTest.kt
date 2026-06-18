package com.quadient.migration.service.inspirebuilder

import tools.jackson.databind.JsonNode
import tools.jackson.dataformat.xml.XmlMapper
import tools.jackson.module.kotlin.KotlinModule
import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.FirstMatch
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.Table
import com.quadient.migration.api.dto.migrationmodel.TextStyle
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.api.dto.migrationmodel.builder.DisplayRuleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.ShapeBuilder
import com.quadient.migration.api.repository.AttachmentRepository
import com.quadient.migration.api.repository.DisplayRuleRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ImageRepository
import com.quadient.migration.api.repository.ParagraphStyleRepository
import com.quadient.migration.api.repository.TextStyleRepository
import com.quadient.migration.api.repository.VariableRepository
import com.quadient.migration.api.repository.VariableStructureRepository
import com.quadient.migration.service.DesignerIcmDataCache
import com.quadient.migration.service.DesignerResourcePathProvider
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.shared.BinOp
import com.quadient.migration.shared.BorderLine
import com.quadient.migration.shared.BorderOptions
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.DataType
import com.quadient.migration.shared.DocumentObjectType.*
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.Literal
import com.quadient.migration.shared.LiteralDataType
import com.quadient.migration.shared.PageOptions
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.SkipOptions
import com.quadient.migration.shared.TableAlignment
import com.quadient.migration.shared.TablePdfTaggingRule
import com.quadient.migration.shared.VariablePathData
import com.quadient.migration.shared.centimeters
import com.quadient.migration.shared.millimeters
import com.quadient.migration.shared.toIcmPath
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.aTable
import com.quadient.migration.tools.getFlowAreaContentFlow
import com.quadient.migration.tools.getFlowAreaContentFlowId
import com.quadient.migration.tools.model.aCell
import com.quadient.migration.tools.model.aVariable
import com.quadient.migration.tools.model.aDisplayRule
import com.quadient.migration.tools.model.aDocObj
import com.quadient.migration.tools.model.aDocumentObjectRef
import com.quadient.migration.tools.model.aImage
import com.quadient.migration.tools.model.aParagraph
import com.quadient.migration.tools.model.aRow
import com.quadient.migration.tools.model.aSelectByLanguage
import com.quadient.migration.tools.model.aText
import com.quadient.migration.tools.model.aTextDef
import com.quadient.migration.tools.model.aTextStyle
import com.quadient.migration.tools.model.aVariableStructure
import com.quadient.migration.tools.model.anArea
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeOfSize
import com.quadient.migration.tools.shouldNotBeEmpty
import com.quadient.migration.tools.shouldNotBeNull
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DesignerDocumentObjectBuilderTest {
    val documentObjectRepository = mockk<DocumentObjectRepository>()
    val textStyleRepository = mockk<TextStyleRepository>()
    val paragraphStyleRepository = mockk<ParagraphStyleRepository>()
    val variableRepository = mockk<VariableRepository>()
    val variableStructureRepository = mockk<VariableStructureRepository>()
    val displayRuleRepository = mockk<DisplayRuleRepository>()
    val imageRepository = mockk<ImageRepository>()
    val attachmentRepository = mockk<AttachmentRepository>()
    val ipsService = mockk<IpsService>()
    val config = aProjectConfig(targetDefaultFolder = "defaultFolder", output = InspireOutput.Designer)
    val resourcePathProvider = DesignerResourcePathProvider(config)
    val icmDataCache = DesignerIcmDataCache(ipsService, resourcePathProvider)

    private val subject = aSubject(config)

    private val xmlMapper = XmlMapper.builder().addModule(KotlinModule.Builder().build()).build()

    @BeforeEach
    fun setUp() {
        every { variableStructureRepository.listAll() } returns emptyList()
        every { textStyleRepository.listAll() } returns emptyList()
        every { paragraphStyleRepository.listAll() } returns emptyList()
        every { ipsService.gatherFontData(any()) } returns "Arial,Regular,icm://Fonts/arial.ttf;"
        every { ipsService.fileExists(any<IcmPath>()) } returns false
    }

    @Test
    fun `buildDocumentObject correctly constructs complex template structure with pages and standalone block`() {
        // given
        val block = aDocObj(
            "B_1", Block, listOf(
                aParagraph(aText(StringValue("Hello there!")))
            ), true
        ).mock()

        val standaloneBlock = aDocObj("B_2", Block, listOf(aParagraph(aText(StringValue("I am alone"))))).mock()
        val page = aDocObj(
            "P_1", Page, listOf(
                anArea(
                    listOf(aDocumentObjectRef(block.id)),
                    Position(20.millimeters(), 25.millimeters(), 160.millimeters(), 10.centimeters())
                )
            ), options = PageOptions(20.centimeters(), 25.centimeters())
        ).mock()
        val template = aDocObj(
            "T_1", Template, listOf(
                aDocumentObjectRef(page.id), aDocumentObjectRef(standaloneBlock.id)
            )
        ).mock()

        // when
        val result =
            subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        result["Page"].size().shouldBeEqualTo(4)
        val pageId = result["Page"].first { it["Name"].stringValue() == page.nameOrId() }["Id"].stringValue()
        val pageData = result["Page"].last { it["Id"].stringValue() == pageId }
        pageData["Width"].stringValue().shouldBeEqualTo("0.2")
        pageData["Height"].stringValue().shouldBeEqualTo("0.25")

        val virtualPageId = result["Page"].first { it["Name"].stringValue() == "Virtual Page" }["Id"].stringValue()

        val pageFlowAreaId = result["FlowArea"].first { it["ParentId"].stringValue() == pageId }["Id"].stringValue()
        val pageFlowArea = result["FlowArea"].last { it["Id"].stringValue() == pageFlowAreaId }
        pageFlowArea["Pos"]["X"].stringValue().shouldBeEqualTo("0.02")
        pageFlowArea["Pos"]["Y"].stringValue().shouldBeEqualTo("0.025")
        pageFlowArea["Size"]["X"].stringValue().shouldBeEqualTo("0.16")
        pageFlowArea["Size"]["Y"].stringValue().shouldBeEqualTo("0.1")
        val flowId = pageFlowArea["FlowId"].stringValue()
        val contentFlowId = getFlowAreaContentFlowId(result, flowId)
        result["Flow"].first { it["Id"].stringValue() == contentFlowId }["Name"].stringValue().shouldBeEqualTo(block.nameOrId())
        result["Flow"].last { it["Id"].stringValue() == contentFlowId }["FlowContent"]["P"]["T"][""].stringValue()
            .shouldBeEqualTo("Hello there!")

        val virtualPageFlowAreaId =
            result["FlowArea"].first { it["ParentId"].stringValue() == virtualPageId }["Id"].stringValue()
        val virtualPageFlowArea = result["FlowArea"].last { it["Id"].stringValue() == virtualPageFlowAreaId }
        virtualPageFlowArea["Pos"]["X"].stringValue().shouldBeEqualTo("0.015")
        virtualPageFlowArea["Pos"]["Y"].stringValue().shouldBeEqualTo("0.015")
        virtualPageFlowArea["Size"]["X"].stringValue().shouldBeEqualTo("0.18")
        virtualPageFlowArea["Size"]["Y"].stringValue().shouldBeEqualTo("0.267")
        val virtualFlowId = virtualPageFlowArea["FlowId"].stringValue()
        val virtualContentFlowId = getFlowAreaContentFlowId(result, virtualFlowId)
        result["Flow"].first { it["Id"].stringValue() == virtualContentFlowId }["Name"].stringValue()
            .shouldBeEqualTo(standaloneBlock.nameOrId())
        result["Flow"].last { it["Id"].stringValue() == virtualContentFlowId }["ExternalLocation"].stringValue()
            .shouldBeEqualTo("icm://${config.defaultTargetFolder}/${standaloneBlock.nameOrId()}.wfd")

        result["Root"]["AllowRuntimeModifications"].stringValue().shouldBeEqualTo("True")
        result["Pages"]["MainFlow"].stringValue().shouldBeEqualTo(flowId)
    }

    @Test
    fun `buildDocumentObject creates flow area with placeholder text in case of invalid image`() {
        // given
        val image = aImage("Img_1", sourcePath = null, skip = SkipOptions(true, "img placeholder", null)).mock()
        val page = aDocObj(
            "P_1", Page,
            listOf(
                anArea(
                    listOf(ImageRef(image.id)),
                    Position(60.millimeters(), 60.millimeters(), 10.centimeters(), 10.centimeters()),
                )
            ),
        ).mock()
        val template = aDocObj("T_1", Template, listOf(aDocumentObjectRef(page.id))).mock()

        // when
        val result =
            subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        result["Image"].shouldBeEqualTo(null)
        val flowArea = result["FlowArea"].last()
        flowArea["Pos"]["X"].stringValue().shouldBeEqualTo("0.06")
        flowArea["Pos"]["Y"].stringValue().shouldBeEqualTo("0.06")
        flowArea["Size"]["X"].stringValue().shouldBeEqualTo("0.1")
        flowArea["Size"]["Y"].stringValue().shouldBeEqualTo("0.1")
        val flowId = flowArea["FlowId"].stringValue()

        val flow = result["Flow"].last { it["Id"].stringValue() == flowId }
        flow["FlowContent"]["P"]["T"][""].stringValue().shouldBeEqualTo("img placeholder")
    }

    @Test
    fun `buildDocumentObject creates image area with image in case of flow area only with valid image ref`() {
        val Image = aImage("Img_1").mock()
        val page = aDocObj(
            "P_1", Page, listOf(
                anArea(
                    listOf(
                        ImageRef(Image.id)
                    ), Position(60.millimeters(), 120.millimeters(), 20.centimeters(), 10.centimeters())
                ),
            )
        ).mock()
        val template = aDocObj("T_1", Template, listOf(aDocumentObjectRef(page.id))).mock()

        // when
        val result =
            subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val imageObject = result["ImageObject"].last()
        imageObject["Pos"]["X"].stringValue().shouldBeEqualTo("0.06")
        imageObject["Pos"]["Y"].stringValue().shouldBeEqualTo("0.12")
        imageObject["Size"]["X"].stringValue().shouldBeEqualTo("0.2")
        imageObject["Size"]["Y"].stringValue().shouldBeEqualTo("0.1")
        val imageId = imageObject["ImageId"].stringValue()

        val image = result["Image"].last { it["Id"].stringValue() == imageId }
        image["ImageLocation"].stringValue()
            .shouldBeEqualTo("VCSLocation,icm://${config.defaultTargetFolder}/${Image.nameOrId()}.jpg")
    }

    @Test
    fun `buildDocumentObject creates image with alternate text from Image`() {
        // given
        val Image = aImage("Img_1", alternateText = "Description of the image").mock()
        val page = aDocObj(
            "P_1", Page, listOf(
                anArea(
                    listOf(
                        ImageRef(Image.id)
                    ), Position(60.millimeters(), 120.millimeters(), 20.centimeters(), 10.centimeters())
                ),
            )
        ).mock()
        val template = aDocObj("T_1", Template, listOf(aDocumentObjectRef(page.id))).mock()

        // when
        val result =
            subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val imageObject = result["ImageObject"].last()
        val imageId = imageObject["ImageId"].stringValue()

        val image = result["Image"].last { it["Id"].stringValue() == imageId }
        image["PDFAdvanced"]["Tagging"]["AlternateText"].stringValue().shouldBeEqualTo("Description of the image")
    }

    @Test
    fun `buildDocumentObject creates path object with geometry and path definitions`() {
        // given
        val obj = ShapeBuilder()
            .name("shape-1")
            .position { left(20.millimeters()); top(30.millimeters()); width(40.millimeters()); height(50.millimeters()) }
            .moveTo(1.millimeters(), 2.millimeters())
            .lineTo(3.millimeters(), 4.millimeters())
            .conicTo(5.millimeters(), 6.millimeters(), 7.millimeters(), 8.millimeters())
            .bezierTo(9.millimeters(), 10.millimeters(), 11.millimeters(), 12.millimeters(), 13.millimeters(), 14.millimeters())
            .fill(Color.fromHex("#FF0000"))
            .lineFill(Color.fromHex("#00FF00"))
            .lineWidth(0.7.millimeters())
            .build()

        val page = DocumentObjectBuilder("P_1", Page)
            .shape(obj)
            .build()
            .mock()
        val template = DocumentObjectBuilder("T_1", Template).documentObjectRef(page).build().mock()

        // when
        val xml = subject.buildDocumentObject(template)
        val result = xmlMapper.readTree(xml.trimIndent())["Layout"]["Layout"]

        // then
        result["PathObject"].first()["Name"].stringValue().shouldBeEqualTo("shape-1")

        val resultShape = result["PathObject"].last()
        resultShape["Pos"]["X"].stringValue().shouldBeEqualTo("0.02")
        resultShape["Pos"]["Y"].stringValue().shouldBeEqualTo("0.03")
        resultShape["Size"]["X"].stringValue().shouldBeEqualTo("0.04")
        resultShape["Size"]["Y"].stringValue().shouldBeEqualTo("0.05")
        resultShape["LineWidth"].stringValue().toDouble().shouldBeEqualTo(0.0007)
        val fillColor = result.getColorForFillStyle(resultShape["FillStyleId"].stringValue())
        fillColor["RGB"].stringValue().shouldBeEqualTo("1.0,0.0,0.0")
        val lineColor = result.getColorForFillStyle(resultShape["OutlineStyleId"].stringValue())
        lineColor["RGB"].stringValue().shouldBeEqualTo("0.0,1.0,0.0")

        val path = resultShape["Path"].toList()
        val first = path[1]
        first["X"].stringValue().shouldBeEqualTo("0.001")
        first["Y"].stringValue().shouldBeEqualTo("0.002")
        val second = path[2]
        second["X"].stringValue().shouldBeEqualTo("0.003")
        second["Y"].stringValue().shouldBeEqualTo("0.004")
        val third = path[3]
        third["X"].stringValue().shouldBeEqualTo("0.007")
        third["Y"].stringValue().shouldBeEqualTo("0.008")
        third["X1"].stringValue().shouldBeEqualTo("0.005")
        third["Y1"].stringValue().shouldBeEqualTo("0.006")
        val fourth = path[4]
        fourth["X"].stringValue().shouldBeEqualTo("0.013")
        fourth["Y"].stringValue().shouldBeEqualTo("0.014")
        fourth["X1"].stringValue().shouldBeEqualTo("0.011")
        fourth["Y1"].stringValue().shouldBeEqualTo("0.012")
        fourth["X2"].stringValue().shouldBeEqualTo("0.009")
        fourth["Y2"].stringValue().shouldBeEqualTo("0.01")
    }

    @Test
    fun `buildDocumentObject uses inline condition flow when block is used with display rule`() {
        // given
        val rule = aDisplayRule(
            Literal("A", LiteralDataType.String), BinOp.Equals, Literal("B", LiteralDataType.String)
        ).mock()
        val block = aDocObj(
            "B_1", Block, listOf(aParagraph(aText(StringValue("Hi"))))
        ).mock()
        val template = aDocObj("T_1", Template, listOf(aDocumentObjectRef(block.id, rule.id))).mock()

        // when
        val result =
            subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val contentFlow = getFlowAreaContentFlow(result)
        contentFlow["Type"].stringValue().shouldBeEqualTo("InlCond")
        val condition = contentFlow["Condition"]
        condition["Value"].stringValue().shouldBeEqualTo("return (String('A')==String('B'));")
        val successFlowId = condition[""].stringValue()
        result["Flow"].first { it["Id"].stringValue() == successFlowId }["Name"].stringValue()
            .shouldBeEqualTo(block.nameOrId())
    }

    @Test
    fun `buildDocumentObject uses inline condition row when table row is under display rule`() {
        // given
        val rule = aDisplayRule(
            Literal("A", LiteralDataType.String), BinOp.Equals, Literal("B", LiteralDataType.String)
        ).mock()
        val block = aDocObj(
            "B_1", Block, listOf(
                Table(
                    listOf(
                        aRow(
                            listOf(
                                aCell(aParagraph(aText(StringValue("A")))),
                                aCell(aParagraph(aText(StringValue("B"))))
                            )
                        ), aRow(
                            listOf(
                                aCell(aParagraph(aText(StringValue("C")))),
                                aCell(aParagraph(aText(StringValue("D"))))
                            ), rule.id
                        )
                    ),
                    listOf(),
                    pdfTaggingRule = TablePdfTaggingRule.Table,
                    pdfAlternateText = "Table alt text",
                    firstHeader = emptyList(),
                    footer = emptyList(),
                    lastFooter = emptyList(),
                    columnWidths = emptyList(),
                    minWidth = null,
                    maxWidth = null,
                    percentWidth = null,
                    border = null,
                    alignment = TableAlignment.Left,
                )
            )
        ).mock()

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val rowSetId = result["Table"].last()["RowSetId"].stringValue()
        val rowIds = result["RowSet"].last { it["Id"].stringValue() == rowSetId }["SubRowId"]
        rowIds.size().shouldBeEqualTo(2)

        val secondRow = result["RowSet"].last { it["Id"].stringValue() == rowIds[1].stringValue() }
        secondRow["RowSetType"].stringValue().shouldBeEqualTo("InlCond")
        secondRow["RowSetCondition"][0]["Condition"].stringValue().shouldBeEqualTo("return (String('A')==String('B'));")
        val pdfAdvanced = result["Table"].last()["PDFAdvanced"]
        pdfAdvanced.shouldNotBeNull()
        pdfAdvanced["Tagging"]["Rule"].stringValue().shouldBeEqualTo("Table")
        pdfAdvanced["Tagging"]["AlternateText"].stringValue().shouldBeEqualTo("Table alt text")
    }

    @Test
    fun `buildDocumentObject creates twice used block only once`() {
        // given
        val block = aDocObj("B_1", Block, listOf(aParagraph(aText(StringValue("Hi"))))).mock()
        val template = aDocObj(
            "T_1", Template, listOf(
                aDocumentObjectRef(block.id), aDocumentObjectRef(block.id)
            )
        ).mock()

        // when
        val result =
            subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        result["Flow"].filter { it["Name"]?.stringValue() == block.nameOrId() }.size.shouldBeEqualTo(1)
    }

    @Test
    fun `buildDocumentObject correctly sets table style name`() {
        val block = DocumentObjectBuilder("T1", Block)
            .table { tableStyleName("testTableStyle1").addRow { addCell { string("table1") } } }
            .table { tableStyleName("testTableStyle1").addRow { addCell { string("table2") } } }
            .table { tableStyleName("testTableStyle2").addRow { addCell { string("table3") } } }
            .build()

        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        val tableStyles = result["TableStyle"].filter { it["Name"]?.stringValue() != null }.map { it["Name"].stringValue() }.toSet()
        tableStyles.shouldBeEqualTo(setOf("testTableStyle1", "testTableStyle2"))
        val tables = result["Table"].filter { it["TableStyleId"]?.stringValue() != null }.map { it["TableStyleId"].stringValue() }
        tables.shouldBeOfSize(3)
        tables.toSet().shouldBeEqualTo(setOf("Others.testTableStyle1", "Others.testTableStyle2"))
    }

    @Test
    fun `buildDocumentObject correctly sets table widths`() {
        val table = aTable(
            minWidth = Size.ofMillimeters(111),
            maxWidth = Size.ofMillimeters(222),
            percentWidth = 66.6
        )
        val block = aDocObj("T1", Block, listOf(table)).mock()

        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        result["Table"].last()["MinWidth"].stringValue().toDouble().shouldBeEqualTo(0.111)
        result["Table"].last()["MaxWidth"].stringValue().toDouble().shouldBeEqualTo(0.222)
        result["Table"].last()["PercentWidth"].stringValue().toDouble().shouldBeEqualTo(66.6)
    }

    @Test
    fun `buildDocumentObject correctly sets table alignment`() {
        val table = aTable(alignment = TableAlignment.Center)
        val block = aDocObj("T1", Block, listOf(table)).mock()

        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        result["Table"].last()["TableAlignment"].stringValue().shouldBeEqualTo("Center")
    }

    @Test
    fun `buildDocumentObject correctly sets table border options`() {
        val table = aTable(border = BorderOptions(
            leftLine = BorderLine(color = Color(255, 0, 0), width = Size.ofMillimeters(0.5)),
            rightLine = BorderLine(color = Color(0, 255, 0), width = Size.ofMillimeters(0.3)),
            topLine = BorderLine(color = Color(0, 0, 255), width = Size.ofMillimeters(0.2)),
            bottomLine = BorderLine(color = Color(255, 255, 0), width = Size.ofMillimeters(0.4)),
            paddingTop = Size.ofMillimeters(1),
            paddingBottom = Size.ofMillimeters(2),
            paddingLeft = Size.ofMillimeters(3),
            paddingRight = Size.ofMillimeters(4),
            fill = Color(128, 128, 128),
        ))
        val block = aDocObj("T1", Block, listOf(table)).mock()

        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        val borderId = result["Table"].last()["BorderId"].stringValue()
        val borderStyle = result["BorderStyle"]?.last { it["Id"]?.stringValue() == borderId }
        val fillStyleId = borderStyle?.get("FillStyleId")?.stringValue()
        val fillStyle = result["FillStyle"]?.last { it["Id"]?.stringValue() == fillStyleId }
        val colorId = fillStyle?.get("ColorId")?.stringValue()
        val color = result["Color"]?.last { it["Id"]?.stringValue() == colorId }

        color?.get("RGB")?.stringValue().shouldBeEqualTo("0.5019607843137255,0.5019607843137255,0.5019607843137255")

        borderStyle?.get("Margin")["UpperLeft"]["Y"]?.stringValue().shouldBeEqualTo("0.001")
        borderStyle?.get("Margin")["LowerRight"]["Y"]?.stringValue().shouldBeEqualTo("0.002")
        borderStyle?.get("Margin")["UpperLeft"]["X"]?.stringValue().shouldBeEqualTo("0.003")
        borderStyle?.get("Margin")["LowerRight"]["X"]?.stringValue().shouldBeEqualTo("0.004")

        result.assertLine(borderId, "LeftLine", expectedColor = Color(255, 0, 0), expectedWidth = 5.0E-4)
        result.assertLine(borderId, "RightLine", expectedColor = Color(0, 255, 0), expectedWidth = 3.0E-4)
        result.assertLine(borderId, "TopLine", expectedColor = Color(0, 0, 255), expectedWidth = 2.0E-4)
        result.assertLine(borderId, "BottomLine", expectedColor = Color(255, 255, 0), expectedWidth = 4.0E-4)
    }

    @Test
    fun `buildDocumentObject correctly sets border options for a cell`() {
        // given
        val bodyRow = aRow(listOf(aCell(aParagraph("test"), border = BorderOptions(
            leftLine = BorderLine(color = Color(255, 0, 0), width = Size.ofMillimeters(0.5)),
            rightLine = BorderLine(color = Color(0, 255, 0), width = Size.ofMillimeters(0.3)),
            topLine = BorderLine(color = Color(0, 0, 255), width = Size.ofMillimeters(0.2)),
            bottomLine = BorderLine(color = Color(255, 255, 0), width = Size.ofMillimeters(0.4)),
            paddingTop = Size.ofMillimeters(1),
            paddingBottom = Size.ofMillimeters(2),
            paddingLeft = Size.ofMillimeters(3),
            paddingRight = Size.ofMillimeters(4),
            fill = Color(128, 128, 128),
        ))))
        val table = aTable(rows = listOf(bodyRow))
        val block = aDocObj("T1", Block, listOf(table)).mock()

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val borderId = result["Cell"].last()["BorderId"].stringValue()
        val borderStyle = result["BorderStyle"]?.last { it["Id"]?.stringValue() == borderId }
        val fillStyleId = borderStyle?.get("FillStyleId")?.stringValue()
        val fillStyle = result["FillStyle"]?.last { it["Id"]?.stringValue() == fillStyleId }
        val colorId = fillStyle?.get("ColorId")?.stringValue()
        val color = result["Color"]?.last { it["Id"]?.stringValue() == colorId }

        color?.get("RGB")?.stringValue().shouldBeEqualTo("0.5019607843137255,0.5019607843137255,0.5019607843137255")

        borderStyle?.get("Margin")["UpperLeft"]["Y"]?.stringValue().shouldBeEqualTo("0.001")
        borderStyle?.get("Margin")["LowerRight"]["Y"]?.stringValue().shouldBeEqualTo("0.002")
        borderStyle?.get("Margin")["UpperLeft"]["X"]?.stringValue().shouldBeEqualTo("0.003")
        borderStyle?.get("Margin")["LowerRight"]["X"]?.stringValue().shouldBeEqualTo("0.004")

        result.assertLine(borderId, "LeftLine", expectedColor = Color(255, 0, 0), expectedWidth = 5.0E-4)
        result.assertLine(borderId, "RightLine", expectedColor = Color(0, 255, 0), expectedWidth = 3.0E-4)
        result.assertLine(borderId, "TopLine", expectedColor = Color(0, 0, 255), expectedWidth = 2.0E-4)
        result.assertLine(borderId, "BottomLine", expectedColor = Color(255, 255, 0), expectedWidth = 4.0E-4)
    }

    @Test
    fun `buildDocumentObject correctly creates header, first header, footer and last footer rows`() {
        // given
        val headerRow = aRow(listOf(aCell(aParagraph(aText(StringValue("Header"))))) )
        val firstHeaderRow = aRow(listOf(aCell(aParagraph(aText(StringValue("FirstHeader"))))) )
        val bodyRow = aRow(listOf(aCell(aParagraph(aText(StringValue("Body"))))) )
        val footerRow = aRow(listOf(aCell(aParagraph(aText(StringValue("Footer"))))) )
        val lastFooterRow = aRow(listOf(aCell(aParagraph(aText(StringValue("LastFooter"))))) )
        val table = aTable(
            rows = listOf(bodyRow),
            header = listOf(headerRow),
            firstHeader = listOf(firstHeaderRow),
            footer = listOf(footerRow),
            lastFooter = listOf(lastFooterRow),
        )
        val block = aDocObj("T1", Block, listOf(table)).mock()

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val tableNode = result["Table"].last()
        val rowSetId = tableNode["RowSetId"].stringValue()
        val rowSet = result["RowSet"].last { it["Id"].stringValue() == rowSetId }
        val subRowIds = rowSet["SubRowId"]
        subRowIds.size().shouldBeEqualTo(5) // first header, header, body, footer, last footer

        result.assertRowContent(subRowIds[0].stringValue(), "FirstHeader")
        result.assertRowContent(subRowIds[1].stringValue(), "Header")
        result.assertRowContent(subRowIds[2].stringValue(), "Body")
        result.assertRowContent(subRowIds[3].stringValue(), "Footer")
        result.assertRowContent(subRowIds[4].stringValue(), "LastFooter")
    }

    private fun JsonNode.assertRowContent(rowSetId: String, expectedText: String) {
        val rowSet = this["RowSet"].last { it["Id"].stringValue() == rowSetId }
        val cell = this["Cell"].last { it["Id"].stringValue() == rowSet["SubRowId"].stringValue() }
        val flowId = cell["FlowId"].stringValue()
        val flow = this["Flow"].last { it["Id"].stringValue() == flowId }
        flow["FlowContent"]["P"]["T"][""].stringValue().shouldBeEqualTo(expectedText)
    }

    private fun JsonNode?.assertLine(borderStyleId: String, line: String, expectedColor: Color, expectedWidth: Double) {
        val borderStyle = this?.get("BorderStyle")?.last { it["Id"]?.stringValue() == borderStyleId }

        val lineFillStyleId = borderStyle?.get(line)?.get("FillStyle")?.stringValue()
        val lineFillStyle = this?.get("FillStyle")?.last { it["Id"]?.stringValue() == lineFillStyleId }
        val lineColorId = lineFillStyle?.get("ColorId")?.stringValue()
        val lineColor = this?.get("Color")?.last { it["Id"]?.stringValue() == lineColorId }
        lineColor?.get("RGB")?.stringValue().shouldBeEqualTo("${expectedColor.red.toDouble() / 255.0},${expectedColor.green.toDouble() / 255.0},${expectedColor.blue.toDouble() / 255.0}")

        borderStyle?.get(line)["LineWidth"]?.stringValue()?.toDouble().shouldBeEqualTo(expectedWidth)
    }

    private fun JsonNode.getColorForFillStyle(fillStyleId: String): JsonNode {
        val fillStyle = this["FillStyle"].last { it["Id"]?.stringValue() == fillStyleId }
        val colorId = fillStyle["ColorId"].stringValue()
        return this["Color"].last { it["Id"]?.stringValue() == colorId }
    }

    @Test
    fun `buildDocumentObject names multiple composite flows with numbers`() {
        // given
        val innerBlock = aDocObj("B_2", Block, listOf(aParagraph(aText(StringValue("In between")))), internal = true).mock()
        val block = aDocObj(
            "B_1", Block, listOf(
                aParagraph(aText(StringValue("Hi"))),
                aDocumentObjectRef(innerBlock.id),
                aParagraph(aText(StringValue("Bye")))
            ), internal = true
        ).mock()
        val template = aDocObj("T_1", Template, listOf(aDocumentObjectRef(block.id))).mock()

        // when
        val result =
            subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val areaFlowId = result["FlowArea"].last()["FlowId"].stringValue()
        result["Flow"].first { it["Id"].stringValue() == areaFlowId }["Name"].stringValue()
            .shouldBeEqualTo(block.nameOrId())
        val areaFlowRefs = result["Flow"].last { it["Id"].stringValue() == areaFlowId }["FlowContent"]["P"]["T"]["O"]

        result["Flow"].first { it["Id"].stringValue() == areaFlowRefs[0]["Id"].stringValue() }["Name"].stringValue()
            .shouldBeEqualTo("${block.nameOrId()} 1")
        result["Flow"].first { it["Id"].stringValue() == areaFlowRefs[1]["Id"].stringValue() }["Name"].stringValue()
            .shouldBeEqualTo(innerBlock.nameOrId())
        result["Flow"].first { it["Id"].stringValue() == areaFlowRefs[2]["Id"].stringValue() }["Name"].stringValue()
            .shouldBeEqualTo("${block.nameOrId()} 2")
    }

    @Test
    fun `snippet is inlined for designer output`() {
        val snippet = DocumentObjectBuilder("S_1", Snippet)
            .internal(false)
            .string("snippet text")
            .build()
            .mock()
        val block = DocumentObjectBuilder("B_1", Block)
            .paragraph { string("Bye") }
            .documentObjectRef(snippet)
            .internal(true)
            .build()
            .mock()

        val template = DocumentObjectBuilder("T_1", Template).documentObjectRef(block).build().mock()

        val result =
            subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        val mainFlowId = result["Pages"]["MainFlow"].stringValue()
        val mainFlow = result["Flow"].last { it["Id"].stringValue() == mainFlowId }
        val subFlow1Id = mainFlow["FlowContent"]["P"]["T"]["O"][0]["Id"].stringValue()
        val subFlow1 = result["Flow"].last { it["Id"].stringValue() == subFlow1Id }
        val subFlow2Id = mainFlow["FlowContent"]["P"]["T"]["O"][1]["Id"].stringValue()
        val subFlow2 = result["Flow"].last { it["Id"].stringValue() == subFlow2Id }

        subFlow2["FlowContent"]["P"]["T"][""].stringValue().shouldBeEqualTo("snippet text")
        subFlow1["FlowContent"]["P"]["T"][""].stringValue().shouldBeEqualTo("Bye")
    }

    @Test
    fun `buildDocumentObject creates multiple times used variable only once`() {
        // given
        val variable = aVariable("V_1").mock()

        val block1 = aDocObj(
            "B_1", Block, listOf(
                aParagraph(
                    aText(
                        listOf(StringValue("First usage: "), VariableRef(variable.id))
                    )
                )
            ), true
        ).mock()
        val block2 = aDocObj(
            "B_2", Block, listOf(
                aParagraph(
                    aText(
                        listOf(StringValue("Second usage: "), VariableRef(variable.id))
                    )
                )
            ), true
        ).mock()
        val template = aDocObj(
            "T_1", Template, listOf(
                aDocumentObjectRef(block1.id), aDocumentObjectRef(block2.id)
            )
        ).mock()
        val varStructure = aVariableStructure(
            structure = mapOf(
                variable.id to VariablePathData("Data.Records.Value")
            )
        ).mock()
        val config = aProjectConfig(defaultVariableStructure = varStructure.id)

        // when
        val subject = aSubject(config)
        val result =
            subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val variableData = result["Variable"].single { it["Name"]?.stringValue() == variable.nameOrId() }
        variableData["ParentId"].stringValue().shouldBeEqualTo("Data.Records.Value")
        val variableId = variableData["Id"].stringValue()

        val firstBlockId = result["Flow"].first { it["Name"].stringValue() == block1.nameOrId() }["Id"].stringValue()
        val firstBlockContent = result["Flow"].last { it["Id"].stringValue() == firstBlockId }["FlowContent"]["P"]["T"]
        firstBlockContent[""].stringValue().shouldBeEqualTo("First usage: ")
        firstBlockContent["O"]["Id"].stringValue().shouldBeEqualTo(variableId)

        val secondBlockId = result["Flow"].first { it["Name"].stringValue() == block2.nameOrId() }["Id"].stringValue()
        val secondBlockContent = result["Flow"].last { it["Id"].stringValue() == secondBlockId }["FlowContent"]["P"]["T"]
        secondBlockContent[""].stringValue().shouldBeEqualTo("Second usage: ")
        secondBlockContent["O"]["Id"].stringValue().shouldBeEqualTo(variableId)
    }

    @Test
    fun `block with first match is built to inline condition flow with multiple options`() {
        // given
        val defaultFlowModel = aDocObj("B_10", Block, listOf(aParagraph(aText(StringValue("I am default"))))).mock()
        val rule1 = aDisplayRule(
            Literal("A", LiteralDataType.String), BinOp.Equals, Literal("B", LiteralDataType.String), id = "R_1"
        ).mock()
        val flow1 = aDocObj("B_11", Block, listOf(aParagraph(aText(StringValue("flow 1 content")))), internal = false).mock()

        val rule2 = aDisplayRule(
            Literal("C", LiteralDataType.String), BinOp.Equals, Literal("C", LiteralDataType.String), id = "R_2"
        ).mock()

        val block = aDocObj(
            "B_1", Block, listOf(
                FirstMatch(
                    cases = listOf(
                        FirstMatch.Case(
                            DisplayRuleRef(rule1.id), listOf(aDocumentObjectRef(flow1.id)), null
                        ), FirstMatch.Case(
                            DisplayRuleRef(rule2.id),
                            listOf(aParagraph(aText(StringValue("flow 2 content")))),
                            null
                        )
                    ), default = defaultFlowModel.content
                )
            )
        ).mock()

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val conditionFlow = result["Flow"].last { it["Type"]?.stringValue() == "InlCond" }
        val conditions = conditionFlow["Condition"]
        conditions.size().shouldBeEqualTo(2)

        conditions[0]["Value"].stringValue().shouldBeEqualTo("return (String('A')==String('B'));")
        val firstConditionFlowId = conditions[0][""].stringValue()

        val firstConditionFlow = result["Flow"].last { it["Id"].stringValue() == firstConditionFlowId }
        firstConditionFlow["WebEditingType"].stringValue().shouldBeEqualTo("Section")
        val firstConditionContentFlowId = firstConditionFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()

        result["Flow"].last { it["Id"].stringValue() == firstConditionContentFlowId }["ExternalLocation"].stringValue()
            .shouldBeEqualTo("icm://${config.defaultTargetFolder}/${flow1.nameOrId()}.wfd")

        conditions[1]["Value"].stringValue().shouldBeEqualTo("return (String('C')==String('C'));")
        val secondConditionFlowId = conditions[1][""].stringValue()

        val secondConditionFlow = result["Flow"].last { it["Id"].stringValue() == secondConditionFlowId }
        secondConditionFlow["WebEditingType"].stringValue().shouldBeEqualTo("Section")
        val secondConditionContentFlowId = secondConditionFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()

        result["Flow"].last { it["Id"].stringValue() == secondConditionContentFlowId }["FlowContent"]["P"]["T"][""].stringValue()
            .shouldBeEqualTo("flow 2 content")

        val defaultFlowId = conditionFlow["Default"].stringValue()

        val defaultFlow = result["Flow"].last { it["Id"].stringValue() == defaultFlowId }
        defaultFlow["WebEditingType"].stringValue().shouldBeEqualTo("Section")
        val defaultContentFlowId = defaultFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()

        result["Flow"].last { it["Id"].stringValue() == defaultContentFlowId }["FlowContent"]["P"]["T"][""].stringValue()
            .shouldBeEqualTo("I am default")
    }

    @Test
    fun `snippet with first match produces inline condition flow without section wrapper flows`() {
        // given
        val rule1 = DisplayRuleBuilder("R_1").comparison { value("A").equals().value("B") }.build().mock()
        val rule2 = DisplayRuleBuilder("R_2").comparison { value("C").equals().value("C") }.build().mock()

        val snippet = DocumentObjectBuilder("S_1", Snippet)
            .firstMatch {
                case {
                    displayRuleRef(rule1)
                    string("case 1 content")
                }
                case {
                    displayRuleRef(rule2)
                    string("case 2 content")
                }
                defaultParagraph { string("default content") }
            }
            .build().mock()

        val template = DocumentObjectBuilder("T_1", Template).documentObjectRef(snippet).build().mock()

        // when
        val result = subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val conditionFlow = result["Flow"].last { it["Type"]?.stringValue() == "InlCond" }
        val conditions = conditionFlow["Condition"]
        conditions.size().shouldBeEqualTo(2)

        val caseFlow = result["Flow"].last { it["Id"].stringValue() == conditions[0][""].stringValue() }
        caseFlow["SectionFlow"]?.stringValue().shouldBeEqualTo("False")
        caseFlow["FlowContent"]["P"]["T"][""].stringValue().shouldBeEqualTo("case 1 content")
    }

    @Test
    fun `build block under display rule has its content wrapped in inline condition`() {
        // given
        val rule = aDisplayRule(Literal("A", LiteralDataType.String), BinOp.Equals, Literal("B", LiteralDataType.String)).mock()

        val block = aDocObj("B_1", Block, listOf(aParagraph(aText(StringValue("Text")))), displayRuleRef = rule.id).mock()

        // when
        val result =
            subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val contentFlow = getFlowAreaContentFlow(result)
        contentFlow["Type"].stringValue().shouldBeEqualTo("InlCond")
        result["Flow"].last { it["Id"].stringValue() == contentFlow["Condition"][""].stringValue() }["FlowContent"]["P"]["T"][""].stringValue()
            .shouldBeEqualTo("Text")
    }

    @Test
    fun `build block using source base template enriches layout with other modules`() {
        // given
        val block = aDocObj("B_1", Block, listOf(aParagraph(aText("Text")))).mock()
        val config = aProjectConfig(sourceBaseTemplatePath = "icm://sourceBaseTemplate.wfd")

        every { ipsService.wfd2xml("icm://sourceBaseTemplate.wfd".toIcmPath()) } returns """
        <Workflow>
          <Property>
            <Name>AFPApplyMediumOrientation</Name>
            <Value>1</Value>
          </Property>
          <Property>
            <Name>AFPApplyPageTransformation</Name>
            <Value>1</Value>
          </Property>
          <Property>
            <Name>PreviewTypes</Name>
            <Value>HtmlPreview&#xa;PagePreview&#xa;SMSPreview</Value>
          </Property>
          <DataInput>
            <Id>DataInput1</Id>
            <Name>DataInput1</Name>
            <ModulePos X="11" Y="12"/>
          </DataInput>
          <Layout>
            <Id>Layout1</Id>
            <Name>Layout1</Name>
            <ModulePos X="45" Y="19"/>
            <Layout>
            </Layout>
          </Layout>
        </Workflow>
        """.trimIndent()

        // when
        val subject = aSubject(config)
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        result["Property"].size().shouldBeEqualTo(3)
        result["Property"].first { it["Name"].stringValue() == "PreviewTypes" }["Value"].stringValue()
            .shouldBeEqualTo("HtmlPreview\nPagePreview\nSMSPreview")
        result["DataInput"].shouldNotBeNull()
        val layout = result["Layout"]["Layout"]
        val contentFlow = getFlowAreaContentFlow(layout)
        contentFlow["FlowContent"]["P"]["T"][""].stringValue().shouldBeEqualTo("Text")
    }

    @Test
    fun `loading of source base template is cached during single run`() {
        // given
        val block1 = aDocObj("B_1", Block, listOf(aParagraph(aText("Text")))).mock()
        val block2 = aDocObj("B_2", Block, listOf(aParagraph(aText("Text")))).mock()
        val config = aProjectConfig(sourceBaseTemplatePath = "icm://sourceBaseTemplate.wfd")

        every { ipsService.wfd2xml("icm://sourceBaseTemplate.wfd".toIcmPath()) } returns """
        <Workflow>
          <Property>
            <Name>AFPApplyPageTransformation</Name>
            <Value>1</Value>
          </Property>
          <DataInput>
            <Id>DataInput1</Id>
            <Name>DataInput1</Name>
          </DataInput>
          <Layout>
            <Id>Layout1</Id>
            <Name>Layout1</Name>
            <Layout>
            </Layout>
          </Layout>
        </Workflow>
        """.trimIndent()

        // when
        val subject = aSubject(config)
        subject.buildDocumentObject(block1)
        subject.buildDocumentObject(block2)

        // then
        verify(exactly = 1) { ipsService.wfd2xml(any<IcmPath>()) }
    }

    @Test
    fun `block uses the assigned variable structure`() {
        // given
        val variable = aVariable("V_1").mock()
        val varNoPath = aVariable("V_2").mock()
        val variableStructureA = aVariableStructure(
            "VS_1", structure = mapOf(
                variable.id to VariablePathData("Data.Records.Value"),
                varNoPath.id to VariablePathData("", "No Path Variable")
            )
        ).mock()
        val variableStructureB = aVariableStructure(
            "VS_2", structure = mapOf(variable.id to VariablePathData("Data.Clients.Value"))
        ).mock()
        every { variableStructureRepository.listAll() } returns listOf(variableStructureA, variableStructureB)
        val config = aProjectConfig(defaultVariableStructure = variableStructureB.id)

        val block = aDocObj(
            "B_1", Block, listOf(
                aParagraph(
                    aText(
                        listOf(
                            StringValue("Text"), VariableRef(variable.id), VariableRef(varNoPath.id)
                        )
                    )
                )
            ), VariableStructureRef = variableStructureA.id
        ).mock()

        // when
        val subject = aSubject(config)
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        verify(exactly = 1) { variableStructureRepository.findOrFail(variableStructureA.id) }
        verify(exactly = 0) { variableStructureRepository.findOrFail(variableStructureB.id) }

        result["Variable"].first { it["Name"].stringValue() == variable.nameOrId() }["ParentId"].stringValue()
            .shouldBeEqualTo("Data.Records.Value")

        val flow = result["Flow"].first { it["Type"]?.stringValue() == "Simple" }
        flow["FlowContent"]["P"]["T"][""][0].stringValue().shouldBeEqualTo("Text")
        flow["FlowContent"]["P"]["T"][""][1].stringValue().shouldBeEqualTo("\$No Path Variable\$")
    }

    @Test
    fun `buildDocumentObject creates QR barcode on page with correct position and properties`() {
        // given
        val page = DocumentObjectBuilder("P_1", Page)
            .barcode {
                qr {
                    data("012345")
                    position { left(20.millimeters()); top(30.millimeters()); width(29.millimeters()); height(29.millimeters()) }
                    errorCorrection(com.quadient.migration.shared.QrCodeErrorCorrectionLevel.M)
                    moduleWidth(Size.ofMillimeters(1))
                    quietZone(Size.ofMillimeters(4))
                }
            }
            .build()
            .mock()
        val template = DocumentObjectBuilder("T_1", Template).documentObjectRef(page).build().mock()

        // when
        val result = subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val barcode = result["Barcode"].last()
        barcode["Pos"]["X"].stringValue().shouldBeEqualTo("0.02")
        barcode["Pos"]["Y"].stringValue().shouldBeEqualTo("0.03")
        barcode["Size"]["X"].stringValue().shouldBeEqualTo("0.029")
        barcode["Size"]["Y"].stringValue().shouldBeEqualTo("0.029")
        barcode["BarcodeName"].stringValue().shouldBeEqualTo("QR")
        barcode["ConvertString"].stringValue().shouldBeEqualTo("012345")
    }

    @Test
    fun `buildDocumentObject creates Code39 barcode on page with correct barcode name`() {
        // given
        val page = DocumentObjectBuilder("P_1", Page)
            .barcode {
                code39 {
                    data("ABC123")
                    position { left(10.millimeters()); top(10.millimeters()); width(50.millimeters()); height(20.millimeters()) }
                }
            }
            .build()
            .mock()
        val template = DocumentObjectBuilder("T_1", Template).documentObjectRef(page).build().mock()

        // when
        val result = subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val barcode = result["Barcode"].last()
        barcode["BarcodeName"].stringValue().shouldBeEqualTo("Code 39")
        barcode["ConvertString"].stringValue().shouldBeEqualTo("ABC123")
        barcode["Pos"]["X"].stringValue().shouldBeEqualTo("0.01")
        barcode["Pos"]["Y"].stringValue().shouldBeEqualTo("0.01")
    }

    @Test
    fun `buildDocumentObject with barcode driven by variable creates barcode with variable reference`() {
        // given
        val barcodeVar = aVariable("barcodeVar", name = "BarcodeData", dataType = DataType.String).mock()
        val variableStructure = aVariableStructure(
            structure = mapOf(barcodeVar.id to com.quadient.migration.shared.VariablePathData("Data.Records.Value"))
        ).mock()

        val page = DocumentObjectBuilder("P_1", Page)
            .barcode {
                qr {
                    variableRef(barcodeVar)
                    position { left(20.millimeters()); top(30.millimeters()); width(29.millimeters()); height(29.millimeters()) }
                }
            }
            .build()
            .mock()

        val template = DocumentObjectBuilder("T_1", Template)
            .variableStructureRef(variableStructure.id)
            .documentObjectRef(page)
            .build()
            .mock()

        // when
        val result = subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val barcode = result["Barcode"].last()
        barcode["BarcodeName"].stringValue().shouldBeEqualTo("QR")
        val variableId = barcode["VariableId"].stringValue()
        variableId.shouldNotBeEmpty()
        val variable = result["Variable"].first { it["Id"].stringValue() == variableId }
        variable["Name"].stringValue().shouldBeEqualTo("BarcodeData")
    }

    @Test
    fun `buildDocumentObject with page containing barcode and text creates both elements`() {
        // given
        val page = DocumentObjectBuilder("P_1", Page)
            .barcode {
                qr {
                    data("test-data")
                    position { left(20.millimeters()); top(30.millimeters()); width(29.millimeters()); height(29.millimeters()) }
                }
            }
            .string("Some text on the page")
            .build()
            .mock()
        val template = DocumentObjectBuilder("T_1", Template).documentObjectRef(page).build().mock()

        // when
        val result = subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        result["Barcode"].shouldNotBeNull()
        result["Barcode"].last()["BarcodeName"].stringValue().shouldBeEqualTo("QR")
        result["FlowArea"].shouldNotBeNull()
    }

    @Test
    fun `font data are gathered only once per multiple builds`() {
        // given
        val textStyle = aTextStyle("TS_1", definition = aTextDef(fontFamily = "Calibri", bold = true)).mock()

        val blockA = aDocObj("B_1", Block, listOf(aParagraph(aText(StringValue("Hello There!"), textStyle.id)))).mock()
        val blockB = aDocObj("B_2", Block, listOf(aParagraph(aText(StringValue("Bye!"), textStyle.id)))).mock()
        every { ipsService.gatherFontData(any()) } returns "Calibri,Bold,icm://calibrib.ttf;"

        // when
        subject.buildDocumentObject(blockA)
        subject.buildDocumentObject(blockB)

        // then
        verify(exactly = 1) { ipsService.gatherFontData("icm://".toIcmPath()) }
    }

    private fun DocumentObject.mock(): DocumentObject {
        val id = this.id
        every { documentObjectRepository.findOrFail(id) } returns this
        return this
    }

    private fun Image.mock(): Image {
        val id = this.id
        every { imageRepository.findOrFail(id) } returns this
        every { imageRepository.find(id) } returns this
        return this
    }

    private fun DisplayRule.mock(): DisplayRule {
        val id = this.id
        every { displayRuleRepository.findOrFail(id) } returns this
        return this
    }

    private fun Variable.mock(): Variable {
        val id = this.id
        every { variableRepository.findOrFail(id) } returns this
        return this
    }

    private fun VariableStructure.mock(): VariableStructure {
        val id = this.id
        every { variableStructureRepository.findOrFail(id) } returns this
        return this
    }

    private fun TextStyle.mock(): TextStyle {
        val id = this.id
        every { textStyleRepository.findOrFail(id) } returns this
        val currentAllStyles = textStyleRepository.listAll()
        every { textStyleRepository.listAll() } returns currentAllStyles + this
        return this
    }

    private fun aSubject(config: ProjectConfig) = DesignerDocumentObjectBuilder(
        documentObjectRepository,
        textStyleRepository,
        paragraphStyleRepository,
        variableRepository,
        variableStructureRepository,
        displayRuleRepository,
        imageRepository,
        attachmentRepository,
        resourcePathProvider,
        config,
        icmDataCache,
    )

    @Test
    fun `builds language variable if defined`() {
        // given
        val languageVariable = aVariable("LangVar", dataType = DataType.String).mock()
        val structure = aVariableStructure(
            languageVariable = "LangVar",
            structure = mapOf("LangVar" to VariablePathData("Data.Language"))
        )
        val block = aDocObj(
            id = "obj", content = listOf(
                aSelectByLanguage(
                    mapOf(
                        "en" to listOf(aParagraph("en")), "de" to listOf(aParagraph("de")), "es" to listOf()
                    )
                ),
            ),
            VariableStructureRef = structure.id
        )
        every { variableStructureRepository.findOrFail(any()) } returns structure
        every { variableRepository.findOrFail(any()) } returns languageVariable

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val languageVarId = result["Layout"]["Layout"]["Data"]["LanguageVariable"].stringValue()
        languageVarId.shouldNotBeEmpty()
        val langVarData = result["Layout"]["Layout"]["Variable"].find { it["Id"]?.stringValue() == languageVarId }
        langVarData!!["Id"].stringValue().shouldBeEqualTo(languageVarId)
    }
}
