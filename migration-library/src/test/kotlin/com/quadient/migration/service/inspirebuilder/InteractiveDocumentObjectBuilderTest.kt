package com.quadient.migration.service.inspirebuilder

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.PathsConfig
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.data.DisplayRuleModel
import com.quadient.migration.data.DisplayRuleModelRef
import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.data.FirstMatchModel
import com.quadient.migration.data.ImageModel
import com.quadient.migration.data.ImageModelRef
import com.quadient.migration.data.ParagraphStyleModelRef
import com.quadient.migration.data.StringModel
import com.quadient.migration.data.TabModel
import com.quadient.migration.data.TableModel
import com.quadient.migration.data.TabsModel
import com.quadient.migration.data.TextStyleModelRef
import com.quadient.migration.data.VariableModel
import com.quadient.migration.data.VariableModelRef
import com.quadient.migration.data.VariableStructureModel
import com.quadient.migration.persistence.repository.DisplayRuleInternalRepository
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.repository.FileInternalRepository
import com.quadient.migration.persistence.repository.ImageInternalRepository
import com.quadient.migration.persistence.repository.ParagraphStyleInternalRepository
import com.quadient.migration.persistence.repository.TextStyleInternalRepository
import com.quadient.migration.persistence.repository.VariableInternalRepository
import com.quadient.migration.persistence.repository.VariableStructureInternalRepository
import com.quadient.migration.service.getBaseTemplateFullPath
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.shared.BinOp.*
import com.quadient.migration.shared.DataType
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.DocumentObjectType.*
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.Literal
import com.quadient.migration.shared.LiteralDataType
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.SkipOptions
import com.quadient.migration.shared.TabType
import com.quadient.migration.shared.VariablePathData
import com.quadient.migration.shared.millimeters
import com.quadient.migration.shared.toIcmPath
import com.quadient.migration.tools.model.aBlock
import com.quadient.migration.tools.model.aDisplayRule
import com.quadient.migration.tools.model.aParagraph
import com.quadient.migration.tools.model.aParaDef
import com.quadient.migration.tools.model.aParaStyle
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.model.aCell
import com.quadient.migration.tools.model.aDocObj
import com.quadient.migration.tools.model.aDocumentObjectRef
import com.quadient.migration.tools.model.aImage
import com.quadient.migration.tools.model.aRow
import com.quadient.migration.tools.model.aSelectByLanguage
import com.quadient.migration.tools.model.aTemplate
import com.quadient.migration.tools.model.aText
import com.quadient.migration.tools.model.aTextDef
import com.quadient.migration.tools.model.aTextStyle
import com.quadient.migration.tools.model.aVariable
import com.quadient.migration.tools.model.aVariableStructureModel
import com.quadient.migration.tools.model.anArea
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeNull
import com.quadient.migration.tools.shouldNotBeEmpty
import com.quadient.migration.tools.shouldNotBeEqualTo
import com.quadient.migration.tools.shouldNotBeNull
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class InteractiveDocumentObjectBuilderTest {
    val documentObjectRepository = mockk<DocumentObjectInternalRepository>()
    val textStyleRepository = mockk<TextStyleInternalRepository>()
    val paragraphStyleRepository = mockk<ParagraphStyleInternalRepository>()
    val variableRepository = mockk<VariableInternalRepository>()
    val variableStructureRepository = mockk<VariableStructureInternalRepository>()
    val displayRuleRepository = mockk<DisplayRuleInternalRepository>()
    val imageRepository = mockk<ImageInternalRepository>()
    val fileRepository = mockk<FileInternalRepository>()
    val config = aProjectConfig()
    val ipsService = mockk<IpsService>()

    private val subject = aSubject(config)

    private val xmlMapper = XmlMapper().also { it.findAndRegisterModules() }

    @BeforeEach
    fun setUp() {
        every { variableStructureRepository.listAllModel() } returns emptyList()
        every { ipsService.fileExists(any()) } returns true
    }

    @Test
    fun `build of simple block with string and var ref that is not part of structure results in single flow with one paragraph and text`() {
        // given
        val variable = aVariable("var1", "varName1")
        val block = aBlock(
            "1", listOf(aParagraph(aText(listOf(StringModel("some text"), VariableModelRef(variable.id)))))
        )
        every { variableRepository.findModelOrFail(eq(variable.id)) } returns variable

        // when
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val flowDefinitions = result["Flow"]
        flowDefinitions.size().shouldBeEqualTo(3)
        flowDefinitions[2]["FlowContent"]["P"]["T"][""].textValue().shouldBeEqualTo("some text$${variable.name}$")

        Assertions.assertNull(result["Variable"])
    }

    @Test
    fun `build of block with styled paragraph and text fetches both styles from db and uses them`() {
        // given
        val paraStyle = aParaStyle("paraStyle1", definition = aParaDef(Size.ofMillimeters(100)))
        val textStyle = aTextStyle("textStyle1", definition = aTextDef(fontFamily = "Arial"))

        val block = aBlock(
            "1", listOf(aParagraph(aText(StringModel("some text"), textStyle.id), paraStyle.id))
        )
        every { textStyleRepository.firstWithDefinitionModel(textStyle.id) } returns textStyle
        every { paragraphStyleRepository.firstWithDefinitionModel(paraStyle.id) } returns paraStyle

        // when
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val flowDefinitions = result["Flow"]
        flowDefinitions.size().shouldBeEqualTo(3)
        val paragraph = flowDefinitions[2]["FlowContent"]["P"]
        paragraph["Id"].textValue().shouldBeEqualTo("ParagraphStyles.${paraStyle.name}")
        val text = paragraph["T"]
        text["Id"].textValue().shouldBeEqualTo("TextStyles.${textStyle.name}")
    }

    @Test
    fun `build template with reference to block results in direct external flow`() {
        // given
        val block = aBlock("1", listOf(aParagraph(aText(StringModel("Hello")))))
        val template = aTemplate("2", listOf(aDocumentObjectRef(block.id)))

        every { documentObjectRepository.findModelOrFail(block.id) } returns block

        // when
        val result = subject.buildDocumentObject(template, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val flowDefinitions = result["Flow"]
        flowDefinitions.size().shouldBeEqualTo(3)

        val mainFlow = flowDefinitions[1]
        mainFlow["FlowContent"]["P"]["T"]["O"]["Id"].textValue().shouldBeEqualTo("SR_1")

        val directExternalFlow = flowDefinitions[2]
        directExternalFlow["Id"].textValue().shouldBeEqualTo("SR_1")
        directExternalFlow["Type"].textValue().shouldBeEqualTo("DirectExternal")
        directExternalFlow["ExternalLocation"].textValue()
            .shouldBeEqualTo("icm://Interactive/${config.interactiveTenant}/Blocks/${block.nameOrId()}.jld")
    }

    @Test
    fun `build block with table has correctly promoted column widths and spans`() {
        // given
        val variable = aVariable("clientName")
        val block = aBlock(
            "1", listOf(
                TableModel(
                    listOf(
                        aRow(
                            listOf(
                                aCell(
                                    aParagraph(
                                        aText(
                                            listOf(
                                                StringModel("Hello "),
                                                VariableModelRef(variable.id),
                                                StringModel(" how are you?")
                                            )
                                        )
                                    )
                                ), aCell(aParagraph(aText(StringModel("First row, second cell "))))
                            )
                        ), aRow(
                            listOf(
                                aCell(aParagraph(aText(StringModel("Second row, first cell")))),
                                aCell(aParagraph(aText(StringModel("Second row, second cell"))), mergeLeft = true)
                            )
                        )
                    ), listOf(
                        TableModel.ColumnWidthModel(Size.ofMillimeters(150), 10.0),
                        TableModel.ColumnWidthModel(Size.ofCentimeters(3), 1.0)
                    )
                )
            )
        )
        every { variableRepository.findModelOrFail(eq(variable.id)) } returns variable

        // when
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        result["Table"].size().shouldBeEqualTo(2)
        result["RowSet"].size().shouldBeEqualTo(6)
        result["Cell"].size().shouldBeEqualTo(8)

        val table = result["Table"][1]
        table["DisplayAsImage"].textValue().shouldBeEqualTo("False")
        val firstColumnWidth = table["ColumnWidths"][0]
        firstColumnWidth["MinWidth"].textValue().shouldBeEqualTo("0.15")
        firstColumnWidth["PercentWidth"].textValue().shouldBeEqualTo("10.0")
        val secondColumnWidth = table["ColumnWidths"][1]
        secondColumnWidth["MinWidth"].textValue().shouldBeEqualTo("0.03")
        secondColumnWidth["PercentWidth"].textValue().shouldBeEqualTo("1.0")

        val lastCell = result["Cell"][7]
        lastCell["SpanLeft"].textValue().shouldBeEqualTo("True")
        lastCell["FlowToNextPage"].textValue().shouldBeEqualTo("True")

        result["Flow"].size().shouldBeEqualTo(11)
    }

    @Test
    fun `build of more nested and complex structure is correctly wrapped into section if required`() {
        // given
        val externalBlock = aBlock("1", listOf(aParagraph(aText(StringModel("Hello")))))
        val internalBlock = aBlock("2", listOf(aParagraph(aText(StringModel("Sir")))), internal = true)

        val section = aBlock(
            "5", type = Section, content = listOf(
                aDocumentObjectRef(externalBlock.id),
                aParagraph(aText(StringModel("In between"))),
                aDocumentObjectRef(internalBlock.id)
            ), internal = true
        )
        val template = aTemplate("10", listOf(aDocumentObjectRef(section.id)))

        every { documentObjectRepository.findModelOrFail(externalBlock.id) } returns externalBlock
        every { documentObjectRepository.findModelOrFail(internalBlock.id) } returns internalBlock
        every { documentObjectRepository.findModelOrFail(section.id) } returns section

        // when
        val result = subject.buildDocumentObject(template, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val mainFlow = result["Flow"].first { it["Id"].textValue() == "Def.MainFlow" }
        val sectionFlowRef = mainFlow["FlowContent"]["P"]["T"]["O"]["Id"].textValue()

        result["Flow"].first { it["Id"].textValue() == sectionFlowRef }["Name"].textValue()
            .shouldBeEqualTo(section.nameOrId())
        val sectionFlow = result["Flow"].last { it["Id"].textValue() == sectionFlowRef }
        sectionFlow["SectionFlow"].textValue().shouldBeEqualTo("True")
        val sectionRefs = sectionFlow["FlowContent"]["P"]["T"]["O"]
        sectionRefs.size().shouldBeEqualTo(3)

        val nestedExternalFlowRef = sectionRefs[0]["Id"].textValue()
        result["Flow"].first { it["Id"].textValue() == nestedExternalFlowRef }["Name"].textValue()
            .shouldBeEqualTo(externalBlock.nameOrId())
        val nestedExternalFlow = result["Flow"].last { it["Id"].textValue() == nestedExternalFlowRef }
        nestedExternalFlow["Type"].textValue().shouldBeEqualTo("DirectExternal")

        val inBetweenFlowRef = sectionRefs[1]["Id"].textValue()
        result["Flow"].first { it["Id"].textValue() == inBetweenFlowRef }["Name"].textValue()
            .shouldBeEqualTo("${section.nameOrId()} 1")
        val inBetweenFlow = result["Flow"].last { it["Id"].textValue() == inBetweenFlowRef }
        inBetweenFlow["FlowContent"]["P"]["T"][""].textValue().shouldBeEqualTo("In between")

        val nestedInternalFlowRef = sectionRefs[2]["Id"].textValue()
        result["Flow"].first { it["Id"].textValue() == nestedInternalFlowRef }["Name"].textValue()
            .shouldBeEqualTo(internalBlock.nameOrId())
        val nestedInternalFlow = result["Flow"].last { it["Id"].textValue() == nestedInternalFlowRef }
        nestedInternalFlow["FlowContent"]["P"]["T"][""].textValue().shouldBeEqualTo("Sir")
    }

    @Test
    fun `build block with variable refs in variable structure with converted value based on type`() {
        // given
        val longVar = mockVar(aVariable("var1", "varName1", dataType = DataType.Integer64, defaultValue = "2025"))
        val currencyVar = mockVar(aVariable("var2", "varName2", dataType = DataType.Currency, defaultValue = "249.99"))
        val boolVar = mockVar(aVariable("var3", "varName3", dataType = DataType.Boolean, defaultValue = "TRUE"))
        val variableStructure = mockVarStructure(
            aVariableStructureModel(
                structure = mapOf(
                    VariableModelRef(longVar.id) to VariablePathData("Data.Clients.Value"),
                    VariableModelRef(currencyVar.id) to VariablePathData("Data.Clients.Value", "Money"),
                    VariableModelRef(boolVar.id) to VariablePathData("Data.Clients.Value")
                )
            )
        )
        val config = aProjectConfig(defaultVariableStructure = variableStructure.id)

        val block = aBlock(
            "1", listOf(
                aParagraph(
                    aText(
                        listOf(
                            VariableModelRef(longVar.id), VariableModelRef(currencyVar.id), VariableModelRef(boolVar.id)
                        )
                    )
                )
            )
        )

        // when
        val subject = aSubject(config)
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val variableDefinitions = result["Variable"]
        variableDefinitions.size().shouldBeEqualTo(12)
        val longVarForward = variableDefinitions.first { it["Name"].textValue() == "varName1" }

        longVarForward["Name"].textValue().shouldBeEqualTo(longVar.name)
        longVarForward["ParentId"].textValue().shouldBeEqualTo("Data.Clients.Value")
        longVarForward["Forward"]["useExisting"].textValue().shouldBeEqualTo("True")

        val longVarContent = variableDefinitions.last { it["Id"].textValue() == longVarForward["Id"].textValue() }
        longVarContent["Type"].textValue().shouldBeEqualTo("Disconnected")
        longVarContent["VarType"].textValue().shouldBeEqualTo("Int64")
        longVarContent["Content"].textValue().shouldBeEqualTo("2025")

        val currencyVarId = variableDefinitions.first { it["Name"].textValue() == "Money" }["Id"].textValue()
        variableDefinitions.last { it["Id"].textValue() == currencyVarId }["Content"].textValue()
            .shouldBeEqualTo("249.99")

        val boolVarId = variableDefinitions.first { it["Name"].textValue() == "varName3" }["Id"].textValue()
        variableDefinitions.last { it["Id"].textValue() == boolVarId }["Content"].textValue().shouldBeEqualTo("True")

        val clientsVar = variableDefinitions.first { it["Name"].textValue() == "Clients" }
        clientsVar["ParentId"].textValue().shouldBeEqualTo("Def.Data")
        variableDefinitions.first { it["Name"].textValue() == "Value" }["ParentId"].textValue()
            .shouldBeEqualTo(clientsVar["Id"].textValue())
        variableDefinitions.first { it["Name"].textValue() == "Count" }["ParentId"].textValue()
            .shouldBeEqualTo(clientsVar["Id"].textValue())

        result["Data"]["RepeatedBy"].textValue().shouldBeEqualTo("Data.Clients")
    }

    @Test
    fun `build template with external block using display rule wraps the block in condition flow`() {
        // given
        val displayRule =
            aDisplayRule(Literal("A", LiteralDataType.String), Equals, Literal("B", LiteralDataType.String))

        val block = aBlock("1", listOf(aParagraph(aText(StringModel("Hello")))))
        val template = aTemplate("2", listOf(aDocumentObjectRef(block.id, displayRule.id)))

        every { documentObjectRepository.findModelOrFail(block.id) } returns block
        every { displayRuleRepository.findModelOrFail(displayRule.id) } returns displayRule

        // when
        val result = subject.buildDocumentObject(template, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val mainFlow = result["Flow"].first { it["Id"].textValue() == "Def.MainFlow" }
        val conditionFlowRef = mainFlow["FlowContent"]["P"]["T"]["O"]["Id"].textValue()

        val conditionFlow = result["Flow"].last { it["Id"].textValue() == conditionFlowRef }
        conditionFlow["Type"].textValue().shouldBeEqualTo("Condition")
        val condition = conditionFlow["Condition"]
        val successFlowRef = condition[""].textValue()
        val conditionVarRef = condition["VarId"].textValue()

        val conditionVar = result["Variable"].last { it["Id"].textValue() == conditionVarRef }
        conditionVar["VarType"].textValue().shouldBeEqualTo("Bool")
        conditionVar["Type"].textValue().shouldBeEqualTo("Calculated")
        conditionVar["Script"].textValue().shouldBeEqualTo("return (String('A')==String('B'));")

        val successFlow = result["Flow"].last { it["Id"].textValue() == successFlowRef }
        successFlow["Type"].textValue().shouldBeEqualTo("DirectExternal")
    }

    @Test
    fun `build block with styled paragraph and styled text under display rule wraps the text in inline condition flow`() {
        // given
        val variable = mockVar(aVariable("V1", "ClientName"))
        val variableStructure = mockVarStructure(
            aVariableStructureModel(
                structure = mapOf(
                    VariableModelRef(variable.id) to VariablePathData("Data.Clients", "Client Name"),
                )
            )
        )

        val paraStyle = aParaStyle("P1", definition = aParaDef(10.millimeters()))
        val textStyle = aTextStyle("T1", definition = aTextDef(bold = true))
        val displayRule = aDisplayRule(
            Literal(variable.id, LiteralDataType.Variable), Equals, Literal("Jon", LiteralDataType.String)
        )

        val block = aBlock(
            "1", listOf(
                aParagraph(
                    aText(
                        StringModel("Hello There"), textStyle.id, DisplayRuleModelRef(displayRule.id)
                    ), paraStyle.id
                )
            )
        )
        val config = aProjectConfig(defaultVariableStructure = variableStructure.id)

        every { displayRuleRepository.findModelOrFail(displayRule.id) } returns displayRule
        every { textStyleRepository.firstWithDefinitionModel(textStyle.id) } returns textStyle
        every { paragraphStyleRepository.firstWithDefinitionModel(paraStyle.id) } returns paraStyle

        // when
        val subject = aSubject(config)
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        result["Variable"].first { it["ParentId"]?.textValue() == "Data.Clients" }["Name"].textValue()
            .shouldBeEqualTo("Client Name")

        val mainFlow = result["Flow"].first { it["Id"].textValue() == "Def.MainFlow" }
        val topSimpleFlowRef = mainFlow["FlowContent"]["P"]["T"]["O"]["Id"].textValue()

        val topSimpleFlow = result["Flow"].last { it["Id"].textValue() == topSimpleFlowRef }
        topSimpleFlow["Type"].textValue().shouldBeEqualTo("Simple")
        val topFlowParagraph = topSimpleFlow["FlowContent"]["P"]
        topFlowParagraph["Id"].textValue().shouldBeEqualTo("ParagraphStyles.${paraStyle.name}")
        val topFlowText = topFlowParagraph["T"]
        Assertions.assertNull(topFlowText["Id"])
        val conditionFlowRef = topFlowText["O"]["Id"].textValue()

        val conditionFlow = result["Flow"].last { it["Id"].textValue() == conditionFlowRef }
        conditionFlow["Type"].textValue().shouldBeEqualTo("InlCond")
        val condition = conditionFlow["Condition"]
        condition["Value"].textValue().shouldBeEqualTo("return (DATA.Clients.Client_Name==String('Jon'));")
        val finalFlowRef = condition[""].textValue()

        val finalFlow = result["Flow"].last { it["Id"].textValue() == finalFlowRef }
        finalFlow["Type"].textValue().shouldBeEqualTo("Simple")
        val finalFlowPara = finalFlow["FlowContent"]["P"]
        finalFlowPara["Id"].textValue().shouldBeEqualTo("ParagraphStyles.${paraStyle.name}")
        val finalFlowText = finalFlowPara["T"]
        finalFlowText["Id"].textValue().shouldBeEqualTo("TextStyles.${textStyle.name}")
        finalFlowText[""].textValue().shouldBeEqualTo("Hello There")

        result["Data"]["RepeatedBy"].shouldBeNull()
    }

    @Test
    fun `build block with styled paragraph under display rule and one of its texts under display rule wraps it in multiple condition flows`() {
        // given
        val variable = mockVar(aVariable("V1", "900_MailCount"))
        val variableStructure = mockVarStructure(
            aVariableStructureModel(
                structure = mapOf(VariableModelRef(variable.id) to VariablePathData("Data.1.Value"))
            )
        )
        val config = aProjectConfig(defaultVariableStructure = variableStructure.id)

        val textStyle = aTextStyle("T1", definition = aTextDef(bold = true))
        val paraStyle = aParaStyle("P1", definition = aParaDef(10.millimeters()))

        val paraDisplayRule = aDisplayRule(
            Literal(variable.id, LiteralDataType.Variable),
            GreaterOrEqualThan,
            Literal("540", LiteralDataType.Number),
            id = "R_para"
        )
        val textDisplayRule = aDisplayRule(
            Literal("A", LiteralDataType.String), NotEquals, Literal("B", LiteralDataType.String), id = "R_text"
        )

        val block = mockObj(
            aBlock(
                "1", listOf(
                    aParagraph(
                        listOf(
                            aText(
                                StringModel("This is")
                            ), aText(
                                StringModel("Preposterous!"), textStyle.id, DisplayRuleModelRef(textDisplayRule.id)
                            )
                        ), ParagraphStyleModelRef(paraStyle.id), DisplayRuleModelRef(paraDisplayRule.id)
                    )
                )
            )
        )

        every { displayRuleRepository.findModelOrFail(paraDisplayRule.id) } returns paraDisplayRule
        every { displayRuleRepository.findModelOrFail(textDisplayRule.id) } returns textDisplayRule
        every { textStyleRepository.firstWithDefinitionModel(textStyle.id) } returns textStyle
        every { paragraphStyleRepository.firstWithDefinitionModel(paraStyle.id) } returns paraStyle

        // when
        val subject = aSubject(config)
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val mainFlow = result["Flow"].first { it["Id"].textValue() == "Def.MainFlow" }
        val topSimpleFlowRef = mainFlow["FlowContent"]["P"]["T"]["O"]["Id"].textValue()

        val topSimpleFlow = result["Flow"].last { it["Id"].textValue() == topSimpleFlowRef }
        topSimpleFlow["Type"].textValue().shouldBeEqualTo("Simple")
        val topFlowParagraph = topSimpleFlow["FlowContent"]["P"]
        Assertions.assertNull(topFlowParagraph["Id"])
        val conditionFlowRef = topFlowParagraph["T"]["O"]["Id"].textValue()

        val conditionFlow = result["Flow"].last { it["Id"].textValue() == conditionFlowRef }
        conditionFlow["Type"].textValue().shouldBeEqualTo("InlCond")
        val condition = conditionFlow["Condition"]
        condition["Value"].textValue().shouldBeEqualTo("return (DATA._1.Current._900_MailCount>=540);")
        val paraFlowRef = condition[""].textValue()

        val paraFlow = result["Flow"].last { it["Id"].textValue() == paraFlowRef }
        val paraFlowPara = paraFlow["FlowContent"]["P"]
        paraFlowPara["Id"].textValue().shouldBeEqualTo("ParagraphStyles.${paraStyle.name}")
        paraFlowPara["T"][0][""].textValue().shouldBeEqualTo("This is")
        val secondText = paraFlowPara["T"][1]
        Assertions.assertNull(secondText["Id"])
        val textConditionFlowRef = secondText["O"]["Id"].textValue()

        val textConditionFlow = result["Flow"].last { it["Id"].textValue() == textConditionFlowRef }
        textConditionFlow["Type"].textValue().shouldBeEqualTo("InlCond")
        val textCondition = textConditionFlow["Condition"]
        textCondition["Value"].textValue().shouldBeEqualTo("return (String('A')!=String('B'));")
        val textFlowRef = textCondition[""].textValue()

        val textFlow = result["Flow"].last { it["Id"].textValue() == textFlowRef }
        val textFlowPara = textFlow["FlowContent"]["P"]
        textFlowPara["Id"].textValue().shouldBeEqualTo("ParagraphStyles.${paraStyle.name}")
        val textFlowText = textFlowPara["T"]
        textFlowText["Id"].textValue().shouldBeEqualTo("TextStyles.${textStyle.name}")
        textFlowText[""].textValue().shouldBeEqualTo("Preposterous!")
    }

    @Test
    fun `build block with table that has one row under display rule`() {
        // given
        val displayRule = aDisplayRule(
            Literal("TRUE", LiteralDataType.Boolean), Equals, Literal("FalSe", LiteralDataType.Boolean)
        )

        val block = aBlock(
            "1", listOf(
                TableModel(
                    listOf(
                        aRow(
                            listOf(
                                TableModel.CellModel(listOf(aParagraph(aText(StringModel("First row, first cell"))))),
                                TableModel.CellModel(listOf(aParagraph(aText(StringModel("First row, second cell")))))
                            )
                        ), aRow(
                            listOf(
                                TableModel.CellModel(listOf(aParagraph(aText(StringModel("Second row, first cell"))))),
                                TableModel.CellModel(listOf(aParagraph(aText(StringModel("Second row, second cell")))))
                            ), displayRule.id
                        )
                    ), listOf()
                )
            )
        )

        every { displayRuleRepository.findModelOrFail(displayRule.id) } returns displayRule
        every { documentObjectRepository.findModel(block.id) } returns block

        // when
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val conditionalRow = result["RowSet"].last { it["RowSetType"]?.textValue() == "Condition" }
        val rowCondition = conditionalRow["RowSetCondition"][0]
        val subRowRef = rowCondition["SubRowId"].textValue()
        val conditionVarRef = rowCondition["VariableId"].textValue()

        val conditionVar = result["Variable"].last { it["Id"].textValue() == conditionVarRef }
        conditionVar["Type"].textValue().shouldBeEqualTo("Calculated")
        conditionVar["VarType"].textValue().shouldBeEqualTo("Bool")
        conditionVar["Script"].textValue().shouldBeEqualTo("return (true==false);")

        val subRow = result["RowSet"].last { it["Id"].textValue() == subRowRef }
        subRow["RowSetType"].textValue().shouldBeEqualTo("Row")
    }

    @Test
    fun `build block with unavailable text style throws exception`() {
        // given
        val block = aBlock("1", listOf(aParagraph(aText(listOf(), TextStyleModelRef("textStyle1")))))
        every { textStyleRepository.firstWithDefinitionModel("textStyle1") } returns null

        // when
        val result = assertThrows<IllegalStateException> { subject.buildDocumentObject(block, null) }

        // then
        result.message.shouldBeEqualTo("Text style definition for textStyle1 not found.")
    }

    @Test
    fun `build block with unavailable paragraph style throws exception`() {
        // given
        val block = aBlock("1", listOf(aParagraph(aText(listOf()), "paraStyle1")))
        every { paragraphStyleRepository.firstWithDefinitionModel("paraStyle1") } returns null

        // when
        val result = assertThrows<IllegalStateException> { subject.buildDocumentObject(block, null) }

        // then
        result.message.shouldBeEqualTo("Paragraph style definition for paraStyle1 not found.")
    }

    @Test
    fun `build of template skips unsupported block and template is created without it`() {
        // given
        val unsupportedBlock = aBlock("1", skip = SkipOptions(true, null, null))
        every { documentObjectRepository.findModelOrFail("block1") } returns unsupportedBlock
        val template = aTemplate("10", listOf(aDocumentObjectRef("block1")))

        // when
        val result = subject.buildDocumentObject(template, null)
        val wfdXml = xmlMapper.readTree(result.trimIndent())

        // then
        val mainFlowText = wfdXml["Flow"]["FlowContent"]["P"]["T"]
        assert(mainFlowText != null)
        Assertions.assertNull(mainFlowText["O"])
    }

    @Test
    fun `build block with variable ref that is not found throws exception`() {
        // given
        val block = aBlock("1", listOf(aParagraph(aText(VariableModelRef("var1")))))
        every { variableRepository.findModelOrFail("var1") } throws IllegalStateException("Record 'var1' not found.")

        // when
        val result = assertThrows<IllegalStateException> { subject.buildDocumentObject(block, null) }

        // then
        result.message.shouldBeEqualTo("Record 'var1' not found.")
    }

    @Test
    fun `build template with block using display rule with unmapped variable creates dummy display rule`() {
        // given
        val variable = mockVar(aVariable("V_999", "DollarsInBank", dataType = DataType.Currency))

        val displayRule = mockRule(
            aDisplayRule(
                Literal(variable.id, LiteralDataType.Variable),
                GreaterOrEqualThan,
                Literal("25000", LiteralDataType.Number),
                negation = true
            )
        )

        val block = mockObj(aBlock("1", listOf(aParagraph(aText(StringModel("Hello"))))))
        val template = aTemplate("2", listOf(aDocumentObjectRef(block.id, displayRule.id)))

        every { variableRepository.findModel(variable.id) } returns variable
        every { variableStructureRepository.listAllModel() } returns listOf()

        // when
        val result = subject.buildDocumentObject(template, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val conditionVariable = result["Variable"].last { it["Type"]?.textValue() == "Calculated" }
        conditionVariable["Script"].textValue()
            .shouldBeEqualTo("return not (String('DollarsInBank>=25000')==String('unmapped'));")
    }

    @Test
    fun `build block with paragraph containing strings and document object refs inside one paragraph text`() {
        // given
        val variable = aVariable("V_100", "Salutation")

        val internalBlock = aBlock(
            "1", listOf(
                aParagraph(
                    aText(listOf(StringModel("Dear "), VariableModelRef(variable.id)))
                )
            ), internal = true
        )

        val externalBlock = aBlock("2", listOf(aParagraph(aText(StringModel("This is a good day!")))))

        val block = aBlock(
            "10", listOf(
                aParagraph(
                    aText(
                        listOf(
                            StringModel("Hello, "),
                            aDocumentObjectRef(internalBlock.id),
                            StringModel(". How are you?"),
                            aDocumentObjectRef(externalBlock.id)
                        )
                    )
                )
            )
        )

        every { documentObjectRepository.findModelOrFail(internalBlock.id) } returns internalBlock
        every { documentObjectRepository.findModelOrFail(externalBlock.id) } returns externalBlock
        every { variableRepository.findModelOrFail(variable.id) } returns variable
        every { variableStructureRepository.listAllModel() } returns listOf()

        // when
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val mainFlow = result["Flow"].first { it["Id"].textValue() == "Def.MainFlow" }
        val blockFlowRef = mainFlow["FlowContent"]["P"]["T"]["O"]["Id"].textValue()

        val sectionFlow = result["Flow"].last { it["Id"].textValue() == blockFlowRef }
        val sectionTextContent = sectionFlow["FlowContent"]["P"]["T"]
        sectionTextContent[""].size().shouldBeEqualTo(2)
        sectionTextContent["O"].size().shouldBeEqualTo(2)
    }

    @Test
    fun `build block with image`() {
        // given
        val image = aImage("Dog", options = ImageOptions(Size.ofPoints(120), Size.ofPoints(90)))
        val block = aBlock("1", listOf(ImageModelRef(image.id)))

        every { imageRepository.findModelOrFail(image.id) } returns image

        // when
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        result["Image"].first()["Name"].textValue().shouldBeEqualTo("Image_Dog")
        val imageContent = result["Image"].last()
        imageContent["ImageLocation"].textValue()
            .shouldBeEqualTo("VCSLocation,icm://Interactive/${config.interactiveTenant}/Resources/Images/${image.sourcePath}")
        imageContent["UseResizeWidth"].textValue().shouldBeEqualTo("True")
        imageContent["UseResizeHeight"].textValue().shouldBeEqualTo("True")
        imageContent["ResizeImageWidth"].textValue().shouldBeEqualTo(image.options?.resizeWidth?.toMeters().toString())
        imageContent["ResizeImageHeight"].textValue()
            .shouldBeEqualTo(image.options?.resizeHeight?.toMeters().toString())
    }

    @Test
    fun `build block with image uses alternateText from ImageModel`() {
        // given
        val image = aImage("Dog", alternateText = "A cute dog picture")
        val block = aBlock("1", listOf(ImageModelRef(image.id)))

        every { imageRepository.findModelOrFail(image.id) } returns image

        // when
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        result["Image"].first()["Name"].textValue().shouldBeEqualTo("Image_Dog")
        val imageContent = result["Image"].last()
        val varId = imageContent["AlternativeTextVar"].textValue()
        varId.shouldNotBeNull()

        val tagging = imageContent["PDFAdvanced"]["Tagging"]
        tagging["AlternateTextType"].textValue().shouldBeEqualTo("2")
        tagging["AlternateTextNodeId"].textValue().shouldBeEqualTo(varId)
        tagging["AlternateText"].textValue().shouldBeEqualTo("")
        tagging["Rule"].textValue().shouldBeEqualTo("Figure")

        val varData = result["Variable"].first { it["Id"].textValue() == varId }
        varData["Name"].textValue().shouldBeEqualTo("Alternate text variable for Image_Dog")
        varData["CustomProperty"].textValue().shouldBeEqualTo("{\"ValueWrapperVariable\":true}")

        val varContent = result["Variable"].last { it["Id"].textValue() == varId }
        varContent["Type"].textValue().shouldBeEqualTo("Calculated")
        varContent["Script"].textValue().shouldBeEqualTo("return 'A cute dog picture';")

        result["Root"]["LockedWebNodes"]["LockedWebNode"].textValue().shouldBeEqualTo(varId)
    }

    @Test
    fun `build template with two blocks referencing same image results in single image definition`() {
        // given
        val image = mockImage(aImage("Dog"))
        val block1 = mockObj(aBlock("1", listOf(ImageModelRef(image.id)), internal = true))
        val block2 = mockObj(aBlock("2", listOf(ImageModelRef(image.id)), internal = true))

        val template = aTemplate("3", listOf(aDocumentObjectRef(block1.id), aDocumentObjectRef(block2.id)))

        // when
        val result = subject.buildDocumentObject(template, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        result["Image"].filter { it["Name"]?.textValue() == image.nameOrId() }.size.shouldBeEqualTo(1)
    }

    @Test
    fun `build block with unknown image and image with missing source path renders placeholder texts instead`() {
        // given
        val catImage = aImage("Cat", imageType = ImageType.Unknown, skip = SkipOptions(true, "Cat placeholder", null))
        val dogImage = aImage("Dog", sourcePath = "", skip = SkipOptions(true, "Dog placeholder", null))

        val block = aBlock(
            "1", listOf(ImageModelRef(catImage.id), aParagraph(aText(ImageModelRef(dogImage.id))))
        )

        every { imageRepository.findModelOrFail(catImage.id) } returns catImage
        every { imageRepository.findModelOrFail(dogImage.id) } returns dogImage

        // when
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        assert(result["Image"] == null)
        val blockFlow = result["Flow"].last { it["Id"].textValue() != "Def.MainFlow" }
        val paragraphs = blockFlow["FlowContent"]["P"]
        paragraphs.size().shouldBeEqualTo(2)
        paragraphs[0]["T"][""].textValue().shouldBeEqualTo("Cat placeholder")
        paragraphs[1]["T"][""].textValue().shouldBeEqualTo("Dog placeholder")
    }

    @Test
    fun `paragraph with first match is built to inline condition flow with multiple options`() {
        // given
        val rule1 = mockRule(
            aDisplayRule(
                Literal("A", LiteralDataType.String), Equals, Literal("B", LiteralDataType.String), id = "R_1"
            )
        )

        val rule2 = mockRule(
            aDisplayRule(
                Literal("C", LiteralDataType.String), Equals, Literal("C", LiteralDataType.String), id = "R_2"
            )
        )

        val block = mockObj(
            aDocObj(
                "B_1", Block, listOf(
                    aParagraph(
                        aText(
                            listOf(
                                StringModel("Hello, "), FirstMatchModel(
                                    cases = listOf(
                                        FirstMatchModel.CaseModel(
                                            DisplayRuleModelRef(rule1.id),
                                            listOf(aParagraph(aText(StringModel("Mike")))),
                                            null
                                        ), FirstMatchModel.CaseModel(
                                            DisplayRuleModelRef(rule2.id),
                                            listOf(aParagraph(aText(StringModel("Jon")))),
                                            null
                                        )
                                    ), emptyList()
                                ), StringModel(", how are you?")
                            )
                        )
                    )
                )
            )
        )

        // when
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val mainFlow = result["Flow"].first { it["Id"].textValue() == "Def.MainFlow" }
        val contentFlow =
            result["Flow"].last { it["Id"].textValue() == mainFlow["FlowContent"]["P"]["T"]["O"]["Id"].textValue() }
        val contentFlowText = contentFlow["FlowContent"]["P"]["T"]

        contentFlowText[""][0].textValue().shouldBeEqualTo("Hello, ")
        contentFlowText[""][1].textValue().shouldBeEqualTo(", how are you?")

        val firstMatch = result["Flow"].last { it["Id"].textValue() == contentFlowText["O"]["Id"].textValue() }
        firstMatch["Type"].textValue().shouldBeEqualTo("InlCond")
        firstMatch["Default"].textValue().shouldBeEqualTo("")
        val conditions = firstMatch["Condition"]
        conditions.size().shouldBeEqualTo(2)

        conditions[0]["Value"].textValue().shouldBeEqualTo("return (String('A')==String('B'));")
        val firstConditionFlowId = conditions[0][""].textValue()

        result["Flow"].last { it["Id"].textValue() == firstConditionFlowId }["FlowContent"]["P"]["T"][""].textValue()
            .shouldBeEqualTo("Mike")

        conditions[1]["Value"].textValue().shouldBeEqualTo("return (String('C')==String('C'));")
        val secondConditionFlowId = conditions[1][""].textValue()

        result["Flow"].last { it["Id"].textValue() == secondConditionFlowId }["FlowContent"]["P"]["T"][""].textValue()
            .shouldBeEqualTo("Jon")
    }

    @Test
    fun `build of internal flow under display rule wraps in in condition flow`() {
        // given
        val rule = mockRule(
            aDisplayRule(Literal("C", LiteralDataType.String), Equals, Literal("C", LiteralDataType.String))
        )
        val internalBlock = mockObj(
            aDocObj(
                "B_1", Block, listOf(aParagraph(aText(StringModel("Text")))), true, displayRuleRef = rule.id
            )
        )
        val template = aDocObj("T_1", Template, listOf(aDocumentObjectRef(internalBlock.id)))

        // when
        val result = subject.buildDocumentObject(template, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val mainFlow = result["Flow"].first { it["Id"].textValue() == "Def.MainFlow" }
        val condFlowId = mainFlow["FlowContent"]["P"]["T"]["O"]["Id"].textValue()

        val condFlow = result["Flow"].last { it["Id"].textValue() == condFlowId }
        condFlow["Type"].textValue().shouldBeEqualTo("Condition")

        val innerFlow = result["Flow"].last { it["Id"].textValue() == condFlow["Condition"][""].textValue() }
        innerFlow["FlowContent"]["P"]["T"][""].textValue().shouldBeEqualTo("Text")
    }

    @Test
    fun `external block with multiple flows under display rule is built to section flow wrapped in condition flow`() {
        // given
        val rule = mockRule(
            aDisplayRule(Literal("C", LiteralDataType.String), Equals, Literal("C", LiteralDataType.String))
        )
        val innerBlock = mockObj(aDocObj("B_2", Block, listOf(aParagraph(aText(StringModel("Inner Text"))))))
        val block = aDocObj(
            "B_1", Block, listOf(
                aDocumentObjectRef(innerBlock.id),
                aParagraph(aText(StringModel("Text"))),
            ), false, displayRuleRef = rule.id
        )

        // when
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val mainFlow = result["Flow"].first { it["Id"].textValue() == "Def.MainFlow" }
        val condFlow =
            result["Flow"].last { it["Id"].textValue() == mainFlow["FlowContent"]["P"]["T"]["O"]["Id"].textValue() }
        condFlow["Type"].textValue().shouldBeEqualTo("Condition")

        val sectionFlow = result["Flow"].last { it["Id"].textValue() == condFlow["Condition"][""].textValue() }
        sectionFlow["SectionFlow"].textValue().shouldBeEqualTo("True")
        val sectionFlowRefs = sectionFlow["FlowContent"]["P"]["T"]["O"]
        sectionFlowRefs.size().shouldBeEqualTo(2)
    }

    @Test
    fun `build page with multiple areas with interactive flow`() {
        val page = aDocObj(
            "P_1", Page, listOf(
                anArea(listOf(aParagraph(aText("interactive flow text 1"))), interactiveFlowName = "Logo"),
                anArea(listOf(aParagraph(aText("main flow text 1")))),
                aParagraph(aText("main flow text 2")),
                anArea(
                    listOf(aParagraph(aText("interactive flow text 2"))), interactiveFlowName = "Def.InteractiveFlow1"
                ),
                anArea(listOf(aParagraph(aText("main flow text 3"))), interactiveFlowName = "Def.MainFlow"),
                anArea(listOf(aParagraph(aText("interactive flow text 3"))), interactiveFlowName = "Flow BT 1")
            )
        )

        every {
            ipsService.wfd2xml(
                getBaseTemplateFullPath(
                    config, null
                ).toString()
            )
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
        val result = subject.buildDocumentObject(page, null).let { xmlMapper.readTree(it.trimIndent()) }

        val mainFlow = result["Flow"].first { it["Id"].textValue() == "Def.MainFlow" }
        val mainFlowContentFlowId = mainFlow["FlowContent"]["P"]["T"]["O"]["Id"].textValue()
        val mainFlowContentFlow = result["Flow"].last { it["Id"].textValue() == mainFlowContentFlowId }
        val mainFlowParagraphs = mainFlowContentFlow["FlowContent"]["P"]
        mainFlowParagraphs.size().shouldBeEqualTo(3)
        mainFlowParagraphs[0]["T"][""].textValue().shouldBeEqualTo("main flow text 1")
        mainFlowParagraphs[1]["T"][""].textValue().shouldBeEqualTo("main flow text 2")
        mainFlowParagraphs[2]["T"][""].textValue().shouldBeEqualTo("main flow text 3")

        val interactiveFlow = result["Flow"].first { it["Id"].textValue() == "Def.InteractiveFlow1" }
        val interactiveFlowContentFlowId = interactiveFlow["FlowContent"]["P"]["T"]["O"]["Id"].textValue()
        val interactiveFlowContentFlow = result["Flow"].last { it["Id"].textValue() == interactiveFlowContentFlowId }
        val interactiveFlowParagraphs = interactiveFlowContentFlow["FlowContent"]["P"]
        interactiveFlowParagraphs.size().shouldBeEqualTo(3)
        interactiveFlowParagraphs[0]["T"][""].textValue().shouldBeEqualTo("interactive flow text 1")
        interactiveFlowParagraphs[1]["T"][""].textValue().shouldBeEqualTo("interactive flow text 2")
        interactiveFlowParagraphs[2]["T"][""].textValue().shouldBeEqualTo("interactive flow text 3")
    }

    @Test
    fun `build page with single area with interactive flow`() {
        val page = aDocObj(
            "P_1", Page, listOf(
                anArea(listOf(aParagraph(aText("interactive flow text 1"))), interactiveFlowName = "Logo")
            )
        )

        every {
            ipsService.wfd2xml(
                getBaseTemplateFullPath(
                    config, null
                ).toString()
            )
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
                    <Pages>
                        <InteractiveFlow>
                            <FlowId>80</FlowId>
                            <FlowType>Normal</FlowType>
                        </InteractiveFlow>
                    </Pages>
                </Layout>
            </Layout>
        </Workflow>""".trimMargin()

        // when
        val result = subject.buildDocumentObject(page, null).let { xmlMapper.readTree(it.trimIndent()) }

        val interactiveFlow = result["Flow"].first { it["Id"].textValue() == "Def.InteractiveFlow0" }
        val interactiveFlowContentFlowId = interactiveFlow["FlowContent"]["P"]["T"]["O"]["Id"].textValue()
        val interactiveFlowContentFlow = result["Flow"].last { it["Id"].textValue() == interactiveFlowContentFlowId }
        interactiveFlowContentFlow["FlowContent"]["P"]["T"][""].textValue().shouldBeEqualTo("interactive flow text 1")
    }

    @Test
    fun `first match in cell is wrapped in simple flow`() {
        // given
        val rule1 = mockRule(
            aDisplayRule(
                Literal("A", LiteralDataType.String), Equals, Literal("A", LiteralDataType.String), id = "rule1"
            )
        )
        val rule2 = mockRule(
            aDisplayRule(
                Literal("B", LiteralDataType.String), Equals, Literal("B", LiteralDataType.String), id = "rule2"
            )
        )
        val firstMatch = FirstMatchModel(
            cases = listOf(
                FirstMatchModel.CaseModel(
                    displayRuleRef = DisplayRuleModelRef(rule1.id),
                    content = listOf(aParagraph(aText(StringModel("first case")))),
                    name = "First"
                ), FirstMatchModel.CaseModel(
                    displayRuleRef = DisplayRuleModelRef(rule2.id),
                    content = listOf(aParagraph(aText(StringModel("second case")))),
                    name = "Second"
                )
            ), default = listOf(aParagraph(aText(StringModel("default case"))))
        )
        val block = aBlock(
            "B1", listOf(
                TableModel(
                    rows = listOf(
                        aRow(
                            listOf(
                                aCell(firstMatch)
                            )
                        )
                    ), columnWidths = listOf()
                )
            )
        )


        // when
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val cellNode = result["Cell"].last()
        val cellFlow = result["Flow"].last { it["Id"].textValue() == cellNode["FlowId"].textValue() }
        cellFlow["Type"].textValue().shouldBeEqualTo("Simple")
        cellFlow["SectionFlow"].textValue().shouldBeEqualTo("True")
        cellFlow["WebEditingType"].textValue().shouldBeEqualTo("Section")

        val inlineCondFlowId = cellFlow["FlowContent"]["P"]["T"]["O"]["Id"].textValue()
        val inlineCondFlow = result["Flow"].last { it["Id"].textValue() == inlineCondFlowId }
        inlineCondFlow["Type"].textValue().shouldBeEqualTo("InlCond")

        val conditions = inlineCondFlow["Condition"]
        conditions.size().shouldBeEqualTo(2)

        val firstCaseFlowId = conditions[0][""].textValue()
        val firstCase = result["Flow"].last { it["Id"].textValue() == firstCaseFlowId }

        val firstCaseFlow =
            result["Flow"].last { it["Id"].textValue() == firstCase["FlowContent"]["P"]["T"]["O"]["Id"].textValue() }
        firstCaseFlow["FlowContent"]["P"]["T"][""].textValue().shouldBeEqualTo("first case")

        val secondCaseFlowId = conditions[1][""].textValue()
        val secondCase = result["Flow"].last { it["Id"].textValue() == secondCaseFlowId }

        val secondCaseFlow =
            result["Flow"].last { it["Id"].textValue() == secondCase["FlowContent"]["P"]["T"]["O"]["Id"].textValue() }
        secondCaseFlow["FlowContent"]["P"]["T"][""].textValue().shouldBeEqualTo("second case")

        val default = result["Flow"].last { it["Id"].textValue() == inlineCondFlow["Default"].textValue() }

        val defaultFlow =
            result["Flow"].last { it["Id"].textValue() == default["FlowContent"]["P"]["T"]["O"]["Id"].textValue() }
        defaultFlow["FlowContent"]["P"]["T"][""].textValue().shouldBeEqualTo("default case")
    }

    @Test
    fun `build of simple text style accepts the specified values`() {
        every { paragraphStyleRepository.listAllModel() } returns listOf()
        every { textStyleRepository.listAllModel() } returns listOf(
            aTextStyle("textStyle1", definition = aTextDef(bold = true, underline = true)),
        )
        every { ipsService.gatherFontData(any()) } returns "Arial,Regular,icm://Interactive/${config.interactiveTenant}/Resources/Fonts/arial.ttf;"

        // when
        val result = subject.buildStyles(textStyleRepository.listAllModel(), paragraphStyleRepository.listAllModel())

        // then
        val textStyleDefinitions = xmlMapper.readTree(result.trimIndent())["Layout"]["Layout"]["TextStyle"]
        textStyleDefinitions.size().shouldBeEqualTo(3)

        val textStyle = textStyleDefinitions[2]
        textStyle["Bold"].textValue().shouldBeEqualTo("True")
        textStyle["Italic"].textValue().shouldBeEqualTo("False")
        textStyle["Underline"].textValue().shouldBeEqualTo("True")
    }

    @Test
    fun `build of simple style delta works correctly`() {
        every { paragraphStyleRepository.listAllModel() } returns listOf(
            aParaStyle("paraStyle1", definition = aParaDef(leftIndent = Size.ofCentimeters(1)))
        )
        every { textStyleRepository.listAllModel() } returns listOf(
            aTextStyle("textStyle1", definition = aTextDef(bold = true, underline = true)),
        )
        every { ipsService.gatherFontData(any()) } returns "Arial,Regular,icm://Interactive/${config.interactiveTenant}/Resources/Fonts/arial.ttf;"

        // when
        val result = subject.buildStyleLayoutDelta(textStyleRepository.listAllModel(), paragraphStyleRepository.listAllModel())

        // then
        val textStyleDefinitions = xmlMapper.readTree(result.trimIndent())["TextStyle"]
        textStyleDefinitions.size().shouldBeEqualTo(3)

        val textStyle = textStyleDefinitions[2]
        textStyle["Bold"].textValue().shouldBeEqualTo("True")
        textStyle["Italic"].textValue().shouldBeEqualTo("False")
        textStyle["Underline"].textValue().shouldBeEqualTo("True")

        val paraStyleDefinitions = xmlMapper.readTree(result.trimIndent())["ParaStyle"]
        paraStyleDefinitions .size().shouldBeEqualTo(3)

        val paraStyle = paraStyleDefinitions[2]
        paraStyle ["LeftIndent"].textValue().shouldBeEqualTo("0.01")
    }

    @Test
    fun `build of text style with font size converts the value to millimeters`() {
        val sizeInMs = Size.ofPoints(12)
        every { paragraphStyleRepository.listAllModel() } returns listOf()
        every { textStyleRepository.listAllModel() } returns listOf(
            aTextStyle("textStyle1", definition = aTextDef(size = sizeInMs)),
        )
        every { ipsService.gatherFontData(any()) } returns "Arial,Regular,icm://Interactive/${config.interactiveTenant}/Resources/Fonts/arial.ttf;"
        val expectedValue = sizeInMs.toMeters()

        // when
        val result = subject.buildStyles(textStyleRepository.listAllModel(), paragraphStyleRepository.listAllModel())

        // then
        val textStyleDefinitions = xmlMapper.readTree(result.trimIndent())["Layout"]["Layout"]["TextStyle"]
        textStyleDefinitions.size().shouldBeEqualTo(3)

        val textStyle = textStyleDefinitions[2]
        textStyle["FontSize"].textValue().shouldBeEqualTo(expectedValue.toString())
    }

    @Test
    fun `styles have correctly set layout`() {
        every { ipsService.gatherFontData(any()) } returns "Arial,Regular,icm://Interactive/${config.interactiveTenant}/Resources/Fonts/arial.ttf;"

        // when
        val result = subject.buildStyles(emptyList(), emptyList())

        // then
        val layout = xmlMapper.readTree(result.trimIndent())["Layout"]
        layout["Name"].textValue().shouldBeEqualTo("DocumentLayout")

        val root = layout["Layout"]["Root"]
        root["AllowRuntimeModifications"].textValue().shouldBeEqualTo("True")

        val page = layout["Layout"]["Page"]
        page.size().shouldBeEqualTo(2)
        page[0]["Name"].textValue().shouldBeEqualTo("Page 1")

        val pages = layout["Layout"]["Pages"]
        pages["Id"].textValue().shouldBeEqualTo("Def.Pages")
        pages["SelectionType"].textValue().shouldBeEqualTo("Simple")
        pages["MainFlow"].textValue().shouldBeEqualTo("SR_2")

    }

    @Test
    fun `paragraph number values are converted to meters`() {
        every { textStyleRepository.listAllModel() } returns listOf()
        every { paragraphStyleRepository.listAllModel() } returns listOf(
            aParaStyle(
                "paraStyle1", definition = aParaDef(
                    leftIndent = Size.ofMillimeters(7.5),
                    rightIndent = Size.ofCentimeters(0.5),
                    defaultTabSize = Size.ofMeters(0.03),
                    spaceBefore = Size.ofMillimeters(4),
                    spaceAfter = Size.ofMillimeters(6),
                    firstLineIndent = Size.ofCentimeters(0.8),
                    lineSpacing = LineSpacing.Exact(Size.ofCentimeters(1.5)),
                    tabs = TabsModel(listOf(TabModel(Size.ofMillimeters(25), TabType.Left)), false)
                )
            ),
        )
        every { ipsService.gatherFontData(any()) } returns "Arial,Regular,icm://Interactive/${config.interactiveTenant}/Resources/Fonts/arial.ttf;"

        // when
        val result = subject.buildStyles(textStyleRepository.listAllModel(), paragraphStyleRepository.listAllModel())

        // then
        val textStyleDefinitions = xmlMapper.readTree(result.trimIndent())["Layout"]["Layout"]["ParaStyle"]
        textStyleDefinitions.size().shouldBeEqualTo(3)

        val textStyle = textStyleDefinitions[2]
        textStyle["LeftIndent"].textValue().shouldBeEqualTo("0.0075")
        textStyle["RightIndent"].textValue().shouldBeEqualTo("0.005")
        textStyle["TabulatorProperties"]["Default"].textValue().shouldBeEqualTo("0.03")
        textStyle["TabulatorProperties"]["Tabulator"]["Pos"].textValue().shouldBeEqualTo("0.025")
        textStyle["SpaceBefore"].textValue().shouldBeEqualTo("0.004")
        textStyle["SpaceAfter"].textValue().shouldBeEqualTo("0.006")
        textStyle["FirstLineLeftIndent"].textValue().shouldBeEqualTo("0.0155")
        textStyle["LineSpacing"].textValue().shouldBeEqualTo("0.015")
        textStyle["LineSpacingType"].textValue().shouldBeEqualTo("Exact")
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

        val arialFont = fonts.last { it["Id"].textValue() == "Def.Font" }
        val arialSubFonts = arialFont["SubFont"]
        arialSubFonts.size().shouldBeEqualTo(2)
        val arialRegular = arialSubFonts.first { it["Name"].textValue() == "Regular" }
        arialRegular["FontLocation"].textValue()
            .shouldBeEqualTo("VCSLocation,icm://Interactive/tenant/Resources/Fonts/arial.ttf")
        val arialBold = arialSubFonts.first { it["Name"].textValue() == "Bold" }
        arialBold["FontLocation"].textValue()
            .shouldBeEqualTo("VCSLocation,icm://Interactive/tenant/Resources/Fonts/arialbd.ttf")

        val tahomaId = fonts.first { it["Name"].textValue() == "Tahoma" }["Id"].textValue()
        val tahomaFont = fonts.last { it["Id"].textValue() == tahomaId }
        tahomaFont["FontName"].textValue().shouldBeEqualTo("Tahoma")
        tahomaFont["SubFont"]["Name"].textValue().shouldBeEqualTo("Italic")
        tahomaFont["SubFont"]["Italic"].textValue().shouldBeEqualTo("True")
        tahomaFont["SubFont"]["Bold"].textValue().shouldBeEqualTo("False")
        tahomaFont["SubFont"]["FontLocation"].textValue()
            .shouldBeEqualTo("VCSLocation,icm://Interactive/tenant/Resources/Fonts/Tahoma/tahoma.ttf")

        val unknownId = fonts.first { it["Name"].textValue() == "Unknown" }["Id"].textValue()
        val unknownFont = fonts.last { it["Id"].textValue() == unknownId }
        unknownFont["SubFont"].shouldBeNull()

        layout["TextStyle"].last { it["FontId"]?.textValue() == tahomaId }["SubFont"].textValue()
            .shouldBeEqualTo("Italic")
    }

    @Test
    fun `block with unassigned variable structure uses the one specified in project config`() {
        val variable = mockVar(aVariable("V_1"))
        val variableStructureA = mockVarStructure(
            aVariableStructureModel(
                "VS_1", structure = mapOf(VariableModelRef(variable.id) to VariablePathData("Data.Records.Value"))
            )
        )
        val variableStructureB = mockVarStructure(
            aVariableStructureModel(
                "VS_2", structure = mapOf(VariableModelRef(variable.id) to VariablePathData("Data.Clients.Value"))
            )
        )
        val config = aProjectConfig(defaultVariableStructure = variableStructureB.id)

        val block = mockObj(
            aDocObj(
                "B_1", Block, listOf(
                    aParagraph(aText(listOf(StringModel("Text"), VariableModelRef(variable.id))))
                )
            )
        )

        // when
        val subject = aSubject(config)
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        verify(exactly = 1) { variableStructureRepository.findModelOrFail(variableStructureB.id) }
        verify(exactly = 0) { variableStructureRepository.findModelOrFail(variableStructureA.id) }
        result["Variable"].first { it["Name"].textValue() == variable.nameOrId() }["ParentId"].textValue()
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
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val flowDefinitions = result["Flow"]
        val languageFlows = flowDefinitions.filter { it["Type"]?.textValue() == "Language" }
        languageFlows.size.shouldBeEqualTo(2)

        val firstLanguageFlow = languageFlows[0]
        val firstSubFlows = firstLanguageFlow["Condition"].associate { it["Value"]?.textValue() to it[""].textValue() }
        firstSubFlows.count().shouldBeEqualTo(5)
        flowDefinitions.last { it["Id"]?.textValue() == firstSubFlows["en"] }["FlowContent"]["P"]["T"][""].textValue()
            .shouldBeEqualTo("en")
        flowDefinitions.last { it["Id"]?.textValue() == firstSubFlows["de"] }["FlowContent"]["P"]["T"][""].textValue()
            .shouldBeEqualTo("de")
        flowDefinitions.last { it["Id"]?.textValue() == firstSubFlows["es"] }["FlowContent"]["P"]["T"][""].textValue()
            .shouldBeEqualTo("es")

        flowDefinitions.last { it["Id"]?.textValue() == firstSubFlows["ru"] }["FlowContent"]["P"].shouldBeNull()
        flowDefinitions.last { it["Id"]?.textValue() == firstSubFlows["fi"] }["FlowContent"]["P"].shouldBeNull()

        val defaultFlowId = firstLanguageFlow["Default"].textValue()
        defaultFlowId.shouldNotBeEmpty()
        for (subflowId in firstSubFlows.values) {
            defaultFlowId.shouldNotBeEqualTo(subflowId)
        }

        val secondLanguageFlow = languageFlows[1]
        val secondSubFlows =
            secondLanguageFlow["Condition"].associate { it["Value"]?.textValue() to it[""].textValue() }
        secondSubFlows.count().shouldBeEqualTo(5)
        flowDefinitions.last { it["Id"]?.textValue() == secondSubFlows["fi"] }["FlowContent"]["P"]["T"][""].textValue()
            .shouldBeEqualTo("fi")
        flowDefinitions.last { it["Id"]?.textValue() == secondSubFlows["en"] }["FlowContent"]["P"].shouldBeNull()
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
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }

        // then
        val flowDefinitions = result["Flow"]
        val languageFlow = flowDefinitions.find { it["Type"]?.textValue() == "Language" }!!
        val subFlows = languageFlow["Condition"].associate { it["Value"]?.textValue() to it[""].textValue() }
        subFlows.count().shouldBeEqualTo(2)
        val defaultFlowId = languageFlow["Default"].asText()
        defaultFlowId.shouldBeEqualTo(subFlows["en_us"])

        val enContent =
            flowDefinitions.findLast { it["Id"]?.textValue() == subFlows["en_us"] }!!["FlowContent"]["P"]["T"][""].textValue()
        val deContent =
            flowDefinitions.findLast { it["Id"]?.textValue() == subFlows["de"] }!!["FlowContent"]["P"]["T"][""].textValue()

        enContent.shouldBeEqualTo("en_us")
        deContent.shouldBeEqualTo("de")
    }

    private fun mockObj(documentObject: DocumentObjectModel): DocumentObjectModel {
        every { documentObjectRepository.findModelOrFail(documentObject.id) } returns documentObject
        return documentObject
    }

    private fun mockImage(image: ImageModel): ImageModel {
        every { imageRepository.findModelOrFail(image.id) } returns image
        return image
    }

    private fun mockRule(rule: DisplayRuleModel): DisplayRuleModel {
        every { displayRuleRepository.findModelOrFail(rule.id) } returns rule
        return rule
    }

    private fun mockVar(variable: VariableModel): VariableModel {
        every { variableRepository.findModelOrFail(variable.id) } returns variable
        return variable
    }

    private fun mockVarStructure(variableStructure: VariableStructureModel): VariableStructureModel {
        every { variableStructureRepository.findModelOrFail(variableStructure.id) } returns variableStructure
        return variableStructure
    }

    private fun aSubject(config: ProjectConfig) = InteractiveDocumentObjectBuilder(
        documentObjectRepository,
        textStyleRepository,
        paragraphStyleRepository,
        variableRepository,
        variableStructureRepository,
        displayRuleRepository,
        imageRepository,
        fileRepository,
        config,
        ipsService,
    )

    @Nested
    inner class PathTest {

        @ParameterizedTest
        @CsvSource(
            // projectCfg.paths ,targetFolder   ,defaultTargetFolder,expected
            "               ,                   ,                   ,icm://Interactive/tenant/Resources/Images/Image_T_1.jpg",
            "null           ,null               ,null               ,icm://Interactive/tenant/Resources/Images/Image_T_1.jpg",
            "null           ,                   ,                   ,icm://Interactive/tenant/Resources/Images/Image_T_1.jpg",
            "               ,null               ,                   ,icm://Interactive/tenant/Resources/Images/Image_T_1.jpg",
            "               ,                   ,null               ,icm://Interactive/tenant/Resources/Images/Image_T_1.jpg",
            "               ,relative           ,                   ,icm://Interactive/tenant/Resources/Images/relative/Image_T_1.jpg",
            "               ,relative           ,null               ,icm://Interactive/tenant/Resources/Images/relative/Image_T_1.jpg",
            "root           ,relative           ,                   ,icm://Interactive/tenant/root/relative/Image_T_1.jpg",
            "root           ,relative           ,null               ,icm://Interactive/tenant/root/relative/Image_T_1.jpg",
            "               ,icm://absolute/    ,                   ,icm://absolute/Image_T_1.jpg",
            "/root/         ,icm://absolute/    ,                   ,icm://absolute/Image_T_1.jpg",
            "               ,icm://absolute/    ,shouldIgnoreToo    ,icm://absolute/Image_T_1.jpg",
            "shouldIgnore   ,icm://absolute/    ,shouldIgnoreToo    ,icm://absolute/Image_T_1.jpg",
            "imagesConfig   ,                   ,                   ,icm://Interactive/tenant/imagesConfig/Image_T_1.jpg",
            "/imagesConfig/ ,                   ,                   ,icm://Interactive/tenant/imagesConfig/Image_T_1.jpg",
            "imagesConfig   ,subDir             ,                   ,icm://Interactive/tenant/imagesConfig/subDir/Image_T_1.jpg",
            "imagesConfig   ,                   ,default            ,icm://Interactive/tenant/imagesConfig/default/Image_T_1.jpg",
            "imagesConfig   ,subDir             ,default            ,icm://Interactive/tenant/imagesConfig/subDir/Image_T_1.jpg",
            "               ,                   ,default            ,icm://Interactive/tenant/Resources/Images/default/Image_T_1.jpg",
            "               ,subDir             ,shouldIgnore       ,icm://Interactive/tenant/Resources/Images/subDir/Image_T_1.jpg",
        )
        fun testImagesPath(imagesPath: String?, targetFolder: String?, defaultTargetFolder: String?, expected: String) {
            val config = aProjectConfig(
                output = InspireOutput.Interactive,
                paths = PathsConfig(images = imagesPath.nullToNull()?.let(IcmPath::from)),
                targetDefaultFolder = defaultTargetFolder.nullToNull(),
                interactiveTenant = "tenant"
            )
            val pathTestSubject = aSubject(config)
            val image = aImage("T_1", targetFolder = targetFolder.nullToNull())

            val path = pathTestSubject.getImagePath(image)

            path.shouldBeEqualTo(expected)
        }

        @ParameterizedTest
        @CsvSource(
            // targetFolder   ,defaultTargetFolder  ,expected
            "                   ,                   ,icm://Interactive/tenant/Templates/T_1name.jld",
            "null               ,                   ,icm://Interactive/tenant/Templates/T_1name.jld",
            "                   ,null               ,icm://Interactive/tenant/Templates/T_1name.jld",
            "null               ,null               ,icm://Interactive/tenant/Templates/T_1name.jld",
            "relative           ,                   ,icm://Interactive/tenant/Templates/relative/T_1name.jld",
            "relative           ,null               ,icm://Interactive/tenant/Templates/relative/T_1name.jld",
            "relative           ,default            ,icm://Interactive/tenant/Templates/relative/T_1name.jld",
            "icm://absolute/    ,                   ,icm://absolute/T_1name.jld",
            "icm://absolute/    ,default            ,icm://absolute/T_1name.jld",
            "                   ,default            ,icm://Interactive/tenant/Templates/default/T_1name.jld",
            "null               ,default            ,icm://Interactive/tenant/Templates/default/T_1name.jld",
        )
        fun testDocumentObjectPath(targetFolder: String?, defaultTargetFolder: String?, expected: String) {
            val config = aProjectConfig(
                output = InspireOutput.Interactive,
                targetDefaultFolder = defaultTargetFolder.nullToNull(),
                interactiveTenant = "tenant"
            )
            val pathTestSubject = aSubject(config)
            val image = aDocObj("T_1", type = DocumentObjectType.Template, targetFolder = targetFolder.nullToNull())

            val path = pathTestSubject.getDocumentObjectPath(image)

            path.shouldBeEqualTo(expected)
        }

        @ParameterizedTest
        @CsvSource(
            // styleDefPath        ,defaultTargetFolder  ,expected
            "                      ,                       ,icm://Interactive/tenant/CompanyStyles/projectNameStyles.jld",
            "                      ,null                   ,icm://Interactive/tenant/CompanyStyles/projectNameStyles.jld",
            "                      ,default                ,icm://Interactive/tenant/CompanyStyles/default/projectNameStyles.jld",
            "icm://some/path/f.jld ,default                ,icm://some/path/f.jld",
        )
        fun testCompanyStylesPath(styleDefPath: String?, defaultTargetFolder: String?, expected: String) {
            val config = aProjectConfig(
                name = "projectName",
                output = InspireOutput.Interactive,
                targetDefaultFolder = defaultTargetFolder.nullToNull(),
                interactiveTenant = "tenant",
                styleDefinitionPath = styleDefPath?.toIcmPath()
            )
            val pathTestSubject = aSubject(config)

            val path = pathTestSubject.getStyleDefinitionPath("jld")

            path.shouldBeEqualTo(expected)
        }

        @ParameterizedTest
        @CsvSource(
            ",icm://Interactive/tenant/Resources/Fonts", "Fonts,icm://Interactive/tenant/Fonts"
        )
        fun testFontRootFolder(cfgFontsPath: String?, expected: String) {
            val config = aProjectConfig(
                output = InspireOutput.Interactive,
                interactiveTenant = "tenant",
                paths = PathsConfig(fonts = cfgFontsPath?.toIcmPath())
            )
            val pathTestSubject = aSubject(config)

            val path = pathTestSubject.getFontRootFolder()

            path.shouldBeEqualTo(expected)
        }

        @Test
        fun companyStylePathMustBeAbsolute() {
            val config = aProjectConfig(
                name = "projectName",
                output = InspireOutput.Interactive,
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
    }
}