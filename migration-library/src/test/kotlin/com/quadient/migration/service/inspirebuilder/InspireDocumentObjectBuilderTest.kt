package com.quadient.migration.service.inspirebuilder

import tools.jackson.dataformat.xml.XmlMapper
import tools.jackson.module.kotlin.KotlinModule
import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.ProjectConfig
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
import com.quadient.migration.api.dto.migrationmodel.builder.DisplayRuleBuilder
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
import com.quadient.migration.service.DesignerIcmDataCache
import com.quadient.migration.service.DesignerResourcePathProvider
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.BinOp
import com.quadient.migration.shared.Color
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
import com.quadient.migration.shared.QrCodeErrorCorrectionLevel
import com.quadient.migration.shared.QrCodeSize
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.SkipOptions
import com.quadient.migration.shared.TableAction
import com.quadient.migration.shared.millimeters
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.getFlowAreaContentFlow
import com.quadient.migration.tools.model.*
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeNull
import com.quadient.migration.tools.shouldNotBeNull
import com.quadient.wfdxml.api.layoutnodes.TextStyleInheritFlag
import com.quadient.wfdxml.internal.module.layout.LayoutImpl
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.collections.last

class InspireDocumentObjectBuilderTest {
    private val config = aProjectConfig(output = InspireOutput.Designer)
    private val documentObjectRepository = mockk<DocumentObjectRepository>()
    private val textStyleRepository = mockk<TextStyleRepository>()
    private val paragraphStyleRepository = mockk<ParagraphStyleRepository>()
    private val variableRepository = mockk<VariableRepository>()
    private val variableStructureRepository = mockk<VariableStructureRepository>()
    private val displayRuleRepository = mockk<DisplayRuleRepository>()
    private val imageRepository = mockk<ImageRepository>()
    private val attachmentRepository = mockk<AttachmentRepository>()
    private val ipsService = mockk<IpsService>()
    private val resourcePathProvider = DesignerResourcePathProvider(config)
    private val icmDataCache = DesignerIcmDataCache(ipsService, resourcePathProvider)

    private val xmlMapper = XmlMapper.builder().addModule(KotlinModule.Builder().build()).build()

    private var subject = DesignerDocumentObjectBuilder(
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
        val paraStyleId = paraStyles.first { it["Name"]?.stringValue() == paragraphStyle.name }["Id"].stringValue()
        val paraStyleContent = paraStyles.last { it["Id"].stringValue() == paraStyleId }

        paraStyleContent["PDFAdvanced"]["Tagging"]["Rule"].stringValue().shouldBeEqualTo("P")
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
        val hyperlinkStyleId = result["TextStyle"].first { it["Name"]?.stringValue() == "text_url_1" }["Id"].stringValue()
        val hyperlinkStyleContent = result["TextStyle"].last { it["Id"].stringValue() == hyperlinkStyleId }

        val urlVarId = hyperlinkStyleContent["URLLink"].stringValue()
        hyperlinkStyleContent["URLAlternateText"].stringValue().shouldBeEqualTo("Link to example website")

        result["Variable"].first { it["Name"].stringValue() == "text_url_1" }
        val urlVariableContent = result["Variable"].last { it["Id"].stringValue() == urlVarId }
        urlVariableContent["Type"].stringValue().shouldBeEqualTo("Constant")
        urlVariableContent["VarType"].stringValue().shouldBeEqualTo("String")
        urlVariableContent["Content"].stringValue().shouldBeEqualTo("https://www.example.com")
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
            result["TextStyle"].first { it["Name"]?.stringValue() == "${textStyle.name}_url_1" }["Id"].stringValue()
        val hyperlinkStyleContent = result["TextStyle"].last { it["Id"].stringValue() == hyperlinkStyleId }

        hyperlinkStyleContent["Type"].stringValue().shouldBeEqualTo("Delta")
        hyperlinkStyleContent["AncestorId"].stringValue().shouldBeEqualTo("Def.TextStyleHyperlink")
        val inheritFlags = hyperlinkStyleContent["InheritFlag"]

        inheritFlags.size().shouldBeEqualTo(TextStyleInheritFlag.entries.size - 2)

        assert(inheritFlags.none { it.stringValue() == "Underline" })
        assert(inheritFlags.none { it.stringValue() == "FillStyle" })
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
        contentFlow["FlowContent"]["P"]["T"][""].stringValue().shouldBeEqualTo("See attached: ")
        val attachmentFlowId = contentFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()

        val attachmentFlow = result["Flow"].last { it["Id"].stringValue() == attachmentFlowId }
        attachmentFlow["Type"].stringValue().shouldBeEqualTo("DirectExternal")
        attachmentFlow["ExternalLocation"].stringValue().shouldBeEqualTo("icm://document.pdf")
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
        val placeholderFlowId = contentFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()
        val placeholderFlow = result["Flow"].last { it["Id"].stringValue() == placeholderFlowId }
        placeholderFlow["FlowContent"]["P"]["T"][""].stringValue().shouldBeEqualTo("Attachment not available")
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
        flow["FlowContent"]["P"]["T"][""].stringValue().shouldBeEqualTo("Text  more text")
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
            mockObj(DocumentObjectBuilder("B_1", Block).string("Image: ").imageRef(image.id).build())

        // when
        val result =
            subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val flowId = result["FlowArea"].last()["FlowId"].stringValue()
        val flowAreaFlow = result["Flow"].last { it["Id"].stringValue() == flowId }
        val refIds = flowAreaFlow["FlowContent"]["P"]["T"]["O"]
        val attachmentFlow = result["Flow"].last { it["Id"].stringValue() == refIds[1]["Id"].stringValue() }

        attachmentFlow["Type"].stringValue().shouldBeEqualTo("DirectExternal")
        attachmentFlow["ExternalLocation"].stringValue().shouldBeEqualTo("icm://resolved.pdf")
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
            DocumentObjectBuilder("B_1", Block).attachmentRef(attachment.id).build()
        )

