package com.quadient.migration.service.inspirebuilder

import tools.jackson.dataformat.xml.XmlMapper
import tools.jackson.module.kotlin.KotlinModule
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.Tab
import com.quadient.migration.api.dto.migrationmodel.Tabs
import com.quadient.migration.api.dto.migrationmodel.TextStyle
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.api.dto.migrationmodel.builder.DisplayRuleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.EmailObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ImageBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.SmsObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.TextStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.VariableBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.VariableStructureBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.AreaBuilder
import com.quadient.migration.api.repository.AttachmentRepository
import com.quadient.migration.api.repository.DisplayRuleRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ImageRepository
import com.quadient.migration.api.repository.ParagraphStyleRepository
import com.quadient.migration.api.repository.TextStyleRepository
import com.quadient.migration.api.repository.VariableRepository
import com.quadient.migration.api.repository.VariableStructureRepository
import com.quadient.migration.service.InteractiveIcmDataCache
import com.quadient.migration.service.InteractiveResourcePathProvider
import com.quadient.migration.service.getBaseTemplateFullPath
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.BinOp.*
import com.quadient.migration.shared.ColumnDistribution
import com.quadient.migration.shared.DataType
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.DocumentObjectType.*
import com.quadient.migration.shared.GridAlignment
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.Literal
import com.quadient.migration.shared.LiteralDataType
import com.quadient.migration.shared.OnMobile
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.SkipOptions
import com.quadient.migration.shared.TabType
import com.quadient.migration.shared.VariablePathData
import com.quadient.migration.shared.centimeters
import com.quadient.migration.shared.millimeters
import com.quadient.migration.tools.model.aBlock
import com.quadient.migration.tools.model.aDisplayRule
import com.quadient.migration.tools.model.aParagraph
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.model.aDocObj
import com.quadient.migration.tools.model.aDocumentObjectRef
import com.quadient.migration.tools.model.aImage
import com.quadient.migration.tools.model.aSelectByLanguage
import com.quadient.migration.tools.model.aTemplate
import com.quadient.migration.tools.model.aText
import com.quadient.migration.tools.model.aTextDef
import com.quadient.migration.tools.model.aTextStyle
import com.quadient.migration.tools.model.aVariable
import com.quadient.migration.tools.model.aVariableStructure
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeNull
import com.quadient.migration.tools.shouldBeOfSize
import com.quadient.migration.tools.shouldNotBeEmpty
import com.quadient.migration.tools.shouldNotBeEqualTo
import com.quadient.migration.tools.shouldNotBeNull
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.collections.emptyList

class InteractiveDocumentObjectBuilderTest {
    val documentObjectRepository = mockk<DocumentObjectRepository>()
    val textStyleRepository = mockk<TextStyleRepository>()
    val paragraphStyleRepository = mockk<ParagraphStyleRepository>()
    val variableRepository = mockk<VariableRepository>()
    val variableStructureRepository = mockk<VariableStructureRepository>()
    val displayRuleRepository = mockk<DisplayRuleRepository>()
    val imageRepository = mockk<ImageRepository>()
    val attachmentRepository = mockk<AttachmentRepository>()
    val config = aProjectConfig()
    val ipsService = mockk<IpsService>()
    val resourcePathProvider = InteractiveResourcePathProvider(config)
    val icmDataCache = InteractiveIcmDataCache(ipsService, resourcePathProvider)

    private val subject = aSubject(config)

    private val xmlMapper = XmlMapper.builder().addModule(KotlinModule.Builder().build()).build()

    @BeforeEach
    fun setUp() {
        every { variableStructureRepository.listAll() } returns emptyList()
        every { ipsService.fileExists(any<IcmPath>()) } returns true
        every { ipsService.wfd2xml(any<IcmPath>()) } returns "<Workflow><Layout><Layout></Layout></Layout></Workflow>"
    }

    @Test
    fun `build of simple block with string and var ref that is not part of structure results in single flow with one paragraph and text`() {
        // given
        val variable = VariableBuilder("var1").name("varName1").dataType(DataType.String).build().mock()
        val block =
            DocumentObjectBuilder("1", Block).paragraph { text { string("some text").variableRef(variable) } }.build()

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val flowDefinitions = result["Flow"]
        flowDefinitions.size().shouldBeEqualTo(3)
        flowDefinitions[2]["FlowContent"]["P"]["T"][""].stringValue().shouldBeEqualTo("some text$${variable.name}$")
    }

    @Test
    fun `build of block with styled paragraph and text fetches both styles from db and uses them`() {
        // given
        val paraStyle = ParagraphStyleBuilder("paraStyle1")
            .definition { leftIndent(Size.ofMillimeters(100)) }
            .build()
            .mock()
        val textStyle = TextStyleBuilder("textStyle1")
            .definition { fontFamily("Arial") }
            .build()
            .mock()

        val block = DocumentObjectBuilder("1", Block).paragraph {
            styleRef(paraStyle).text {
                styleRef(textStyle).string("some text")
            }
        }.build()

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val flowDefinitions = result["Flow"]
        flowDefinitions.size().shouldBeEqualTo(3)
        val paragraph = flowDefinitions[2]["FlowContent"]["P"]
        paragraph["Id"].stringValue().shouldBeEqualTo("ParagraphStyles.${paraStyle.nameOrId()}")
        val text = paragraph["T"]
        text["Id"].stringValue().shouldBeEqualTo("TextStyles.${textStyle.nameOrId()}")
    }

    @Test
    fun `build of block where style definition does not exist uses style names as-is without failing`() {
        // given
        val paraStyle =
            ParagraphStyleBuilder("PS1").name("Heading Display").definition { alignment(Alignment.Left) }.build().mock()
        val textStyle =
            TextStyleBuilder("TS1").name("Body Display").definition { fontFamily("Arial") }.build().mock()

        val block = DocumentObjectBuilder("1", Block).paragraph {
            text { string("some text").styleRef(textStyle.id) }.styleRef(paraStyle.id)
        }.build()

        every { icmDataCache.fileExists(resourcePathProvider.getStyleDefinitionPath()) } returns false

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then - style names are used as-is since resolution is unavailable
        val paragraph = result["Flow"].last()["FlowContent"]["P"]
        paragraph["Id"].stringValue().shouldBeEqualTo("ParagraphStyles.${paraStyle.nameOrId()}")
        paragraph["T"]["Id"].stringValue().shouldBeEqualTo("TextStyles.${textStyle.nameOrId()}")
    }

    @Test
    fun `build of block where style names match display names in style definition resolves to internal style names`() {
        // given
        val paraStyle =
            ParagraphStyleBuilder("PS1").name("Heading Display").definition { alignment(Alignment.Left) }.build().mock()
        val textStyle =
            TextStyleBuilder("TS1").name("Body Display").definition { fontFamily("Arial") }.build().mock()

        val block = DocumentObjectBuilder("1", Block).paragraph {
            text { string("some text").styleRef(textStyle.id) }.styleRef(paraStyle.id)
        }.build()

        every { ipsService.wfd2xml(eq(resourcePathProvider.getStyleDefinitionPath())) } returns """
            <Workflow>
                <Layout>
                    <Layout>
                        <ParaStyle>
                            <Id>10</Id>
                            <Name>heading_internal</Name>
                            <CustomProperty>{&quot;DisplayName&quot;:&quot;Heading Display&quot;}</CustomProperty>
                        </ParaStyle>
                        <TextStyle>
                            <Id>20</Id>
                            <Name>body_internal</Name>
                            <CustomProperty>{&quot;DisplayName&quot;:&quot;Body Display&quot;}</CustomProperty>
                        </TextStyle>
                    </Layout>
                </Layout>
            </Workflow>
        """.trimIndent()

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val paragraph = result["Flow"].last()["FlowContent"]["P"]
        paragraph["Id"].stringValue().shouldBeEqualTo("ParagraphStyles.heading_internal")
        paragraph["T"]["Id"].stringValue().shouldBeEqualTo("TextStyles.body_internal")
    }

    @Test
    fun `build template with reference to block results in direct external flow`() {
        // given
        val block = DocumentObjectBuilder("1", Block).string("Hello").build().mock()
        val template = DocumentObjectBuilder("2", Template).documentObjectRef(block).build()

        // when
        val result = subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val flowDefinitions = result["Flow"]
        flowDefinitions.size().shouldBeEqualTo(3)

        val mainFlow = flowDefinitions[1]
        mainFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue().shouldBeEqualTo("SR_1")

        val directExternalFlow = flowDefinitions[2]
        directExternalFlow["Id"].stringValue().shouldBeEqualTo("SR_1")
        directExternalFlow["Type"].stringValue().shouldBeEqualTo("DirectExternal")
        directExternalFlow["ExternalLocation"].stringValue()
            .shouldBeEqualTo("icm://Interactive/${config.interactiveTenant}/Blocks/${block.nameOrId()}.jld")
    }

    @Test
    fun `build block with table has correctly promoted column widths and spans`() {
        // given
        val variable = VariableBuilder("clientName").dataType(DataType.String).build().mock()
        val block = DocumentObjectBuilder("1", Block).table {
            addRow {
                addCell {
                    paragraph {
                        text {
                            string("Hello ").variableRef(variable).string(" how are you?")
                        }
                    }
                }
                addCell { string("First row, second cell") }
            }
            addRow {
                addCell { string("Second row, first cell") }
                addCell { string("Second row, second cell").mergeLeft(true) }
            }
            addColumnWidth(150.millimeters(), 10.0)
            addColumnWidth(3.centimeters(), 1.0)
        }.build()

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        result["Table"].size().shouldBeEqualTo(2)
        result["RowSet"].size().shouldBeEqualTo(6)
        result["Cell"].size().shouldBeEqualTo(8)

        val table = result["Table"][1]
        table["DisplayAsImage"].stringValue().shouldBeEqualTo("False")
        val firstColumnWidth = table["ColumnWidths"][0]
        firstColumnWidth["MinWidth"].stringValue().shouldBeEqualTo("0.15")
        firstColumnWidth["PercentWidth"].stringValue().shouldBeEqualTo("10.0")
        val secondColumnWidth = table["ColumnWidths"][1]
        secondColumnWidth["MinWidth"].stringValue().shouldBeEqualTo("0.03")
        secondColumnWidth["PercentWidth"].stringValue().shouldBeEqualTo("1.0")

        val lastCell = result["Cell"][7]
        lastCell["SpanLeft"].stringValue().shouldBeEqualTo("True")
        lastCell["FlowToNextPage"].stringValue().shouldBeEqualTo("True")

        result["Flow"].size().shouldBeEqualTo(11)
    }

