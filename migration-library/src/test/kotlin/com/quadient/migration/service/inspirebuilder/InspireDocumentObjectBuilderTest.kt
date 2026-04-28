package com.quadient.migration.service.inspirebuilder

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.dto.migrationmodel.Attachment
import com.quadient.migration.api.dto.migrationmodel.AttachmentRef
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Hyperlink
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle
import com.quadient.migration.api.dto.migrationmodel.TextStyle
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.api.dto.migrationmodel.builder.AttachmentBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ImageBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.TextStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.VariableBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.VariableStructureBuilder
import com.quadient.migration.api.repository.AttachmentRepository
import com.quadient.migration.api.repository.DisplayRuleRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ImageRepository
import com.quadient.migration.api.repository.ParagraphStyleRepository
import com.quadient.migration.api.repository.TextStyleRepository
import com.quadient.migration.api.repository.VariableRepository
import com.quadient.migration.api.repository.VariableStructureRepository
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.BinOp
import com.quadient.migration.shared.DataType
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.ColumnApplyTo
import com.quadient.migration.shared.ColumnBalancingType
import com.quadient.migration.shared.DocumentObjectType.Block
import com.quadient.migration.shared.Function
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.ImageType.*
import com.quadient.migration.shared.Literal
import com.quadient.migration.shared.LiteralDataType
import com.quadient.migration.shared.ParagraphPdfTaggingRule
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.SkipOptions
import com.quadient.migration.shared.millimeters
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.getFlowAreaContentFlow
import com.quadient.migration.tools.model.*
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeNull
import com.quadient.wfdxml.api.layoutnodes.TextStyleInheritFlag
import com.quadient.wfdxml.internal.module.layout.LayoutImpl
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.collections.last

class InspireDocumentObjectBuilderTest {
    private val documentObjectRepository = mockk<DocumentObjectRepository>()
    private val textStyleRepository = mockk<TextStyleRepository>()
    private val paragraphStyleRepository = mockk<ParagraphStyleRepository>()
    private val variableRepository = mockk<VariableRepository>()
    private val variableStructureRepository = mockk<VariableStructureRepository>()
    private val displayRuleRepository = mockk<DisplayRuleRepository>()
    private val imageRepository = mockk<ImageRepository>()
    private val attachmentRepository = mockk<AttachmentRepository>()
    private val ipsService = mockk<IpsService>()

    private val xmlMapper = XmlMapper().also { it.findAndRegisterModules() }

    private var subject = DesignerDocumentObjectBuilder(
        documentObjectRepository,
        textStyleRepository,
        paragraphStyleRepository,
        variableRepository,
        variableStructureRepository,
        displayRuleRepository,
        imageRepository,
        attachmentRepository,
        aProjectConfig(output = InspireOutput.Designer),
        ipsService,
    )

    @BeforeEach
    fun setUp() {
        every { variableStructureRepository.listAll() } returns emptyList()
        every { textStyleRepository.listAll() } returns emptyList()
        every { paragraphStyleRepository.listAll() } returns emptyList()
        every { ipsService.gatherFontData(any()) } returns "Arial,Regular,icm://Fonts/arial.ttf;"
        every { ipsService.fileExists(any<IcmPath>()) } returns true
    }

    @Test
    fun `buildStyles creates ParagraphStyle with PdfTaggingRule Paragraph`() {
        // given
        val paragraphStyle = mockParagraphStyle(ParagraphStyleBuilder("PS_Para").name("PS_Para").definition {
            leftIndent(Size.ofMillimeters(0))
            pdfTaggingRule(ParagraphPdfTaggingRule.Paragraph)
        }.build())

        // when
        val result = subject.buildStyles(emptyList(), listOf(paragraphStyle))
            .let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val paraStyles = result["Layout"]["Layout"]["ParaStyle"]
        val paraStyleId = paraStyles.first { it["Name"]?.textValue() == paragraphStyle.name }["Id"].textValue()
        val paraStyleContent = paraStyles.last { it["Id"].textValue() == paraStyleId }

        paraStyleContent["PDFAdvanced"]["Tagging"]["Rule"].textValue().shouldBeEqualTo("P")
    }

    @Test
    fun `buildDocumentObject with hyperlink creates text style with URL variable and alternate text`() {
        // given
        val hyperlink = Hyperlink("https://www.example.com", "Click here", "Link to example website")
        val textContent = listOf(StringValue("Visit our site: "), hyperlink, StringValue(" for more info"))
        val block = mockObj(aBlock("B_1", listOf(aParagraph(aText(textContent)))))

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val hyperlinkStyleId = result["TextStyle"].first { it["Name"]?.textValue() == "text_url_1" }["Id"].textValue()
        val hyperlinkStyleContent = result["TextStyle"].last { it["Id"].textValue() == hyperlinkStyleId }

        val urlVarId = hyperlinkStyleContent["URLLink"].textValue()
        hyperlinkStyleContent["URLAlternateText"].textValue().shouldBeEqualTo("Link to example website")

        result["Variable"].first { it["Name"].textValue() == "text_url_1" }
        val urlVariableContent = result["Variable"].last { it["Id"].textValue() == urlVarId }
        urlVariableContent["Type"].textValue().shouldBeEqualTo("Constant")
        urlVariableContent["VarType"].textValue().shouldBeEqualTo("String")
        urlVariableContent["Content"].textValue().shouldBeEqualTo("https://www.example.com")
    }