        // when
        val result =
            subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val imageId = result["ImageObject"].last()["ImageId"].stringValue()
        val image = result["Image"].last { it["Id"].stringValue() == imageId }
        image["ImageLocation"].stringValue().shouldBeEqualTo("VCSLocation,icm://resolved.png")
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
        val allSheetNameVariableIds = result["Pages"]["SheetNameVariableId"].toList().map { it.stringValue() ?: "" }

        // then
        allSheetNameVariableIds.count { it.isBlank() }.shouldBeEqualTo(37)

        val pdfSheetNameVariableIds = allSheetNameVariableIds.filter { it.isNotBlank() }
        pdfSheetNameVariableIds.size.shouldBeEqualTo(5)

        val variableNames =
            pdfSheetNameVariableIds.map { varId -> result["Variable"].first { it["Id"].stringValue() == varId }["Name"].stringValue() }
        variableNames.shouldBeEqualTo(listOf("TaggingTitle", "TaggingAuthor", "TaggingSubject", "TaggingKeywords", "TaggingProduce"))

        val variableScripts =
            pdfSheetNameVariableIds.map { varId -> result["Variable"].last { it["Id"].stringValue() == varId }["Script"].stringValue() }
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
        val allSheetNameVariableIds = result["Pages"]["SheetNameVariableId"].toList().map { it.stringValue() ?: "" }
        allSheetNameVariableIds.size.shouldBeEqualTo(39)
        val pdfAuthorSheetName = allSheetNameVariableIds[38]

        val variableScript = result["Variable"].last { it["Id"].stringValue() == pdfAuthorSheetName }["Script"].stringValue()
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
        val variableData = result["Variable"].first { it["Name"].stringValue() == "Name" }
        variableData["ParentId"].stringValue().shouldBeEqualTo("Data.Clients.Value")
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
        val authorVariableId = result["Pages"]["SheetNameVariableId"].last().stringValue()
        val variableScript = result["Variable"].last { it["Id"].stringValue() == authorVariableId }["Script"].stringValue()
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
        val repeatedRowSetId = result["Table"].last()["RowSetId"].stringValue()
        val repeatedRowSet = result["RowSet"].last { it["Id"].stringValue() == repeatedRowSetId }

        repeatedRowSet["RowSetType"].stringValue().shouldBeEqualTo("Repeated")
        val arrayVarId = repeatedRowSet["VariableId"].stringValue()
        val arrayVar = result["Variable"].last { it["Id"].stringValue() == arrayVarId }
        arrayVar["Type"].stringValue().shouldBeEqualTo("DataVariable")
        arrayVar["VarType"].stringValue().shouldBeEqualTo("Array")

        val multipleRowId = repeatedRowSet["SubRowId"].stringValue()
        val multipleRow = result["RowSet"].last { it["Id"].stringValue() == multipleRowId }
        multipleRow["RowSetType"].stringValue().shouldBeEqualTo("RowSet")
        val secondRowId = multipleRow["SubRowId"][1].stringValue()

        val secondRow = result["RowSet"].last { it["Id"].stringValue() == secondRowId }
        secondRow["RowSetType"].stringValue().shouldBeEqualTo("Row")

        val secondCellId = secondRow["SubRowId"][1].stringValue()
        val secondCell = result["Cell"].last { it["Id"].stringValue() == secondCellId }

        val secondCellFlow = result["Flow"].last { it["Id"].stringValue() == secondCell["FlowId"].stringValue() }
        val variableId = secondCellFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()

        val variable = result["Variable"].first { it["Id"].stringValue() == variableId }
        variable["Name"].stringValue().shouldBeEqualTo("Job Name")
        variable["ParentId"].stringValue().shouldBeEqualTo("Data.Clients.Value")

        result["Root"]["LockedWebNodes"]["LockedWebNode"].stringValue().shouldBeEqualTo(repeatedRowSetId)
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
        val tableRowSetId = result["Table"].last()["RowSetId"].stringValue()
        val conditionRowSet = result["RowSet"].last { it["Id"].stringValue() == tableRowSetId }
        conditionRowSet["RowSetType"].stringValue().shouldBeEqualTo("InlCond")
        conditionRowSet["RowSetCondition"][0]["Condition"].stringValue()
            .shouldBeEqualTo("return (String('A')==String('B'));")

        val repeatedRowSet = result["RowSet"].last {
            it["Id"].stringValue() == conditionRowSet["RowSetCondition"][0]["SubRowId"].stringValue()
        }
        repeatedRowSet["RowSetType"].stringValue().shouldBeEqualTo("Repeated")
        val arrayVarId = repeatedRowSet["VariableId"].stringValue()
        val arrayVar = result["Variable"].last { it["Id"].stringValue() == arrayVarId }
        arrayVar["Type"].stringValue().shouldBeEqualTo("DataVariable")
        arrayVar["VarType"].stringValue().shouldBeEqualTo("Array")

