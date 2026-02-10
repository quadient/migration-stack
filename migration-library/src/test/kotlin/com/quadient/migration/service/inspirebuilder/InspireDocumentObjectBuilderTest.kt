package com.quadient.migration.service.inspirebuilder

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.dto.migrationmodel.Attachment
import com.quadient.migration.api.dto.migrationmodel.AttachmentRef
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Hyperlink
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.TextStyle
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.api.repository.AttachmentRepository
import com.quadient.migration.api.repository.DisplayRuleRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ImageRepository
import com.quadient.migration.api.repository.ParagraphStyleRepository
import com.quadient.migration.api.repository.TextStyleRepository
import com.quadient.migration.api.repository.VariableRepository
import com.quadient.migration.api.repository.VariableStructureRepository
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.shared.BinOp
import com.quadient.migration.shared.Function
import com.quadient.migration.shared.Literal
import com.quadient.migration.shared.LiteralDataType
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.SkipOptions
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.getFlowAreaContentFlow
import com.quadient.migration.tools.model.*
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.wfdxml.api.layoutnodes.TextStyleInheritFlag
import com.quadient.wfdxml.internal.module.layout.LayoutImpl
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
        every { ipsService.fileExists(any()) } returns true
    }

    @Test
    fun `buildStyles creates ParagraphStyle with PdfTaggingRule Paragraph`() {
        // given
        val paragraphStyle = aParaStyle(
            "PS_Para",
            definition = aParaDef(
                leftIndent = Size.ofMillimeters(0),
                pdfTaggingRule = com.quadient.migration.shared.ParagraphPdfTaggingRule.Paragraph
            )
        )
        every { paragraphStyleRepository.listAll() } returns listOf(paragraphStyle)

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
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

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
            subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

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
        val attachment = aAttachment("Attachment_1", name = "document", sourcePath = "C:/attachments/document.pdf")
        every { attachmentRepository.findOrFail(attachment.id) } returns attachment
        val block = mockObj(
            aBlock("B_1", listOf(aParagraph(aText(listOf(StringValue("See attached: "), AttachmentRef(attachment.id))))))
        )

        // when
        val result =
            subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

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
        val attachment = aAttachment("Attachment_1", skip = SkipOptions(true, "Attachment not available", "Missing source"))
        every { attachmentRepository.findOrFail(attachment.id) } returns attachment
        val block = mockObj(
            aBlock("B_1", listOf(aParagraph(aText(listOf(AttachmentRef(attachment.id))))))
        )

        // when
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

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
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val flow = getFlowAreaContentFlow(result)
        flow["FlowContent"]["P"]["T"][""].textValue().shouldBeEqualTo("Text  more text")
    }

    private fun mockObj(documentObject: DocumentObject): DocumentObject {
        every { documentObjectRepository.findOrFail(documentObject.id) } returns documentObject
        return documentObject
    }

    private fun mockAttachment(attachment: Attachment): Attachment {
        every { attachmentRepository.findOrFail(attachment.id) } returns attachment
        return attachment
    }

    private fun mockTextStyle(textStyle: TextStyle): TextStyle {
        every { textStyleRepository.firstWithDefinition(textStyle.id) } returns textStyle
        val currentAllStyles = textStyleRepository.listAll()
        every { textStyleRepository.listAll() } returns currentAllStyles + textStyle
        return textStyle
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

    private fun DisplayRule.toScript(): String {
        return definition?.toScript(
            layout = LayoutImpl(),
            variableStructure = aVariableStructure("some struct"),
            findVar = { aVariable(it) }
        ) ?: error("No definition")
    }
}
