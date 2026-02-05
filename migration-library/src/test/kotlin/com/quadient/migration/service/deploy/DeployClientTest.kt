@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.service.deploy

import com.quadient.migration.api.dto.migrationmodel.StatusTracking
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.FileRef
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.Paragraph
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.data.StatusEvent
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.repository.FileInternalRepository
import com.quadient.migration.persistence.repository.ImageInternalRepository
import com.quadient.migration.persistence.repository.ParagraphStyleInternalRepository
import com.quadient.migration.persistence.repository.TextStyleInternalRepository
import com.quadient.migration.service.Storage
import com.quadient.migration.service.inspirebuilder.DesignerDocumentObjectBuilder
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.shared.IcmFileMetadata
import com.quadient.migration.shared.MetadataPrimitive
import com.quadient.migration.shared.MetadataValue
import com.quadient.migration.tools.aActiveStatusEvent
import com.quadient.migration.tools.aDeployedStatus
import com.quadient.migration.tools.aDeployedStatusEvent
import com.quadient.migration.tools.aErrorStatusEvent
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.model.aBlock
import com.quadient.migration.tools.model.aFile
import com.quadient.migration.tools.model.aImage
import com.quadient.migration.tools.model.aParaStyle
import com.quadient.migration.tools.model.aTextStyle
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeNull
import com.quadient.migration.tools.shouldNotBeNull
import com.quadient.migration.tools.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


class DeployClientTest {
    val documentObjectRepository = mockk<DocumentObjectInternalRepository>()
    val imageRepository = mockk<ImageInternalRepository>()
    val fileRepository = mockk<FileInternalRepository>()
    val textStyleRepository = mockk<TextStyleInternalRepository>()
    val paragraphStyleRepository = mockk<ParagraphStyleInternalRepository>()
    val statusTrackingRepository = mockk<StatusTrackingRepository>()
    val documentObjectBuilder = mockk<DesignerDocumentObjectBuilder>()
    val ipsService = mockk<IpsService>()
    val storage = mockk<Storage>()

    private val subject = DesignerDeployClient(
        documentObjectRepository,
        imageRepository,
        fileRepository,
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
        every { documentObjectBuilder.getFilePath(any()) } answers { callOriginal() }
        every { documentObjectBuilder.getFilePath(any(), any(), any(), any(), any()) } answers { callOriginal() }
    }

    @Test
    fun `write metadata post processor`() {
        val doc1 = aBlock(id = "doc1", metadata = mapOf("docMeta" to listOf(MetadataPrimitive.Str("v1"))))
        val doc2 = aBlock(id = "doc2", metadata = mapOf("docMeta" to listOf(MetadataPrimitive.Str("v2"))))
        val img1 = aImage(id = "img2", metadata = mapOf("imgMeta" to listOf(MetadataPrimitive.Str("v3"))))
        val img2 = aImage(id = "img2", metadata = mapOf("imgMeta" to listOf(MetadataPrimitive.Str("v4"))))

        every { documentObjectRepository.findModel("doc1") } returns doc1
        every { documentObjectRepository.findModel("doc2") } returns doc2
        every { imageRepository.findModel("img1") } returns img1
        every { imageRepository.findModel("img2") } returns img2
        every { ipsService.writeMetadata(any()) } returns Unit

        val deploymentResult = DeploymentResult(Uuid.random()).apply {
            deployed.add(DeploymentInfo("doc1", ResourceType.DocumentObject, "icm://doc1.wfd"))
            deployed.add(DeploymentInfo("doc2", ResourceType.DocumentObject, "icm://doc2.wfd"))
            deployed.add(DeploymentInfo("img1", ResourceType.Image, "icm://img1.png"))
            deployed.add(DeploymentInfo("img2", ResourceType.Image, "icm://img2.png"))
        }

        subject.postProcessors[0](deploymentResult)

        //path=icm://doc1.wfd, metadata={docMeta=MetadataValue(values=[Str(value=v1)], system=false)}), IcmFileMetadata(path=icm://doc2.wfd, metadata={docMeta=MetadataValue(values=[Str(value=v2)], system=false)}), IcmFileMetadata(path=icm://img1.png, metadata={imgMeta=MetadataValue(values=[Str(value=v3)], system=false)}), IcmFileMetadata(path=icm://img2.png, metadata={imgMeta=MetadataValue(values=[Str(value=v4)], system=false)})]
        verify {
            ipsService.writeMetadata(
                listOf(
                    IcmFileMetadata(
                        path = "icm://doc1.wfd",
                        metadata = mapOf(
                            "docMeta" to MetadataValue(
                                listOf(MetadataPrimitive.Str("v1")),
                                system = false
                            )
                        )
                    ),
                    IcmFileMetadata(
                        path = "icm://doc2.wfd",
                        metadata = mapOf(
                            "docMeta" to MetadataValue(
                                listOf(MetadataPrimitive.Str("v2")),
                                system = false
                            )
                        )
                    ),
                    IcmFileMetadata(
                        path = "icm://img1.png",
                        metadata = mapOf(
                            "imgMeta" to MetadataValue(
                                listOf(MetadataPrimitive.Str("v3")),
                                system = false
                            )
                        )
                    ),
                    IcmFileMetadata(
                        path = "icm://img2.png",
                        metadata = mapOf(
                            "imgMeta" to MetadataValue(
                                listOf(MetadataPrimitive.Str("v4")),
                                system = false
                            )
                        )
                    ),
                )
            )
        }
    }

