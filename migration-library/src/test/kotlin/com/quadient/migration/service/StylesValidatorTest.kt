package com.quadient.migration.service

import com.quadient.migration.Postgres
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.TextStyleBuilder
import com.quadient.migration.service.deploy.DeployClient
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.tools.aDocumentObjectRepository
import com.quadient.migration.tools.aParaStyleRepository
import com.quadient.migration.tools.aTextStyleRepository
import com.quadient.migration.tools.shouldBeEmpty
import com.quadient.migration.tools.shouldBeEqualTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Postgres
class StylesValidatorTest {
    val documentObjectRepository = aDocumentObjectRepository()
    val textStyleRepository = aTextStyleRepository()
    val paraStyleRepository = aParaStyleRepository()

    val ipsService = mockk<IpsService>()
    val documentObjectBuilder = mockk<InspireDocumentObjectBuilder>()
    val deployClient = mockk<DeployClient>()
    val subject = StylesValidator(
        documentObjectRepository = documentObjectRepository,
        textStyleRepository = textStyleRepository,
        paragraphStyleRepository = paraStyleRepository,
        documentObjectBuilder = documentObjectBuilder,
        deployClient = deployClient,
        ipsService = ipsService
    )

    @BeforeEach
    fun init() {
        every { documentObjectBuilder.getStyleDefinitionPath() } returns "somepath"
        every { ipsService.fileExists(any()) } returns true
    }

    @Test
    fun `validates correctly with multiple styles in xml`() {
        every { deployClient.getAllDocumentObjectsToDeploy() } returns listOf(
            DocumentObjectBuilder(
                "id", DocumentObjectType.Block
            ).paragraph { text { string("text").styleRef("not-found-text") }.styleRef("found-para") }.build(),
            DocumentObjectBuilder(
                "id2", DocumentObjectType.Block
            ).paragraph { text { string("text").styleRef("found-text") }.styleRef("not-found-para") }.build(),
            DocumentObjectBuilder("id3", DocumentObjectType.Block).paragraph {
                text {
                    string("text").styleRef("ts3")
                }.styleRef("ps3")
            }.build(),
        )
        paraStyleRepository.upsert(ParagraphStyleBuilder("found-para").build())
        paraStyleRepository.upsert(ParagraphStyleBuilder("not-found-para").build())
        paraStyleRepository.upsert(ParagraphStyleBuilder("ps3").name("Para Display Name").build())
        textStyleRepository.upsert(TextStyleBuilder("found-text").build())
        textStyleRepository.upsert(TextStyleBuilder("not-found-text").build())
        textStyleRepository.upsert(TextStyleBuilder("ts3").name("Text Display Name").build())
        val xml = buildXml(
            textStyles = listOf("found-text", "dummy"),
            paraStyles = listOf("found-para", "dummy"),
            textStylesWithDisplayName = listOf("internal-text" to "Text Display Name"),
            paraStylesWithDisplayName = listOf("internal-para" to "Para Display Name"),
        )
        every { ipsService.wfd2xml(any()) } returns xml

        val result = subject.validateAll()

        result.textStyles.sorted().shouldBeEqualTo(listOf("Text Display Name", "found-text"))
        result.paragraphStyles.sorted().shouldBeEqualTo(listOf("Para Display Name", "found-para"))
        result.missingTextStyles.shouldBeEqualTo(listOf("not-found-text"))
        result.missingParagraphStyles.shouldBeEqualTo(listOf("not-found-para"))
    }

    @Test
    fun `validates correctly with single style in xml`() {
        every { deployClient.getAllDocumentObjectsToDeploy() } returns listOf(
            DocumentObjectBuilder("id", DocumentObjectType.Block).paragraph {
                text { string("text").styleRef("not-found-text") }.styleRef("found-para")
            }.build(),
            DocumentObjectBuilder("id2", DocumentObjectType.Block).paragraph {
                text { string("text").styleRef("found-text") }.styleRef("not-found-para")
            }.build(),
        )
        paraStyleRepository.upsert(ParagraphStyleBuilder("found-para").build())
        paraStyleRepository.upsert(ParagraphStyleBuilder("not-found-para").build())
        textStyleRepository.upsert(TextStyleBuilder("found-text").build())
        textStyleRepository.upsert(TextStyleBuilder("not-found-text").build())
        val xml = buildXml(
            textStyles = listOf("found-text"), paraStyles = listOf("found-para")
        )
        every { ipsService.wfd2xml(any()) } returns xml

        val result = subject.validateAll()

        result.textStyles.shouldBeEqualTo(listOf("found-text"))
        result.paragraphStyles.shouldBeEqualTo(listOf("found-para"))
        result.missingTextStyles.shouldBeEqualTo(listOf("not-found-text"))
        result.missingParagraphStyles.shouldBeEqualTo(listOf("not-found-para"))
    }

    @Test
    fun `resolves style references correctly`() {
        every { deployClient.getAllDocumentObjectsToDeploy() } returns listOf(
            DocumentObjectBuilder("id", DocumentObjectType.Block).paragraph {
                styleRef("para1")
            }.build()
        )
        paraStyleRepository.upsert(ParagraphStyleBuilder("para1").styleRef("para2").build())
        paraStyleRepository.upsert(ParagraphStyleBuilder("para2").styleRef("para3").build())
        paraStyleRepository.upsert(ParagraphStyleBuilder("para3").build())

        val xml = buildXml(textStyles = listOf(), paraStyles = listOf("found-para"))
        every { ipsService.wfd2xml(any()) } returns xml

        val result = subject.validateAll()

        result.textStyles.shouldBeEmpty()
        result.paragraphStyles.shouldBeEmpty()
        result.missingTextStyles.shouldBeEmpty()
        result.missingParagraphStyles.shouldBeEqualTo(listOf("para3"))
    }


    private fun buildXml(
        textStyles: List<String>,
        paraStyles: List<String>,
        textStylesWithDisplayName: List<Pair<String, String>> = emptyList(),
        paraStylesWithDisplayName: List<Pair<String, String>> = emptyList(),
    ): String {
        return """ <WorkFlow version="1.0">
                       <Layout>
                           <Layout>
                                ${textStyles.joinToString("\n") { """<TextStyle><Name>$it</Name><Id>$it</Id></TextStyle>""" }}
                                ${textStylesWithDisplayName.joinToString("\n") { (name, displayName) -> """<TextStyle><Name>$name</Name><CustomProperty>{"DisplayName":"$displayName"}</CustomProperty></TextStyle>""" }}
                                ${paraStyles.joinToString("\n") { """<ParaStyle><Name>$it</Name><Id>$it</Id></ParaStyle>""" }}
                                ${paraStylesWithDisplayName.joinToString("\n") { (name, displayName) -> """<ParaStyle><Name>$name</Name><CustomProperty>{"DisplayName":"$displayName"}</CustomProperty></ParaStyle>""" }}
                           </Layout>
                       </Layout>
                   </WorkFlow>
               """.trimIndent()
    }
}