        val innerRow = result["RowSet"].last { it["Id"].stringValue() == repeatedRowSet["SubRowId"].stringValue() }
        innerRow["RowSetType"].stringValue().shouldBeEqualTo("Row")

        val secondCell = result["Cell"].last { it["Id"].stringValue() == innerRow["SubRowId"][1].stringValue() }
        val secondCellFlow = result["Flow"].last { it["Id"].stringValue() == secondCell["FlowId"].stringValue() }
        val variableId = secondCellFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()
        val variable = result["Variable"].first { it["Id"].stringValue() == variableId }
        variable["Name"].stringValue().shouldBeEqualTo("Client Name")
        variable["ParentId"].stringValue().shouldBeEqualTo("Data.Clients.Value")
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
        val tableRowSetId = result["Table"].last()["RowSetId"].stringValue()
        val tableRowSet = result["RowSet"].last { it["Id"].stringValue() == tableRowSetId }
        tableRowSet["RowSetType"].stringValue().shouldBeEqualTo("Row")

        val cell = result["Cell"].last { it["Id"].stringValue() == tableRowSet["SubRowId"].stringValue() }
        val flow = result["Flow"].last { it["Id"].stringValue() == cell["FlowId"].stringValue() }
        // first paragraph = warning, second paragraph = original "Name"
        flow["FlowContent"]["P"][0]["T"][""].stringValue().shouldBeEqualTo($$"<repeated row by unmapped $Clients$>")
        flow["FlowContent"]["P"][1]["T"][""].stringValue().shouldBeEqualTo("Name")
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
        val tableRowSetId = result["Table"].last()["RowSetId"].stringValue()
        val multipleRowSet = result["RowSet"].last { it["Id"].stringValue() == tableRowSetId }
        multipleRowSet["RowSetType"].stringValue().shouldBeEqualTo("RowSet")

        val firstSingleRow = result["RowSet"].last { it["Id"].stringValue() == multipleRowSet["SubRowId"][0].stringValue() }
        val firstCell = result["Cell"].last { it["Id"].stringValue() == firstSingleRow["SubRowId"].stringValue() }
        val firstFlow = result["Flow"].last { it["Id"].stringValue() == firstCell["FlowId"].stringValue() }
        firstFlow["FlowContent"]["P"][0]["T"][""].stringValue()
            .shouldBeEqualTo($$"<repeated row by unmapped $Data.Clients.Value$>")
        firstFlow["FlowContent"]["P"][1]["T"][""].stringValue().shouldBeEqualTo($$"$surname$")