    @Test
    fun `progress report before first deploy`() {
        every { statusTrackingRepository.listAll() } returns emptyList()

        givenNewExternalDocumentObject("1", deps = listOf("2"))
        givenNewExternalDocumentObject("2", deps = listOf("3", "4"))
        givenNewExternalDocumentObject("3", imageDeps = listOf("1", "2", "3"), fileDeps = listOf("1", "2"))
        givenInternalDocumentObject("4", deps = listOf("1", "5"))
        givenInternalDocumentObject("5", deps = listOf("6"))
        givenNewExternalDocumentObject("6", deps = listOf("7"))
        givenNewExternalDocumentObject("7", deps = listOf())
        givenNewImage("1")
        givenNewImage("2")
        givenNewImage("3")
        givenNewFile("1")
        givenNewFile("2")

        val result = subject.progressReport()

        result.items.size.shouldBeEqualTo(12)
        for (i in arrayOf(1, 2, 3, 6, 7)) {
            result.items[Pair(i.toString(), ResourceType.DocumentObject)].shouldBeNew()
        }
        for (i in arrayOf(4, 5)) {
            result.items[Pair(i.toString(), ResourceType.DocumentObject)].shouldBeInline()
        }
        for (i in arrayOf(1, 2, 3)) {
            result.items[Pair(i.toString(), ResourceType.Image)].shouldBeNew()
        }
        for (i in arrayOf(1, 2)) {
            result.items[Pair(i.toString(), ResourceType.File)].shouldBeNew()
        }
    }

    @Test
    fun `progress report after deploy`() {
        val currentDeployTimestamp = Clock.System.now()
        val deploymentId = Uuid.random()

        givenNewExternalDocumentObject("1", deps = listOf("2"))
        givenChangedExternalDocumentObject("2", deps = listOf("3", "4"), deployTimestamp = currentDeployTimestamp, deploymentId = deploymentId)
        givenChangedExternalDocumentObject("8", deps = listOf("3", "4"), deployTimestamp = currentDeployTimestamp, icmPath = "icm://other.wfd", deploymentId = deploymentId)
        givenExistingExternalDocumentObject("3", imageDeps = listOf("1", "2", "3"), fileDeps = listOf("1", "2"), deployTimestamp = currentDeployTimestamp, deploymentId = deploymentId)
        givenInternalDocumentObject("4", deps = listOf("1", "5"))
        givenInternalDocumentObject("5", deps = listOf("6"))
        givenNewExternalDocumentObject("6", deps = listOf("7"))
        givenNewExternalDocumentObject("7", deps = listOf())
        givenNewImage("1")
        givenChangedImage("2", deployTimestamp = currentDeployTimestamp, deploymentId = deploymentId)
        givenNewImage("3")
        givenNewFile("1")
        givenChangedFile("2", deployTimestamp = currentDeployTimestamp, deploymentId = deploymentId)

        every { statusTrackingRepository.listAll() } returns listOf(
            aDeployedStatus("random", deploymentId = deploymentId, timestamp = currentDeployTimestamp),
        )

        val result = subject.progressReport()

        result.items.size.shouldBeEqualTo(13)
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
        for (i in arrayOf(1)) {
            result.items[Pair(i.toString(), ResourceType.File)].shouldBeNew()
        }
        for (i in arrayOf(8)) {
            result.items[Pair(i.toString(), ResourceType.DocumentObject)].shouldBeChangedPath()
        }
    }

