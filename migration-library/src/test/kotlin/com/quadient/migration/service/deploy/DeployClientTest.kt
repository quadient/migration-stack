@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.service.deploy

import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.data.DocumentObjectModelRef
import com.quadient.migration.data.ImageModelRef
import com.quadient.migration.data.ParagraphModel
import com.quadient.migration.data.ParagraphStyleModelRef
import com.quadient.migration.data.StatusEvent
import com.quadient.migration.data.TextStyleModelRef
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.repository.ImageInternalRepository
import com.quadient.migration.persistence.repository.ParagraphStyleInternalRepository
import com.quadient.migration.persistence.repository.TextStyleInternalRepository
import com.quadient.migration.service.Storage
import com.quadient.migration.service.inspirebuilder.DesignerDocumentObjectBuilder
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.tools.aActiveStatusEvent
import com.quadient.migration.tools.aDeployedStatusEvent
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.model.aBlock
import com.quadient.migration.tools.model.aImage
import com.quadient.migration.tools.model.aParaStyle
import com.quadient.migration.tools.model.aTextStyle
import com.quadient.migration.tools.shouldBeEqualTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.uuid.ExperimentalUuidApi


class DeployClientTest {
    val documentObjectRepository = mockk<DocumentObjectInternalRepository>()
    val imageRepository = mockk<ImageInternalRepository>()
    val textStyleRepository = mockk<TextStyleInternalRepository>()
    val paragraphStyleRepository = mockk<ParagraphStyleInternalRepository>()
    val statusTrackingRepository = mockk<StatusTrackingRepository>()
    val documentObjectBuilder = mockk<DesignerDocumentObjectBuilder>()
    val ipsService = mockk<IpsService>()
    val storage = mockk<Storage>()

    private val subject = DesignerDeployClient(
        documentObjectRepository,
        imageRepository,
        statusTrackingRepository,
        textStyleRepository,
        paragraphStyleRepository,
        documentObjectBuilder,
        ipsService,
        storage
    )

    @BeforeEach
    fun setup() {
        every { documentObjectBuilder.getProperty("projectConfig") } returns aProjectConfig()
        every { documentObjectBuilder.getDocumentObjectPath(any()) } answers { callOriginal() }
        every { documentObjectBuilder.getImagePath(any()) } answers { callOriginal() }
    }

    @Test
    fun `preflight report correctly handles all cases`() {
        givenNewExternalDocumentObject("1", deps = listOf("2"), textStyles = listOf("3"), paragraphStyles = listOf("3"))
        givenNewExternalDocumentObject("2", deps = listOf("3", "4"))
        givenNewExternalDocumentObject("3", imageDeps = listOf("1", "2", "3"))
        givenInternalDocumentObject("4", deps = listOf("1", "5"))
        givenInternalDocumentObject("5", deps = listOf("6"))
        givenChangedExternalDocumentObject(
            "6", deps = listOf("7"), textStyles = listOf("2"), paragraphStyles = listOf("2")
        )
        givenExistingExternalDocumentObject("7", textStyles = listOf("1"), paragraphStyles = listOf("1"))
        givenNewImage("1")
        givenChangedImage("2")
        givenExistingImage("3")

        val result = subject.preFlightCheck()

        result.items.size.shouldBeEqualTo(16)
        result.shouldContainNewDocumentObject("1")
        result.shouldContainNewDocumentObject("2")
        result.shouldContainNewDocumentObject("3")
        result.shouldContainInlineDocumentObject("4")
        result.shouldContainInlineDocumentObject("5")
        result.shouldContainChangedDocumentObject("6")
        result.shouldContainDependencyDocumentObject("7")
        result.shouldContainNewImage("1")
        result.shouldContainChangedImage("2")
        result.shouldContainDependencyImage("3")
        result.shouldContainTextStyle("1")
        result.shouldContainTextStyle("2")
        result.shouldContainTextStyle("3")
        result.shouldContainParagraphStyle("1")
        result.shouldContainParagraphStyle("2")
        result.shouldContainParagraphStyle("3")
    }

    private fun DeploymentReport.shouldContainTextStyle(id: String) {
        assertEquals(DeployKind.Inline, this.items[Pair(id, ResourceType.TextStyle)]?.deployKind)
    }

    private fun DeploymentReport.shouldContainParagraphStyle(id: String) {
        assertEquals(DeployKind.Inline, this.items[Pair(id, ResourceType.ParagraphStyle)]?.deployKind)
    }

    private fun DeploymentReport.shouldContainNewImage(id: String) {
        assertEquals(DeployKind.New, this.items[Pair(id, ResourceType.Image)]?.deployKind)
    }

    private fun DeploymentReport.shouldContainChangedImage(id: String) {
        assertEquals(DeployKind.Overwrite, this.items[Pair(id, ResourceType.Image)]?.deployKind)
    }

