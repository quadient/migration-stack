package com.quadient.migration.service.inspirebuilder

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.quadient.migration.api.InspireOutput
import com.quadient.migration.data.DisplayRuleModel
import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.data.HyperlinkModel
import com.quadient.migration.data.StringModel
import com.quadient.migration.data.TextStyleModel
import com.quadient.migration.data.TextStyleModelRef
import com.quadient.migration.persistence.repository.*
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.shared.BinOp
import com.quadient.migration.shared.Function
import com.quadient.migration.shared.Literal
import com.quadient.migration.shared.LiteralDataType
import com.quadient.migration.shared.Size
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.model.*
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.wfdxml.api.layoutnodes.TextStyleInheritFlag
import com.quadient.wfdxml.internal.module.layout.LayoutImpl
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InspireDocumentObjectBuilderTest {
    private val documentObjectRepository = mockk<DocumentObjectInternalRepository>()
    private val textStyleRepository = mockk<TextStyleInternalRepository>()
    private val paragraphStyleRepository = mockk<ParagraphStyleInternalRepository>()
    private val variableRepository = mockk<VariableInternalRepository>()
    private val variableStructureRepository = mockk<VariableStructureInternalRepository>()
    private val displayRuleRepository = mockk<DisplayRuleInternalRepository>()
    private val imageRepository = mockk<ImageInternalRepository>()
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
        aProjectConfig(output = InspireOutput.Designer),
        ipsService,
    )

    @BeforeEach
    fun setUp() {
        every { variableStructureRepository.listAllModel() } returns emptyList()
        every { textStyleRepository.listAllModel() } returns emptyList()
        every { paragraphStyleRepository.listAllModel() } returns emptyList()
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
        every { paragraphStyleRepository.listAllModel() } returns listOf(paragraphStyle)

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
        val hyperlink = HyperlinkModel("https://www.example.com", "Click here", "Link to example website")
        val textContent = listOf(StringModel("Visit our site: "), hyperlink, StringModel(" for more info"))
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
                                StringModel("Some text before link. "),
                                HyperlinkModel("https://www.example.com", "Link text"),
                                StringModel(" Some text after link.")
                            ), TextStyleModelRef(textStyle.id)
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

    private fun mockObj(documentObject: DocumentObjectModel): DocumentObjectModel {
        every { documentObjectRepository.findModelOrFail(documentObject.id) } returns documentObject
        return documentObject
    }

    private fun mockTextStyle(textStyle: TextStyleModel): TextStyleModel {
        every { textStyleRepository.firstWithDefinitionModel(textStyle.id) } returns textStyle
        val currentAllStyles = textStyleRepository.listAllModel()
        every { textStyleRepository.listAllModel() } returns currentAllStyles + textStyle
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

    private fun DisplayRuleModel.toScript(): String {
        return definition?.toScript(
            layout = LayoutImpl(),
            variableStructure = aVariableStructureModel("some struct"),
            findVar = { aVariable(it) }
        ) ?: error("No definition")
    }
}