    @Test
    fun `progress report status complex scenario`() {
        val scenarioStart = Clock.System.now() - 2.hours

        // Step 1: pre-deploy report
        val aBlockEvents = mutableListOf(aActiveStatusEvent(scenarioStart))
        val bBlockEvents = mutableListOf(aActiveStatusEvent(scenarioStart))
        updateMocks(aBlockEvents, bBlockEvents)

        val preDeployReport = subject.progressReport()

        preDeployReport.items.forEach {
            it.value.lastStatus::class.shouldBeEqualTo(LastStatus.None::class)
            it.value.deployKind.shouldBeEqualTo(DeployKind.Create)
        }

        // Step 2: report with A deployed and B error
        val firstDeploymentId = Uuid.random()
        val firstDeploymentTimestamp = scenarioStart + 1.minutes
        aBlockEvents.add(aDeployedStatusEvent(firstDeploymentId, "icm://block_A.wfd", firstDeploymentTimestamp))
        bBlockEvents.add(
            aErrorStatusEvent(
                firstDeploymentId, "icm://block_B.wfd", firstDeploymentTimestamp, error = "Paragraph not found!"
            )
        )
        updateMocks(aBlockEvents, bBlockEvents)

        val firstDeploymentReport = subject.progressReport()
        firstDeploymentReport.items[Pair("block_A", ResourceType.DocumentObject)].apply {
            this.shouldNotBeNull()
            this!!.lastStatus::class.shouldBeEqualTo(LastStatus.Created::class)
            this.deployKind.shouldBeEqualTo(DeployKind.Keep)
        }
        firstDeploymentReport.items[Pair("block_B", ResourceType.DocumentObject)].apply {
            this.shouldNotBeNull()
            this!!.lastStatus::class.shouldBeEqualTo(LastStatus.Error::class)
            this.errorMessage.shouldBeEqualTo("Paragraph not found!")
            this.deployKind.shouldBeEqualTo(DeployKind.Create)
        }

        // Step 3: report with B successfully redeployed
        val secondDeploymentId = Uuid.random()
        val secondDeploymentTimestamp = scenarioStart + 5.minutes
        bBlockEvents.add(aDeployedStatusEvent(secondDeploymentId, "icm://block_B.wfd", secondDeploymentTimestamp))
        updateMocks(aBlockEvents, bBlockEvents)

        val secondDeploymentReport = subject.progressReport()
        secondDeploymentReport.items[Pair("block_A", ResourceType.DocumentObject)].apply {
            this.shouldNotBeNull()
            this!!.lastStatus::class.shouldBeEqualTo(LastStatus.Unchanged::class)
            this.deployKind.shouldBeEqualTo(DeployKind.Keep)
        }
        secondDeploymentReport.items[Pair("block_B", ResourceType.DocumentObject)].apply {
            this.shouldNotBeNull()
            this!!.lastStatus::class.shouldBeEqualTo(LastStatus.Created::class)
            this.deployKind.shouldBeEqualTo(DeployKind.Keep)
        }

        // Step 4: activate all
        aBlockEvents.add(aActiveStatusEvent(scenarioStart + 10.minutes))
        bBlockEvents.add(aActiveStatusEvent(scenarioStart + 10.minutes))
        updateMocks(aBlockEvents, bBlockEvents)

        val activateAllReport = subject.progressReport()
        activateAllReport.items[Pair("block_A", ResourceType.DocumentObject)].apply {
            this.shouldNotBeNull()
            this!!.lastStatus::class.shouldBeEqualTo(LastStatus.Unchanged::class)
            this.deployKind.shouldBeEqualTo(DeployKind.Overwrite)
        }
        activateAllReport.items[Pair("block_B", ResourceType.DocumentObject)].apply {
            this.shouldNotBeNull()
            this!!.lastStatus::class.shouldBeEqualTo(LastStatus.Created::class)
            this.deployKind.shouldBeEqualTo(DeployKind.Overwrite)
        }

        // Step 5: report with A error and B deployed for second time
        val thirdDeploymentId = Uuid.random()
        val thirdDeploymentTimestamp = scenarioStart + 30.minutes
        aBlockEvents.add(
            aErrorStatusEvent(
                thirdDeploymentId, "icm://block_A.wfd", thirdDeploymentTimestamp, error = "Connection lost"
            )
        )
        bBlockEvents.add(aDeployedStatusEvent(thirdDeploymentId, "icm://block_B.wfd", thirdDeploymentTimestamp))
        updateMocks(aBlockEvents, bBlockEvents)

        val thirdDeploymentReport = subject.progressReport()
        thirdDeploymentReport.items[Pair("block_A", ResourceType.DocumentObject)].apply {
            this.shouldNotBeNull()
            this!!.lastStatus::class.shouldBeEqualTo(LastStatus.Error::class)
            this.errorMessage.shouldBeEqualTo("Connection lost")
            this.deployKind.shouldBeEqualTo(DeployKind.Overwrite)
        }
        thirdDeploymentReport.items[Pair("block_B", ResourceType.DocumentObject)].apply {
            this.shouldNotBeNull()
            this!!.lastStatus::class.shouldBeEqualTo(LastStatus.Overwritten::class)
            this.deployKind.shouldBeEqualTo(DeployKind.Keep)
        }
    }

