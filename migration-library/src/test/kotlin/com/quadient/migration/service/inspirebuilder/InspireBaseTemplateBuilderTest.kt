package com.quadient.migration.service.inspirebuilder

import com.quadient.migration.api.dto.migrationmodel.builder.BaseTemplateBuilder
import com.quadient.migration.api.repository.BaseTemplateRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.service.InteractiveIcmDataCache
import com.quadient.migration.service.InteractiveResourcePathProvider
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.millimeters
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.model.aDocObj
import com.quadient.migration.tools.model.aDocumentObjectRef
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeNull
import com.quadient.migration.tools.shouldNotBeNull
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.dataformat.xml.XmlMapper
import tools.jackson.module.kotlin.KotlinModule

class InspireBaseTemplateBuilderTest {
    private val ipsService = mockk<IpsService>()
    private val config = aProjectConfig()
    private val resourcePathProvider = InteractiveResourcePathProvider(config)
    private val icmDataCache = InteractiveIcmDataCache(ipsService, resourcePathProvider)
    private val baseTemplateRepository = mockk<BaseTemplateRepository>()
    private val documentObjectRepository = mockk<DocumentObjectRepository>()
    private val subject = InspireBaseTemplateBuilder(
        config, icmDataCache, resourcePathProvider, baseTemplateRepository, documentObjectRepository
    )
    private val xmlMapper = XmlMapper.builder().addModule(KotlinModule.Builder().build()).build()

    @BeforeEach
    fun setUp() {
        every { ipsService.wfd2xml(any<IcmPath>()) } returns """
        <Workflow>
          <Layout>
            <Id>Layout1</Id>
            <Name>Layout1</Name>
            <Layout>
            </Layout>
          </Layout>
        </Workflow>
        """.trimIndent()
        every { ipsService.fileExists(any<IcmPath>()) } returns false
        every { ipsService.gatherFontData(any()) } returns "Arial,Regular,icm://Fonts/arial.ttf;"
        every { baseTemplateRepository.findUsages(any()) } returns emptyList()
    }

    @Test
    fun `buildBaseTemplate sets external styles layout when style definition exists`() {
        // given
        every { ipsService.fileExists(any<IcmPath>()) } returns true
        val baseTemplate = BaseTemplateBuilder("BT_1").build()

        // when
        val result = subject.buildBaseTemplate(baseTemplate).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val styleDefinitionPath = resourcePathProvider.getStyleDefinitionPath()
        result["Root"]["ExternalStylesLayout"].stringValue().shouldBeEqualTo("VCSLocation,$styleDefinitionPath")
    }

    @Test
    fun `buildBaseTemplate does not set external styles layout when style definition does not exist`() {
        // given
        every { ipsService.fileExists(any<IcmPath>()) } returns false
        val baseTemplate = BaseTemplateBuilder("BT_1").build()

        // when
        val result = subject.buildBaseTemplate(baseTemplate).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        result["Root"]["ExternalStylesLayout"].shouldBeNull()
    }

    @Test
    fun `buildBaseTemplate redirects Arial font to ICM location`() {
        // given
        val baseTemplate = BaseTemplateBuilder("BT_1").build()

        // when
        val result = subject.buildBaseTemplate(baseTemplate).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        result["Font"]["SubFont"]["FontLocation"].stringValue().shouldBeEqualTo("VCSLocation,icm://Fonts/arial.ttf")
    }

    @Test
    fun `buildBaseTemplate creates page with name and size`() {
        // given
        val baseTemplate = BaseTemplateBuilder("BT_1").addPage {
            name("Page 1")
            pageSize(210.millimeters(), 297.millimeters())
        }.build()

        // when
        val result = subject.buildBaseTemplate(baseTemplate).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val pageId = result["Page"].first { it["Name"].stringValue() == "Page 1" }["Id"].stringValue()
        val pageData = result["Page"].last { it["Id"].stringValue() == pageId }
        pageData["Width"].stringValue().shouldBeEqualTo(210.millimeters().toMeters().toString())
        pageData["Height"].stringValue().shouldBeEqualTo(297.millimeters().toMeters().toString())
    }