    @Test
    fun `buildDocumentObject with hyperlink inherits from base text style correctly`() {
        // given
        val textStyle = mockTextStyle(
            aTextStyle(
                "TS_Base", definition = aTextDef(
                    fontFamily = "Calibri", size = Size.ofPoints(14), bold = true, italic = true
                )
            )
        )

        val block = mockObj(
            aBlock(
                "B_1", listOf(
                    aParagraph(
                        aText(
                            listOf(
                                StringValue("Some text before link. "),
                                Hyperlink("https://www.example.com", "Link text"),
                                StringValue(" Some text after link.")
                            ), TextStyleRef(textStyle.id)
                        )
                    )
                )
            )
        )

        // when
        val result =
            subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val hyperlinkStyleId =
            result["TextStyle"].first { it["Name"]?.textValue() == "${textStyle.name}_url_1" }["Id"].textValue()
        val hyperlinkStyleContent = result["TextStyle"].last { it["Id"].textValue() == hyperlinkStyleId }

        hyperlinkStyleContent["Type"].textValue().shouldBeEqualTo("Delta")
        hyperlinkStyleContent["AncestorId"].textValue().shouldBeEqualTo("Def.TextStyleHyperlink")
        val inheritFlags = hyperlinkStyleContent["InheritFlag"]

        inheritFlags.size().shouldBeEqualTo(TextStyleInheritFlag.entries.size - 2)

        assert(inheritFlags.none { it.textValue() == "Underline" })
        assert(inheritFlags.none { it.textValue() == "FillStyle" })
    }

    @Test
    fun `attachment reference creates DirectExternal flow with correct structure`() {
        // given
        val attachment = mockAttachment(aAttachment("Attachment_1", name = "document", sourcePath = "C:/attachments/document.pdf"))
        val block = mockObj(
            aBlock("B_1", listOf(aParagraph(aText(listOf(StringValue("See attached: "), AttachmentRef(attachment.id))))))
        )

        // when
        val result =
            subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val contentFlow = getFlowAreaContentFlow(result)
        contentFlow["FlowContent"]["P"]["T"][""].textValue().shouldBeEqualTo("See attached: ")
        val attachmentFlowId = contentFlow["FlowContent"]["P"]["T"]["O"]["Id"].textValue()

        val attachmentFlow = result["Flow"].last { it["Id"].textValue() == attachmentFlowId }
        attachmentFlow["Type"].textValue().shouldBeEqualTo("DirectExternal")
        attachmentFlow["ExternalLocation"].textValue().shouldBeEqualTo("icm://document.pdf")
    }

    @Test
    fun `attachment reference with skip and placeholder creates simple flow with placeholder text`() {
        // given
        val attachment = mockAttachment(aAttachment("Attachment_1", skip = SkipOptions(true, "Attachment not available", "Missing source")))
        val block = mockObj(
            aBlock("B_1", listOf(aParagraph(aText(listOf(AttachmentRef(attachment.id))))))
        )

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val contentFlow = getFlowAreaContentFlow(result)
        val placeholderFlowId = contentFlow["FlowContent"]["P"]["T"]["O"]["Id"].textValue()
        val placeholderFlow = result["Flow"].last { it["Id"].textValue() == placeholderFlowId }
        placeholderFlow["FlowContent"]["P"]["T"][""].textValue() == "Attachment not available"
    }

    @Test
    fun `attachment reference with skip but no placeholder does not create flow`() {
        // given
        val attachment = mockAttachment(aAttachment("Attachment_", skip = SkipOptions(true, null, "Not needed")))
        val block = mockObj(
            aBlock(
                "B_1", listOf(
                    aParagraph(
                        aText(
                            listOf(
                                StringValue("Text "), AttachmentRef(attachment.id), StringValue(" more text")
                            )
                        )
                    )
                )
            )
        )

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val flow = getFlowAreaContentFlow(result)
        flow["FlowContent"]["P"]["T"][""].textValue().shouldBeEqualTo("Text  more text")
    }