    @Test
    fun `build of more nested and complex structure is correctly wrapped into section if required`() {
        // given
        val externalBlock = DocumentObjectBuilder("1", Block).string("Hello").build().mock()
        val block = DocumentObjectBuilder("2", Block).string("Sir").internal(true).build().mock()

        val section = DocumentObjectBuilder("5", Section).documentObjectRef(externalBlock).string("In between")
            .documentObjectRef(block).internal(true).build().mock()
        val template = DocumentObjectBuilder("10", Template).documentObjectRef(section).build()

        // when
        val result = subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val mainFlow = result["Flow"].first { it["Id"].stringValue() == "Def.MainFlow" }
        val sectionFlowRef = mainFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()

        result["Flow"].first { it["Id"].stringValue() == sectionFlowRef }["Name"].stringValue()
            .shouldBeEqualTo(section.nameOrId())
        val sectionFlow = result["Flow"].last { it["Id"].stringValue() == sectionFlowRef }
        sectionFlow["SectionFlow"].stringValue().shouldBeEqualTo("True")
        val sectionRefs = sectionFlow["FlowContent"]["P"]["T"]["O"]
        sectionRefs.size().shouldBeEqualTo(3)

        val nestedExternalFlowRef = sectionRefs[0]["Id"].stringValue()
        result["Flow"].first { it["Id"].stringValue() == nestedExternalFlowRef }["Name"].stringValue()
            .shouldBeEqualTo(externalBlock.nameOrId())
        val nestedExternalFlow = result["Flow"].last { it["Id"].stringValue() == nestedExternalFlowRef }
        nestedExternalFlow["Type"].stringValue().shouldBeEqualTo("DirectExternal")

        val inBetweenFlowRef = sectionRefs[1]["Id"].stringValue()
        result["Flow"].first { it["Id"].stringValue() == inBetweenFlowRef }["Name"].stringValue()
            .shouldBeEqualTo("${section.nameOrId()} 1")
        val inBetweenFlow = result["Flow"].last { it["Id"].stringValue() == inBetweenFlowRef }
        inBetweenFlow["FlowContent"]["P"]["T"][""].stringValue().shouldBeEqualTo("In between")

        val nestedFlowRef = sectionRefs[2]["Id"].stringValue()
        result["Flow"].first { it["Id"].stringValue() == nestedFlowRef }["Name"].stringValue()
            .shouldBeEqualTo(block.nameOrId())
        val nestedFlow = result["Flow"].last { it["Id"].stringValue() == nestedFlowRef }
        nestedFlow["FlowContent"]["P"]["T"][""].stringValue().shouldBeEqualTo("Sir")
    }

    @Test
    fun `build block with variable refs in variable structure with converted value based on type`() {
        // given
        val longVar =
            VariableBuilder("var1").name("varName1").dataType(DataType.Integer64).defaultValue("2025").build().mock()
        val currencyVar =
            VariableBuilder("var2").name("varName2").dataType(DataType.Currency).defaultValue("249.99").build().mock()
        val boolVar =
            VariableBuilder("var3").name("varName3").dataType(DataType.Boolean).defaultValue("TRUE").build().mock()
        val variableStructure = VariableStructureBuilder("vs1").addVariable(longVar.id, "Data.Clients.Value")
            .addVariable(currencyVar.id, "Data.Clients.Value", "Money").addVariable(boolVar.id, "Data.Clients.Value")
            .build().mock()

        val config = aProjectConfig(defaultVariableStructure = variableStructure.id)

        val block = DocumentObjectBuilder("1", Block).paragraph {
            text {
                variableRef(longVar)
                variableRef(currencyVar)
                variableRef(boolVar)
            }
        }.build()

        // when
        val subject = aSubject(config)
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val variableDefinitions = result["Variable"]
        variableDefinitions.size().shouldBeEqualTo(73)
        val longVarForward = variableDefinitions.first { it["Name"]?.stringValue() == "varName1" }

        longVarForward["Name"].stringValue().shouldBeEqualTo(longVar.name)
        longVarForward["ParentId"].stringValue().shouldBeEqualTo("Data.Clients.Value")
        longVarForward["Forward"]["useExisting"].stringValue().shouldBeEqualTo("True")

        val longVarContent = variableDefinitions.last { it["Id"].stringValue() == longVarForward["Id"].stringValue() }
        longVarContent["Type"].stringValue().shouldBeEqualTo("Disconnected")
        longVarContent["VarType"].stringValue().shouldBeEqualTo("Int64")
        longVarContent["Content"].stringValue().shouldBeEqualTo("2025")

        val currencyVarId = variableDefinitions.first { it["Name"]?.stringValue() == "Money" }["Id"].stringValue()
        variableDefinitions.last { it["Id"].stringValue() == currencyVarId }["Content"].stringValue()
            .shouldBeEqualTo("249.99")

        val boolVarId = variableDefinitions.first { it["Name"]?.stringValue() == "varName3" }["Id"].stringValue()
        variableDefinitions.last { it["Id"].stringValue() == boolVarId }["Content"].stringValue().shouldBeEqualTo("True")

        val clientsVar = variableDefinitions.first { it["Name"]?.stringValue() == "Clients" }
        clientsVar["ParentId"].stringValue().shouldBeEqualTo("Def.Data")
        variableDefinitions.first { it["Name"]?.stringValue() == "Value" }["ParentId"].stringValue()
            .shouldBeEqualTo(clientsVar["Id"].stringValue())
        variableDefinitions.first { it["Name"]?.stringValue() == "Count" }["ParentId"].stringValue()
            .shouldBeEqualTo(clientsVar["Id"].stringValue())

        result["Data"]["RepeatedBy"].stringValue().shouldBeEqualTo("Data.Clients")
    }

    @Test
    fun `build template with external block using display rule wraps the block in condition flow`() {
        // given
        val displayRule = DisplayRuleBuilder("R_1").comparison { value("A").equals().value("B") }.build().mock()

        val block = DocumentObjectBuilder("1", Block).string("Hello").build().mock()
        val template = DocumentObjectBuilder("2", Template).documentObjectRef(block, displayRule).build()

        // when
        val result = subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val mainFlow = result["Flow"].first { it["Id"].stringValue() == "Def.MainFlow" }
        val conditionFlowRef = mainFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()

        val conditionFlow = result["Flow"].last { it["Id"].stringValue() == conditionFlowRef }
        conditionFlow["Type"].stringValue().shouldBeEqualTo("Condition")
        val condition = conditionFlow["Condition"]
        val successFlowRef = condition[""].stringValue()
        val conditionVarRef = condition["VarId"].stringValue()

        val conditionVarForwardRef = result["Variable"].first { it["Id"].stringValue() == conditionVarRef }
        conditionVarForwardRef["ParentId"].stringValue().shouldBeEqualTo(conditionFlowRef)
        conditionVarForwardRef["Name"].stringValue().shouldBeEqualTo("cond_R_1")

        val conditionVar = result["Variable"].last { it["Id"].stringValue() == conditionVarRef }
        conditionVar["VarType"].stringValue().shouldBeEqualTo("Bool")
        conditionVar["Type"].stringValue().shouldBeEqualTo("Calculated")
        conditionVar["Script"].stringValue().shouldBeEqualTo("return (String('A')==String('B'));")

        val successFlow = result["Flow"].last { it["Id"].stringValue() == successFlowRef }
        successFlow["Type"].stringValue().shouldBeEqualTo("DirectExternal")
    }

    @Test
    fun `build block with styled paragraph and styled text under display rule wraps the text in inline condition flow`() {
        // given
        val variable = VariableBuilder("V1").name("ClientName").dataType(DataType.String).build().mock()
        val variableStructure = VariableStructureBuilder("VS1")
            .addVariable(variable.id, "Data.Clients", "Client Name")
            .build()
            .mock()

        val paraStyle = ParagraphStyleBuilder("P1")
            .definition { leftIndent(Size.ofMillimeters(10)) }
            .build()
            .mock()
        val textStyle = TextStyleBuilder("T1").definition { bold(true) }.build().mock()
        val displayRule = DisplayRuleBuilder("R1").comparison { variable(variable.id).equals().value("Jon") }.build()

        val block = DocumentObjectBuilder("1", Block).paragraph {
            text { string("Hello There").styleRef(textStyle.id).displayRuleRef(displayRule.id) }.styleRef(paraStyle.id)
        }.build()
        val config = aProjectConfig(defaultVariableStructure = variableStructure.id)

        every { displayRuleRepository.findOrFail(displayRule.id) } returns displayRule

        // when
        val subject = aSubject(config)
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        result["Variable"].first { it["ParentId"]?.stringValue() == "Data.Clients" }["Name"].stringValue()
            .shouldBeEqualTo("Client Name")

        val mainFlow = result["Flow"].first { it["Id"].stringValue() == "Def.MainFlow" }
        val topSimpleFlowRef = mainFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()

        val topSimpleFlow = result["Flow"].last { it["Id"].stringValue() == topSimpleFlowRef }
        topSimpleFlow["Type"].stringValue().shouldBeEqualTo("Simple")
        val topFlowParagraph = topSimpleFlow["FlowContent"]["P"]
        topFlowParagraph["Id"].stringValue().shouldBeEqualTo("ParagraphStyles.${paraStyle.nameOrId()}")
        val topFlowText = topFlowParagraph["T"]
        Assertions.assertNull(topFlowText["Id"])
        val conditionFlowRef = topFlowText["O"]["Id"].stringValue()

        val conditionFlow = result["Flow"].last { it["Id"].stringValue() == conditionFlowRef }
        conditionFlow["Type"].stringValue().shouldBeEqualTo("InlCond")
        val condition = conditionFlow["Condition"]
        condition["Value"].stringValue().shouldBeEqualTo("return (DATA.Clients.Client_Name==String('Jon'));")
        val finalFlowRef = condition[""].stringValue()

        val finalFlow = result["Flow"].last { it["Id"].stringValue() == finalFlowRef }
        finalFlow["Type"].stringValue().shouldBeEqualTo("Simple")
        val finalFlowPara = finalFlow["FlowContent"]["P"]
        finalFlowPara["Id"].stringValue().shouldBeEqualTo("ParagraphStyles.${paraStyle.nameOrId()}")
        val finalFlowText = finalFlowPara["T"]
        finalFlowText["Id"].stringValue().shouldBeEqualTo("TextStyles.${textStyle.nameOrId()}")
        finalFlowText[""].stringValue().shouldBeEqualTo("Hello There")

        result["Data"]["RepeatedBy"].shouldBeNull()
    }

