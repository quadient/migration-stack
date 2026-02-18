package com.quadient.migration.service.inspirebuilder

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.PathsConfig
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.FirstMatch
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.Table
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.api.repository.AttachmentRepository
import com.quadient.migration.api.repository.DisplayRuleRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ImageRepository
import com.quadient.migration.api.repository.ParagraphStyleRepository
import com.quadient.migration.api.repository.TextStyleRepository
import com.quadient.migration.api.repository.VariableRepository
import com.quadient.migration.api.repository.VariableStructureRepository
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.shared.AttachmentType
import com.quadient.migration.shared.BinOp
import com.quadient.migration.shared.BorderLine
import com.quadient.migration.shared.BorderOptions
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.DataType
import com.quadient.migration.shared.DocumentObjectType
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
import com.quadient.migration.tools.model.aAttachment
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
import com.quadient.migration.tools.shouldNotBeEmpty
import com.quadient.migration.tools.shouldNotBeNull
import io.mockk.InternalPlatformDsl.toArray
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

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
    val config = aProjectConfig(targetDefaultFolder = "defaultFolder")

    private val subject = aSubject(config)

    private val xmlMapper = XmlMapper().also { it.findAndRegisterModules() }

    @BeforeEach
    fun setUp() {
        every { variableStructureRepository.listAll() } returns emptyList()
        every { textStyleRepository.listAll() } returns emptyList()
        every { paragraphStyleRepository.listAll() } returns emptyList()
        every { ipsService.gatherFontData(any()) } returns "Arial,Regular,icm://Fonts/arial.ttf;"
    }

    @Test
    fun `buildDocumentObject correctly constructs complex template structure with pages and standalone block`() {
        // given
        val block = mockObj(
            aDocObj(
                "B_1", Block, listOf(
                    aParagraph(aText(StringValue("Hello there!")))
                ), true
            )
        )

        val standaloneBlock = mockObj(
            aDocObj("B_2", Block, listOf(aParagraph(aText(StringValue("I am alone")))))
        )
        val page = mockObj(
            aDocObj(
                "P_1", Page, listOf(
                    anArea(
                        listOf(aDocumentObjectRef(block.id)),
                        Position(20.millimeters(), 25.millimeters(), 160.millimeters(), 10.centimeters())
                    )
                ), options = PageOptions(20.centimeters(), 25.centimeters())
            )
        )
        val template = mockObj(
            aDocObj(
                "T_1", Template, listOf(
                    aDocumentObjectRef(page.id), aDocumentObjectRef(standaloneBlock.id)
                )
            )
        )

        // when
        val result =
            subject.buildDocumentObject(template, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        result["Page"].size().shouldBeEqualTo(4)
        val pageId = result["Page"].first { it["Name"].textValue() == page.nameOrId() }["Id"].textValue()
        val pageData = result["Page"].last { it["Id"].textValue() == pageId }
        pageData["Width"].textValue().shouldBeEqualTo("0.2")
        pageData["Height"].textValue().shouldBeEqualTo("0.25")

        val virtualPageId = result["Page"].first { it["Name"].textValue() == "Virtual Page" }["Id"].textValue()

        val pageFlowAreaId = result["FlowArea"].first { it["ParentId"].textValue() == pageId }["Id"].textValue()
        val pageFlowArea = result["FlowArea"].last { it["Id"].textValue() == pageFlowAreaId }
        pageFlowArea["Pos"]["X"].textValue().shouldBeEqualTo("0.02")
        pageFlowArea["Pos"]["Y"].textValue().shouldBeEqualTo("0.025")
        pageFlowArea["Size"]["X"].textValue().shouldBeEqualTo("0.16")
        pageFlowArea["Size"]["Y"].textValue().shouldBeEqualTo("0.1")
        val flowId = pageFlowArea["FlowId"].textValue()
        val contentFlowId = getFlowAreaContentFlowId(result, flowId)
        result["Flow"].first { it["Id"].textValue() == contentFlowId }["Name"].textValue().shouldBeEqualTo(block.nameOrId())
        result["Flow"].last { it["Id"].textValue() == contentFlowId }["FlowContent"]["P"]["T"][""].textValue()
            .shouldBeEqualTo("Hello there!")

        val virtualPageFlowAreaId =
            result["FlowArea"].first { it["ParentId"].textValue() == virtualPageId }["Id"].textValue()
        val virtualPageFlowArea = result["FlowArea"].last { it["Id"].textValue() == virtualPageFlowAreaId }
        virtualPageFlowArea["Pos"]["X"].textValue().shouldBeEqualTo("0.015")
        virtualPageFlowArea["Pos"]["Y"].textValue().shouldBeEqualTo("0.015")
        virtualPageFlowArea["Size"]["X"].textValue().shouldBeEqualTo("0.18")
        virtualPageFlowArea["Size"]["Y"].textValue().shouldBeEqualTo("0.267")
        val virtualFlowId = virtualPageFlowArea["FlowId"].textValue()
        val virtualContentFlowId = getFlowAreaContentFlowId(result, virtualFlowId)
        result["Flow"].first { it["Id"].textValue() == virtualContentFlowId }["Name"].textValue()
            .shouldBeEqualTo(standaloneBlock.nameOrId())
        result["Flow"].last { it["Id"].textValue() == virtualContentFlowId }["ExternalLocation"].textValue()
            .shouldBeEqualTo("icm://${config.defaultTargetFolder}/${standaloneBlock.nameOrId()}.wfd")

        result["Root"]["AllowRuntimeModifications"].textValue().shouldBeEqualTo("True")
        result["Pages"]["MainFlow"].textValue().shouldBeEqualTo(flowId)
    }

    @Test
    fun `buildDocumentObject creates flow area with placeholder text in case of invalid image`() {
        // given
        val image = mockImg(aImage("Img_1", sourcePath = null, skip = SkipOptions(true, "img placeholder", null)))
        val page = mockObj(
            aDocObj(
                "P_1", Page,
                listOf(
                    anArea(
                        listOf(ImageRef(image.id)),
                        Position(60.millimeters(), 60.millimeters(), 10.centimeters(), 10.centimeters()),
                    )
                ),
            )
        )
        val template = mockObj(aDocObj("T_1", Template, listOf(aDocumentObjectRef(page.id))))

        // when
        val result =
            subject.buildDocumentObject(template, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        result["Image"].shouldBeEqualTo(null)
        val flowArea = result["FlowArea"].last()
        flowArea["Pos"]["X"].textValue().shouldBeEqualTo("0.06")
        flowArea["Pos"]["Y"].textValue().shouldBeEqualTo("0.06")
        flowArea["Size"]["X"].textValue().shouldBeEqualTo("0.1")
        flowArea["Size"]["Y"].textValue().shouldBeEqualTo("0.1")
        val flowId = flowArea["FlowId"].textValue()

        val flow = result["Flow"].last { it["Id"].textValue() == flowId }
        flow["FlowContent"]["P"]["T"][""].textValue().shouldBeEqualTo("img placeholder")
    }

    @Test
    fun `buildDocumentObject creates image area with image in case of flow area only with valid image ref`() {
        val Image = mockImg(aImage("Img_1"))
        val page = mockObj(
            aDocObj(
                "P_1", Page, listOf(
                    anArea(
                        listOf(
                            ImageRef(Image.id)
                        ), Position(60.millimeters(), 120.millimeters(), 20.centimeters(), 10.centimeters())
                    ),
                )
            )
        )
        val template = mockObj(aDocObj("T_1", Template, listOf(aDocumentObjectRef(page.id))))

        // when
        val result =
            subject.buildDocumentObject(template, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val imageObject = result["ImageObject"].last()
        imageObject["Pos"]["X"].textValue().shouldBeEqualTo("0.06")
        imageObject["Pos"]["Y"].textValue().shouldBeEqualTo("0.12")
        imageObject["Size"]["X"].textValue().shouldBeEqualTo("0.2")
        imageObject["Size"]["Y"].textValue().shouldBeEqualTo("0.1")
        val imageId = imageObject["ImageId"].textValue()

        val image = result["Image"].last { it["Id"].textValue() == imageId }
        image["ImageLocation"].textValue()
            .shouldBeEqualTo("VCSLocation,icm://${config.defaultTargetFolder}/${Image.nameOrId()}.jpg")
    }

    @Test
    fun `buildDocumentObject creates image with alternate text from Image`() {
        // given
        val Image = mockImg(aImage("Img_1", alternateText = "Description of the image"))
        val page = mockObj(
            aDocObj(
                "P_1", Page, listOf(
                    anArea(
                        listOf(
                            ImageRef(Image.id)
                        ), Position(60.millimeters(), 120.millimeters(), 20.centimeters(), 10.centimeters())
                    ),
                )
            )
        )
        val template = mockObj(aDocObj("T_1", Template, listOf(aDocumentObjectRef(page.id))))

        // when
        val result =
            subject.buildDocumentObject(template, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val imageObject = result["ImageObject"].last()
        val imageId = imageObject["ImageId"].textValue()

        val image = result["Image"].last { it["Id"].textValue() == imageId }
        image["PDFAdvanced"]["Tagging"]["AlternateText"].textValue().shouldBeEqualTo("Description of the image")
    }

    @Test
    fun `buildDocumentObject uses inline condition flow when block is used with display rule`() {
        // given
        val rule = mockRule(
            aDisplayRule(
                Literal("A", LiteralDataType.String), BinOp.Equals, Literal("B", LiteralDataType.String)
            )
        )
        val block = mockObj(
            aDocObj(
                "B_1", Block, listOf(aParagraph(aText(StringValue("Hi"))))
            )
        )
        val template = mockObj(aDocObj("T_1", Template, listOf(aDocumentObjectRef(block.id, rule.id))))

        // when
        val result =
            subject.buildDocumentObject(template, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val contentFlow = getFlowAreaContentFlow(result)
        contentFlow["Type"].textValue().shouldBeEqualTo("InlCond")
        val condition = contentFlow["Condition"]
        condition["Value"].textValue().shouldBeEqualTo("return (String('A')==String('B'));")
        val successFlowId = condition[""].textValue()
        result["Flow"].first { it["Id"].textValue() == successFlowId }["Name"].textValue()
            .shouldBeEqualTo(block.nameOrId())
    }

    @Test
    fun `buildDocumentObject uses inline condition row when table row is under display rule`() {
        // given
        val rule = mockRule(
            aDisplayRule(
                Literal("A", LiteralDataType.String), BinOp.Equals, Literal("B", LiteralDataType.String)
            )
        )
        val block = mockObj(
            aDocObj(
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
            )
        )

        // when
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val rowSetId = result["Table"].last()["RowSetId"].textValue()
        val rowIds = result["RowSet"].last { it["Id"].textValue() == rowSetId }["SubRowId"]
        rowIds.size().shouldBeEqualTo(2)

        val secondRow = result["RowSet"].last { it["Id"].textValue() == rowIds[1].textValue() }
        secondRow["RowSetType"].textValue().shouldBeEqualTo("InlCond")
        secondRow["RowSetCondition"][0]["Condition"].textValue().shouldBeEqualTo("return (String('A')==String('B'));")
        val pdfAdvanced = result["Table"].last()["PDFAdvanced"]
        pdfAdvanced.shouldNotBeNull()
        pdfAdvanced["Tagging"]["Rule"].textValue().shouldBeEqualTo("Table")
        pdfAdvanced["Tagging"]["AlternateText"].textValue().shouldBeEqualTo("Table alt text")
    }

    @Test
    fun `buildDocumentObject creates twice used block only once`() {
        // given
        val block = mockObj(aDocObj("B_1", Block, listOf(aParagraph(aText(StringValue("Hi"))))))
        val template = mockObj(
            aDocObj(
                "T_1", Template, listOf(
                    aDocumentObjectRef(block.id), aDocumentObjectRef(block.id)
                )
            )
        )

        // when
        val result =
            subject.buildDocumentObject(template, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        result["Flow"].filter { it["Name"]?.textValue() == block.nameOrId() }.size.shouldBeEqualTo(1)
    }

    @Test
    fun `buildDocumentObject correctly sets table widths`() {
        val table = aTable(
            minWidth = Size.ofMillimeters(111),
            maxWidth = Size.ofMillimeters(222),
            percentWidth = 66.6
        )
        val block = mockObj(aDocObj("T1", Block, listOf(table)))

        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        result["Table"].last()["MinWidth"].textValue().toDouble().shouldBeEqualTo(0.111)
        result["Table"].last()["MaxWidth"].textValue().toDouble().shouldBeEqualTo(0.222)
        result["Table"].last()["PercentWidth"].textValue().toDouble().shouldBeEqualTo(66.6)
    }

    @Test
    fun `buildDocumentObject correctly sets table alignment`() {
        val table = aTable(alignment = TableAlignment.Center)
        val block = mockObj(aDocObj("T1", Block, listOf(table)))

        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        result["Table"].last()["TableAlignment"].textValue().shouldBeEqualTo("Center")
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
        val block = mockObj(aDocObj("T1", Block, listOf(table)))

        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        val borderId = result["Table"].last()["BorderId"].textValue()
        val borderStyle = result["BorderStyle"]?.last { it["Id"]?.textValue() == borderId }
        val fillStyleId = borderStyle?.get("FillStyleId")?.textValue()
        val fillStyle = result["FillStyle"]?.last { it["Id"]?.textValue() == fillStyleId }
        val colorId = fillStyle?.get("ColorId")?.textValue()
        val color = result["Color"]?.last { it["Id"]?.textValue() == colorId }

        color?.get("RGB")?.textValue().shouldBeEqualTo("0.5019607843137255,0.5019607843137255,0.5019607843137255")

        borderStyle?.get("Margin")["UpperLeft"]["Y"]?.textValue().shouldBeEqualTo("0.001")
        borderStyle?.get("Margin")["LowerRight"]["Y"]?.textValue().shouldBeEqualTo("0.002")
        borderStyle?.get("Margin")["UpperLeft"]["X"]?.textValue().shouldBeEqualTo("0.003")
        borderStyle?.get("Margin")["LowerRight"]["X"]?.textValue().shouldBeEqualTo("0.004")

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
        val block = mockObj(aDocObj("T1", Block, listOf(table)))

        // when
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val borderId = result["Cell"].last()["BorderId"].textValue()
        val borderStyle = result["BorderStyle"]?.last { it["Id"]?.textValue() == borderId }
        val fillStyleId = borderStyle?.get("FillStyleId")?.textValue()
        val fillStyle = result["FillStyle"]?.last { it["Id"]?.textValue() == fillStyleId }
        val colorId = fillStyle?.get("ColorId")?.textValue()
        val color = result["Color"]?.last { it["Id"]?.textValue() == colorId }

        color?.get("RGB")?.textValue().shouldBeEqualTo("0.5019607843137255,0.5019607843137255,0.5019607843137255")

        borderStyle?.get("Margin")["UpperLeft"]["Y"]?.textValue().shouldBeEqualTo("0.001")
        borderStyle?.get("Margin")["LowerRight"]["Y"]?.textValue().shouldBeEqualTo("0.002")
        borderStyle?.get("Margin")["UpperLeft"]["X"]?.textValue().shouldBeEqualTo("0.003")
        borderStyle?.get("Margin")["LowerRight"]["X"]?.textValue().shouldBeEqualTo("0.004")

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
        val block = mockObj(aDocObj("T1", Block, listOf(table)))

        // when
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val tableNode = result["Table"].last()
        val rowSetId = tableNode["RowSetId"].textValue()
        val rowSet = result["RowSet"].last { it["Id"].textValue() == rowSetId }
        val subRowIds = rowSet["SubRowId"]
        subRowIds.size().shouldBeEqualTo(5) // first header, header, body, footer, last footer

        result.assertRowContent(subRowIds[0].textValue(), "FirstHeader")
        result.assertRowContent(subRowIds[1].textValue(), "Header")
        result.assertRowContent(subRowIds[2].textValue(), "Body")
        result.assertRowContent(subRowIds[3].textValue(), "Footer")
        result.assertRowContent(subRowIds[4].textValue(), "LastFooter")
    }

    private fun com.fasterxml.jackson.databind.JsonNode.assertRowContent(rowSetId: String, expectedText: String) {
        val rowSet = this["RowSet"].last { it["Id"].textValue() == rowSetId }
        val contentRowSet = this["RowSet"].last { it["Id"].textValue() == rowSet["SubRowId"].textValue() }
        val cell = this["Cell"].last { it["Id"].textValue() == contentRowSet["SubRowId"].textValue() }
        val flowId = cell["FlowId"].textValue()
        val flow = this["Flow"].last { it["Id"].textValue() == flowId }
        flow["FlowContent"]["P"]["T"][""].textValue().shouldBeEqualTo(expectedText)
    }

    private fun com.fasterxml.jackson.databind.JsonNode?.assertLine(borderStyleId: String, line: String, expectedColor: Color, expectedWidth: Double) {
        val borderStyle = this?.get("BorderStyle")?.last { it["Id"]?.textValue() == borderStyleId }

        val lineFillStyleId = borderStyle?.get(line)?.get("FillStyle")?.textValue()
        val lineFillStyle = this?.get("FillStyle")?.last { it["Id"]?.textValue() == lineFillStyleId }
        val lineColorId = lineFillStyle?.get("ColorId")?.textValue()
        val lineColor = this?.get("Color")?.last { it["Id"]?.textValue() == lineColorId }
        lineColor?.get("RGB")?.textValue().shouldBeEqualTo("${expectedColor.red.toDouble() / 255.0},${expectedColor.green.toDouble() / 255.0},${expectedColor.blue.toDouble() / 255.0}")

        borderStyle?.get(line)["LineWidth"]?.textValue()?.toDouble().shouldBeEqualTo(expectedWidth)
    }

    @Test
    fun `buildDocumentObject names multiple composite flows with numbers`() {
        // given
        val innerBlock = mockObj(
            aDocObj("B_2", Block, listOf(aParagraph(aText(StringValue("In between")))), internal = true)
        )
        val block = mockObj(
            aDocObj(
                "B_1", Block, listOf(
                    aParagraph(aText(StringValue("Hi"))),
                    aDocumentObjectRef(innerBlock.id),
                    aParagraph(aText(StringValue("Bye")))
                ), internal = true
            )
        )
        val template = mockObj(aDocObj("T_1", Template, listOf(aDocumentObjectRef(block.id))))

        // when
        val result =
            subject.buildDocumentObject(template, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val areaFlowId = result["FlowArea"].last()["FlowId"].textValue()
        result["Flow"].first { it["Id"].textValue() == areaFlowId }["Name"].textValue()
            .shouldBeEqualTo(block.nameOrId())
        val areaFlowRefs = result["Flow"].last { it["Id"].textValue() == areaFlowId }["FlowContent"]["P"]["T"]["O"]

        result["Flow"].first { it["Id"].textValue() == areaFlowRefs[0]["Id"].textValue() }["Name"].textValue()
            .shouldBeEqualTo("${block.nameOrId()} 1")
        result["Flow"].first { it["Id"].textValue() == areaFlowRefs[1]["Id"].textValue() }["Name"].textValue()
            .shouldBeEqualTo(innerBlock.nameOrId())
        result["Flow"].first { it["Id"].textValue() == areaFlowRefs[2]["Id"].textValue() }["Name"].textValue()
            .shouldBeEqualTo("${block.nameOrId()} 2")
    }

    @Test
    fun `buildDocumentObject creates multiple times used variable only once`() {
        // given
        val variable = mockVar(aVariable("V_1"))

        val block1 = mockObj(
            aDocObj(
                "B_1", Block, listOf(
                    aParagraph(
                        aText(
                            listOf(StringValue("First usage: "), VariableRef(variable.id))
                        )
                    )
                ), true
            )
        )
        val block2 = mockObj(
            aDocObj(
                "B_2", Block, listOf(
                    aParagraph(
                        aText(
                            listOf(StringValue("Second usage: "), VariableRef(variable.id))
                        )
                    )
                ), true
            )
        )
        val template = mockObj(
            aDocObj(
                "T_1", Template, listOf(
                    aDocumentObjectRef(block1.id), aDocumentObjectRef(block2.id)
                )
            )
        )
        val varStructure = mockVarStructure(
            aVariableStructure(
                structure = mapOf(
                    variable.id to VariablePathData("Data.Records.Value")
                )
            )
        )
        val config = aProjectConfig(defaultVariableStructure = varStructure.id)

        // when
        val subject = aSubject(config)
        val result =
            subject.buildDocumentObject(template, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val variableData = result["Variable"].single { it["Name"]?.textValue() == variable.nameOrId() }
        variableData["ParentId"].textValue().shouldBeEqualTo("Data.Records.Value")
        val variableId = variableData["Id"].textValue()

        val firstBlockId = result["Flow"].first { it["Name"].textValue() == block1.nameOrId() }["Id"].textValue()
        val firstBlockContent = result["Flow"].last { it["Id"].textValue() == firstBlockId }["FlowContent"]["P"]["T"]
        firstBlockContent[""].textValue().shouldBeEqualTo("First usage: ")
        firstBlockContent["O"]["Id"].textValue().shouldBeEqualTo(variableId)

        val secondBlockId = result["Flow"].first { it["Name"].textValue() == block2.nameOrId() }["Id"].textValue()
        val secondBlockContent = result["Flow"].last { it["Id"].textValue() == secondBlockId }["FlowContent"]["P"]["T"]
        secondBlockContent[""].textValue().shouldBeEqualTo("Second usage: ")
        secondBlockContent["O"]["Id"].textValue().shouldBeEqualTo(variableId)
    }

    @Test
    fun `block with first match is built to inline condition flow with multiple options`() {
        // given
        val defaultFlowModel = mockObj(aDocObj("B_10", Block, listOf(aParagraph(aText(StringValue("I am default"))))))
        val rule1 = mockRule(
            aDisplayRule(
                Literal("A", LiteralDataType.String), BinOp.Equals, Literal("B", LiteralDataType.String), id = "R_1"
            )
        )
        val flow1 =
            mockObj(aDocObj("B_11", Block, listOf(aParagraph(aText(StringValue("flow 1 content")))), internal = false))

        val rule2 = mockRule(
            aDisplayRule(
                Literal("C", LiteralDataType.String), BinOp.Equals, Literal("C", LiteralDataType.String), id = "R_2"
            )
        )

        val block = mockObj(
            aDocObj(
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
            )
        )

        // when
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val conditionFlow = result["Flow"].last { it["Type"]?.textValue() == "InlCond" }
        val conditions = conditionFlow["Condition"]
        conditions.size().shouldBeEqualTo(2)

        conditions[0]["Value"].textValue().shouldBeEqualTo("return (String('A')==String('B'));")
        val firstConditionFlowId = conditions[0][""].textValue()

        val firstConditionFlow = result["Flow"].last { it["Id"].textValue() == firstConditionFlowId }
        firstConditionFlow["WebEditingType"].textValue().shouldBeEqualTo("Section")
        val firstConditionContentFlowId = firstConditionFlow["FlowContent"]["P"]["T"]["O"]["Id"].textValue()

        result["Flow"].last { it["Id"].textValue() == firstConditionContentFlowId }["ExternalLocation"].textValue()
            .shouldBeEqualTo("icm://${config.defaultTargetFolder}/${flow1.nameOrId()}.wfd")

        conditions[1]["Value"].textValue().shouldBeEqualTo("return (String('C')==String('C'));")
        val secondConditionFlowId = conditions[1][""].textValue()

        val secondConditionFlow = result["Flow"].last { it["Id"].textValue() == secondConditionFlowId }
        secondConditionFlow["WebEditingType"].textValue().shouldBeEqualTo("Section")
        val secondConditionContentFlowId = secondConditionFlow["FlowContent"]["P"]["T"]["O"]["Id"].textValue()

        result["Flow"].last { it["Id"].textValue() == secondConditionContentFlowId }["FlowContent"]["P"]["T"][""].textValue()
            .shouldBeEqualTo("flow 2 content")

        val defaultFlowId = conditionFlow["Default"].textValue()

        val defaultFlow = result["Flow"].last { it["Id"].textValue() == defaultFlowId }
        defaultFlow["WebEditingType"].textValue().shouldBeEqualTo("Section")
        val defaultContentFlowId = defaultFlow["FlowContent"]["P"]["T"]["O"]["Id"].textValue()

        result["Flow"].last { it["Id"].textValue() == defaultContentFlowId }["FlowContent"]["P"]["T"][""].textValue()
            .shouldBeEqualTo("I am default")
    }

    @Test
    fun `build block under display rule has its content wrapped in inline condition`() {
        // given
        val rule = mockRule(
            aDisplayRule(Literal("A", LiteralDataType.String), BinOp.Equals, Literal("B", LiteralDataType.String))
        )

        val block =
            mockObj(aDocObj("B_1", Block, listOf(aParagraph(aText(StringValue("Text")))), displayRuleRef = rule.id))

        // when
        val result =
            subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val contentFlow = getFlowAreaContentFlow(result)
        contentFlow["Type"].textValue().shouldBeEqualTo("InlCond")
        result["Flow"].last { it["Id"].textValue() == contentFlow["Condition"][""].textValue() }["FlowContent"]["P"]["T"][""].textValue()
            .shouldBeEqualTo("Text")
    }

    @Test
    fun `build block using source base template enriches layout with other modules`() {
        // given
        val block = mockObj(aDocObj("B_1", Block, listOf(aParagraph(aText("Text")))))
        val config = aProjectConfig(sourceBaseTemplatePath = "icm://sourceBaseTemplate.wfd")

        every { ipsService.wfd2xml("icm://sourceBaseTemplate.wfd") } returns """
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
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        result["Property"].size().shouldBeEqualTo(3)
        result["Property"].first { it["Name"].textValue() == "PreviewTypes" }["Value"].textValue()
            .shouldBeEqualTo("HtmlPreview\nPagePreview\nSMSPreview")
        result["DataInput"].shouldNotBeNull()
        val layout = result["Layout"]["Layout"]
        val contentFlow = getFlowAreaContentFlow(layout)
        contentFlow["FlowContent"]["P"]["T"][""].textValue().shouldBeEqualTo("Text")
    }

    @Test
    fun `loading of source base template is cached during single run`() {
        // given
        val block1 = mockObj(aDocObj("B_1", Block, listOf(aParagraph(aText("Text")))))
        val block2 = mockObj(aDocObj("B_2", Block, listOf(aParagraph(aText("Text")))))
        val config = aProjectConfig(sourceBaseTemplatePath = "icm://sourceBaseTemplate.wfd")

        every { ipsService.wfd2xml("icm://sourceBaseTemplate.wfd") } returns """
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
        subject.buildDocumentObject(block1, null)
        subject.buildDocumentObject(block2, null)

        // then
        verify(exactly = 1) { ipsService.wfd2xml(any()) }
    }

    @Test
    fun `block uses the assigned variable structure`() {
        // given
        val variable = mockVar(aVariable("V_1"))
        val varNoPath = mockVar(aVariable("V_2"))
        val variableStructureA = mockVarStructure(
            aVariableStructure(
                "VS_1", structure = mapOf(
                    variable.id to VariablePathData("Data.Records.Value"),
                    varNoPath.id to VariablePathData("", "No Path Variable")
                )
            )
        )
        val variableStructureB = mockVarStructure(
            aVariableStructure(
                "VS_2", structure = mapOf(variable.id to VariablePathData("Data.Clients.Value"))
            )
        )
        every { variableStructureRepository.listAll() } returns listOf(variableStructureA, variableStructureB)
        val config = aProjectConfig(defaultVariableStructure = variableStructureB.id)

        val block = mockObj(
            aDocObj(
                "B_1", Block, listOf(
                    aParagraph(
                        aText(
                            listOf(
                                StringValue("Text"), VariableRef(variable.id), VariableRef(varNoPath.id)
                            )
                        )
                    )
                ), VariableStructureRef = variableStructureA.id
            )
        )

        // when
        val subject = aSubject(config)
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        verify(exactly = 1) { variableStructureRepository.findOrFail(variableStructureA.id) }
        verify(exactly = 0) { variableStructureRepository.findOrFail(variableStructureB.id) }

        result["Variable"].first { it["Name"].textValue() == variable.nameOrId() }["ParentId"].textValue()
            .shouldBeEqualTo("Data.Records.Value")

        val flow = result["Flow"].first { it["Type"]?.textValue() == "Simple" }
        flow["FlowContent"]["P"]["T"][""][0].textValue().shouldBeEqualTo("Text")
        flow["FlowContent"]["P"]["T"][""][1].textValue().shouldBeEqualTo("\$No Path Variable\$")
    }

    @Test
    fun `font data are gathered only once per multiple builds`() {
        // given
        val textStyle = aTextStyle("TS_1", definition = aTextDef(fontFamily = "Calibri", bold = true))
        every { textStyleRepository.listAll() } returns listOf(textStyle)
        every { textStyleRepository.firstWithDefinition(textStyle.id) } returns textStyle

        val blockA =
            mockObj(aDocObj("B_1", Block, listOf(aParagraph(aText(StringValue("Hello There!"), textStyle.id)))))
        val blockB = mockObj(aDocObj("B_2", Block, listOf(aParagraph(aText(StringValue("Bye!"), textStyle.id)))))
        every { ipsService.gatherFontData(any()) } returns "Calibri,Bold,icm://calibrib.ttf;"

        // when
        subject.buildDocumentObject(blockA, null)
        subject.buildDocumentObject(blockB, null)

        // then
        verify(exactly = 1) { ipsService.gatherFontData("icm://") }
    }

    private fun mockObj(documentObject: DocumentObject): DocumentObject {
        every { documentObjectRepository.findOrFail(documentObject.id) } returns documentObject
        return documentObject
    }

    private fun mockImg(image: Image): Image {
        every { imageRepository.findOrFail(image.id) } returns image
        every { imageRepository.find(image.id) } returns image
        return image
    }

    private fun mockRule(rule: DisplayRule): DisplayRule {
        every { displayRuleRepository.findOrFail(rule.id) } returns rule
        return rule
    }

    private fun mockVar(variable: Variable): Variable {
        every { variableRepository.findOrFail(variable.id) } returns variable
        return variable
    }

    private fun mockVarStructure(variableStructure: VariableStructure): VariableStructure {
        every { variableStructureRepository.findOrFail(variableStructure.id) } returns variableStructure
        return variableStructure
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
        config,
        ipsService,
    )

    @Test
    fun `builds language variable if defined`() {
        // given
        val languageVariable = mockVar(aVariable("LangVar", dataType = DataType.String))
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
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val languageVarId = result["Layout"]["Layout"]["Data"]["LanguageVariable"].textValue()
        languageVarId.shouldNotBeEmpty()
        val langVarData = result["Layout"]["Layout"]["Variable"].find { it["Id"]?.textValue() == languageVarId }
        langVarData!!["Id"].textValue().shouldBeEqualTo(languageVarId)
    }

    @Nested
    inner class PathTest {

        @ParameterizedTest
        @CsvSource(
            // projectCfg.paths ,targetFolder   ,defaultTargetFolder,expected
            "               ,                   ,                   ,icm://Image_T_1.jpg",
            "null           ,null               ,null               ,icm://Image_T_1.jpg",
            "null           ,                   ,                   ,icm://Image_T_1.jpg",
            "               ,null               ,                   ,icm://Image_T_1.jpg",
            "               ,                   ,null               ,icm://Image_T_1.jpg",
            "               ,relative           ,                   ,icm://relative/Image_T_1.jpg",
            "               ,relative           ,null               ,icm://relative/Image_T_1.jpg",
            "root           ,relative           ,                   ,icm://root/relative/Image_T_1.jpg",
            "root           ,relative           ,null               ,icm://root/relative/Image_T_1.jpg",
            "               ,icm://absolute/    ,                   ,icm://absolute/Image_T_1.jpg",
            "/root/         ,icm://absolute/    ,                   ,icm://absolute/Image_T_1.jpg",
            "               ,icm://absolute/    ,shouldIgnoreToo    ,icm://absolute/Image_T_1.jpg",
            "shouldIgnore   ,icm://absolute/    ,shouldIgnoreToo    ,icm://absolute/Image_T_1.jpg",
            "imagesConfig   ,                   ,                   ,icm://imagesConfig/Image_T_1.jpg",
            "/imagesConfig/ ,                   ,                   ,icm://imagesConfig/Image_T_1.jpg",
            "imagesConfig   ,subDir             ,                   ,icm://imagesConfig/subDir/Image_T_1.jpg",
            "imagesConfig   ,                   ,default            ,icm://imagesConfig/default/Image_T_1.jpg",
            "imagesConfig   ,subDir             ,default            ,icm://imagesConfig/subDir/Image_T_1.jpg",
            "               ,                   ,default            ,icm://default/Image_T_1.jpg",
            "               ,subDir             ,shouldIgnore       ,icm://subDir/Image_T_1.jpg",
        )
        fun testImagesPath(imagesPath: String?, targetFolder: String?, defaultTargetFolder: String?, expected: String) {
            val config = aProjectConfig(
                output = InspireOutput.Designer,
                paths = PathsConfig(images = imagesPath.nullToNull()?.let(IcmPath::from)),
                targetDefaultFolder = defaultTargetFolder.nullToNull(),
            )
            val pathTestSubject = aSubject(config)
            val image = aImage("T_1", targetFolder = targetFolder.nullToNull())

            val path = pathTestSubject.getImagePath(image)

            path.shouldBeEqualTo(expected)
        }

        @ParameterizedTest
        @CsvSource(
            // targetFolder   ,defaultTargetFolder  ,expected
            "                   ,                   ,icm://T_1name.wfd",
            "null               ,                   ,icm://T_1name.wfd",
            "                   ,null               ,icm://T_1name.wfd",
            "null               ,null               ,icm://T_1name.wfd",
            "relative           ,                   ,icm://relative/T_1name.wfd",
            "relative           ,null               ,icm://relative/T_1name.wfd",
            "relative           ,default            ,icm://relative/T_1name.wfd",
            "icm://absolute/    ,                   ,icm://absolute/T_1name.wfd",
            "icm://absolute/    ,default            ,icm://absolute/T_1name.wfd",
            "                   ,default            ,icm://default/T_1name.wfd",
            "null               ,default            ,icm://default/T_1name.wfd",
        )
        fun testDocumentObjectPath(targetFolder: String?, defaultTargetFolder: String?, expected: String) {
            val config = aProjectConfig(
                output = InspireOutput.Designer,
                targetDefaultFolder = defaultTargetFolder.nullToNull(),
            )
            val pathTestSubject = aSubject(config)
            val image = aDocObj("T_1", type = DocumentObjectType.Template, targetFolder = targetFolder.nullToNull())

            val path = pathTestSubject.getDocumentObjectPath(image)

            path.shouldBeEqualTo(expected)
        }

        @ParameterizedTest
        @CsvSource(
            // styleDefPath        ,defaultTargetFolder  ,expected
            "                      ,                       ,icm://projectNameStyles.wfd",
            "                      ,null                   ,icm://projectNameStyles.wfd",
            "                      ,default                ,icm://default/projectNameStyles.wfd",
            "                      ,default                ,icm://default/projectNameStyles.wfd",
            "icm://some/path/f.wfd ,default                ,icm://some/path/f.wfd",
        )
        fun testCompanyStylesPath(styleDefPath: String?, defaultTargetFolder: String?, expected: String) {
            val config = aProjectConfig(
                name = "projectName",
                output = InspireOutput.Designer,
                targetDefaultFolder = defaultTargetFolder.nullToNull(),
                styleDefinitionPath = styleDefPath?.toIcmPath()
            )
            val pathTestSubject = aSubject(config)

            val path = pathTestSubject.getStyleDefinitionPath()

            path.shouldBeEqualTo(expected)
        }

        @ParameterizedTest
        @CsvSource(
            ",icm://", "Resources/Fonts,icm://Resources/Fonts"
        )
        fun testFontRootFolder(cfgFontsPath: String?, expected: String) {
            val config =
                aProjectConfig(output = InspireOutput.Designer, paths = PathsConfig(fonts = cfgFontsPath?.toIcmPath()))
            val pathTestSubject = aSubject(config)

            val path = pathTestSubject.getFontRootFolder()

            path.shouldBeEqualTo(expected)
        }

        @Test
        fun companyStylePathMustBeAbsolute() {
            val config = aProjectConfig(
                name = "projectName",
                output = InspireOutput.Designer,
                styleDefinitionPath = "somePath.wfd".toIcmPath()
            )
            val pathTestSubject = aSubject(config)

            assertThrows<IllegalArgumentException> { pathTestSubject.getStyleDefinitionPath() }
        }

        private fun String?.nullToNull() = when (this?.trim()) {
            "null" -> null
            null -> ""
            else -> this.trim()
        }

        @ParameterizedTest
        @CsvSource(
            // attachmentType,paths.documents,paths.attachments,targetFolder,defaultTargetFolder,expected
            "Document,,,,                   ,icm://Attachment_F1.pdf",
            "Document,,,relative,           ,icm://relative/Attachment_F1.pdf",
            "Document,Docs,,relative,       ,icm://Docs/relative/Attachment_F1.pdf",
            "Document,,,icm://absolute/,    ,icm://absolute/Attachment_F1.pdf",
            "Document,,,                ,def,icm://def/Attachment_F1.pdf",
            "Attachment,,,,                 ,icm://Attachment_F1.pdf",
            "Attachment,,Attach,relative,   ,icm://Attach/relative/Attachment_F1.pdf",
            "Attachment,,,icm://absolute/,  ,icm://absolute/Attachment_F1.pdf",
        )
        fun testAttachmentPath(
            attachmentType: String,
            documentsPath: String?,
            attachmentsPath: String?,
            targetFolder: String?,
            defaultTargetFolder: String?,
            expected: String
        ) {
            val config = aProjectConfig(
                output = InspireOutput.Designer,
                paths = PathsConfig(
                    documents = documentsPath.nullToNull()?.let(IcmPath::from),
                    attachments = attachmentsPath.nullToNull()?.let(IcmPath::from)
                ),
                targetDefaultFolder = defaultTargetFolder.nullToNull(),
            )
            val pathTestSubject = aSubject(config)
            val attachment = aAttachment("F1", targetFolder = targetFolder.nullToNull(), attachmentType = AttachmentType.valueOf(attachmentType))

            val path = pathTestSubject.getAttachmentPath(attachment)

            path.shouldBeEqualTo(expected)
        }

        @Test
        fun `attachment path appends extension from sourcePath when attachmentName lacks one`() {
            val config = aProjectConfig(output = InspireOutput.Designer)
            val pathTestSubject = aSubject(config)
            val attachment = aAttachment("F1", name = "document", sourcePath = "C:/attachments/doc.pdf")

            val path = pathTestSubject.getAttachmentPath(attachment)

            path.shouldBeEqualTo("icm://document.pdf")
        }

        @Test
        fun `attachment path preserves attachmentName extension when present`() {
            val config = aProjectConfig(output = InspireOutput.Designer)
            val pathTestSubject = aSubject(config)
            val attachment = aAttachment("F1", name = "report.docx", sourcePath = "attachment.pdf")

            val path = pathTestSubject.getAttachmentPath(attachment)

            path.shouldBeEqualTo("icm://report.docx")
        }
    }
}