    @Test
    fun `image reference with targetAttachmentId resolves to attachment in wfd-xml`() {
        // given
        val targetAttachment = mockAttachment(
            AttachmentBuilder("Attachment_Target").name("resolved").sourcePath("C:/attachments/resolved.pdf").build()
        )
        val image = mockImage(
            ImageBuilder("Image_1").sourcePath("C:/images/original.png")
                .imageType(Png).targetAttachmentId(targetAttachment.id).build()
        )
        val block =
            mockObj(DocumentObjectBuilder("B_1", DocumentObjectType.Block).string("Image: ").imageRef(image.id).build())

        // when
        val result =
            subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val flowId = result["FlowArea"].last()["FlowId"].textValue()
        val flowAreaFlow = result["Flow"].last { it["Id"].textValue() == flowId }
        val refIds = flowAreaFlow["FlowContent"]["P"]["T"]["O"]
        val attachmentFlow = result["Flow"].last { it["Id"].textValue() == refIds[1]["Id"].textValue() }

        attachmentFlow["Type"].textValue().shouldBeEqualTo("DirectExternal")
        attachmentFlow["ExternalLocation"].textValue().shouldBeEqualTo("icm://resolved.pdf")
    }

    @Test
    fun `attachment reference with targetImageId resolves to image in wfd-xml`() {
        // given
        val targetImage = mockImage(
            ImageBuilder("Image_Target").name("resolved").sourcePath("C:/images/resolved.png").imageType(Png).build()
        )
        val attachment = mockAttachment(
            AttachmentBuilder("Attachment_1").sourcePath("C:/attachments/original.pdf").targetImageId(targetImage.id)
                .build()
        )
        val block = mockObj(
            DocumentObjectBuilder("B_1", DocumentObjectType.Block).attachmentRef(attachment.id).build()
        )

        // when
        val result =
            subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val imageId = result["ImageObject"].last()["ImageId"].textValue()
        val image = result["Image"].last { it["Id"].textValue() == imageId }
        image["ImageLocation"].textValue().shouldBeEqualTo("VCSLocation,icm://resolved.png")
    }

    @Test
    fun `build template with pdf metadata creates sheet names with correct values`() {
        // given
        val template = DocumentObjectBuilder("T_1", DocumentObjectType.Template)
            .string("Template content")
            .pdfMetadata {
                title("Test Title")
                author("Test Author")
                subject("Test Subject")
                keywords("a,b,c")
                producer("Test Producer")
            }
            .build()

        // when
        val result =
            subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]
        val allSheetNameVariableIds = result["Pages"]["SheetNameVariableId"].map { it.textValue() }

        // then
        allSheetNameVariableIds.count { it.isBlank() }.shouldBeEqualTo(37)

        val pdfSheetNameVariableIds = allSheetNameVariableIds.filter { it.isNotBlank() }
        pdfSheetNameVariableIds.size.shouldBeEqualTo(5)

        val variableNames =
            pdfSheetNameVariableIds.map { varId -> result["Variable"].first { it["Id"].textValue() == varId }["Name"].textValue() }
        variableNames.shouldBeEqualTo(listOf("TaggingTitle", "TaggingAuthor", "TaggingSubject", "TaggingKeywords", "TaggingProduce"))