    @Test
    fun `build block with styled paragraph under display rule and one of its texts under display rule wraps it in multiple condition flows`() {
        // given
        val variable = VariableBuilder("V1").name("900_MailCount").dataType(DataType.String).build().mock()
        val variableStructure = VariableStructureBuilder("VS1").addVariable(variable.id, "Data.1.Value").build().mock()
        val config = aProjectConfig(defaultVariableStructure = variableStructure.id)

        val textStyle = TextStyleBuilder("T1").definition { bold(true) }.build().mock()
        val paraStyle = ParagraphStyleBuilder("P1")
            .definition { leftIndent(Size.ofMillimeters(10)) }
            .build()
            .mock()

        val paraDisplayRule =
            DisplayRuleBuilder("R_para").comparison { variable(variable).greaterOrEqualThan().value(540.0) }.build()
                .mock()
        val textDisplayRule =
            DisplayRuleBuilder("R_text").comparison { value("A").notEquals().value("B") }.build().mock()

        val block = DocumentObjectBuilder("1", Block).paragraph {
            text { string("This is") }
            text {
                string("Preposterous!").styleRef(textStyle).displayRuleRef(textDisplayRule)
            }.styleRef(paraStyle).displayRuleRef(paraDisplayRule)
        }.build().mock()

        // when
        val subject = aSubject(config)
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val mainFlow = result["Flow"].first { it["Id"].stringValue() == "Def.MainFlow" }
        val topSimpleFlowRef = mainFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()

        val topSimpleFlow = result["Flow"].last { it["Id"].stringValue() == topSimpleFlowRef }
        topSimpleFlow["Type"].stringValue().shouldBeEqualTo("Simple")
        val topFlowParagraph = topSimpleFlow["FlowContent"]["P"]
        Assertions.assertNull(topFlowParagraph["Id"])
        val conditionFlowRef = topFlowParagraph["T"]["O"]["Id"].stringValue()

        val conditionFlow = result["Flow"].last { it["Id"].stringValue() == conditionFlowRef }
        conditionFlow["Type"].stringValue().shouldBeEqualTo("InlCond")
        val condition = conditionFlow["Condition"]
        condition["Value"].stringValue().shouldBeEqualTo("return (DATA._1.Current._900_MailCount>=540.0);")
        val paraFlowRef = condition[""].stringValue()

        val paraFlow = result["Flow"].last { it["Id"].stringValue() == paraFlowRef }
        val paraFlowPara = paraFlow["FlowContent"]["P"]
        paraFlowPara["Id"].stringValue().shouldBeEqualTo("ParagraphStyles.${paraStyle.nameOrId()}")
        paraFlowPara["T"][0][""].stringValue().shouldBeEqualTo("This is")
        val secondText = paraFlowPara["T"][1]
        Assertions.assertNull(secondText["Id"])
        val textConditionFlowRef = secondText["O"]["Id"].stringValue()

        val textConditionFlow = result["Flow"].last { it["Id"].stringValue() == textConditionFlowRef }
        textConditionFlow["Type"].stringValue().shouldBeEqualTo("InlCond")
        val textCondition = textConditionFlow["Condition"]
        textCondition["Value"].stringValue().shouldBeEqualTo("return (String('A')!=String('B'));")
        val textFlowRef = textCondition[""].stringValue()

        val textFlow = result["Flow"].last { it["Id"].stringValue() == textFlowRef }
        val textFlowPara = textFlow["FlowContent"]["P"]
        textFlowPara["Id"].stringValue().shouldBeEqualTo("ParagraphStyles.${paraStyle.nameOrId()}")
        val textFlowText = textFlowPara["T"]
        textFlowText["Id"].stringValue().shouldBeEqualTo("TextStyles.${textStyle.nameOrId()}")
        textFlowText[""].stringValue().shouldBeEqualTo("Preposterous!")
    }

    @Test
    fun `build block with table that has one row under display rule`() {
        // given
        val displayRule = DisplayRuleBuilder("R_1").comparison {
            value(true).equals().value(false)
        }.build().mock()

        val block = DocumentObjectBuilder("1", Block).table {
            addRow {
                addCell { string("First row, first cell") }
                addCell { string("First row, second cell") }
            }
            addRow {
                addCell { string("Second row, first cell") }
                addCell { string("Second row, second cell") }
                displayRuleRef(displayRule)
            }
        }.build().mock()

        val config = aProjectConfig(defaultVariableStructure = null)
        val subject = aSubject(config)

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val conditionalRow = result["RowSet"].last { it["RowSetType"]?.stringValue() == "Condition" }
        val conditionalRowId = conditionalRow["Id"].stringValue()
        val rowCondition = conditionalRow["RowSetCondition"][0]
        val subRowRef = rowCondition["SubRowId"].stringValue()
        val conditionVarRef = rowCondition["VariableId"].stringValue()

        val conditionVarForwardRef = result["Variable"].first { it["Id"].stringValue() == conditionVarRef }
        conditionVarForwardRef["ParentId"].stringValue().shouldBeEqualTo(conditionalRowId)
        conditionVarForwardRef["Name"].stringValue().shouldBeEqualTo("cond_R_1")

        val conditionVar = result["Variable"].last { it["Id"].stringValue() == conditionVarRef }
        conditionVar["Type"].stringValue().shouldBeEqualTo("Calculated")
        conditionVar["VarType"].stringValue().shouldBeEqualTo("Bool")
        conditionVar["Script"].stringValue().shouldBeEqualTo("return (true==false);")

        val subRow = result["RowSet"].last { it["Id"].stringValue() == subRowRef }
        subRow["RowSetType"].stringValue().shouldBeEqualTo("Row")
    }

    @Test
    fun `build of template skips unsupported block and template is created without it`() {
        // given
        val unsupportedBlock = DocumentObjectBuilder("1", Block).skip().build().mock()
        val template = DocumentObjectBuilder("10", Template).documentObjectRef(unsupportedBlock).build()

        // when
        val result = subject.buildDocumentObject(template)
        val wfdXml = xmlMapper.readTree(result.trimIndent())

        // then
        val mainFlowText = wfdXml["Flow"]["FlowContent"]["P"]["T"]
        assert(mainFlowText != null)
        Assertions.assertNull(mainFlowText["O"])
    }

    @Test
    fun `build block with variable ref that is not found throws exception`() {
        // given
        val block = DocumentObjectBuilder("1", Block).variableRef("var1").build()
        every { variableRepository.findOrFail("var1") } throws IllegalStateException("Record 'var1' not found.")

        // when
        val result = assertThrows<IllegalStateException> { subject.buildDocumentObject(block) }

        // then
        result.message.shouldBeEqualTo("Record 'var1' not found.")
    }

    @Test
    fun `build template with block using display rule with unmapped variable creates dummy display rule`() {
        // given
        val variable = VariableBuilder("V_999").name("DollarsInBank").dataType(DataType.Currency).build().mock()

        val displayRule = DisplayRuleBuilder("R_1").group {
            comparison { variable(variable).greaterOrEqualThan().value(25000.0) }.negate()
        }.build().mock()

        val block = DocumentObjectBuilder("1", Block).string("Hello").build().mock()
        val template = DocumentObjectBuilder("2", Template).documentObjectRef(block, displayRule).build()

        // when
        val result = subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val conditionVariable = result["Variable"].last { it["Type"]?.stringValue() == "Calculated" }
        conditionVariable["Script"].stringValue()
            .shouldBeEqualTo("return not (String('DollarsInBank>=25000.0')==String('unmapped'));")
    }

    @Test
    fun `build block with paragraph containing strings and document object refs inside one paragraph text`() {
        // given
        val variable = VariableBuilder("V_100").name("Salutation").dataType(DataType.String).build().mock()

        val innerBlock = DocumentObjectBuilder("1", Block).paragraph {
            text { string("Dear ").variableRef(variable) }
        }.internal(true).build().mock()

        val externalBlock = DocumentObjectBuilder("2", Block).string("This is a good day!").build().mock()

        val block = DocumentObjectBuilder("10", Block).paragraph {
            text {
                string("Hello, ").documentObjectRef(innerBlock).string(". How are you?")
                    .documentObjectRef(externalBlock)
            }
        }.build()

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val mainFlow = result["Flow"].first { it["Id"].stringValue() == "Def.MainFlow" }
        val blockFlowRef = mainFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()

        val sectionFlow = result["Flow"].last { it["Id"].stringValue() == blockFlowRef }
        val sectionTextContent = sectionFlow["FlowContent"]["P"]["T"]
        sectionTextContent[""].size().shouldBeEqualTo(2)
        sectionTextContent["O"].size().shouldBeEqualTo(2)
    }

    @Test
    fun `build block with image`() {
        // given
        val image = ImageBuilder("Dog").name("Image_Dog").sourcePath("somePath").imageType(ImageType.Jpeg)
            .options(ImageOptions(Size.ofPoints(120), Size.ofPoints(90))).build().mock()
        val block = DocumentObjectBuilder("1", Block).imageRef(image).build()

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        result["Image"].first()["Name"].stringValue().shouldBeEqualTo("Image_Dog")
        val imageContent = result["Image"].last()
        imageContent["ImageLocation"].stringValue()
            .shouldBeEqualTo("VCSLocation,icm://Interactive/${config.interactiveTenant}/Resources/Images/${image.name}.jpg")
        imageContent["UseResizeWidth"].stringValue().shouldBeEqualTo("True")
        imageContent["UseResizeHeight"].stringValue().shouldBeEqualTo("True")
        imageContent["ResizeImageWidth"].stringValue().shouldBeEqualTo(image.options?.resizeWidth?.toMeters().toString())
        imageContent["ResizeImageHeight"].stringValue()
            .shouldBeEqualTo(image.options?.resizeHeight?.toMeters().toString())
    }

    @Test
    fun `build block with image uses alternateText from Image`() {
        // given
        val image =
            ImageBuilder("Dog").name("Image_Dog").sourcePath("somePath").alternateText("A cute dog picture").build()
                .mock()
        val block = DocumentObjectBuilder("1", Block).imageRef(image).build()

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        result["Image"].first()["Name"].stringValue().shouldBeEqualTo("Image_Dog")
        val imageContent = result["Image"].last()
        val varId = imageContent["AlternativeTextVar"].stringValue()
        varId.shouldNotBeNull()

        val tagging = imageContent["PDFAdvanced"]["Tagging"]
        tagging["AlternateTextType"].stringValue().shouldBeEqualTo("2")
        tagging["AlternateTextNodeId"].stringValue().shouldBeEqualTo(varId)
        tagging["AlternateText"].stringValue().shouldBeEqualTo("")
        tagging["Rule"].stringValue().shouldBeEqualTo("Figure")

        val varData = result["Variable"].first { it["Id"].stringValue() == varId }
        varData["Name"].stringValue().shouldBeEqualTo("Alternate text variable for Image_Dog")
        varData["CustomProperty"].stringValue().shouldBeEqualTo("{\"ValueWrapperVariable\":true}")

        val varContent = result["Variable"].last { it["Id"].stringValue() == varId }
        varContent["Type"].stringValue().shouldBeEqualTo("Calculated")
        varContent["Script"].stringValue().shouldBeEqualTo("return 'A cute dog picture';")

        result["Root"]["LockedWebNodes"]["LockedWebNode"].stringValue().shouldBeEqualTo(varId)
    }

