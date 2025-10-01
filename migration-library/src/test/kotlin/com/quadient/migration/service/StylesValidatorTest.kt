package com.quadient.migration.service

import com.quadient.migration.Postgres
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ParagraphStyleRepository
import com.quadient.migration.api.repository.TextStyleRepository
import com.quadient.migration.data.ParagraphStyleModelRef
import com.quadient.migration.data.TextStyleModelRef
import com.quadient.migration.service.deploy.DeployClient
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.tools.aParagraphStyle
import com.quadient.migration.tools.aTextStyle
import com.quadient.migration.tools.model.aBlock
import com.quadient.migration.tools.model.aDocumentObjectInternalRepository
import com.quadient.migration.tools.model.aParaStyleInternalRepository
import com.quadient.migration.tools.model.aParagraph
import com.quadient.migration.tools.model.aText
import com.quadient.migration.tools.model.aTextStyleInternalRepository
import com.quadient.migration.tools.shouldBeEmpty
import com.quadient.migration.tools.shouldBeEqualTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Postgres
class StylesValidatorTest {
    val documentObjectInternalRepository = aDocumentObjectInternalRepository()
    val textStyleInternalRepository = aTextStyleInternalRepository()
    val paraStyleInternalRepository = aParaStyleInternalRepository()
    val textStyleRepository = TextStyleRepository(textStyleInternalRepository)
    val paraStyleRepository = ParagraphStyleRepository(paraStyleInternalRepository)

    val ipsService = mockk<IpsService>()
    val documentObjectBuilder = mockk<InspireDocumentObjectBuilder>()
    val deployClient = mockk<DeployClient>()
    val subject = StylesValidator(
        documentObjectRepository = documentObjectInternalRepository,
        textStyleRepository = textStyleInternalRepository,
        paragraphStyleRepository = paraStyleInternalRepository,
        documentObjectBuilder = documentObjectBuilder,
        deployClient = deployClient,
        ipsService = ipsService
    )

    @BeforeEach
    fun init() {
        every { documentObjectBuilder.getStyleDefinitionPath() } returns "somepath"
    }

    @Test
    fun `validates correctly with multiple styles in xml`() {
        every { deployClient.getAllDocumentObjectsToDeploy() } returns listOf(
            aBlock(
                id = "id", content = listOf(
                    aParagraph(
                        styleRef = ParagraphStyleModelRef("found-para"),
                        content = listOf(aText(content = listOf(), styleRef = TextStyleModelRef("not-found-text")))
                    )
                )
            ),
            aBlock(
                id = "id2", content = listOf(
                    aParagraph(
                        styleRef = ParagraphStyleModelRef("not-found-para"),
                        content = listOf(aText(content = listOf(), styleRef = TextStyleModelRef("found-text")))
                    )
                )
            )
        )
        paraStyleRepository.upsert(aParagraphStyle(id = "found-para"))
        paraStyleRepository.upsert(aParagraphStyle(id = "not-found-para"))
        textStyleRepository.upsert(aTextStyle(id = "found-text"))
        textStyleRepository.upsert(aTextStyle(id = "not-found-text"))
        val xml = buildXml(
            textStyles = listOf("stylefound-text", "dummy"), paraStyles = listOf("stylefound-para", "dummy")
        )
        every { ipsService.wfd2xml(any()) } returns xml

        val result = subject.validateAll()

        result.textStyles.shouldBeEqualTo(listOf("stylefound-text"))
        result.paragraphStyles.shouldBeEqualTo(listOf("stylefound-para"))
        result.missingTextStyles.shouldBeEqualTo(listOf("stylenot-found-text"))
        result.missingParagraphStyles.shouldBeEqualTo(listOf("stylenot-found-para"))
    }

    @Test
    fun `validates correctly with single style in xml`() {
        every { deployClient.getAllDocumentObjectsToDeploy() } returns listOf(
            aBlock(
                id = "id", content = listOf(
                    aParagraph(
                        styleRef = ParagraphStyleModelRef("found-para"),
                        content = listOf(aText(content = listOf(), styleRef = TextStyleModelRef("not-found-text")))
                    )
                )
            ),
            aBlock(
                id = "id2", content = listOf(
                    aParagraph(
                        styleRef = ParagraphStyleModelRef("not-found-para"),
                        content = listOf(aText(content = listOf(), styleRef = TextStyleModelRef("found-text")))
                    )
                )
            )
        )
        paraStyleRepository.upsert(aParagraphStyle(id = "found-para"))
        paraStyleRepository.upsert(aParagraphStyle(id = "not-found-para"))
        textStyleRepository.upsert(aTextStyle(id = "found-text"))
        textStyleRepository.upsert(aTextStyle(id = "not-found-text"))
        val xml = buildXml(
            textStyles = listOf("stylefound-text"), paraStyles = listOf("stylefound-para")
        )
        every { ipsService.wfd2xml(any()) } returns xml

        val result = subject.validateAll()

        result.textStyles.shouldBeEqualTo(listOf("stylefound-text"))
        result.paragraphStyles.shouldBeEqualTo(listOf("stylefound-para"))
        result.missingTextStyles.shouldBeEqualTo(listOf("stylenot-found-text"))
        result.missingParagraphStyles.shouldBeEqualTo(listOf("stylenot-found-para"))
    }

    @Test
    fun `resolves style references correctly`() {
        every { deployClient.getAllDocumentObjectsToDeploy() } returns listOf(
            aBlock(
                id = "id", content = listOf(aParagraph(styleRef = ParagraphStyleModelRef("para1")))
            )
        )
        paraStyleRepository.upsert(aParagraphStyle(id = "para1", definition = ParagraphStyleRef("para2")))
        paraStyleRepository.upsert(aParagraphStyle(id = "para2", definition = ParagraphStyleRef("para3")))
        paraStyleRepository.upsert(aParagraphStyle(id = "para3"))

        val xml = buildXml(textStyles = listOf(), paraStyles = listOf("stylefound-para"))
        every { ipsService.wfd2xml(any()) } returns xml

        val result = subject.validateAll()

        result.textStyles.shouldBeEmpty()
        result.paragraphStyles.shouldBeEmpty()
        result.missingTextStyles.shouldBeEmpty()
        result.missingParagraphStyles.shouldBeEqualTo(listOf("stylepara3"))
    }


    private fun buildXml(textStyles: List<String>, paraStyles: List<String>): String {
        return """ <WorkFlow version="1.0">
                       <Layout>
                           <Layout>
                                ${textStyles.joinToString("\n") { """<TextStyle><Name>$it</Name><Id>$it</Id></TextStyle>""" }}
                                ${paraStyles.joinToString("\n") { """<ParaStyle><Name>$it</Name><Id>$it</Id></ParaStyle>""" }}
                           </Layout>
                       </Layout>
                   </WorkFlow>
               """.trimIndent()
    }
}