    private fun updateMocks(aBlockEvents: List<StatusEvent>, bBlockEvents: List<StatusEvent>) {
        givenExternalDocumentObject("block_A", events = aBlockEvents)
        givenExternalDocumentObject("block_B", events = bBlockEvents)

        every { statusTrackingRepository.listAll() } returns listOf(
            StatusTracking("block_A", "project", ResourceType.DocumentObject, aBlockEvents),
            StatusTracking("block_B", "project", ResourceType.DocumentObject, bBlockEvents),
        )
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
        this.deploymentId.shouldNotBeNull()
        this.deployTimestamp.shouldNotBeNull()
        this.errorMessage.shouldBeNull()
    }

    private fun ProgressReportItem?.shouldBeKept() {
        this.shouldNotBeNull()
        this?.nextIcmPath.shouldStartWith("icm://")
        this?.deployKind.shouldBeEqualTo(DeployKind.Keep)
        this!!.lastStatus::class.shouldBeEqualTo(LastStatus.Created::class)
        this.deploymentId.shouldNotBeNull()
        this.deployTimestamp.shouldNotBeNull()
        this.errorMessage.shouldBeNull()
    }

    private fun ProgressReportItem?.shouldBeChangedPath() {
        this.shouldNotBeNull()
        this?.nextIcmPath.shouldStartWith("icm://")
        this?.deployKind.shouldBeEqualTo(DeployKind.Create)
        this!!.lastStatus::class.shouldBeEqualTo(LastStatus.Created::class)
        this.deploymentId.shouldNotBeNull()
        this.deployTimestamp.shouldNotBeNull()
        this.errorMessage.shouldBeNull()
    }

    private fun givenNewImage(id: String) {
        givenImage(id = id, events = listOf(aActiveStatusEvent(Clock.System.now() - 1.hours)))
    }

    private fun givenChangedImage(id: String, deployTimestamp: Instant, deploymentId: Uuid) {
        givenImage(
            id = id, events = listOf(
                aActiveStatusEvent(timestamp = deployTimestamp - 1.seconds),
                aDeployedStatusEvent(deploymentId, timestamp = deployTimestamp),
                aActiveStatusEvent(timestamp = deployTimestamp + 1.seconds)
            )
        )
    }

    private fun givenImage(id: String, events: List<StatusEvent> = listOf()) {
        every { imageRepository.findModelOrFail(id) } returns aImage(id = id)
        every {
            statusTrackingRepository.findEventsRelevantToOutput(
                id, ResourceType.Image, any()
            )
        } returns events
    }