    @Test
    fun `build template with two blocks referencing same image results in single image definition`() {
        // given
        val image = aImage("Dog").mock()
        val block1 = aBlock("1", listOf(ImageRef(image.id)), internal = true).mock()
        val block2 = aBlock("2", listOf(ImageRef(image.id)), internal = true).mock()

        val template = aTemplate("3", listOf(aDocumentObjectRef(block1.id), aDocumentObjectRef(block2.id)))

        // when
        val result = subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        result["Image"].filter { it["Name"]?.stringValue() == image.nameOrId() }.size.shouldBeEqualTo(1)
    }

    @Test
    fun `build block with unknown image and image with missing source path renders placeholder texts instead`() {
        // given
        val catImage =
            aImage("Cat", imageType = ImageType.Unknown, skip = SkipOptions(true, "Cat placeholder", null)).mock()
        val dogImage = aImage("Dog", sourcePath = "", skip = SkipOptions(true, "Dog placeholder", null)).mock()

        val block = aBlock(
            "1", listOf(ImageRef(catImage.id), aParagraph(aText(ImageRef(dogImage.id))))
        )

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        assert(result["Image"] == null)
        val blockFlow = result["Flow"].last { it["Id"].stringValue() != "Def.MainFlow" }
        val paragraphs = blockFlow["FlowContent"]["P"]
        paragraphs.size().shouldBeEqualTo(2)
        paragraphs[0]["T"][""].stringValue().shouldBeEqualTo("Cat placeholder")
        paragraphs[1]["T"][""].stringValue().shouldBeEqualTo("Dog placeholder")
    }

    @Test
    fun `paragraph with first match is built to inline condition flow with multiple options`() {
        // given
        val rule1 = DisplayRuleBuilder("R_1").comparison { value("A").equals().value("B") }.build().mock()
        val rule2 = DisplayRuleBuilder("R_2").comparison { value("C").equals().value("C") }.build().mock()

        val block = DocumentObjectBuilder(
            "B_1", Block
        ).paragraph {
            text {
                string("Hello, ").firstMatch {
                    addCase().displayRuleRef(rule1).string("Mike")
                    addCase().displayRuleRef(rule2).string("Jon")
                }.string(", how are you?")
            }
        }.build()

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val mainFlow = result["Flow"].first { it["Id"].stringValue() == "Def.MainFlow" }
        val contentFlow =
            result["Flow"].last { it["Id"].stringValue() == mainFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue() }
        val contentFlowText = contentFlow["FlowContent"]["P"]["T"]

        contentFlowText[""][0].stringValue().shouldBeEqualTo("Hello, ")
        contentFlowText[""][1].stringValue().shouldBeEqualTo(", how are you?")

        val firstMatch = result["Flow"].last { it["Id"].stringValue() == contentFlowText["O"]["Id"].stringValue() }
        firstMatch["Type"].stringValue().shouldBeEqualTo("InlCond")
        firstMatch["Default"].stringValue().shouldBeEqualTo("")
        val conditions = firstMatch["Condition"]
        conditions.size().shouldBeEqualTo(2)

        conditions[0]["Value"].stringValue().shouldBeEqualTo("return (String('A')==String('B'));")
        val firstConditionFlowId = conditions[0][""].stringValue()

        result["Flow"].last { it["Id"].stringValue() == firstConditionFlowId }["FlowContent"]["P"]["T"][""].stringValue()
            .shouldBeEqualTo("Mike")

        conditions[1]["Value"].stringValue().shouldBeEqualTo("return (String('C')==String('C'));")
        val secondConditionFlowId = conditions[1][""].stringValue()

        result["Flow"].last { it["Id"].stringValue() == secondConditionFlowId }["FlowContent"]["P"]["T"][""].stringValue()
            .shouldBeEqualTo("Jon")
    }

    @Test
    fun `build of flow under display rule wraps in in condition flow`() {
        // given
        val rule = DisplayRuleBuilder("R_1").comparison { value("C").equals().value("C") }.build().mock()
        val innerBlock =
            DocumentObjectBuilder("B_1", Block).string("Text").internal(true).displayRuleRef(rule).build().mock()
        val template = DocumentObjectBuilder("T_1", Template).documentObjectRef(innerBlock).build()

        // when
        val result = subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val mainFlow = result["Flow"].first { it["Id"].stringValue() == "Def.MainFlow" }
        val condFlowId = mainFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()

        val condFlow = result["Flow"].last { it["Id"].stringValue() == condFlowId }
        condFlow["Type"].stringValue().shouldBeEqualTo("Condition")

        val innerFlow = result["Flow"].last { it["Id"].stringValue() == condFlow["Condition"][""].stringValue() }
        innerFlow["FlowContent"]["P"]["T"][""].stringValue().shouldBeEqualTo("Text")
    }

    @Test
    fun `variable string content is wrapped in paragraph`() {
        val subject = aSubject(aProjectConfig(defaultVariableStructure = "VS_1"))

        val variable1 = VariableBuilder("V_1").dataType(DataType.String).build().mock()
        val variable2 = VariableBuilder("V_2").dataType(DataType.String).build().mock()
        val variable3 = VariableBuilder("V_3").dataType(DataType.String).build().mock()
        VariableStructureBuilder("VS_1")
            .addVariable(variable1.id, "Data", "V_1")
            .addVariable(variable2.id, "Data", "V_2")
            .addVariable(variable3.id, "Data", "V_3")
            .build()
            .mock()
        val docObj = DocumentObjectBuilder("B_1", Block)
            .paragraph { string("Paragraph") }
            .string("Hello World")
            .variableRef(variable1)
            .variableRef(variable2)
            .string("Screw this World")
            .variableRef(variable3)
            .paragraph { string("Paragraph 2") }
            .string("Goodbye World")
            .build()
            .mock()

        val result = subject.buildDocumentObject(docObj).let { xmlMapper.readTree(it.trimIndent()) }

        val flowId = result["Flow"].first { it["Id"].stringValue() == "Def.MainFlow" }["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()
        val flow = result["Flow"].last { it["Id"].stringValue() == flowId }

        val contents = flow["FlowContent"]["P"].toList()
        contents[0]["T"][""].stringValue().shouldBeEqualTo("Paragraph")
        contents[1]["T"].toList()[0][""].stringValue().shouldBeEqualTo("Hello World")
        val v1 = contents[1]["T"].toList()[1]["O"]["Id"].stringValue()
        val v2 = contents[1]["T"].toList()[2]["O"]["Id"].stringValue()
        contents[1]["T"].toList()[3][""].stringValue().shouldBeEqualTo("Screw this World")
        val v3 = contents[1]["T"].toList()[4]["O"]["Id"].stringValue()
        contents[2]["T"][""].stringValue().shouldBeEqualTo("Paragraph 2")
        contents[3]["T"][""].stringValue().shouldBeEqualTo("Goodbye World")

        result["Variable"].first { it["Id"].stringValue() == v1 }["Name"].stringValue().shouldBeEqualTo("V_1")
        result["Variable"].first { it["Id"].stringValue() == v2 }["Name"].stringValue().shouldBeEqualTo("V_2")
        result["Variable"].first { it["Id"].stringValue() == v3 }["Name"].stringValue().shouldBeEqualTo("V_3")
    }

    @Test
    fun `external block with multiple flows under display rule is built to section flow wrapped in condition flow`() {
        // given
        val rule = DisplayRuleBuilder("R_1").comparison { value("C").equals().value("C") }.build().mock()
        val innerBlock = DocumentObjectBuilder("B_2", Block).string("Inner Text").build().mock()
        val block = DocumentObjectBuilder("B_1", Block).documentObjectRef(innerBlock).string("Text").internal(false)
            .displayRuleRef(rule).build()

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val mainFlow = result["Flow"].first { it["Id"].stringValue() == "Def.MainFlow" }
        val condFlow =
            result["Flow"].last { it["Id"].stringValue() == mainFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue() }
        condFlow["Type"].stringValue().shouldBeEqualTo("Condition")

        val sectionFlow = result["Flow"].last { it["Id"].stringValue() == condFlow["Condition"][""].stringValue() }
        sectionFlow["SectionFlow"].stringValue().shouldBeEqualTo("True")
        val sectionFlowRefs = sectionFlow["FlowContent"]["P"]["T"]["O"]
        sectionFlowRefs.size().shouldBeEqualTo(2)
    }

    @ParameterizedTest
    @CsvSource("Page", "Template")
    fun `build template interactive flow areas inline correctly`(contentType: DocumentObjectType) {
        val areaContent = listOf<DocumentContent>(
            AreaBuilder().paragraph { string("interactive flow text 1") }.interactiveFlowName("Logo").build(),
            AreaBuilder().paragraph { string("main flow text 1") }.build(),
            ParagraphBuilder().string("main flow text 2").build(),
            AreaBuilder().paragraph { string("interactive flow text 2") }.interactiveFlowName("Def.InteractiveFlow1").build(),
            AreaBuilder().paragraph { string("main flow text 3") }.interactiveFlowName("Def.MainFlow").build(),
            AreaBuilder().paragraph { string("interactive flow text 3") }.interactiveFlowName("Flow BT 1").build()
        )

        val template = if (contentType == Page) {
            val page = DocumentObjectBuilder("P_1", Page).internal(true).content(areaContent).build().mock()
            DocumentObjectBuilder("T_1", Template).documentObjectRef(page).build()
        } else {
            DocumentObjectBuilder("T_1", Template).content(areaContent).build()
        }

        every {
            ipsService.wfd2xml(getBaseTemplateFullPath(config, null))
        } returns """<Workflow>
            <Layout>
                <Layout>
                    <Flow>
                        <Id>79</Id>
                        <Name>Letter Content</Name>
                    </Flow>
                    <Flow>
                        <Id>80</Id>
                        <Name>Flow BT 1</Name>
                        <CustomProperty>{&quot;customName&quot;:&quot;Logo&quot;}</CustomProperty>
                    </Flow>
                    <Flow>
                        <Id>81</Id>
                        <Name>Flow BT 2</Name>
                        <CustomProperty>{&quot;customName&quot;:&quot;Address Block&quot;}</CustomProperty>
                    </Flow>
                    <Pages>
                        <InteractiveFlow>
                            <FlowId>79</FlowId>
                            <FlowType>Normal</FlowType>
                        </InteractiveFlow>
                        <InteractiveFlow>
                            <FlowId>80</FlowId>
                            <FlowType>Normal</FlowType>
                        </InteractiveFlow>
                        <InteractiveFlow>
                            <FlowId>81</FlowId>
                            <FlowType>Normal</FlowType>
                        </InteractiveFlow>
                    </Pages>
                </Layout>
            </Layout>
        </Workflow>""".trimMargin()

        // when
        val result = subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }

        val mainFlow = result["Flow"].first { it["Id"].stringValue() == "Def.MainFlow" }
        val mainFlowContentFlowId = mainFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()
        val mainFlowContentFlow = result["Flow"].last { it["Id"].stringValue() == mainFlowContentFlowId }
        val mainFlowParagraphs = mainFlowContentFlow["FlowContent"]["P"]
        mainFlowParagraphs.size().shouldBeEqualTo(3)
        mainFlowParagraphs[0]["T"][""].stringValue().shouldBeEqualTo("main flow text 1")
        mainFlowParagraphs[1]["T"][""].stringValue().shouldBeEqualTo("main flow text 2")
        mainFlowParagraphs[2]["T"][""].stringValue().shouldBeEqualTo("main flow text 3")
        result["Flow"].first { it["Id"].stringValue() == mainFlowContentFlowId }["Name"].stringValue()
            .shouldBeEqualTo("T_1")

        val interactiveFlow = result["Flow"].first { it["Id"].stringValue() == "Def.InteractiveFlow1" }
        val interactiveFlowContentFlowId = interactiveFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()
        val interactiveFlowContentFlow = result["Flow"].last { it["Id"].stringValue() == interactiveFlowContentFlowId }
        val interactiveFlowParagraphs = interactiveFlowContentFlow["FlowContent"]["P"]
        interactiveFlowParagraphs.size().shouldBeEqualTo(3)
        interactiveFlowParagraphs[0]["T"][""].stringValue().shouldBeEqualTo("interactive flow text 1")
        interactiveFlowParagraphs[1]["T"][""].stringValue().shouldBeEqualTo("interactive flow text 2")
        interactiveFlowParagraphs[2]["T"][""].stringValue().shouldBeEqualTo("interactive flow text 3")
        result["Flow"].first { it["Id"].stringValue() == interactiveFlowContentFlowId }["Name"].stringValue()
            .shouldBeEqualTo("T_1_Flow BT 1")
    }