        val variableScripts =
            pdfSheetNameVariableIds.map { varId -> result["Variable"].last { it["Id"].textValue() == varId }["Script"].textValue() }
        variableScripts.shouldBeEqualTo(
            listOf(
                "return 'Test Title';",
                "return 'Test Author';",
                "return 'Test Subject';",
                "return 'a,b,c';",
                "return 'Test Producer';"
            )
        )
    }

    @Test
    fun `build template with variable string pdf metadata creates concatenated script`() {
        // given
        val variable = mockVar(
            VariableBuilder("middleName").name("Middle Name").dataType(DataType.String).build())
        val varNoStruct = mockVar(VariableBuilder("noStruct").dataType(DataType.String).build())
        val variableStructure = mockVarStructure(
            VariableStructureBuilder("vs1").addVariable("middleName", "Data.Clients.Value").build())

        val template = DocumentObjectBuilder("T_1", DocumentObjectType.Template)
            .variableStructureRef(variableStructure.id)
            .string("Template content")
            .pdfMetadata {
                author {
                    string("Jon ")
                    variableRef(variable.id)
                    string(" Doe ")
                    variableRef(varNoStruct.id)
                }
            }
            .build()

        // when
        val result =
            subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val allSheetNameVariableIds = result["Pages"]["SheetNameVariableId"].map { it.textValue() }
        allSheetNameVariableIds.size.shouldBeEqualTo(39)
        val pdfAuthorSheetName = allSheetNameVariableIds[38]

        val variableScript = result["Variable"].last { it["Id"].textValue() == pdfAuthorSheetName }["Script"].textValue()
        variableScript.shouldBeEqualTo("return 'Jon ' + DATA.Clients.Current.Middle_Name.toString() + ' Doe ' + '\$noStruct$';")
    }

    @Test
    fun `variableRefPath entry in variable structure resolves to correct context path in script`() {
        // given
        val clientsArray = mockVar(VariableBuilder("clientsArray").name("Clients").dataType(DataType.Array).build())
        val nameVar = mockVar(VariableBuilder("nameVar").name("name").dataType(DataType.String).build())
        val variableStructure = mockVarStructure(
            VariableStructureBuilder("vs1").addVariable(clientsArray.id, "Data")
                .addVariable(nameVar.id, VariableRef(clientsArray.id), "Name").build()
        )

        val template =
            DocumentObjectBuilder("T_1", DocumentObjectType.Template).variableStructureRef(variableStructure.id)
                .paragraph { text { variableRef(nameVar.id) } }.build()

        // when
        val result =
            subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val variableData = result["Variable"].first { it["Name"].textValue() == "Name" }
        variableData["ParentId"].textValue().shouldBeEqualTo("Data.Clients.Value")
    }

    @Test
    fun `variableRefPath through subtree resolves to correct context path in script`() {
        // given — address subtree inside clients array
        val clientsArray = mockVar(
            VariableBuilder("clientsArray").name("Clients").dataType(DataType.Array).build()
        )
        val addressSubtree = mockVar(
            VariableBuilder("addressSubtree").name("Address").dataType(DataType.SubTree).build()
        )
        val cityVar = mockVar(
            VariableBuilder("cityVar").name("City").dataType(DataType.String).build()
        )
        val variableStructure = mockVarStructure(
            VariableStructureBuilder("vs2")
                .addVariable(clientsArray.id, "Data")
                .addVariable(addressSubtree.id, VariableRef(clientsArray.id))
                .addVariable(cityVar.id, VariableRef(addressSubtree.id))
                .build()
        )

        val template = DocumentObjectBuilder("T_2", DocumentObjectType.Template)
            .variableStructureRef(variableStructure.id)
            .pdfMetadata { author { variableRef(cityVar.id) } }
            .build()

        // when
        val result =
            subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then — script should reference DATA.Clients.Current.Address.City
        val authorVariableId = result["Pages"]["SheetNameVariableId"].last().textValue()
        val variableScript = result["Variable"].last { it["Id"].textValue() == authorVariableId }["Script"].textValue()
        variableScript.shouldBeEqualTo("return DATA.Clients.Current.Address.City.toString();")
    }

    @Test
    fun `buildDocumentObject creates repeated rowset with literal array path and two inner rows`() {
        // given
        val jobNameVar = mockVar(VariableBuilder("jobNameVar").name("Job Name").dataType(DataType.String).build())
        val variableStructure =
            mockVarStructure(VariableStructureBuilder("VS_1").addVariable(jobNameVar.id, "Data.Clients.Value").build())

        val block = mockObj(DocumentObjectBuilder("B_1", Block).table {
            addRepeatedRow("Data.Clients.Value") {
                addRow {
                    addCell { string("Name: ") }
                    addCell { string("Jon") }
                }
                addRow {
                    addCell { string("Job: ") }
                    addCell { paragraph { text { variableRef(jobNameVar.id) } } }
                }
            }
        }.variableStructureRef(variableStructure.id).build())

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val repeatedRowSetId = result["Table"].last()["RowSetId"].textValue()
        val repeatedRowSet = result["RowSet"].last { it["Id"].textValue() == repeatedRowSetId }

        repeatedRowSet["RowSetType"].textValue().shouldBeEqualTo("Repeated")
        val arrayVarId = repeatedRowSet["VariableId"].textValue()
        val arrayVar = result["Variable"].last { it["Id"].textValue() == arrayVarId }
        arrayVar["Type"].textValue().shouldBeEqualTo("DataVariable")
        arrayVar["VarType"].textValue().shouldBeEqualTo("Array")

        val multipleRowId = repeatedRowSet["SubRowId"].textValue()
        val multipleRow = result["RowSet"].last { it["Id"].textValue() == multipleRowId }
        multipleRow["RowSetType"].textValue().shouldBeEqualTo("RowSet")
        val secondRowId = multipleRow["SubRowId"][1].textValue()

        val secondRow = result["RowSet"].last { it["Id"].textValue() == secondRowId }
        secondRow["RowSetType"].textValue().shouldBeEqualTo("Row")

        val secondCellId = secondRow["SubRowId"][1].textValue()
        val secondCell = result["Cell"].last { it["Id"].textValue() == secondCellId }

        val secondCellFlow = result["Flow"].last { it["Id"].textValue() == secondCell["FlowId"].textValue() }
        val variableId = secondCellFlow["FlowContent"]["P"]["T"]["O"]["Id"].textValue()

        val variable = result["Variable"].first { it["Id"].textValue() == variableId }
        variable["Name"].textValue().shouldBeEqualTo("Job Name")
        variable["ParentId"].textValue().shouldBeEqualTo("Data.Clients.Value")

        result["Root"]["LockedWebNodes"]["LockedWebNode"].textValue().shouldBeEqualTo(repeatedRowSetId)
    }

    @Test
    fun `buildDocumentObject creates repeated rowset with variable ref array path`() {
        // given
        val clientsVar = mockVar(VariableBuilder("clientsVar").name("Clients").dataType(DataType.Array).build())
        val stringVar = mockVar(VariableBuilder("stringVar").dataType(DataType.String).build())

        val variableStructure = mockVarStructure(
            VariableStructureBuilder("VS_1").addVariable(clientsVar.id, "Data")
                .addVariable(stringVar.id, VariableRef(clientsVar.id), "Client Name").build()
        )

        val displayRule = aDisplayRule(
            Literal("A", LiteralDataType.String), BinOp.Equals, Literal("B", LiteralDataType.String)
        )
        every { displayRuleRepository.findOrFail(displayRule.id) } returns displayRule

        val block = mockObj(
            DocumentObjectBuilder("B_1", Block).table {
                addRepeatedRow(VariableRef(clientsVar.id)) {
                    displayRuleRef(displayRule)
                    addRow {
                        addCell { string("Client Name") }
                        addCell { paragraph { text { variableRef(stringVar.id) } } }
                    }
                }
            }.variableStructureRef(variableStructure.id).build()
        )

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val tableRowSetId = result["Table"].last()["RowSetId"].textValue()
        val conditionRowSet = result["RowSet"].last { it["Id"].textValue() == tableRowSetId }
        conditionRowSet["RowSetType"].textValue().shouldBeEqualTo("InlCond")
        conditionRowSet["RowSetCondition"][0]["Condition"].textValue()
            .shouldBeEqualTo("return (String('A')==String('B'));")

        val repeatedRowSet = result["RowSet"].last {
            it["Id"].textValue() == conditionRowSet["RowSetCondition"][0]["SubRowId"].textValue()
        }
        repeatedRowSet["RowSetType"].textValue().shouldBeEqualTo("Repeated")
        val arrayVarId = repeatedRowSet["VariableId"].textValue()
        val arrayVar = result["Variable"].last { it["Id"].textValue() == arrayVarId }
        arrayVar["Type"].textValue().shouldBeEqualTo("DataVariable")
        arrayVar["VarType"].textValue().shouldBeEqualTo("Array")

        val innerRow = result["RowSet"].last { it["Id"].textValue() == repeatedRowSet["SubRowId"].textValue() }
        innerRow["RowSetType"].textValue().shouldBeEqualTo("Row")

        val secondCell = result["Cell"].last { it["Id"].textValue() == innerRow["SubRowId"][1].textValue() }
        val secondCellFlow = result["Flow"].last { it["Id"].textValue() == secondCell["FlowId"].textValue() }
        val variableId = secondCellFlow["FlowContent"]["P"]["T"]["O"]["Id"].textValue()
        val variable = result["Variable"].first { it["Id"].textValue() == variableId }
        variable["Name"].textValue().shouldBeEqualTo("Client Name")
        variable["ParentId"].textValue().shouldBeEqualTo("Data.Clients.Value")
    }

    @Test
    fun `buildDocumentObject creates fallback single row when array variable not mapped in structure`() {
        // given
        val arrayVar = mockVar(VariableBuilder("arrayVar").name("Clients").dataType(DataType.Array).build())
        val variableStructure = mockVarStructure(VariableStructureBuilder("VS_1").build())

        val block = mockObj(
            DocumentObjectBuilder("B_1", Block).table {
                addRepeatedRow(VariableRef(arrayVar.id)) { addRow().addCell().string("Name") }
            }.variableStructureRef(variableStructure.id).build()
        )

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val tableRowSetId = result["Table"].last()["RowSetId"].textValue()
        val tableRowSet = result["RowSet"].last { it["Id"].textValue() == tableRowSetId }
        tableRowSet["RowSetType"].textValue().shouldBeEqualTo("Row")

        val cell = result["Cell"].last { it["Id"].textValue() == tableRowSet["SubRowId"].textValue() }
        val flow = result["Flow"].last { it["Id"].textValue() == cell["FlowId"].textValue() }
        // first paragraph = warning, second paragraph = original "Name"
        flow["FlowContent"]["P"][0]["T"][""].textValue().shouldBeEqualTo($$"<repeated by unmapped $Clients$>")
        flow["FlowContent"]["P"][1]["T"][""].textValue().shouldBeEqualTo("Name")
    }

    @Test
    fun `buildDocumentObject creates fallback multiple rows when variable literal path is not registered in the variable structure`() {
        // given
        val surNameVar = mockVar(VariableBuilder("surname").dataType(DataType.String).build())
        val variableStructure = mockVarStructure(VariableStructureBuilder("VS_1").build())

        val block = mockObj(
            DocumentObjectBuilder("B_1", Block).table {
                addRepeatedRow("Data.Clients.Value") {
                    addRow().addCell().paragraph { text { variableRef(surNameVar.id) } }
                    addRow().addCell().string("Second")
                }
            }.variableStructureRef(variableStructure.id).build()
        )

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val tableRowSetId = result["Table"].last()["RowSetId"].textValue()
        val multipleRowSet = result["RowSet"].last { it["Id"].textValue() == tableRowSetId }
        multipleRowSet["RowSetType"].textValue().shouldBeEqualTo("RowSet")

        val firstSingleRow = result["RowSet"].last { it["Id"].textValue() == multipleRowSet["SubRowId"][0].textValue() }
        val firstCell = result["Cell"].last { it["Id"].textValue() == firstSingleRow["SubRowId"].textValue() }
        val firstFlow = result["Flow"].last { it["Id"].textValue() == firstCell["FlowId"].textValue() }
        firstFlow["FlowContent"]["P"][0]["T"][""].textValue()
            .shouldBeEqualTo($$"<repeated by unmapped $Data.Clients.Value$>")
        firstFlow["FlowContent"]["P"][1]["T"][""].textValue().shouldBeEqualTo($$"$surname$")

        val secondSingleRow =
            result["RowSet"].last { it["Id"].textValue() == multipleRowSet["SubRowId"][1].textValue() }
        val secondCell = result["Cell"].last { it["Id"].textValue() == secondSingleRow["SubRowId"].textValue() }
        val secondFlow = result["Flow"].last { it["Id"].textValue() == secondCell["FlowId"].textValue() }
        secondFlow["FlowContent"]["P"]["T"][""].textValue().shouldBeEqualTo("Second")
    }

    @Test
    fun `buildDocumentObject creates repeated flow with literal array path`() {
        // given
        val nameVar = mockVar(VariableBuilder("nameVar").name("Name").dataType(DataType.String).build())
        val variableStructure = mockVarStructure(
            VariableStructureBuilder("VS_1").addVariable(nameVar.id, "Data.Clients.Value", "Real Name").build()
        )

        val block = mockObj(
            DocumentObjectBuilder("B_1", Block)
                .repeatedContent("Data.Clients") {
                    paragraph { text { variableRef(nameVar.id) } }
                }
                .variableStructureRef(variableStructure.id)
                .build()
        )

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val repeatedFlow = getFlowAreaContentFlow(result)
        val arrayVarId = repeatedFlow["Variable"].textValue()
        val arrayVar = result["Variable"].last { it["Id"].textValue() == arrayVarId }
        arrayVar["Type"].textValue().shouldBeEqualTo("DataVariable")
        arrayVar["VarType"].textValue().shouldBeEqualTo("Array")
        repeatedFlow["SectionFlow"].textValue().shouldBeEqualTo("False")

        val nameVarId = repeatedFlow["FlowContent"]["P"]["T"]["O"]["Id"].textValue()
        val nameVarNode = result["Variable"].first { it["Id"].textValue() == nameVarId }
        nameVarNode["Name"].textValue().shouldBeEqualTo("Real Name")
    }

    @Test
    fun `buildDocumentObject creates repeated flow with variable ref array path`() {
        // given
        val clientsVar = mockVar(VariableBuilder("clientsVar").name("Clients").dataType(DataType.Array).build())
        val nameVar = mockVar(VariableBuilder("nameVar").name("name").dataType(DataType.String).build())

        val variableStructure = mockVarStructure(
            VariableStructureBuilder("VS_1")
                .addVariable(clientsVar.id, "Data")
                .addVariable(nameVar.id, VariableRef(clientsVar.id))
                .build()
        )
        val innerBlock = mockObj(DocumentObjectBuilder("B_inner", Block).internal(true).string("The name is: ").build())
        val block = mockObj(DocumentObjectBuilder("B_1", Block).repeatedContent(VariableRef(clientsVar.id)) {
                documentObjectRef(innerBlock.id)
                paragraph { text { variableRef(nameVar.id) } }
            }.variableStructureRef(variableStructure.id).build())

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val repeatedFlow = result["Flow"].last { it["Type"]?.textValue() == "Repeated" }
        val arrayVar = result["Variable"].first { it["Id"].textValue() == repeatedFlow["Variable"].textValue() }
        arrayVar["Name"].textValue().shouldBeEqualTo("Clients")
        repeatedFlow["SectionFlow"].textValue().shouldBeEqualTo("True")

        val repeatedFlowRefs = repeatedFlow["FlowContent"]["P"]["T"]["O"]
        repeatedFlowRefs.size().shouldBeEqualTo(2)
    }

    @Test
    fun `buildDocumentObject creates fallback flow when repeated block variable is not mapped`() {
        // given
        val arrayVar = mockVar(VariableBuilder("arrayVar").name("Clients").dataType(DataType.Array).build())

        val block = mockObj(
            DocumentObjectBuilder("B_1", Block)
                .repeatedContent(VariableRef(arrayVar.id)) {
                    string("Some content")
                }
                .build()
        )

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val repeatedFallbackFlow = getFlowAreaContentFlow(result)
        repeatedFallbackFlow["Type"].textValue().shouldBeEqualTo("Simple")
        repeatedFallbackFlow["SectionFlow"].textValue().shouldBeEqualTo("False")
        repeatedFallbackFlow["FlowContent"]["P"][0]["T"][""].textValue()
            .shouldBeEqualTo($$"<repeated by unmapped $Clients$>")
        repeatedFallbackFlow["FlowContent"]["P"][1]["T"][""].textValue().shouldBeEqualTo("Some content")
    }

    @Test
    fun `text style with targetId resolves to target style`() {
        // given
        val targetStyle = mockTextStyle(
            TextStyleBuilder("TS_target").name("Target Style").definition { fontFamily("Arial").bold(true) }.build()
        )
        val refStyle = mockTextStyle(TextStyleBuilder("TS_ref").name("Ref Style").styleRef(targetStyle.id).build())

        val block = mockObj(
            DocumentObjectBuilder(
                "B_1", DocumentObjectType.Block
            ).paragraph { text { string("Hello").styleRef(refStyle.id) } }.build()
        )

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val contentFlow = getFlowAreaContentFlow(result)
        val textNodePath = contentFlow["FlowContent"]["P"]["T"]["Id"].textValue()
        textNodePath.shouldBeEqualTo("TextStyles.${targetStyle.name}")

        val textStyleId = result["TextStyle"].first { it["Name"]?.textValue() == targetStyle.name }["Id"].textValue()
        val textStyleContent = result["TextStyle"].last { it["Id"].textValue() == textStyleId }
        textStyleContent["FontId"].textValue().shouldBeEqualTo("Def.Font")
        textStyleContent["Bold"].textValue().shouldBeEqualTo("True")
    }

    @Test
    fun `paragraph style with targetId resolves to target style`() {
        // given
        val targetStyle = mockParagraphStyle(
            ParagraphStyleBuilder("PS_Target").name("Target Style").definition { alignment(Alignment.Center) }.build()
        )
        val refStyle = mockParagraphStyle(
            ParagraphStyleBuilder("PS_Ref").name("Ref Para Style").styleRef(targetStyle.id).build()
        )

        val block = mockObj(
            DocumentObjectBuilder("B_1", DocumentObjectType.Block).paragraph {
                text { string("Hello") }.styleRef(refStyle.id)
            }.build()
        )

        // when
        val result =
            subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val contentFlow = getFlowAreaContentFlow(result)
        val paraNodePath = contentFlow["FlowContent"]["P"]["Id"].textValue()
        paraNodePath.shouldBeEqualTo("ParagraphStyles.${targetStyle.name}")

        val paraStyleId = result["ParaStyle"].first { it["Name"]?.textValue() == targetStyle.name }["Id"].textValue()
        val paraStyleContent = result["ParaStyle"].last { it["Id"].textValue() == paraStyleId }
        paraStyleContent["HAlign"].textValue().shouldBeEqualTo("Center")
    }

    @Test
    fun `simple display rule with single expression`() {
        val rule = aDisplayRule(
            Literal("A", LiteralDataType.String), BinOp.Equals, Literal("B", LiteralDataType.String)
        )

        val result = rule.toScript()

        result.shouldBeEqualTo("""return (String('A')==String('B'));""")
    }

    @Test
    fun `uppercase function`() {
        val rule = aDisplayRule(
            left = Function.UpperCase((Literal("B", LiteralDataType.String))),
            operator = BinOp.Equals,
            right = Literal("B", LiteralDataType.String)
        )

        val result = rule.toScript()

        result.shouldBeEqualTo("""return ((String('B')).toUpperCase()==String('B'));""")
    }

    @Test
    fun `lowercase function`() {
        val rule = aDisplayRule(
            left = Function.LowerCase(Literal("B", LiteralDataType.String)),
            operator = BinOp.Equals,
            right = Literal("B", LiteralDataType.String)
        )

        val result = rule.toScript()

        result.shouldBeEqualTo("""return ((String('B')).toLowerCase()==String('B'));""")
    }

    @Test
    fun `case insensitive equals`() {
        val rule = aDisplayRule(
            left = Literal("A", LiteralDataType.String),
            operator = BinOp.EqualsCaseInsensitive,
            right = Literal("B", LiteralDataType.String)
        )
        val result = rule.toScript()

        result.shouldBeEqualTo("""return (String('A').equalCaseInsensitive(String('B')));""")
    }

    @Test
    fun `case insensitive not equals`() {
        val rule = aDisplayRule(
            left = Literal("A", LiteralDataType.String),
            operator = BinOp.NotEqualsCaseInsensitive,
            right = Literal("B", LiteralDataType.String)
        )

        val result = rule.toScript()

        result.shouldBeEqualTo("""return ((not String('A').equalCaseInsensitive(String('B'))));""")
    }

    @Test
    fun `buildDocumentObject with empty column layout creates section with default values`() {
        // given
        val block = mockObj(
            DocumentObjectBuilder("B_1", Block)
                .columnLayout()
                .string("Column content")
                .build()
        )

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val section = result["Section"].last()
        section["Column"].size().shouldBeEqualTo(2)
        section["Column"][0]["GutterWidth"].textValue().shouldBeEqualTo("0.0")
        section["BalancingType"].textValue().shouldBeEqualTo("FirstColumnBiggest")
        section["AutoFinish"].textValue().shouldBeEqualTo("True")

        val flow = getFlowAreaContentFlow(result)
        val flowText = flow["FlowContent"]["P"]["T"]
        flowText["O"]["Id"].textValue().shouldBeEqualTo(section["Id"].textValue())
        flowText[""].textValue().shouldBeEqualTo("Column content")
    }

    @Test
    fun `buildDocumentObject with column layout is properly assigned to first following paragraph`() {
        // given
        val block = mockObj(
            DocumentObjectBuilder("B_1", Block)
                .paragraph { text { string("before column") } }
                .columnLayout { numberOfColumns(3).applyTo(ColumnApplyTo.WholeTemplate).balancingType(
                    ColumnBalancingType.Balanced).gutterWidth(15.millimeters())                }
                .paragraph { text { string("column content") } }
                .build()
        )

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val section = result["Section"].last()
        section["Column"].size().shouldBeEqualTo(3)
        section["Column"][0]["GutterWidth"].textValue().shouldBeEqualTo("0.015")
        section["BalancingType"].textValue().shouldBeEqualTo("Balanced")
        section["AutoFinish"].textValue().shouldBeEqualTo("False")

        val flow = getFlowAreaContentFlow(result)
        val flowParagraphs = flow["FlowContent"]["P"]
        flowParagraphs[0]["T"]["O"].shouldBeNull()
        flowParagraphs[0]["T"][""].textValue().shouldBeEqualTo("before column")
        flowParagraphs[1]["T"]["O"]["Id"].textValue().shouldBeEqualTo(section["Id"].textValue())
        flowParagraphs[1]["T"][""].textValue().shouldBeEqualTo("column content")
    }

    @Test
    fun `buildDocumentObject with column layout in text is applied to whole paragraph with priority over parent column layout`() {
        // given
        val variable = mockVar(VariableBuilder("var1").name("Var 1").dataType(DataType.String).build())
        val block = mockObj(DocumentObjectBuilder("B_1", Block).columnLayout { numberOfColumns(4) }.paragraph {
            text { string("First value") }
            text { variableRef(variable).columnLayout { numberOfColumns(3) } }
        }.build())

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val flow = getFlowAreaContentFlow(result)
        val flowTexts = flow["FlowContent"]["P"]["T"]
        val firstFlowText = flowTexts[0]
        val sectionId = firstFlowText["O"]["Id"].textValue()
        firstFlowText[""].textValue().shouldBeEqualTo("First value")

        val secondFlowText = flowTexts[1]
        secondFlowText["O"].shouldBeNull()
        secondFlowText[""].textValue().shouldBeEqualTo($$"$Var 1$")

        val section = result["Section"].last { it["Id"].textValue() == sectionId }
        section["Column"].size().shouldBeEqualTo(3)
    }

    private fun DisplayRule.toScript(): String {
        return definition?.toScript(
            layout = LayoutImpl(),
            variableStructure = aVariableStructure("some struct"),
            findVar = { aVariable(it) }
        ) ?: error("No definition")
    }

    private fun mockObj(documentObject: DocumentObject): DocumentObject {
        every { documentObjectRepository.findOrFail(documentObject.id) } returns documentObject
        return documentObject
    }

    private fun mockAttachment(attachment: Attachment): Attachment {
        every { attachmentRepository.findOrFail(attachment.id) } returns attachment
        every { attachmentRepository.find(attachment.id) } returns attachment
        return attachment
    }

    private fun mockVar(variable: Variable): Variable {
        every { variableRepository.findOrFail(variable.id) } returns variable
        every { variableRepository.find(variable.id) } returns variable
        return variable
    }

    private fun mockVarStructure(variableStructure: VariableStructure): VariableStructure {
        every { variableStructureRepository.findOrFail(variableStructure.id) } returns variableStructure
        val currentAllStructures = variableStructureRepository.listAll()
        every { variableStructureRepository.listAll() } returns currentAllStructures + variableStructure
        return variableStructure
    }

    private fun mockTextStyle(textStyle: TextStyle): TextStyle {
        every { textStyleRepository.findOrFail(textStyle.id) } returns textStyle
        val currentAllStyles = textStyleRepository.listAll()
        every { textStyleRepository.listAll() } returns currentAllStyles + textStyle
        return textStyle
    }

    private fun mockParagraphStyle(paragraphStyle: ParagraphStyle): ParagraphStyle {
        every { paragraphStyleRepository.findOrFail(paragraphStyle.id) } returns paragraphStyle
        val currentAllStyles = paragraphStyleRepository.listAll()
        every { paragraphStyleRepository.listAll() } returns currentAllStyles + paragraphStyle
        return paragraphStyle
    }

    private fun mockImage(image: com.quadient.migration.api.dto.migrationmodel.Image): com.quadient.migration.api.dto.migrationmodel.Image {
        every { imageRepository.findOrFail(image.id) } returns image
        every { imageRepository.find(image.id) } returns image
        return image
    }
}