    @Test
    fun `buildBaseTemplate creates interactive flow and flow area per area`() {
        // given
        val baseTemplate = BaseTemplateBuilder("BT_1").addPage {
            name("Page 1")
            addArea("Body") {
                flowToNextPage(true)
                position {
                    left(10.millimeters())
                    top(20.millimeters())
                    width(180.millimeters())
                    height(250.millimeters())
                }
            }
        }.build()

        // when
        val result = subject.buildBaseTemplate(baseTemplate).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val flowId = result["Flow"].first { it["Name"]?.stringValue() == "Body" }["Id"].stringValue()

        val flowAreaId = result["FlowArea"].first { it["FlowId"]?.stringValue() == flowId }["Id"].stringValue()
        val flowAreaStub = result["FlowArea"].first { it["Id"].stringValue() == flowAreaId && it["Name"] != null }
        flowAreaStub["Name"].stringValue().shouldBeEqualTo("BodyArea")
        val flowArea = result["FlowArea"].last { it["Id"].stringValue() == flowAreaId }
        flowArea["FlowingToNextPage"].stringValue().shouldBeEqualTo("True")
        flowArea["Pos"]["X"].stringValue().shouldBeEqualTo(10.millimeters().toMeters().toString())
        flowArea["Pos"]["Y"].stringValue().shouldBeEqualTo(20.millimeters().toMeters().toString())
        flowArea["Size"]["X"].stringValue().shouldBeEqualTo(180.millimeters().toMeters().toString())
        flowArea["Size"]["Y"].stringValue().shouldBeEqualTo(250.millimeters().toMeters().toString())

        result["Pages"]["MainFlow"].stringValue().shouldBeEqualTo(flowId)
        result["Pages"]["InteractiveFlow"]["FlowId"].stringValue().shouldBeEqualTo(flowId)
    }

    @Test
    fun `buildBaseTemplate with multiple pages and areas creates one interactive flow per area`() {
        // given
        val baseTemplate = BaseTemplateBuilder("BT_1")
            .addPage { name("Page 1"); addArea("Header"); addArea("Body") }
            .addPage { name("Page 2"); addArea("Footer") }
            .build()

        // when
        val result = subject.buildBaseTemplate(baseTemplate).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        result["Page"].filter { it["Name"] != null }.size.shouldBeEqualTo(2)
        result["Flow"].filter { it["Name"] != null }.size.shouldBeEqualTo(3)
        result["FlowArea"].filter { it["FlowId"] != null }.size.shouldBeEqualTo(3)
        result["Pages"]["InteractiveFlow"].size().shouldBeEqualTo(3)
    }

    @Test
    fun `buildBaseTemplate picks the largest area as the main flow`() {
        // given
        val baseTemplate = BaseTemplateBuilder("BT_1")
            .addPage {
                name("Page 1")
                addArea("Header") {
                    position { left(0.millimeters()); top(0.millimeters()); width(210.millimeters()); height(20.millimeters()) }
                }
                addArea("Body") {
                    position { left(0.millimeters()); top(20.millimeters()); width(210.millimeters()); height(250.millimeters()) }
                }
            }
            .build()

        // when
        val result = subject.buildBaseTemplate(baseTemplate).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val bodyFlowId = result["Flow"].first { it["Name"]?.stringValue() == "Body" }["Id"].stringValue()
        result["Pages"]["MainFlow"].stringValue().shouldBeEqualTo(bodyFlowId)
    }