    @Test
    fun `first match in cell is wrapped in simple section flow`() {
        // given
        val rule1 = DisplayRuleBuilder("R_1").comparison { value("A").equals().value("A") }.build().mock()
        val rule2 = DisplayRuleBuilder("R_2").comparison { value("B").equals().value("B") }.build().mock()

        val block = DocumentObjectBuilder("B1", Block).table {
            addRow {
                addCell {
                    firstMatch {
                        addCase().displayRuleRef(rule1).string("first case").name("First")
                        addCase().displayRuleRef(rule2).string("second case").name("Second")
                        defaultString("default case")
                    }
                }
            }
        }.build()

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val cellNode = result["Cell"].last()
        val cellFlow = result["Flow"].last { it["Id"].stringValue() == cellNode["FlowId"].stringValue() }
        cellFlow["Type"].stringValue().shouldBeEqualTo("Simple")
        cellFlow["SectionFlow"].stringValue().shouldBeEqualTo("True")
        cellFlow["WebEditingType"].stringValue().shouldBeEqualTo("Section")

        val inlineCondFlowId = cellFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()
        val inlineCondFlow = result["Flow"].last { it["Id"].stringValue() == inlineCondFlowId }
        inlineCondFlow["Type"].stringValue().shouldBeEqualTo("InlCond")

        val conditions = inlineCondFlow["Condition"]
        conditions.size().shouldBeEqualTo(2)

        val firstCaseFlowId = conditions[0][""].stringValue()
        val firstCase = result["Flow"].last { it["Id"].stringValue() == firstCaseFlowId }

        val firstCaseFlow =
            result["Flow"].last { it["Id"].stringValue() == firstCase["FlowContent"]["P"]["T"]["O"]["Id"].stringValue() }
        firstCaseFlow["FlowContent"]["P"]["T"][""].stringValue().shouldBeEqualTo("first case")

        val secondCaseFlowId = conditions[1][""].stringValue()
        val secondCase = result["Flow"].last { it["Id"].stringValue() == secondCaseFlowId }

        val secondCaseFlow =
            result["Flow"].last { it["Id"].stringValue() == secondCase["FlowContent"]["P"]["T"]["O"]["Id"].stringValue() }
        secondCaseFlow["FlowContent"]["P"]["T"][""].stringValue().shouldBeEqualTo("second case")

        val default = result["Flow"].last { it["Id"].stringValue() == inlineCondFlow["Default"].stringValue() }

        val defaultFlow =
            result["Flow"].last { it["Id"].stringValue() == default["FlowContent"]["P"]["T"]["O"]["Id"].stringValue() }
        defaultFlow["FlowContent"]["P"]["T"][""].stringValue().shouldBeEqualTo("default case")
    }

    @Test
    fun `Condition flow in cell is wrapped in simple section flow`() {
        // given
        val refBlock = DocumentObjectBuilder("RefBlock", Block).string("ref content").build().mock()
        val displayRule = DisplayRuleBuilder("rule").comparison { value("C").equals().value("C") }.build().mock()

        val template = DocumentObjectBuilder("T1", Template).table {
            addRow().addCell().documentObjectRef(refBlock.id, displayRule.id)
        }.build()

        // when
        val result = subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val cellNode = result["Cell"].last()
        val cellFlow = result["Flow"].last { it["Id"].stringValue() == cellNode["FlowId"].stringValue() }
        cellFlow["Type"].stringValue().shouldBeEqualTo("Simple")
        cellFlow["SectionFlow"].stringValue().shouldBeEqualTo("True")
        cellFlow["WebEditingType"].stringValue().shouldBeEqualTo("Section")

        val condFlowId = cellFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()
        val condFlow = result["Flow"].last { it["Id"].stringValue() == condFlowId }
        condFlow["Type"].stringValue().shouldBeEqualTo("Condition")
    }

    @Test
    fun `build of simple text style accepts the specified values`() {
        every { paragraphStyleRepository.listAll() } returns listOf()
        every { textStyleRepository.listAll() } returns listOf(
            aTextStyle("textStyle1", definition = aTextDef(bold = true, underline = true)),
        )
        every { ipsService.gatherFontData(any()) } returns "Arial,Regular,icm://Interactive/${config.interactiveTenant}/Resources/Fonts/arial.ttf;"

        // when
        val result = subject.buildStyles(textStyleRepository.listAll(), paragraphStyleRepository.listAll())

        // then
        val textStyleDefinitions = xmlMapper.readTree(result.trimIndent())["Layout"]["Layout"]["TextStyle"]
        textStyleDefinitions.size().shouldBeEqualTo(3)

        val textStyle = textStyleDefinitions[2]
        textStyle["Bold"].stringValue().shouldBeEqualTo("True")
        textStyle["Italic"].stringValue().shouldBeEqualTo("False")
        textStyle["Underline"].stringValue().shouldBeEqualTo("True")
    }

    @Test
    fun `build of simple style delta works correctly`() {
        every { paragraphStyleRepository.listAll() } returns listOf(
            ParagraphStyleBuilder("paraStyle1").definition { leftIndent(Size.ofCentimeters(1)) }.build()
        )
        every { textStyleRepository.listAll() } returns listOf(
            TextStyleBuilder("textStyle1").definition { bold(true).underline(true) }.build()
        )
        every { ipsService.gatherFontData(any()) } returns "Arial,Regular,icm://Interactive/${config.interactiveTenant}/Resources/Fonts/arial.ttf;"

        // when
        val result = subject.buildStyleLayoutDelta(textStyleRepository.listAll(), paragraphStyleRepository.listAll())

        // then
        val textStyleDefinitions = xmlMapper.readTree(result.trimIndent())["TextStyle"]
        textStyleDefinitions.size().shouldBeEqualTo(3)

        val textStyle = textStyleDefinitions[2]
        textStyle["Bold"].stringValue().shouldBeEqualTo("True")
        textStyle["Italic"].stringValue().shouldBeEqualTo("False")
        textStyle["Underline"].stringValue().shouldBeEqualTo("True")

        val paraStyleDefinitions = xmlMapper.readTree(result.trimIndent())["ParaStyle"]
        paraStyleDefinitions .size().shouldBeEqualTo(3)

        val paraStyle = paraStyleDefinitions[2]
        paraStyle ["LeftIndent"].stringValue().shouldBeEqualTo("0.01")
    }

    @Test
    fun `build of text style with font size converts the value to millimeters`() {
        val sizeInMs = Size.ofPoints(12)
        every { paragraphStyleRepository.listAll() } returns listOf()
        every { textStyleRepository.listAll() } returns listOf(
            aTextStyle("textStyle1", definition = aTextDef(size = sizeInMs)),
        )
        every { ipsService.gatherFontData(any()) } returns "Arial,Regular,icm://Interactive/${config.interactiveTenant}/Resources/Fonts/arial.ttf;"
        val expectedValue = sizeInMs.toMeters()

        // when
        val result = subject.buildStyles(textStyleRepository.listAll(), paragraphStyleRepository.listAll())

        // then
        val textStyleDefinitions = xmlMapper.readTree(result.trimIndent())["Layout"]["Layout"]["TextStyle"]
        textStyleDefinitions.size().shouldBeEqualTo(3)

        val textStyle = textStyleDefinitions[2]
        textStyle["FontSize"].stringValue().shouldBeEqualTo(expectedValue.toString())
    }

    @Test
    fun `styles have correctly set layout`() {
        every { ipsService.gatherFontData(any()) } returns "Arial,Regular,icm://Interactive/${config.interactiveTenant}/Resources/Fonts/arial.ttf;"

        // when
        val result = subject.buildStyles(emptyList(), emptyList())

        // then
        val layout = xmlMapper.readTree(result.trimIndent())["Layout"]
        layout["Name"].stringValue().shouldBeEqualTo("DocumentLayout")

        val root = layout["Layout"]["Root"]
        root["AllowRuntimeModifications"].stringValue().shouldBeEqualTo("True")

        val page = layout["Layout"]["Page"]
        page.size().shouldBeEqualTo(2)
        page[0]["Name"].stringValue().shouldBeEqualTo("Page 1")

        val pages = layout["Layout"]["Pages"]
        pages["Id"].stringValue().shouldBeEqualTo("Def.Pages")
        pages["SelectionType"].stringValue().shouldBeEqualTo("Simple")
        pages["MainFlow"].stringValue().shouldBeEqualTo("SR_2")

    }