    private fun givenNewFile(id: String) {
        givenFile(id = id, events = listOf(aActiveStatusEvent(Clock.System.now() - 1.hours)))
    }

    private fun givenChangedFile(id: String, deployTimestamp: Instant, deploymentId: Uuid) {
        givenFile(
            id = id, events = listOf(
                aActiveStatusEvent(timestamp = deployTimestamp - 1.seconds),
                aDeployedStatusEvent(deploymentId, timestamp = deployTimestamp),
                aActiveStatusEvent(timestamp = deployTimestamp + 1.seconds)
            )
        )
    }

    private fun givenFile(id: String, events: List<StatusEvent> = listOf()) {
        every { fileRepository.findModelOrFail(id) } returns aFile(id = id)
        every {
            statusTrackingRepository.findEventsRelevantToOutput(
                id, ResourceType.File, any()
            )
        } returns events
    }

    private fun givenNewExternalDocumentObject(
        id: String,
        deps: List<String> = listOf(),
        imageDeps: List<String> = listOf(),
        fileDeps: List<String> = listOf(),
        textStyles: List<String> = listOf(),
        paragraphStyles: List<String> = listOf(),
    ) {
        givenExternalDocumentObject(
            id = id,
            deps = deps,
            imageDeps = imageDeps,
            fileDeps = fileDeps,
            textStyles = textStyles,
            paragraphStyles = paragraphStyles,
            events = listOf(aActiveStatusEvent(Clock.System.now() - 1.hours))
        )
    }

    private fun givenChangedExternalDocumentObject(
        id: String,
        deps: List<String> = listOf(),
        imageDeps: List<String> = listOf(),
        fileDeps: List<String> = listOf(),
        textStyles: List<String> = listOf(),
        paragraphStyles: List<String> = listOf(),
        deployTimestamp: Instant,
        deploymentId: Uuid,
        icmPath: String = "icm://$id.wfd"
    ) {
        givenExternalDocumentObject(
            id = id,
            deps = deps,
            imageDeps = imageDeps,
            fileDeps = fileDeps,
            textStyles = textStyles,
            paragraphStyles = paragraphStyles,
            events = listOf(aActiveStatusEvent(timestamp = deployTimestamp - 1.hours), aDeployedStatusEvent(deploymentId, icmPath, deployTimestamp), aActiveStatusEvent(timestamp = deployTimestamp + 1.seconds))
        )
    }

    private fun givenExistingExternalDocumentObject(
        id: String,
        deps: List<String> = listOf(),
        imageDeps: List<String> = listOf(),
        fileDeps: List<String> = listOf(),
        textStyles: List<String> = listOf(),
        paragraphStyles: List<String> = listOf(),
        deployTimestamp: Instant,
        deploymentId: Uuid,
    ) {
        givenExternalDocumentObject(
            id,
            deps,
            imageDeps = imageDeps,
            fileDeps = fileDeps,
            textStyles = textStyles,
            paragraphStyles = paragraphStyles, events = listOf(
                aActiveStatusEvent(timestamp = deployTimestamp - 1.seconds),
                aDeployedStatusEvent(deploymentId, "icm://$id.wfd", deployTimestamp)
            )
        )
    }

    val externalObjects = mutableListOf<DocumentObject>()
    private fun givenExternalDocumentObject(
        id: String,
        deps: List<String> = listOf(),
        imageDeps: List<String> = listOf(),
        fileDeps: List<String> = listOf(),
        textStyles: List<String> = listOf(),
        paragraphStyles: List<String> = listOf(),
        events: List<StatusEvent> = listOf(),
    ) {
        externalObjects.add(
            aBlock(id = id, internal = false, content = deps.map {
                DocumentObjectRef(
                    it, null
                )
            } + imageDeps.map { ImageRef(it) } + fileDeps.map { FileRef(it) } + textStyles.map {
                Paragraph(
                    listOf(
                        Paragraph.Text(
                            emptyList(), TextStyleRef(it), null
                        )
                    ), null, null
                )
            } + paragraphStyles.map { Paragraph(emptyList(), ParagraphStyleRef(it), null) })
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
            id = id, internal = true, content = deps.map { DocumentObjectRef(it, null) })
    }
}