        val secondSingleRow =
            result["RowSet"].last { it["Id"].stringValue() == multipleRowSet["SubRowId"][1].stringValue() }
        val secondCell = result["Cell"].last { it["Id"].stringValue() == secondSingleRow["SubRowId"].stringValue() }
        val secondFlow = result["Flow"].last { it["Id"].stringValue() == secondCell["FlowId"].stringValue() }
        secondFlow["FlowContent"]["P"]["T"][""].stringValue().shouldBeEqualTo("Second")
    }

    @Test
    fun `buildDocumentObject creates nested repeated rowsets`() {
        // given
        val itemNameVar = mockVar(VariableBuilder("itemNameVar").name("Item Name").dataType(DataType.String).build())
        val ordersArrayVar = mockVar(VariableBuilder("orders").name("Orders").dataType(DataType.Array).build())

        val variableStructure = mockVarStructure(
            VariableStructureBuilder("VS_1").addVariable(ordersArrayVar.id, "Data.Clients.Value")
                .addVariable(itemNameVar.id, ordersArrayVar).build()
        )

        val block = mockObj(
            DocumentObjectBuilder("B_1", Block).table {
                addRepeatedRow("Data.Clients.Value") {
                    addRepeatedRow(ordersArrayVar) {
                        addRow {
                            addCell { string("Order: ") }
                            addCell { paragraph { text { variableRef(itemNameVar.id) } } }
                        }
                    }
                }
            }.variableStructureRef(variableStructure.id).build()
        )

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then — outer repeated rowset driven by Data.Clients.Value
        val outerRepeatedRowSetId = result["Table"].last()["RowSetId"].stringValue()
        val outerRepeatedRowSet = result["RowSet"].last { it["Id"].stringValue() == outerRepeatedRowSetId }
        outerRepeatedRowSet["RowSetType"].stringValue().shouldBeEqualTo("Repeated")
        val outerArrayVar = result["Variable"].last { it["Id"].stringValue() == outerRepeatedRowSet["VariableId"].stringValue() }
        outerArrayVar["VarType"].stringValue().shouldBeEqualTo("Array")

        // then — inner repeated rowset is nested directly inside the outer
        val innerRepeatedRowSetId = outerRepeatedRowSet["SubRowId"].stringValue()
        val innerRepeatedRowSet = result["RowSet"].last { it["Id"].stringValue() == innerRepeatedRowSetId }
        innerRepeatedRowSet["RowSetType"].stringValue().shouldBeEqualTo("Repeated")
        val innerArrayVar = result["Variable"].last { it["Id"].stringValue() == innerRepeatedRowSet["VariableId"].stringValue() }
        innerArrayVar["VarType"].stringValue().shouldBeEqualTo("Array")

        // then — single row is nested inside the inner repeated rowset
        val singleRowId = innerRepeatedRowSet["SubRowId"].stringValue()
        val singleRow = result["RowSet"].last { it["Id"].stringValue() == singleRowId }
        singleRow["RowSetType"].stringValue().shouldBeEqualTo("Row")

        val secondCellId = singleRow["SubRowId"][1].stringValue()
        val secondCell = result["Cell"].last { it["Id"].stringValue() == secondCellId }
        val secondCellFlow = result["Flow"].last { it["Id"].stringValue() == secondCell["FlowId"].stringValue() }
        val variableId = secondCellFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()
        val variable = result["Variable"].first { it["Id"].stringValue() == variableId }
        variable["Name"].stringValue().shouldBeEqualTo("Item Name")

        val lockedWebNodes = result["Root"]["LockedWebNodes"]["LockedWebNode"]
        val lockedIds = lockedWebNodes.toList().map { it.stringValue() }
        lockedIds.contains(outerRepeatedRowSetId).shouldBeEqualTo(true)
        lockedIds.contains(innerRepeatedRowSetId).shouldBeEqualTo(true)
    }

    @Test
    fun `buildDocumentObject throws when table has no body rows`() {
        // given
        val block = mockObj(DocumentObjectBuilder("B_1", Block).table { }.build())

        // when / then
        val ex = assertThrows<IllegalStateException> { subject.buildDocumentObject(block) }
        ex.message.shouldBeEqualTo("Table has no body rows. At least one body row is required.")
    }

    @Test
    fun `buildDocumentObject throws when table has header rows but no body rows`() {
        // given
        val block = mockObj(
            DocumentObjectBuilder("B_1", Block).table {
                addHeaderRow { addCell { string("Header") } }
            }.build()
        )

        // when / then
        val ex = assertThrows<IllegalStateException> { subject.buildDocumentObject(block) }
        ex.message.shouldBeEqualTo("Table has no body rows. At least one body row is required.")
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
        val arrayVarId = repeatedFlow["Variable"].stringValue()
        val arrayVar = result["Variable"].last { it["Id"].stringValue() == arrayVarId }
        arrayVar["Type"].stringValue().shouldBeEqualTo("DataVariable")
        arrayVar["VarType"].stringValue().shouldBeEqualTo("Array")
        repeatedFlow["SectionFlow"].stringValue().shouldBeEqualTo("False")

        val nameVarId = repeatedFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()
        val nameVarNode = result["Variable"].first { it["Id"].stringValue() == nameVarId }
        nameVarNode["Name"].stringValue().shouldBeEqualTo("Real Name")
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
        val repeatedFlow = result["Flow"].last { it["Type"]?.stringValue() == "Repeated" }
        val arrayVar = result["Variable"].first { it["Id"].stringValue() == repeatedFlow["Variable"].stringValue() }
        arrayVar["Name"].stringValue().shouldBeEqualTo("Clients")
        repeatedFlow["SectionFlow"].stringValue().shouldBeEqualTo("True")

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
        repeatedFallbackFlow["Type"].stringValue().shouldBeEqualTo("Simple")
        repeatedFallbackFlow["SectionFlow"].stringValue().shouldBeEqualTo("False")
        repeatedFallbackFlow["FlowContent"]["P"][0]["T"][""].stringValue()
            .shouldBeEqualTo($$"<repeated content by unmapped $Clients$>")
        repeatedFallbackFlow["FlowContent"]["P"][1]["T"][""].stringValue().shouldBeEqualTo("Some content")
    }

    @Test
    fun `text style with targetId resolves to target style`() {
        // given
        val targetStyle = mockTextStyle(
            TextStyleBuilder("TS_target").name("Target Style").definition { fontFamily("Arial").bold(true) }.build()
        )
        val refStyle = mockTextStyle(TextStyleBuilder("TS_ref").name("Ref Style").styleRef(targetStyle.id).build())

        val block = mockObj(
            DocumentObjectBuilder("B_1", Block)
                .paragraph { text { string("Hello").styleRef(refStyle.id) } }.build()
        )

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val contentFlow = getFlowAreaContentFlow(result)
        val textNodePath = contentFlow["FlowContent"]["P"]["T"]["Id"].stringValue()
        textNodePath.shouldBeEqualTo("TextStyles.${targetStyle.name}")

        val textStyleId = result["TextStyle"].first { it["Name"]?.stringValue() == targetStyle.name }["Id"].stringValue()
        val textStyleContent = result["TextStyle"].last { it["Id"].stringValue() == textStyleId }
        textStyleContent["FontId"].stringValue().shouldBeEqualTo("Def.Font")
        textStyleContent["Bold"].stringValue().shouldBeEqualTo("True")
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
            DocumentObjectBuilder("B_1", Block).paragraph {
                text { string("Hello") }.styleRef(refStyle.id)
            }.build()
        )

        // when
        val result =
            subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val contentFlow = getFlowAreaContentFlow(result)
        val paraNodePath = contentFlow["FlowContent"]["P"]["Id"].stringValue()
        paraNodePath.shouldBeEqualTo("ParagraphStyles.${targetStyle.name}")

        val paraStyleId = result["ParaStyle"].first { it["Name"]?.stringValue() == targetStyle.name }["Id"].stringValue()
        val paraStyleContent = result["ParaStyle"].last { it["Id"].stringValue() == paraStyleId }
        paraStyleContent["HAlign"].stringValue().shouldBeEqualTo("Center")
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
        section["Column"][0]["GutterWidth"].stringValue().shouldBeEqualTo("0.0")
        section["BalancingType"].stringValue().shouldBeEqualTo("FirstColumnBiggest")
        section["AutoFinish"].stringValue().shouldBeEqualTo("True")

        val flow = getFlowAreaContentFlow(result)
        val flowText = flow["FlowContent"]["P"]["T"]
        flowText["O"]["Id"].stringValue().shouldBeEqualTo(section["Id"].stringValue())
        flowText[""].stringValue().shouldBeEqualTo("Column content")
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
        section["Column"][0]["GutterWidth"].stringValue().shouldBeEqualTo("0.015")
        section["BalancingType"].stringValue().shouldBeEqualTo("Balanced")
        section["AutoFinish"].stringValue().shouldBeEqualTo("False")

        val flow = getFlowAreaContentFlow(result)
        val flowParagraphs = flow["FlowContent"]["P"]
        flowParagraphs[0]["T"]["O"].shouldBeNull()
        flowParagraphs[0]["T"][""].stringValue().shouldBeEqualTo("before column")
        flowParagraphs[1]["T"]["O"]["Id"].stringValue().shouldBeEqualTo(section["Id"].stringValue())
        flowParagraphs[1]["T"][""].stringValue().shouldBeEqualTo("column content")
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
        val sectionId = firstFlowText["O"]["Id"].stringValue()
        firstFlowText[""].stringValue().shouldBeEqualTo("First value")

        val secondFlowText = flowTexts[1]
        secondFlowText["O"].shouldBeNull()
        secondFlowText[""].stringValue().shouldBeEqualTo($$"$Var 1$")

        val section = result["Section"].last { it["Id"].stringValue() == sectionId }
        section["Column"].size().shouldBeEqualTo(3)
    }

    @Test
    fun `buildDocumentObject creates QR code`() {
        // given
        val barcodeVar = mockVar(VariableBuilder("barcodeVar").name("BarcodeData").dataType(DataType.String).build())
        val variableStructure = mockVarStructure(
            VariableStructureBuilder("VS_1").addVariable(barcodeVar.id, "Data.Records.Value").build()
        )
        val page = DocumentObjectBuilder("P_1", DocumentObjectType.Page)
            .barcode {
                qr {
                    data("012345")
                    position { left(20.millimeters()); top(30.millimeters()); width(29.millimeters()); height(29.millimeters()) }
                    errorCorrection(QrCodeErrorCorrectionLevel.H)
                    size(QrCodeSize.Fixed45x45)
                    moduleWidth(Size.ofMillimeters(1))
                    quietZone(Size.ofMillimeters(3))
                    barcodeFill(Color.fromHex("#1A2B3C"))
                    backgroundFill(Color.fromHex("#F0E0D0"))
                    variableRef(barcodeVar)
                }
            }
            .build()
        mockObj(page)

        val template = DocumentObjectBuilder("T_1", DocumentObjectType.Template)
            .variableStructureRef(variableStructure.id)
            .documentObjectRef(page)
            .build()

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
        barcode["ShowDataTextProcessed"].stringValue().shouldBeEqualTo("True")
        barcode["FillStyleId"].shouldNotBeNull()
        barcode["FillBackgroungStyleId"].shouldNotBeNull()
        val variableId = barcode["VariableId"].stringValue()
        val variable = result["Variable"].first { it["Id"].stringValue() == variableId }
        variable["Name"].stringValue().shouldBeEqualTo("BarcodeData")

        val barcodeGenerator = barcode["BarcodeGenerator"]
        barcodeGenerator["Type"].stringValue().shouldBeEqualTo("QRBarcodeGenerator")
        barcodeGenerator["ModulWidth"].stringValue().shouldBeEqualTo("0.001")
        barcodeGenerator["WhiteSpace"].stringValue().shouldBeEqualTo("0.003")
        barcodeGenerator["ErrorLevel"].stringValue().shouldBeEqualTo("72")
        barcodeGenerator["PredefinedBarcodeSize"].stringValue().shouldBeEqualTo("45")
    }

    @Test
    fun `buildDocumentObject creates code39 barcode`() {
        // given
        val page = DocumentObjectBuilder("P_1", DocumentObjectType.Page)
            .code39Barcode {
                data("ABC123")
                position { left(10.millimeters()); top(10.millimeters()); width(50.millimeters()); height(20.millimeters()) }
                height(Size.ofMillimeters(15))
                moduleWidth(Size.ofMillimeters(1))
                quietZone(5.0)
                useControlSum(true)
                ratio(3.5)
                interCharacterSpaceRatio(2.0)
                directMetric(true)
                firstBarWidth(Size.ofMillimeters(2))
                secondBarWidth(Size.ofMillimeters(3))
                firstBarSpace(Size.ofMillimeters(4))
                secondBarSpace(Size.ofMillimeters(5))
                barcodeFill(Color.fromHex("#2C3E50"))
                backgroundFill(Color.fromHex("#ECF0F1"))
            }
            .build()
        mockObj(page)

        val template = DocumentObjectBuilder("T_1", DocumentObjectType.Template)
            .documentObjectRef(page)
            .build()

        // when
        val result = subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val barcode = result["Barcode"].last()
        barcode["BarcodeName"].stringValue().shouldBeEqualTo("Code 39")
        barcode["ConvertString"].stringValue().shouldBeEqualTo("ABC123")
        barcode["Pos"]["X"].stringValue().shouldBeEqualTo("0.01")
        barcode["Pos"]["Y"].stringValue().shouldBeEqualTo("0.01")
        barcode["Size"]["X"].stringValue().shouldBeEqualTo("0.05")
        barcode["Size"]["Y"].stringValue().shouldBeEqualTo("0.02")
        barcode["FillStyleId"].shouldNotBeNull()
        barcode["FillBackgroungStyleId"].shouldNotBeNull()

        val barcodeGenerator = barcode["BarcodeGenerator"]
        barcodeGenerator["Type"].stringValue().shouldBeEqualTo("Code39BarcodeGenerator")
        barcodeGenerator["Ratio"].stringValue().shouldBeEqualTo("3.5")
        barcodeGenerator["Height"].stringValue().shouldBeEqualTo("0.015")
        barcodeGenerator["ModulSize"].stringValue().shouldBeEqualTo("0.001")
        barcodeGenerator["WhiteSpace"].stringValue().shouldBeEqualTo("5.0")
        barcodeGenerator["UseControlSum"].stringValue().shouldBeEqualTo("True")
        barcodeGenerator["InterCharacterSpaceRatio"].stringValue().shouldBeEqualTo("2.0")
        barcodeGenerator["UseDirectMetric"].stringValue().shouldBeEqualTo("True")
        barcodeGenerator["ModuleBlackSize0"].stringValue().shouldBeEqualTo("0.002")
        barcodeGenerator["ModuleBlackSize1"].stringValue().shouldBeEqualTo("0.003")
        barcodeGenerator["ModuleSpaceSize0"].stringValue().shouldBeEqualTo("0.004")
        barcodeGenerator["ModuleSpaceSize1"].stringValue().shouldBeEqualTo("0.005")
    }

    @Test
    fun `buildDocumentObject creates barcode in composite flow`() {
        // given
        val inner = mockObj(
            DocumentObjectBuilder("S_1", Block)
                .qrCode { data("QR-123") }
                .internal(true)
                .build()
        )

        val block = mockObj(
            DocumentObjectBuilder("B_1", Block)
                .documentObjectRef(inner)
                .build()
        )

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val barcode = result["Barcode"].last()
        barcode["BarcodeName"].stringValue().shouldBeEqualTo("QR")
        barcode["ConvertString"].stringValue().shouldBeEqualTo("QR-123")
    }

    @Test
    fun `buildDocumentObject creates barcode inline in text content`() {
        // given
        val block = mockObj(
            DocumentObjectBuilder("B_1", Block)
                .paragraph { text { qrCode { data("QR-456") } } }
                .build()
        )

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val barcode = result["Barcode"].last()
        barcode["BarcodeName"].stringValue().shouldBeEqualTo("QR")
        barcode["ConvertString"].stringValue().shouldBeEqualTo("QR-456")
    }

    @Test
    fun `single-language SelectByLanguage is flattened when language matches defaultLanguage`() {
        // given
        val subject = aSubject(aProjectConfig(defaultLanguage = "en", output = InspireOutput.Designer))
        val block = mockObj(
            DocumentObjectBuilder("obj", Block)
                .selectByLanguage {
                    case {
                        language("en")
                        paragraph { string("english only") }
                    }
                }
                .build()
        )

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val flow = getFlowAreaContentFlow(result)
        flow["FlowContent"]["P"]["T"][""].stringValue().shouldBeEqualTo("english only")
        flow["Type"].stringValue().shouldBeEqualTo("Simple")
        result["Flow"].filter { it["Type"]?.stringValue() == "Language" }.size.shouldBeEqualTo(0)
    }

    @Test
    fun `single-language SelectByLanguage keeps Language flow when language differs from defaultLanguage`() {
        // given
        val subject = aSubject(aProjectConfig(defaultLanguage = "fr", output = InspireOutput.Designer))
        val block = mockObj(
            DocumentObjectBuilder("obj", Block)
                .selectByLanguage {
                    case {
                        language("en")
                        paragraph { string("english only") }
                    }
                }
                .build()
        )

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val flow = getFlowAreaContentFlow(result)
        flow["Type"].stringValue().shouldBeEqualTo("Language")
        flow["Condition"]["Value"].stringValue().shouldBeEqualTo("en")
        flow["Default"].stringValue().shouldNotBeNull()
    }

    @Test
    fun `multi-language SelectByLanguage emits a Language flow`() {
        // given
        val block = mockObj(
            DocumentObjectBuilder("obj", Block)
                .selectByLanguage {
                    case {
                        language("en")
                        paragraph { string("english") }
                    }
                    case {
                        language("de")
                        paragraph { string("german") }
                    }
                }
                .build()
        )

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val flow = getFlowAreaContentFlow(result)
        flow["Type"].stringValue().shouldBeEqualTo("Language")
        flow["Condition"].size().shouldBeEqualTo(2)
        flow["Condition"][0]["Value"].stringValue().shouldBeEqualTo("en")
        flow["Condition"][1]["Value"].stringValue().shouldBeEqualTo("de")
        flow["Default"].stringValue().shouldNotBeNull()
    }

    @Test
    fun `buildDocumentObject flattens table with various rows to flows`() {
        // given
        val labelVar = mockVar(VariableBuilder("labelVar").name("Label Name").dataType(DataType.String).build())
        val variableStructure = mockVarStructure(
            VariableStructureBuilder("VS_1").addVariable(labelVar.id, "Data.Labels.Value", "Label Value").build()
        )
        val rowDisplayRule = mockRule(DisplayRuleBuilder("R_1").comparison { value("A").equals().value("B") }.build())

        val block = mockObj(
            DocumentObjectBuilder("B_1", Block).table {
                action(TableAction.Flatten)
                addFirstHeaderRow {
                    addCell { paragraph { text { string("Col1 First Header") } } }
                    addCell { string("Col2 First Header") }
                }
                addHeaderRow {
                    addCell { paragraph { text { string("Col1 Header") } } }
                    addCell { paragraph { text { string("Col2 Header") } } }
                }
                addRow {
                    displayRuleRef(rowDisplayRule)
                    addCell { paragraph { text { string("Name:") } } }
                    addCell { paragraph { text { variableRef(labelVar.id) } } }
                }
                addRow {
                    addCell { paragraph { text { string("Total") } } }
                    addCell { paragraph { text { string("N/A") } } }
                }
            }.variableStructureRef(variableStructure.id).build()
        )

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        result.path("Table").isMissingNode.shouldBeEqualTo(true)

        val flowId = result["FlowArea"].last()["FlowId"].stringValue()
        val sectionFlow = result["Flow"].last { it["Id"].stringValue() == flowId }
        sectionFlow["SectionFlow"].stringValue().shouldBeEqualTo("True")
        val rowFlowRefs = sectionFlow["FlowContent"]["P"]["T"]["O"]
        rowFlowRefs.size().shouldBeEqualTo(4)

        val firstHeaderFlow = result["Flow"].last { it["Id"].stringValue() == rowFlowRefs[0]["Id"].stringValue() }
        firstHeaderFlow["FlowContent"]["P"][0]["T"][""].stringValue().shouldBeEqualTo("Col1 First Header")
        firstHeaderFlow["FlowContent"]["P"][1]["T"][""].stringValue().shouldBeEqualTo("Col2 First Header")

        val headerFlow = result["Flow"].last { it["Id"].stringValue() == rowFlowRefs[1]["Id"].stringValue() }
        headerFlow["FlowContent"]["P"][0]["T"][""].stringValue().shouldBeEqualTo("Col1 Header")
        headerFlow["FlowContent"]["P"][1]["T"][""].stringValue().shouldBeEqualTo("Col2 Header")

        val condFlow = result["Flow"].last { it["Id"].stringValue() == rowFlowRefs[2]["Id"].stringValue() }
        condFlow["Type"].stringValue().shouldBeEqualTo("InlCond")
        condFlow["Condition"]["Value"].stringValue().shouldBeEqualTo("return (String('A')==String('B'));")
        val condSuccessFlowId = condFlow["Condition"][""].stringValue()
        val condSuccessFlow = result["Flow"].last { it["Id"].stringValue() == condSuccessFlowId }
        condSuccessFlow["FlowContent"]["P"][0]["T"][""].stringValue().shouldBeEqualTo("Name:")
        val labelVarId = condSuccessFlow["FlowContent"]["P"][1]["T"]["O"]["Id"].stringValue()
        val labelVarNode = result["Variable"].first { it["Id"].stringValue() == labelVarId }
        labelVarNode["Name"].stringValue().shouldBeEqualTo("Label Value")

        val bodyRow2Flow = result["Flow"].last { it["Id"].stringValue() == rowFlowRefs[3]["Id"].stringValue() }
        bodyRow2Flow["FlowContent"]["P"][0]["T"][""].stringValue().shouldBeEqualTo("Total")
        bodyRow2Flow["FlowContent"]["P"][1]["T"][""].stringValue().shouldBeEqualTo("N/A")
    }

    @Test
    fun `buildDocumentObject flattens table with header, repeated body row wrapped in display rule, and footer to flows`() {
        // given
        val ordersVar = mockVar(VariableBuilder("ordersVar").name("Orders").dataType(DataType.Array).build())
        val itemNameVar = mockVar(VariableBuilder("itemNameVar").name("Item Name").dataType(DataType.String).build())
        val variableStructure = mockVarStructure(
            VariableStructureBuilder("VS_1")
                .addVariable(ordersVar.id, "Data")
                .addVariable(itemNameVar.id, VariableRef(ordersVar.id), "Item")
                .build()
        )
        val repeatDisplayRule =
            mockRule(DisplayRuleBuilder("R_1").comparison { value("X").equals().value("Y") }.build())

        val block = mockObj(
            DocumentObjectBuilder("B_1", Block).table {
                action(TableAction.Flatten)
                addHeaderRow {
                    addCell { paragraph { text { string("Item #") } } }
                    addCell { paragraph { text { string("Amount") } } }
                }
                addRepeatedRow(VariableRef(ordersVar.id)) {
                    displayRuleRef(repeatDisplayRule)
                    addRow {
                        addCell { paragraph { text { string("Order:") } } }
                        addCell { paragraph { text { variableRef(itemNameVar.id) } } }
                    }
                }
                addRow {
                    addCell { paragraph { text { string("Summary") } } }
                    addCell { paragraph { text { string("Done") } } }
                }
            }.variableStructureRef(variableStructure.id).build()
        )

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        result.path("Table").isMissingNode.shouldBeEqualTo(true)

        val flowId = result["FlowArea"].last()["FlowId"].stringValue()
        val sectionFlow = result["Flow"].last { it["Id"].stringValue() == flowId }
        sectionFlow["SectionFlow"].stringValue().shouldBeEqualTo("True")
        val rowFlowRefs = sectionFlow["FlowContent"]["P"]["T"]["O"]
        rowFlowRefs.size().shouldBeEqualTo(3)

        val headerFlow = result["Flow"].last { it["Id"].stringValue() == rowFlowRefs[0]["Id"].stringValue() }
        headerFlow["FlowContent"]["P"][0]["T"][""].stringValue().shouldBeEqualTo("Item #")
        headerFlow["FlowContent"]["P"][1]["T"][""].stringValue().shouldBeEqualTo("Amount")

        val footerFlow = result["Flow"].last { it["Id"].stringValue() == rowFlowRefs[2]["Id"].stringValue() }
        footerFlow["FlowContent"]["P"][0]["T"][""].stringValue().shouldBeEqualTo("Summary")
        footerFlow["FlowContent"]["P"][1]["T"][""].stringValue().shouldBeEqualTo("Done")

        val condFlow = result["Flow"].last { it["Id"].stringValue() == rowFlowRefs[1]["Id"].stringValue() }
        condFlow["Type"].stringValue().shouldBeEqualTo("InlCond")
        condFlow["Condition"]["Value"].stringValue().shouldBeEqualTo("return (String('X')==String('Y'));")

        val repeatedFlowId = condFlow["Condition"][""].stringValue()
        val repeatedFlow = result["Flow"].last { it["Id"].stringValue() == repeatedFlowId }
        repeatedFlow["Type"].stringValue().shouldBeEqualTo("Repeated")
        val arrayVarId = repeatedFlow["Variable"].stringValue()
        val arrayVarNode = result["Variable"].last { it["Id"].stringValue() == arrayVarId }
        arrayVarNode["VarType"].stringValue().shouldBeEqualTo("Array")

        repeatedFlow["FlowContent"]["P"][0]["T"][""].stringValue().shouldBeEqualTo("Order:")
        val itemVarId = repeatedFlow["FlowContent"]["P"][1]["T"]["O"]["Id"].stringValue()
        val itemVarNode = result["Variable"].first { it["Id"].stringValue() == itemVarId }
        itemVarNode["Name"].stringValue().shouldBeEqualTo("Item")
    }

    @Test
    fun `buildDocumentObject flattens table with nested repeated rows each having a display rule`() {
        // given
        val ordersVar = mockVar(VariableBuilder("ordersVar").name("Orders").dataType(DataType.Array).build())
        val itemsVar = mockVar(VariableBuilder("itemsVar").name("Items").dataType(DataType.Array).build())
        val itemNameVar = mockVar(VariableBuilder("itemName").dataType(DataType.String).build())

        val variableStructure = mockVarStructure(
            VariableStructureBuilder("VS_nested").addVariable(ordersVar.id, "Data").addVariable(itemsVar.id, ordersVar)
                .addVariable(itemNameVar.id, itemsVar).build()
        )

        val outerRule = mockRule(DisplayRuleBuilder("R_outer").comparison { value("P").equals().value("P") }.build())
        val innerRule = mockRule(DisplayRuleBuilder("R_inner").comparison { value("Q").equals().value("Q") }.build())

        val block = mockObj(
            DocumentObjectBuilder("B_nested", Block).table {
                action(TableAction.Flatten)
                addRepeatedRow(ordersVar) {
                    displayRuleRef(outerRule)
                    addRepeatedRow(itemsVar) {
                        displayRuleRef(innerRule)
                        addRow {
                            addCell { paragraph { text { string("Product") } } }
                            addCell { paragraph { text { variableRef(itemNameVar) } } }
                        }
                    }
                }
            }.variableStructureRef(variableStructure.id).build()
        )

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        result.path("Table").isMissingNode.shouldBeEqualTo(true)

        val outerCondFlow = getFlowAreaContentFlow(result)
        outerCondFlow["Type"].stringValue().shouldBeEqualTo("InlCond")
        outerCondFlow["Condition"]["Value"].stringValue().shouldBeEqualTo("return (String('P')==String('P'));")

        val ordersRepeatedFlow = result["Flow"].last { it["Id"].stringValue() == outerCondFlow["Condition"][""].stringValue() }
        ordersRepeatedFlow["Type"].stringValue().shouldBeEqualTo("Repeated")
        ordersRepeatedFlow["SectionFlow"].stringValue().shouldBeEqualTo("True")

        val innerCondFlowId = ordersRepeatedFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()
        val innerCondFlow = result["Flow"].last { it["Id"].stringValue() == innerCondFlowId }
        innerCondFlow["Type"].stringValue().shouldBeEqualTo("InlCond")
        innerCondFlow["Condition"]["Value"].stringValue().shouldBeEqualTo("return (String('Q')==String('Q'));")

        val itemsRepeatedFlow = result["Flow"].last { it["Id"].stringValue() == innerCondFlow["Condition"][""].stringValue() }
        itemsRepeatedFlow["Type"].stringValue().shouldBeEqualTo("Repeated")
        itemsRepeatedFlow["FlowContent"]["P"][0]["T"][""].stringValue().shouldBeEqualTo("Product")
        val varId = itemsRepeatedFlow["FlowContent"]["P"][1]["T"]["O"]["Id"].stringValue()
        val varNode = result["Variable"].first { it["Id"].stringValue() == varId }
        varNode["Name"].stringValue().shouldBeEqualTo("itemName")
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

    private fun mockRule(displayRule: DisplayRule): DisplayRule {
        every { displayRuleRepository.findOrFail(displayRule.id) } returns displayRule
        return displayRule
    }

    private fun mockImage(image: com.quadient.migration.api.dto.migrationmodel.Image): com.quadient.migration.api.dto.migrationmodel.Image {
        every { imageRepository.findOrFail(image.id) } returns image
        every { imageRepository.find(image.id) } returns image
        return image
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
}