    @Test
    fun `paragraph number values are converted to meters`() {
        every { textStyleRepository.listAll() } returns listOf()
        every { paragraphStyleRepository.listAll() } returns listOf(
            ParagraphStyleBuilder("paraStyle1").definition {
                leftIndent(Size.ofMillimeters(7.5))
                rightIndent(Size.ofCentimeters(0.5))
                defaultTabSize(Size.ofMeters(0.03))
                spaceBefore(Size.ofMillimeters(4))
                spaceAfter(Size.ofMillimeters(6))
                firstLineIndent(Size.ofCentimeters(0.8))
                lineSpacing(LineSpacing.Exact(Size.ofCentimeters(1.5)))
                tabs(Tabs(listOf(Tab(Size.ofMillimeters(25), TabType.Left)), false))
            }.build()
        )
        every { ipsService.gatherFontData(any()) } returns "Arial,Regular,icm://Interactive/${config.interactiveTenant}/Resources/Fonts/arial.ttf;"

        // when
        val result = subject.buildStyles(textStyleRepository.listAll(), paragraphStyleRepository.listAll())

        // then
        val textStyleDefinitions = xmlMapper.readTree(result.trimIndent())["Layout"]["Layout"]["ParaStyle"]
        textStyleDefinitions.size().shouldBeEqualTo(3)

        val textStyle = textStyleDefinitions[2]
        textStyle["LeftIndent"].stringValue().shouldBeEqualTo("0.0075")
        textStyle["RightIndent"].stringValue().shouldBeEqualTo("0.005")
        textStyle["TabulatorProperties"]["Default"].stringValue().shouldBeEqualTo("0.03")
        textStyle["TabulatorProperties"]["Tabulator"]["Pos"].stringValue().shouldBeEqualTo("0.025")
        textStyle["SpaceBefore"].stringValue().shouldBeEqualTo("0.004")
        textStyle["SpaceAfter"].stringValue().shouldBeEqualTo("0.006")
        textStyle["FirstLineLeftIndent"].stringValue().shouldBeEqualTo("0.0155")
        textStyle["LineSpacing"].stringValue().shouldBeEqualTo("0.015")
        textStyle["LineSpacingType"].stringValue().shouldBeEqualTo("Exact")
    }

    @Test
    fun `font locations are correctly assigned from available font data`() {
        // given
        every { ipsService.gatherFontData(any()) } returns "Arial,Regular,icm://Interactive/${config.interactiveTenant}/Resources/Fonts/arial.ttf;" + "Arial,Bold,icm://Interactive/${config.interactiveTenant}/Resources/Fonts/arialbd.ttf;" + "Tahoma,Regular,icm://Interactive/${config.interactiveTenant}/Resources/Fonts/Tahoma/tahoma.ttf;"

        val textStyles = listOf(
            aTextStyle("ts1", definition = aTextDef(fontFamily = "Arial", bold = true)),
            aTextStyle("ts2", definition = aTextDef(fontFamily = "Arial")),
            aTextStyle("ts3", definition = aTextDef(fontFamily = "Tahoma", italic = true)),
            aTextStyle("ts4", definition = aTextDef(fontFamily = "Unknown", bold = true)),
        )

        // when
        val result = subject.buildStyles(textStyles, emptyList())

        // then
        val layout = xmlMapper.readTree(result.trimIndent())["Layout"]["Layout"]
        val fonts = layout["Font"]

        val arialFont = fonts.last { it["Id"].stringValue() == "Def.Font" }
        val arialSubFonts = arialFont["SubFont"]
        arialSubFonts.size().shouldBeEqualTo(2)
        val arialRegular = arialSubFonts.first { it["Name"].stringValue() == "Regular" }
        arialRegular["FontLocation"].stringValue()
            .shouldBeEqualTo("VCSLocation,icm://Interactive/tenant/Resources/Fonts/arial.ttf")
        val arialBold = arialSubFonts.first { it["Name"].stringValue() == "Bold" }
        arialBold["FontLocation"].stringValue()
            .shouldBeEqualTo("VCSLocation,icm://Interactive/tenant/Resources/Fonts/arialbd.ttf")

        val tahomaId = fonts.first { it["Name"].stringValue() == "Tahoma" }["Id"].stringValue()
        val tahomaFont = fonts.last { it["Id"].stringValue() == tahomaId }
        tahomaFont["FontName"].stringValue().shouldBeEqualTo("Tahoma")
        tahomaFont["SubFont"]["Name"].stringValue().shouldBeEqualTo("Italic")
        tahomaFont["SubFont"]["Italic"].stringValue().shouldBeEqualTo("True")
        tahomaFont["SubFont"]["Bold"].stringValue().shouldBeEqualTo("False")
        tahomaFont["SubFont"]["FontLocation"].stringValue()
            .shouldBeEqualTo("VCSLocation,icm://Interactive/tenant/Resources/Fonts/Tahoma/tahoma.ttf")

        val unknownId = fonts.first { it["Name"].stringValue() == "Unknown" }["Id"].stringValue()
        val unknownFont = fonts.last { it["Id"].stringValue() == unknownId }
        unknownFont["SubFont"].shouldBeNull()

        layout["TextStyle"].last { it["FontId"]?.stringValue() == tahomaId }["SubFont"].stringValue()
            .shouldBeEqualTo("Italic")
    }

    @Test
    fun `block with unassigned variable structure uses the one specified in project config`() {
        val variable = aVariable("V_1").mock()
        val variableStructureA = aVariableStructure(
            "VS_1", structure = mapOf(variable.id to VariablePathData("Data.Records.Value"))
        ).mock()
        val variableStructureB = aVariableStructure(
            "VS_2", structure = mapOf(variable.id to VariablePathData("Data.Clients.Value"))
        ).mock()
        val config = aProjectConfig(defaultVariableStructure = variableStructureB.id)

        val block = aDocObj(
            "B_1", Block, listOf(
                aParagraph(aText(listOf(StringValue("Text"), VariableRef(variable.id))))
            )
        ).mock()

        // when
        val subject = aSubject(config)
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        verify(exactly = 1) { variableStructureRepository.findOrFail(variableStructureB.id) }
        verify(exactly = 0) { variableStructureRepository.findOrFail(variableStructureA.id) }
        result["Variable"].first { it["Name"].stringValue() == variable.nameOrId() }["ParentId"].stringValue()
            .shouldBeEqualTo("Data.Clients.Value")
    }

    @Test
    fun `multiple select by languages always contain all languages`() {
        // given
        val block = aBlock(
            "1", listOf(
                aSelectByLanguage(
                    mapOf(
                        "en" to listOf(aParagraph("en")),
                        "de" to listOf(aParagraph("de")),
                        "es" to listOf(aParagraph("es"))
                    )
                ), aSelectByLanguage(
                    mapOf(
                        "ru" to listOf(aParagraph("ru")),
                        "fi" to listOf(aParagraph("fi")),
                    )
                )
            )
        )

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val flowDefinitions = result["Flow"]
        val languageFlows = flowDefinitions.filter { it["Type"]?.stringValue() == "Language" }
        languageFlows.size.shouldBeEqualTo(2)

        val firstLanguageFlow = languageFlows[0]
        val firstSubFlows = firstLanguageFlow["Condition"].associate { it["Value"]?.stringValue() to it[""].stringValue() }
        firstSubFlows.count().shouldBeEqualTo(5)
        flowDefinitions.last { it["Id"]?.stringValue() == firstSubFlows["en"] }["FlowContent"]["P"]["T"][""].stringValue()
            .shouldBeEqualTo("en")
        flowDefinitions.last { it["Id"]?.stringValue() == firstSubFlows["de"] }["FlowContent"]["P"]["T"][""].stringValue()
            .shouldBeEqualTo("de")
        flowDefinitions.last { it["Id"]?.stringValue() == firstSubFlows["es"] }["FlowContent"]["P"]["T"][""].stringValue()
            .shouldBeEqualTo("es")

        flowDefinitions.last { it["Id"]?.stringValue() == firstSubFlows["ru"] }["FlowContent"]["P"].shouldBeNull()
        flowDefinitions.last { it["Id"]?.stringValue() == firstSubFlows["fi"] }["FlowContent"]["P"].shouldBeNull()

        val defaultFlowId = firstLanguageFlow["Default"].stringValue()
        defaultFlowId.shouldNotBeEmpty()
        for (subflowId in firstSubFlows.values) {
            defaultFlowId.shouldNotBeEqualTo(subflowId)
        }

        val secondLanguageFlow = languageFlows[1]
        val secondSubFlows =
            secondLanguageFlow["Condition"].associate { it["Value"]?.stringValue() to it[""].stringValue() }
        secondSubFlows.count().shouldBeEqualTo(5)
        flowDefinitions.last { it["Id"]?.stringValue() == secondSubFlows["fi"] }["FlowContent"]["P"]["T"][""].stringValue()
            .shouldBeEqualTo("fi")
        flowDefinitions.last { it["Id"]?.stringValue() == secondSubFlows["en"] }["FlowContent"]["P"].shouldBeNull()
    }

    @Test
    fun `select by language default case references default language case if available`() {
        // given
        val block = aBlock(
            "1", listOf(
                aSelectByLanguage(
                    mapOf(
                        "en_us" to listOf(aParagraph("en_us")), "de" to listOf(aParagraph("de"))
                    )
                )
            )
        )

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val flowDefinitions = result["Flow"]
        val languageFlow = flowDefinitions.find { it["Type"]?.stringValue() == "Language" }!!
        val subFlows = languageFlow["Condition"].associate { it["Value"]?.stringValue() to it[""].stringValue() }
        subFlows.count().shouldBeEqualTo(2)
        val defaultFlowId = languageFlow["Default"].asString()
        defaultFlowId.shouldBeEqualTo(subFlows["en_us"])

        val enContent =
            flowDefinitions.findLast { it["Id"]?.stringValue() == subFlows["en_us"] }!!["FlowContent"]["P"]["T"][""].stringValue()
        val deContent =
            flowDefinitions.findLast { it["Id"]?.stringValue() == subFlows["de"] }!!["FlowContent"]["P"]["T"][""].stringValue()

        enContent.shouldBeEqualTo("en_us")
        deContent.shouldBeEqualTo("de")
    }

    @Test
    fun `does not allow snippets with content other than variable string content`() {
        val variable = VariableBuilder("V_1").dataType(DataType.String).build().mock()
        val docObj = DocumentObjectBuilder("B_1", Snippet)
            .paragraph { string("Paragraph") }
            .string("Hello World")
            .variableRef(variable)
            .paragraph { string("Paragraph 2") }
            .string("Goodbye World")
            .build()
            .mock()

        assertThrows<IllegalStateException>("Snippet 'B_1' has invalid content. Snippets must contain either only variable string content or a first match with only variable string content") {
            subject.buildDocumentObject(docObj).let { xmlMapper.readTree(it.trimIndent()) }
        }
    }

