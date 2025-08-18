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
import com.quadient.migration.tools.aDeployedStatus
import com.quadient.migration.tools.aDeployedStatusEvent
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.model.aBlock
import com.quadient.migration.tools.model.aImage
import com.quadient.migration.tools.model.aParaStyle
import com.quadient.migration.tools.model.aTextStyle
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeNull
import com.quadient.migration.tools.shouldNotBeNull
import com.quadient.migration.tools.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


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
        every { documentObjectBuilder.getDocumentObjectPath(any(), any(), any()) } answers { callOriginal() }
        every { documentObjectBuilder.getImagePath(any()) } answers { callOriginal() }
        every { documentObjectBuilder.getImagePath(any(), any(), any(), any(), any()) } answers { callOriginal() }
    }

    @Test
    fun `progress report before first deploy`() {
        every { statusTrackingRepository.listAll() } returns emptyList()

        givenNewExternalDocumentObject("1", deps = listOf("2"))
        givenNewExternalDocumentObject("2", deps = listOf("3", "4"))
        givenNewExternalDocumentObject("3", imageDeps = listOf("1", "2", "3"))
        givenInternalDocumentObject("4", deps = listOf("1", "5"))
        givenInternalDocumentObject("5", deps = listOf("6"))
        givenNewExternalDocumentObject("6", deps = listOf("7"))
        givenNewExternalDocumentObject("7", deps = listOf())
        givenNewImage("1")
        givenNewImage("2")
        givenNewImage("3")

        val result = subject.progressReport()

        result.items.size.shouldBeEqualTo(10)
        for (i in arrayOf(1, 2, 3, 6, 7)) {
            result.items[Pair(i.toString(), ResourceType.DocumentObject)].shouldBeNew()
        }
        for (i in arrayOf(4, 5)) {
            result.items[Pair(i.toString(), ResourceType.DocumentObject)].shouldBeInline()
        }
        for (i in arrayOf(1, 2, 3)) {
            result.items[Pair(i.toString(), ResourceType.Image)].shouldBeNew()
        }
    }

    @Test
    fun `progress report after deploy`() {
        val currentDeployTimestamp = Clock.System.now()
        givenNewExternalDocumentObject("1", deps = listOf("2"))
        givenChangedExternalDocumentObject("2", deps = listOf("3", "4"), deployTimestamp = currentDeployTimestamp)
        givenChangedExternalDocumentObject("8", deps = listOf("3", "4"), deployTimestamp = currentDeployTimestamp, icmPath = "icm://other.wfd")
        givenExistingExternalDocumentObject("3", imageDeps = listOf("1", "2", "3"), deployTimestamp = currentDeployTimestamp)
        givenInternalDocumentObject("4", deps = listOf("1", "5"))
        givenInternalDocumentObject("5", deps = listOf("6"))
        givenNewExternalDocumentObject("6", deps = listOf("7"))
        givenNewExternalDocumentObject("7", deps = listOf())
        givenNewImage("1")
        givenChangedImage("2", deployTimestamp = currentDeployTimestamp)
        givenNewImage("3")

        val lastDeployTimestamp = Clock.System.now()
        every { statusTrackingRepository.listAll() } returns listOf(
            aDeployedStatus("random", deploymentId = Uuid.random(), timestamp = lastDeployTimestamp),
        )

        val result = subject.progressReport()

        result.items.size.shouldBeEqualTo(11)
        for (i in arrayOf(1, 6, 7)) {
            result.items[Pair(i.toString(), ResourceType.DocumentObject)].shouldBeNew()
        }
        for (i in arrayOf(2)) {
            result.items[Pair(i.toString(), ResourceType.DocumentObject)].shouldBeOverwrite()
        }
        for (i in arrayOf(3)) {
            result.items[Pair(i.toString(), ResourceType.DocumentObject)].shouldBeKept()
        }
        for (i in arrayOf(4, 5)) {
            result.items[Pair(i.toString(), ResourceType.DocumentObject)].shouldBeInline()
        }
        for (i in arrayOf(1, 3)) {
            result.items[Pair(i.toString(), ResourceType.Image)].shouldBeNew()
        }
        for (i in arrayOf(8)) {
            result.items[Pair(i.toString(), ResourceType.DocumentObject)].shouldBeChangedPath()
        }
    }

    private fun ProgressReportItem?.shouldBeNew() {
        this.shouldNotBeNull()
        this?.nextIcmPath.shouldStartWith("icm://")
        this?.deployKind.shouldBeEqualTo(DeployKind.Create)
        this?.lastStatus.shouldBeEqualTo(LastStatus.None)
        this?.deploymentId.shouldBeNull()
        this?.deployTimestamp.shouldBeNull()
        this?.errorMessage.shouldBeNull()
    }

    private fun ProgressReportItem?.shouldBeInline() {
        this.shouldNotBeNull()
        this?.nextIcmPath.shouldBeNull()
        this?.deployKind.shouldBeEqualTo(DeployKind.Inline)
        this?.lastStatus.shouldBeEqualTo(LastStatus.Inlined)
        this?.deploymentId.shouldBeNull()
        this?.deployTimestamp.shouldBeNull()
        this?.errorMessage.shouldBeNull()
    }

    private fun ProgressReportItem?.shouldBeOverwrite() {
        this.shouldNotBeNull()
        this?.nextIcmPath.shouldStartWith("icm://")
        this?.deployKind.shouldBeEqualTo(DeployKind.Overwrite)
        this!!.lastStatus::class.shouldBeEqualTo(LastStatus.Created::class)
        this?.deploymentId.shouldNotBeNull()
        this?.deployTimestamp.shouldNotBeNull()
        this?.errorMessage.shouldBeNull()
    }

    private fun ProgressReportItem?.shouldBeKept() {
        this.shouldNotBeNull()
        this?.nextIcmPath.shouldStartWith("icm://")
        this?.deployKind.shouldBeEqualTo(DeployKind.Keep)
        this!!.lastStatus::class.shouldBeEqualTo(LastStatus.Created::class)
        this?.deploymentId.shouldNotBeNull()
        this?.deployTimestamp.shouldNotBeNull()
        this?.errorMessage.shouldBeNull()
    }

    private fun ProgressReportItem?.shouldBeChangedPath() {
        this.shouldNotBeNull()
        this?.nextIcmPath.shouldStartWith("icm://")
        this?.deployKind.shouldBeEqualTo(DeployKind.Create)
        this!!.lastStatus::class.shouldBeEqualTo(LastStatus.Created::class)
        this?.deploymentId.shouldNotBeNull()
        this?.deployTimestamp.shouldNotBeNull()
        this?.errorMessage.shouldBeNull()
    }

    private fun givenNewImage(id: String) {
        givenImage(id = id, events = listOf(aActiveStatusEvent()))
    }

    private fun givenChangedImage(id: String, deployTimestamp: Instant) {
        givenImage(id = id, events = listOf(aActiveStatusEvent(timestamp = deployTimestamp - 1.seconds), aDeployedStatusEvent(timestamp = deployTimestamp), aActiveStatusEvent(timestamp = deployTimestamp + 1.seconds)))
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
        deployTimestamp: Instant,
        icmPath: String = "icm://$id.wfd"
    ) {
        givenExternalDocumentObject(
            id = id,
            deps = deps,
            imageDeps = imageDeps,
            textStyles = textStyles,
            paragraphStyles = paragraphStyles,
            events = listOf(aActiveStatusEvent(timestamp = deployTimestamp - 1.seconds), aDeployedStatusEvent(timestamp = deployTimestamp, icmPath = icmPath), aActiveStatusEvent(timestamp = deployTimestamp + 1.seconds))
        )
    }

    private fun givenExistingExternalDocumentObject(
        id: String,
        deps: List<String> = listOf(),
        imageDeps: List<String> = listOf(),
        textStyles: List<String> = listOf(),
        paragraphStyles: List<String> = listOf(),
        deployTimestamp: Instant,
    ) {
        givenExternalDocumentObject(
            id,
            deps,
            imageDeps = imageDeps,
            textStyles = textStyles,
            paragraphStyles = paragraphStyles,
            events = listOf(aActiveStatusEvent(timestamp = deployTimestamp - 1.seconds), aDeployedStatusEvent(timestamp = deployTimestamp, icmPath = "icm://$id.wfd"))
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