    private fun DeploymentReport.shouldContainDependencyImage(id: String) {
        assertEquals(DeployKind.Reused, this.items[Pair(id, ResourceType.Image)]?.deployKind)
    }

    private fun DeploymentReport.shouldContainNewDocumentObject(id: String) {
        assertEquals(DeployKind.New, this.items[Pair(id, ResourceType.DocumentObject)]?.deployKind)
    }

    private fun DeploymentReport.shouldContainChangedDocumentObject(id: String) {
        assertEquals(DeployKind.Overwrite, this.items[Pair(id, ResourceType.DocumentObject)]?.deployKind)
    }

    private fun DeploymentReport.shouldContainDependencyDocumentObject(id: String) {
        assertEquals(DeployKind.Reused, this.items[Pair(id, ResourceType.DocumentObject)]?.deployKind)
    }

    private fun DeploymentReport.shouldContainInlineDocumentObject(id: String) {
        assertEquals(DeployKind.Inline, this.items[Pair(id, ResourceType.DocumentObject)]?.deployKind)
    }

    private fun givenNewImage(id: String) {
        givenImage(id = id, events = listOf(aActiveStatusEvent()))
    }

    fun givenExistingImage(id: String) {
        givenImage(id = id, events = listOf(aActiveStatusEvent(), aDeployedStatusEvent()))
    }

    private fun givenChangedImage(id: String) {
        givenImage(id = id, events = listOf(aActiveStatusEvent(), aDeployedStatusEvent(), aActiveStatusEvent()))
    }

    private fun givenImage(id: String, events: List<StatusEvent> = listOf()) {
        every { imageRepository.findModelOrFail(id) } returns aImage(id = id)
        every {
            statusTrackingRepository.findEventsRelevantToOutput(
                id, ResourceType.Image, any()
            )
        } returns events
    }

    private fun givenNewExternalDocumentObject(
        id: String,
        deps: List<String> = listOf(),
        imageDeps: List<String> = listOf(),
        textStyles: List<String> = listOf(),
        paragraphStyles: List<String> = listOf(),
    ) {
        givenExternalDocumentObject(
            id = id,
            deps = deps,
            imageDeps = imageDeps,
            textStyles = textStyles,
            paragraphStyles = paragraphStyles,
            events = listOf(aActiveStatusEvent())
        )
    }

    private fun givenChangedExternalDocumentObject(
        id: String,
        deps: List<String> = listOf(),
        imageDeps: List<String> = listOf(),
        textStyles: List<String> = listOf(),
        paragraphStyles: List<String> = listOf(),
    ) {
        givenExternalDocumentObject(
            id = id,
            deps = deps,
            imageDeps = imageDeps,
            textStyles = textStyles,
            paragraphStyles = paragraphStyles,
            events = listOf(aActiveStatusEvent(), aDeployedStatusEvent(), aActiveStatusEvent())
        )
    }

    private fun givenExistingExternalDocumentObject(
        id: String,
        deps: List<String> = listOf(),
        imageDeps: List<String> = listOf(),
        textStyles: List<String> = listOf(),
        paragraphStyles: List<String> = listOf(),
    ) {
        givenExternalDocumentObject(
            id,
            deps,
            imageDeps = imageDeps,
            textStyles = textStyles,
            paragraphStyles = paragraphStyles,
            events = listOf(aActiveStatusEvent(), aDeployedStatusEvent())
        )
    }

    val externalObjects = mutableListOf<DocumentObjectModel>()
    private fun givenExternalDocumentObject(
        id: String,
        deps: List<String> = listOf(),
        imageDeps: List<String> = listOf(),
        textStyles: List<String> = listOf(),
        paragraphStyles: List<String> = listOf(),
        events: List<StatusEvent> = listOf(),
    ) {
        externalObjects.add(
            aBlock(id = id, internal = false, content = deps.map {
                DocumentObjectModelRef(
                    it, null
                )
            } + imageDeps.map { ImageModelRef(it) } + textStyles.map {
                ParagraphModel(
                    listOf(
                        ParagraphModel.TextModel(
                            emptyList(), TextStyleModelRef(it), null
                        )
                    ), null, null
                )
            } + paragraphStyles.map { ParagraphModel(emptyList(), ParagraphStyleModelRef(it), null) })
        )
        every { documentObjectRepository.list(any()) } returns externalObjects
        every {
            statusTrackingRepository.findEventsRelevantToOutput(
                id, ResourceType.DocumentObject, any()
            )
        } returns events
        for (id in textStyles) {
            every { textStyleRepository.findModelOrFail(id) } returns aTextStyle(id = id)
        }
        for (id in paragraphStyles) {
            every { paragraphStyleRepository.findModelOrFail(id) } returns aParaStyle(id)
        }
    }

    private fun givenInternalDocumentObject(id: String, deps: List<String> = listOf()) {
        every { documentObjectRepository.findModelOrFail(id) } returns aBlock(
            id = id, internal = true, content = deps.map { DocumentObjectModelRef(it, null) })
    }
}