    @Test
    fun `builds a simple snippet`() {
        val subject = aSubject(aProjectConfig(defaultVariableStructure = "VS_1"))
        val variable1 = VariableBuilder("V_1").dataType(DataType.String).build().mock()
        val variable2 = VariableBuilder("V_2").dataType(DataType.String).build().mock()
        val variable3 = VariableBuilder("V_3").dataType(DataType.String).build().mock()
        VariableStructureBuilder("VS_1")
            .addVariable(variable1.id, "Data", "V_1")
            .addVariable(variable2.id, "Data", "V_2")
            .build()
            .mock()
        val docObj = DocumentObjectBuilder("B_1", Snippet)
            .string("Hello World")
            .variableRef(variable1)
            .variableRef(variable2)
            .variableRef(variable3)
            .string("Goodbye World")
            .build()
            .mock()

        val result = subject.buildDocumentObject(docObj).let { xmlMapper.readTree(it.trimIndent()) }

        val mainFlow = result["Flow"]
        mainFlow["Id"].stringValue().shouldBeEqualTo("Def.MainFlow")
        mainFlow["Type"].stringValue().shouldBeEqualTo("OverflowableVariableFormatted")

        val varId = mainFlow["Variable"].stringValue()
        val varObj = result["Variable"].last { it["Id"].stringValue() == varId }
        varObj["Type"].stringValue().shouldBeEqualTo("Calculated")
        varObj["VarType"].stringValue().shouldBeEqualTo("String")
        varObj["Script"].stringValue().shouldBeEqualTo($$"""return 'Hello World' + '<var name="V_1">' + '' + '<var name="V_2">' + '$V_3$' + 'Goodbye World';""")
    }

    @Test
    fun `builds a first match snippet`() {
        val subject = aSubject(aProjectConfig(defaultVariableStructure = "VS_1"))
        val variable1 = VariableBuilder("V_1").dataType(DataType.String).build().mock()
        val variable2 = VariableBuilder("V_2").dataType(DataType.String).build().mock()
        VariableStructureBuilder("VS_1")
            .addVariable(variable1.id, "Data", "V_1")
            .addVariable(variable2.id, "Data", "V_2")
            .build()
            .mock()
        val rule1 = aDisplayRule(Literal("A", LiteralDataType.String), Equals, Literal("B", LiteralDataType.String), id = "R_1").mock()
        val rule2 = aDisplayRule(Literal("C", LiteralDataType.String), Equals, Literal("C", LiteralDataType.String), id = "R_2").mock()

        val docObj = DocumentObjectBuilder("S_1", Snippet)
            .firstMatch {
                case {
                    displayRuleRef(rule1.id)
                    string("Hello ")
                    variableRef(variable1)
                }
                case {
                    displayRuleRef(rule2.id)
                    string("Goodbye ")
                    variableRef(variable2)
                }
                defaultString("Default text")
            }
            .build()
            .mock()

        val result = subject.buildDocumentObject(docObj).let { xmlMapper.readTree(it.trimIndent()) }

        // main flow should be SelectByInlineCondition
        val mainFlow = result["Flow"].first { it["Id"].stringValue() == "Def.MainFlow" }
        mainFlow["Type"].stringValue().shouldBeEqualTo("InlCond")

        // should have two conditions
        val conditions = mainFlow["Condition"]
        conditions.size().shouldBeEqualTo(2)

        // first case condition script
        conditions[0]["Value"].stringValue().shouldBeEqualTo("return (String('A')==String('B'));")
        val case1FlowId = conditions[0][""].stringValue()
        val case1Flow = result["Flow"].last { it["Id"].stringValue() == case1FlowId }
        case1Flow["Type"].stringValue().shouldBeEqualTo("OverflowableVariableFormatted")
        val case1VarId = case1Flow["Variable"].stringValue()
        val case1Var = result["Variable"].last { it["Id"].stringValue() == case1VarId }
        case1Var["Type"].stringValue().shouldBeEqualTo("Calculated")
        case1Var["VarType"].stringValue().shouldBeEqualTo("String")
        case1Var["Script"].stringValue().shouldBeEqualTo($"""return 'Hello ' + '<var name="V_1">';""")

        // second case condition script
        conditions[1]["Value"].stringValue().shouldBeEqualTo("return (String('C')==String('C'));")
        val case2FlowId = conditions[1][""].stringValue()
        val case2Flow = result["Flow"].last { it["Id"].stringValue() == case2FlowId }
        case2Flow["Type"].stringValue().shouldBeEqualTo("OverflowableVariableFormatted")
        val case2VarId = case2Flow["Variable"].stringValue()
        val case2Var = result["Variable"].last { it["Id"].stringValue() == case2VarId }
        case2Var["Type"].stringValue().shouldBeEqualTo("Calculated")
        case2Var["VarType"].stringValue().shouldBeEqualTo("String")
        case2Var["Script"].stringValue().shouldBeEqualTo($"""return 'Goodbye ' + '<var name="V_2">';""")

        // default flow
        val defaultFlowId = mainFlow["Default"].stringValue()
        val defaultFlow = result["Flow"].last { it["Id"].stringValue() == defaultFlowId }
        defaultFlow["Type"].stringValue().shouldBeEqualTo("OverflowableVariableFormatted")
        val defaultVarId = defaultFlow["Variable"].stringValue()
        val defaultVar = result["Variable"].last { it["Id"].stringValue() == defaultVarId }
        defaultVar["Type"].stringValue().shouldBeEqualTo("Calculated")
        defaultVar["VarType"].stringValue().shouldBeEqualTo("String")
        defaultVar["Script"].stringValue().shouldBeEqualTo("return 'Default text';")
    }

    @Test
    fun `buildDocumentObject correctly sets table style name`() {
        val block = DocumentObjectBuilder("T1", Block)
            .table { tableStyleName("testTableStyle1").addRow { addCell { string("table1") } } }
            .table { tableStyleName("testTableStyle1").addRow { addCell { string("table2") } } }
            .table { tableStyleName("testTableStyle2").addRow { addCell { string("table3") } } }
            .build()

        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        val tables = result["Table"].filter { it["TableStyleId"]?.stringValue() != null }.map { it["TableStyleId"].stringValue() }
        tables.shouldBeOfSize(3)
        tables.toSet().shouldBeEqualTo(setOf("Others.testTableStyle1", "Others.testTableStyle2"))
    }

    @Test
    fun `table style name correctly resolves to internal names`() {
        // given
        val block = DocumentObjectBuilder("T1", Block)
            .table { tableStyleName("testTableStyle1").addRow { addCell { string("table1") } } }
            .table { tableStyleName("testTableStyle1").addRow { addCell { string("table2") } } }
            .table { tableStyleName("testTableStyle2").addRow { addCell { string("table3") } } }
            .build()

        every { ipsService.wfd2xml(resourcePathProvider.getStyleDefinitionPath()) } returns """
            <Workflow>
                <Layout>
                    <Layout>
                        <TableStyle>
                            <Id>10</Id>
                            <Name>first_internal</Name>
                            <CustomProperty>{&quot;DisplayName&quot;:&quot;testTableStyle1&quot;}</CustomProperty>
                        </TableStyle>
                        <TableStyle>
                            <Id>11</Id>
                            <Name>second_internal</Name>
                            <CustomProperty>{&quot;DisplayName&quot;:&quot;testTableStyle2&quot;}</CustomProperty>
                        </TableStyle>
                    </Layout>
                </Layout>
            </Workflow>
        """.trimIndent()

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val tables = result["Table"].filter { it["TableStyleId"]?.stringValue() != null }.map { it["TableStyleId"].stringValue() }
        tables.shouldBeOfSize(3)
        tables.toSet().shouldBeEqualTo(setOf("Others.first_internal", "Others.second_internal"))
    }

    @Test
    fun `block referencing system variable by path does not create duplicate SystemVariable node`() {
        // given
        val pageCounterVar = VariableBuilder("pageCounter").name("PageCounter").dataType(DataType.Integer).build().mock()
        val variableStructure = VariableStructureBuilder("vs1")
            .addVariable(pageCounterVar.id, "Data.SystemVariable", "PageCounter")
            .build().mock()
        val config = aProjectConfig(defaultVariableStructure = variableStructure.id)
        val block = DocumentObjectBuilder("1", Block).paragraph {
            text { variableRef(pageCounterVar) }
        }.build()

        // when
        val subject = aSubject(config)
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val mainFlow = result["Flow"].first { it["Id"].stringValue() == "Def.MainFlow" }
        val contentFlowId = mainFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()

        val contentFlow = result["Flow"].last { it["Id"].stringValue() == contentFlowId }
        val variableId = contentFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()

        val variableData = result["Variable"].first { it["Id"].stringValue() == variableId }
        variableData["Name"].stringValue().shouldBeEqualTo("PageCounter")
        variableData["ParentId"].stringValue().shouldBeEqualTo("Def.SystemVariables")
        variableData["Forward"]["useExisting"].stringValue().shouldBeEqualTo("True")
    }

    @Test
    fun `build of block with grid layout with all properties set produces matching ECGrid xml`() {
        // given
        val block = DocumentObjectBuilder("1", Block).gridLayout {
            distribution(ColumnDistribution.ThreeColumns_25_25_50)
            verticalAlignment(GridAlignment.Center)
            columnStackingOnMobile(OnMobile.NoStacking)
            paddingTop(10.0)
            paddingBottom(20.0)
            paddingLeft(30.0)
            paddingRight(40.0)
            fullWidthBackground(true)
            column { content { paragraph { text { string("c1") } } } }
            column { content { paragraph { text { string("c2") } } } }
            column { content { paragraph { text { string("c3") } } } }
        }.build()

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val grid = ecGridContent(result)
        grid["ColumnsCount"].stringValue().shouldBeEqualTo("3")
        grid["FullWidthBackground"].stringValue().shouldBeEqualTo("True")
        grid["Distribution"].stringValue().shouldBeEqualTo("25-25-50")
        grid["VerticalAlignment"].stringValue().shouldBeEqualTo("Center")
        grid["OnMobile"].stringValue().shouldBeEqualTo("NoStacking")
        grid["Padding"]["Left"].stringValue().shouldBeEqualTo("30px")
        grid["Padding"]["Top"].stringValue().shouldBeEqualTo("10px")
        grid["Padding"]["Right"].stringValue().shouldBeEqualTo("40px")
        grid["Padding"]["Bottom"].stringValue().shouldBeEqualTo("20px")

        val columns = grid["Columns"]["Column"].toList()
        columns.shouldBeOfSize(3)

        val columnTexts = columns.map { col ->
            val content = ecContentForGrid(result, col["Component"].stringValue())
            val flowId = content["ContentId"].stringValue()
            val flow = result["Flow"].toList().first { it["Id"].stringValue() == flowId && it["FlowContent"] != null }
            flow["FlowContent"]["P"]["T"][""].stringValue()
        }
        columnTexts.shouldBeEqualTo(listOf("c1", "c2", "c3"))
    }