    @Test
    fun `buildBaseTemplate picks the largest area on the first page only as the main flow`() {
        // given
        val baseTemplate = BaseTemplateBuilder("BT_1")
            .addPage {
                name("Title page")
                addArea("Title") {
                    position { left(0.millimeters()); top(0.millimeters()); width(210.millimeters()); height(297.millimeters()) }
                }
            }
            .addPage {
                name("Page 2")
                addArea("Header") {
                    position { left(0.millimeters()); top(0.millimeters()); width(210.millimeters()); height(20.millimeters()) }
                }
                addArea("Body") {
                    position { left(0.millimeters()); top(20.millimeters()); width(210.millimeters()); height(250.millimeters()) }
                }
            }
            .build()

        // when
        val result = subject.buildBaseTemplate(baseTemplate).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        val titleFlowId = result["Flow"].first { it["Name"]?.stringValue() == "Title" }["Id"].stringValue()
        result["Pages"]["MainFlow"].stringValue().shouldBeEqualTo(titleFlowId)
    }

    @Test
    fun `buildBaseTemplate with page without areas creates no flow`() {
        // given
        val baseTemplate = BaseTemplateBuilder("BT_1").addPage { name("Page 1") }.build()

        // when
        val result = subject.buildBaseTemplate(baseTemplate).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        result["Flow"].shouldBeNull()
        result["FlowArea"].shouldBeNull()
        result["Page"].first()["Name"].stringValue().shouldBeEqualTo("Page 1")
    }

    @Test
    fun `buildBaseTemplate registers SMS root when a usage references an Sms document object`() {
        // given
        val baseTemplate = BaseTemplateBuilder("BT_1").build()
        val smsModel = aDocObj("Sms_1", DocumentObjectType.Sms)
        val template = aDocObj("T_1", DocumentObjectType.Template, content = listOf(aDocumentObjectRef(smsModel.id)))
        every { baseTemplateRepository.findUsages(baseTemplate.id) } returns listOf(template)
        every { documentObjectRepository.find(smsModel.id) } returns smsModel

        // when
        val result = subject.buildBaseTemplate(baseTemplate).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        result["SMSRoot"]["FlowId"].shouldNotBeNull()
        result["Pages"]["InteractiveFlow"]["FlowType"].stringValue().shouldBeEqualTo("Normal")
        result["ECPlaceHolder"].shouldBeNull()
    }

    @Test
    fun `buildBaseTemplate registers email component root and HTML interactive flow when a usage references an Email document object`() {
        // given
        val baseTemplate = BaseTemplateBuilder("BT_1").build()
        val emailModel = aDocObj("Email_1", DocumentObjectType.Email)
        val template = aDocObj("T_1", DocumentObjectType.Template, content = listOf(aDocumentObjectRef(emailModel.id)))
        every { baseTemplateRepository.findUsages(baseTemplate.id) } returns listOf(template)
        every { documentObjectRepository.find(emailModel.id) } returns emailModel

        // when
        val result = subject.buildBaseTemplate(baseTemplate).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        result["ECRoot"].shouldNotBeNull()
        result["ECPlaceHolder"]["Id"].stringValue().shouldBeEqualTo("Def.EmailsBody")
        result["Pages"]["InteractiveFlow"]["FlowType"].stringValue().shouldBeEqualTo("HTML")
        result["SMSRoot"].shouldBeNull()
    }

    @Test
    fun `buildBaseTemplate registers neither email nor sms modules when no usage references them`() {
        // given
        val baseTemplate = BaseTemplateBuilder("BT_1").build()
        val block = aDocObj("B_1", DocumentObjectType.Block)
        val template = aDocObj("T_1", DocumentObjectType.Template, content = listOf(aDocumentObjectRef(block.id)))
        every { baseTemplateRepository.findUsages(baseTemplate.id) } returns listOf(template)
        every { documentObjectRepository.find(block.id) } returns block

        // when
        val result = subject.buildBaseTemplate(baseTemplate).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        result["SMSRoot"].shouldBeNull()
        result["ECRoot"].shouldBeNull()
    }

    @Test
    fun `buildBaseTemplate without pages creates empty layout`() {
        // given
        val baseTemplate = BaseTemplateBuilder("BT_1").build()

        // when
        val result = subject.buildBaseTemplate(baseTemplate).let { xmlMapper.readTree(it.trimIndent()) }["Layout"]["Layout"]

        // then
        result["Page"].shouldBeNull()
        result["Flow"].shouldBeNull()
    }
}
