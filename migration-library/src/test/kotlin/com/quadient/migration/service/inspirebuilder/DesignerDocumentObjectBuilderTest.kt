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
import com.quadient.migration.data.StringModel
import com.quadient.migration.data.TableModel
import com.quadient.migration.data.VariableModel
import com.quadient.migration.data.VariableModelRef
import com.quadient.migration.data.VariableStructureModel
import com.quadient.migration.persistence.repository.DisplayRuleInternalRepository
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.repository.ImageInternalRepository
import com.quadient.migration.persistence.repository.ParagraphStyleInternalRepository
import com.quadient.migration.persistence.repository.TextStyleInternalRepository
import com.quadient.migration.persistence.repository.VariableInternalRepository
import com.quadient.migration.persistence.repository.VariableStructureInternalRepository
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.shared.BinOp
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.DocumentObjectType.*
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.Literal
import com.quadient.migration.shared.LiteralDataType
import com.quadient.migration.shared.PageOptions
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.VariablePathData
import com.quadient.migration.shared.centimeters
import com.quadient.migration.shared.millimeters
import com.quadient.migration.shared.toIcmPath
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.model.aCell
import com.quadient.migration.tools.model.aVariable
import com.quadient.migration.tools.model.aDisplayRule
import com.quadient.migration.tools.model.aDocObj
import com.quadient.migration.tools.model.aDocumentObjectRef
import com.quadient.migration.tools.model.aImage
import com.quadient.migration.tools.model.aParagraph
import com.quadient.migration.tools.model.aRow
import com.quadient.migration.tools.model.aText
import com.quadient.migration.tools.model.aVariableStructureModel
import com.quadient.migration.tools.model.anArea
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldNotBeNull
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
    val documentObjectRepository = mockk<DocumentObjectInternalRepository>()
    val textStyleRepository = mockk<TextStyleInternalRepository>()
    val paragraphStyleRepository = mockk<ParagraphStyleInternalRepository>()
    val variableRepository = mockk<VariableInternalRepository>()
    val variableStructureRepository = mockk<VariableStructureInternalRepository>()
    val displayRuleRepository = mockk<DisplayRuleInternalRepository>()
    val imageRepository = mockk<ImageInternalRepository>()
    val ipsService = mockk<IpsService>()
    val config = aProjectConfig(targetDefaultFolder = "defaultFolder")

    private val subject = aSubject(config)

    private val xmlMapper = XmlMapper().also { it.findAndRegisterModules() }

    @BeforeEach
    fun setUp() {
        every { variableStructureRepository.listAllModel() } returns emptyList()
        every { textStyleRepository.listAllModel() } returns emptyList()
        every { paragraphStyleRepository.listAllModel() } returns emptyList()
    }

    @Test
    fun `buildDocumentObject correctly constructs complex template structure with pages and standalone block`() {
        // given
        val block = mockObj(
            aDocObj(
                "B_1", Block, listOf(
                    aParagraph(aText(StringModel("Hello there!")))
                ), true
            )
        )

        val standaloneBlock = mockObj(
            aDocObj("B_2", Block, listOf(aParagraph(aText(StringModel("I am alone")))))
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
        result["Flow"].first { it["Id"].textValue() == flowId }["Name"].textValue().shouldBeEqualTo(block.nameOrId())
        result["Flow"].last { it["Id"].textValue() == flowId }["FlowContent"]["P"]["T"][""].textValue()
            .shouldBeEqualTo("Hello there!")

        val virtualPageFlowAreaId =
            result["FlowArea"].first { it["ParentId"].textValue() == virtualPageId }["Id"].textValue()
        val virtualPageFlowArea = result["FlowArea"].last { it["Id"].textValue() == virtualPageFlowAreaId }
        virtualPageFlowArea["Pos"]["X"].textValue().shouldBeEqualTo("0.015")
        virtualPageFlowArea["Pos"]["Y"].textValue().shouldBeEqualTo("0.015")
        virtualPageFlowArea["Size"]["X"].textValue().shouldBeEqualTo("0.18")
        virtualPageFlowArea["Size"]["Y"].textValue().shouldBeEqualTo("0.267")
        val virtualFlowId = virtualPageFlowArea["FlowId"].textValue()
        result["Flow"].first { it["Id"].textValue() == virtualFlowId }["Name"].textValue()
            .shouldBeEqualTo(standaloneBlock.nameOrId())
        result["Flow"].last { it["Id"].textValue() == virtualFlowId }["ExternalLocation"].textValue()
            .shouldBeEqualTo("icm://${config.defaultTargetFolder}/${standaloneBlock.nameOrId()}.wfd")

        result["Root"]["AllowRuntimeModifications"].textValue().shouldBeEqualTo("True")
        result["Pages"]["MainFlow"].textValue().shouldBeEqualTo(flowId)
    }

    @Test
    fun `buildDocumentObject creates flow area with placeholder text in case of invalid image`() {
        // given
        val image = mockImg(aImage("Img_1", sourcePath = null))
        val page = mockObj(
            aDocObj(
                "P_1", Page,
                listOf(
                    anArea(
                        listOf(ImageModelRef(image.id)),
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
        flow["FlowContent"]["P"]["T"][""].textValue().shouldBeEqualTo("Image without source path: ${image.nameOrId()}")
    }

    @Test
    fun `buildDocumentObject creates image area with image in case of flow area only with valid image ref`() {
        val imageModel = mockImg(aImage("Img_1"))
        val page = mockObj(
            aDocObj(
                "P_1", Page, listOf(
                    anArea(
                        listOf(
                            ImageModelRef(imageModel.id)
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
            .shouldBeEqualTo("VCSLocation,icm://${config.defaultTargetFolder}/${imageModel.nameOrId()}.jpg")
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
                "B_1", Block, listOf(aParagraph(aText(StringModel("Hi"))))
            )
        )
        val template = mockObj(aDocObj("T_1", Template, listOf(aDocumentObjectRef(block.id, rule.id))))

        // when
        val result =
            subject.buildDocumentObject(template, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val areaFlowId = result["FlowArea"].last()["FlowId"].textValue()
        val areaFlow = result["Flow"].last { it["Id"].textValue() == areaFlowId }
        areaFlow["Type"].textValue().shouldBeEqualTo("InlCond")
        val condition = areaFlow["Condition"]
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
                    TableModel(
                        listOf(
                            aRow(
                                listOf(
                                    aCell(aParagraph(aText(StringModel("A")))),
                                    aCell(aParagraph(aText(StringModel("B"))))
                                )
                            ), aRow(
                                listOf(
                                    aCell(aParagraph(aText(StringModel("C")))),
                                    aCell(aParagraph(aText(StringModel("D"))))
                                ), rule.id
                            )
                        ), listOf()
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
    }

    @Test
    fun `buildDocumentObject creates twice used block only once`() {
        // given
        val block = mockObj(aDocObj("B_1", Block, listOf(aParagraph(aText(StringModel("Hi"))))))
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
    fun `buildDocumentObject names multiple composite flows with numbers`() {
        // given
        val innerBlock = mockObj(
            aDocObj("B_2", Block, listOf(aParagraph(aText(StringModel("In between")))), internal = true)
        )
        val block = mockObj(
            aDocObj(
                "B_1", Block, listOf(
                    aParagraph(aText(StringModel("Hi"))),
                    aDocumentObjectRef(innerBlock.id),
                    aParagraph(aText(StringModel("Bye")))
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
                            listOf(StringModel("First usage: "), VariableModelRef(variable.id))
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
                            listOf(StringModel("Second usage: "), VariableModelRef(variable.id))
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
            aVariableStructureModel(
                structure = mapOf(
                    VariableModelRef(variable.id) to VariablePathData("Data.Records.Value")
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
        val defaultFlowModel = mockObj(aDocObj("B_10", Block, listOf(aParagraph(aText(StringModel("I am default"))))))
        val rule1 = mockRule(
            aDisplayRule(
                Literal("A", LiteralDataType.String), BinOp.Equals, Literal("B", LiteralDataType.String), id = "R_1"
            )
        )
        val flow1 =
            mockObj(aDocObj("B_11", Block, listOf(aParagraph(aText(StringModel("flow 1 content")))), internal = false))

        val rule2 = mockRule(
            aDisplayRule(
                Literal("C", LiteralDataType.String), BinOp.Equals, Literal("C", LiteralDataType.String), id = "R_2"
            )
        )

        val block = mockObj(
            aDocObj(
                "B_1", Block, listOf(
                    FirstMatchModel(
                        cases = listOf(
                            FirstMatchModel.CaseModel(
                                DisplayRuleModelRef(rule1.id), listOf(aDocumentObjectRef(flow1.id)), null
                            ), FirstMatchModel.CaseModel(
                                DisplayRuleModelRef(rule2.id),
                                listOf(aParagraph(aText(StringModel("flow 2 content")))),
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
            mockObj(aDocObj("B_1", Block, listOf(aParagraph(aText(StringModel("Text")))), displayRuleRef = rule.id))

        // when
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val flowAreaFlowId = result["FlowArea"].last()["FlowId"].textValue()
        val flowAreaFlow = result["Flow"].last { it["Id"].textValue() == flowAreaFlowId }

        flowAreaFlow["Type"].textValue().shouldBeEqualTo("InlCond")

        result["Flow"].last { it["Id"].textValue() == flowAreaFlow["Condition"][""].textValue() }["FlowContent"]["P"]["T"][""].textValue()
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
        layout["Flow"].last()["FlowContent"]["P"]["T"][""].textValue().shouldBeEqualTo("Text")
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
        every { variableStructureRepository.listAllModel() } returns listOf(variableStructureA, variableStructureB)
        val config = aProjectConfig(defaultVariableStructure = variableStructureB.id)

        val block = mockObj(
            aDocObj(
                "B_1", Block, listOf(
                    aParagraph(aText(listOf(StringModel("Text"), VariableModelRef(variable.id))))
                ), variableStructureModelRef = variableStructureA.id
            )
        )

        // when
        val subject = aSubject(config)
        val result = subject.buildDocumentObject(block, null).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        verify(exactly = 1) { variableStructureRepository.findModelOrFail(variableStructureA.id) }
        verify(exactly = 0) { variableStructureRepository.findModelOrFail(variableStructureB.id) }

        result["Variable"].first { it["Name"].textValue() == variable.nameOrId() }["ParentId"].textValue()
            .shouldBeEqualTo("Data.Records.Value")
    }

    private fun mockObj(documentObject: DocumentObjectModel): DocumentObjectModel {
        every { documentObjectRepository.findModelOrFail(documentObject.id) } returns documentObject
        return documentObject
    }

    private fun mockImg(image: ImageModel): ImageModel {
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

    private fun aSubject(config: ProjectConfig) = DesignerDocumentObjectBuilder(
        documentObjectRepository,
        textStyleRepository,
        paragraphStyleRepository,
        variableRepository,
        variableStructureRepository,
        displayRuleRepository,
        imageRepository,
        config,
        ipsService,
    )

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
    }
}