    @Test
    fun `build of block with grid layout with displayRuleRef wraps grid flow in select-by-condition flow`() {
        // given
        val rule = DisplayRuleBuilder("R_GRID").comparison { value("A").equals().value("A") }.build().mock()
        val block = DocumentObjectBuilder("1", Block).gridLayout {
            displayRuleRef(rule.id)
            column { content { paragraph { text { string("col text") } } } }
        }.build()

        // when
        val result = subject.buildDocumentObject(block).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val gridId = ecGridContent(result)["Id"].stringValue()
        val wrapperFlow = result["Flow"].toList().first {
            it["FlowContent"]?.get("P")?.get("T")?.get("O")?.get("Id")?.stringValue() == gridId
        }
        wrapperFlow["Type"].stringValue().shouldBeEqualTo("Simple")

        val wrapperFlowId = wrapperFlow["Id"].stringValue()
        val conditionFlow = result["Flow"].toList().first { it["Type"]?.stringValue() == "Condition" }
        conditionFlow["Condition"][""].stringValue().shouldBeEqualTo(wrapperFlowId)
    }

    private fun ecGridContent(result: tools.jackson.databind.JsonNode): tools.jackson.databind.JsonNode =
        result["ECGrid"].toList().first { it["ColumnsCount"] != null }

    private fun ecContentForGrid(result: tools.jackson.databind.JsonNode, id: String): tools.jackson.databind.JsonNode =
        result["ECContent"].toList().first { it["Id"]?.stringValue() == id && it["ContentId"] != null }

    @Test
    fun `build template with email model reference maps content to email interactive flow`() {
        // given
        val emailDoc = EmailObjectBuilder("E_1").string("Email content").build().mock()
        val template = DocumentObjectBuilder("T_1", Template).documentObjectRef(emailDoc).build()

        every { ipsService.wfd2xml(getBaseTemplateFullPath(config, null)) } returns """
            <Workflow>
                <Layout>
                    <Layout>
                        <Flow>
                            <Id>65</Id>
                            <Name>Letter Content</Name>
                        </Flow>
                        <Flow>
                            <Id>66</Id>
                            <Name>Body Content</Name>
                            <CustomProperty>{"customName":"Body Content"}</CustomProperty>
                        </Flow>
                        <Pages>
                            <InteractiveFlow>
                                <FlowId>65</FlowId>
                                <FlowType>Normal</FlowType>
                            </InteractiveFlow>
                            <InteractiveFlow>
                                <FlowId>66</FlowId>
                                <FlowType>HTML</FlowType>
                            </InteractiveFlow>
                        </Pages>
                        <ECPlaceHolder>
                            <Id Name="Header">Def.EmailsHeader</Id>
                            <PlaceHolderType>Header</PlaceHolderType>
                            <ContentId/>
                        </ECPlaceHolder>
                        <ECPlaceHolder>
                            <Id Name="Body">Def.EmailsBody</Id>
                            <PlaceHolderType>Body</PlaceHolderType>
                            <ContentId>66</ContentId>
                        </ECPlaceHolder>
                        <ECPlaceHolder>
                            <Id Name="Footer">Def.EmailsFooter</Id>
                            <PlaceHolderType>Footer</PlaceHolderType>
                            <ContentId/>
                        </ECPlaceHolder>
                    </Layout>
                </Layout>
            </Workflow>
        """.trimIndent()

        // when
        val result = subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val emailInteractiveFlow = result["Flow"].first { it["Id"].stringValue() == "Def.InteractiveFlow1" }
        val emailFlowContentId = emailInteractiveFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()
        result["Flow"].last { it["Id"].stringValue() == emailFlowContentId }["FlowContent"]["P"]["T"][""].stringValue()
            .shouldBeEqualTo("Email content")
    }

    @Test
    fun `build template with sms model reference maps content to sms interactive flow`() {
        // given
        val smsDoc = SmsObjectBuilder("S_1").string("SMS content").build().mock()
        val template = DocumentObjectBuilder("T_1", Template).documentObjectRef(smsDoc).build()

        every { ipsService.wfd2xml(getBaseTemplateFullPath(config, null)) } returns """
            <Workflow>
                <Layout>
                    <Layout>
                        <Flow>
                            <Id>65</Id>
                            <Name>Letter Content</Name>
                        </Flow>
                        <Flow>
                            <Id>67</Id>
                            <Name>SMS Content</Name>
                            <CustomProperty>{"customName":"SMS Content"}</CustomProperty>
                        </Flow>
                        <Pages>
                            <InteractiveFlow>
                                <FlowId>65</FlowId>
                                <FlowType>Normal</FlowType>
                            </InteractiveFlow>
                            <InteractiveFlow>
                                <FlowId>67</FlowId>
                                <FlowType>Normal</FlowType>
                            </InteractiveFlow>
                        </Pages>
                        <SMSRoot>
                            <Id Name="SMS">Def.SMSRoot</Id>
                            <FlowId>67</FlowId>
                        </SMSRoot>
                    </Layout>
                </Layout>
            </Workflow>
        """.trimIndent()

        // when
        val result = subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val smsInteractiveFlow = result["Flow"].first { it["Id"].stringValue() == "Def.InteractiveFlow1" }
        val smsFlowContentId = smsInteractiveFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()
        result["Flow"].last { it["Id"].stringValue() == smsFlowContentId }["FlowContent"]["P"]["T"][""].stringValue()
            .shouldBeEqualTo("SMS content")
    }

    @Test
    fun `build template with email model and options creates email sheet name variables`() {
        // given
        val emailDoc = EmailObjectBuilder("E_1")
            .string("Email content")
            .options {
                from("sender@example.com")
                fromName("Sender Name")
                subject("Test Subject")
                to("recipient@example.com")
            }
            .build().mock()
        val template = DocumentObjectBuilder("T_1", Template).documentObjectRef(emailDoc).build()

        every { ipsService.wfd2xml(getBaseTemplateFullPath(config, null)) } returns """
            <Workflow>
                <Layout>
                    <Layout>
                        <Flow>
                            <Id>65</Id>
                            <Name>Letter Content</Name>
                        </Flow>
                        <Flow>
                            <Id>66</Id>
                            <Name>Body Content</Name>
                            <CustomProperty>{"customName":"Body Content"}</CustomProperty>
                        </Flow>
                        <Pages>
                            <InteractiveFlow>
                                <FlowId>65</FlowId>
                                <FlowType>Normal</FlowType>
                            </InteractiveFlow>
                            <InteractiveFlow>
                                <FlowId>66</FlowId>
                                <FlowType>HTML</FlowType>
                            </InteractiveFlow>
                        </Pages>
                        <ECPlaceHolder>
                            <Id Name="Header">Def.EmailsHeader</Id>
                            <PlaceHolderType>Header</PlaceHolderType>
                            <ContentId/>
                        </ECPlaceHolder>
                        <ECPlaceHolder>
                            <Id Name="Body">Def.EmailsBody</Id>
                            <PlaceHolderType>Body</PlaceHolderType>
                            <ContentId>66</ContentId>
                        </ECPlaceHolder>
                        <ECPlaceHolder>
                            <Id Name="Footer">Def.EmailsFooter</Id>
                            <PlaceHolderType>Footer</PlaceHolderType>
                            <ContentId/>
                        </ECPlaceHolder>
                    </Layout>
                </Layout>
            </Workflow>
        """.trimIndent()

        // when
        val result = subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val allSheetNameVariableIds = result["Pages"]["SheetNameVariableId"].toList().map { it.stringValue() ?: "" }
        val emailSheetNameVariableIds = allSheetNameVariableIds.filter { it.isNotBlank() }
        emailSheetNameVariableIds.size.shouldBeEqualTo(4)

        val variableNames = emailSheetNameVariableIds.map { varId ->
            result["Variable"].first { it["Id"].stringValue() == varId }["Name"].stringValue()
        }
        variableNames.shouldBeEqualTo(listOf("EmailFromName", "EmailFrom", "EmailTo", "EmailSubject"))

        val variableScripts = emailSheetNameVariableIds.map { varId ->
            result["Variable"].last { it["Id"].stringValue() == varId }["Script"].stringValue()
        }
        variableScripts.shouldBeEqualTo(listOf(
            "return 'Sender Name';",
            "return 'sender@example.com';",
            "return 'recipient@example.com';",
            "return 'Test Subject';",
        ))
    }

    @Test
    fun `build template with sms model and options creates sms sheet name variable`() {
        // given
        val smsDoc = SmsObjectBuilder("S_1")
            .string("SMS content")
            .options { numberTo("+1234567890") }
            .build().mock()
        val template = DocumentObjectBuilder("T_1", Template).documentObjectRef(smsDoc).build()

        every { ipsService.wfd2xml(getBaseTemplateFullPath(config, null)) } returns """
            <Workflow>
                <Layout>
                    <Layout>
                        <Flow>
                            <Id>65</Id>
                            <Name>Letter Content</Name>
                        </Flow>
                        <Flow>
                            <Id>67</Id>
                            <Name>SMS Content</Name>
                            <CustomProperty>{"customName":"SMS Content"}</CustomProperty>
                        </Flow>
                        <Pages>
                            <InteractiveFlow>
                                <FlowId>65</FlowId>
                                <FlowType>Normal</FlowType>
                            </InteractiveFlow>
                            <InteractiveFlow>
                                <FlowId>67</FlowId>
                                <FlowType>Normal</FlowType>
                            </InteractiveFlow>
                        </Pages>
                        <SMSRoot>
                            <Id Name="SMS">Def.SMSRoot</Id>
                            <FlowId>67</FlowId>
                        </SMSRoot>
                    </Layout>
                </Layout>
            </Workflow>
        """.trimIndent()

        // when
        val result = subject.buildDocumentObject(template).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val allSheetNameVariableIds = result["Pages"]["SheetNameVariableId"].toList().map { it.stringValue() ?: "" }
        val smsSheetNameVariableIds = allSheetNameVariableIds.filter { it.isNotBlank() }
        smsSheetNameVariableIds.size.shouldBeEqualTo(1)

        val variableId = smsSheetNameVariableIds.first()
        result["Variable"].first { it["Id"].stringValue() == variableId }["Name"].stringValue()
            .shouldBeEqualTo("NumberTo")
        result["Variable"].last { it["Id"].stringValue() == variableId }["Script"].stringValue()
            .shouldBeEqualTo("return '+1234567890';")
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
        return this
    }

    private fun ParagraphStyle.mock(): ParagraphStyle {
        val id = this.id
        every { paragraphStyleRepository.findOrFail(id) } returns this
        return this
    }

    private fun aSubject(config: ProjectConfig) = InteractiveDocumentObjectBuilder(
        documentObjectRepository,
        textStyleRepository,
        paragraphStyleRepository,
        variableRepository,
        variableStructureRepository,
        displayRuleRepository,
        imageRepository,
        attachmentRepository,
        config,
        resourcePathProvider,
        icmDataCache,
    )
}