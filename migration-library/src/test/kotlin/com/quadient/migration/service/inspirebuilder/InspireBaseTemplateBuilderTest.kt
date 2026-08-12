package com.quadient.migration.service.inspirebuilder

import com.quadient.migration.api.dto.migrationmodel.builder.BaseTemplateBuilder
import com.quadient.migration.shared.millimeters
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeNull
import org.junit.jupiter.api.Test
import tools.jackson.dataformat.xml.XmlMapper
import tools.jackson.module.kotlin.KotlinModule

class InspireBaseTemplateBuilderTest {
    private val subject = InspireBaseTemplateBuilder()
    private val xmlMapper = XmlMapper.builder().addModule(KotlinModule.Builder().build()).build()

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
        val flowId = "Def.InteractiveFlow0"

        val flowAreaId = result["FlowArea"].first { it["FlowId"]?.stringValue() == flowId }["Id"].stringValue()
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
        result["Flow"].size().shouldBeEqualTo(3)
        result["FlowArea"].filter { it["FlowId"] != null }.size.shouldBeEqualTo(3)
        result["Pages"]["InteractiveFlow"].size().shouldBeEqualTo(3)
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
