@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.service.deploy

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.dto.migrationmodel.Attachment
import com.quadient.migration.api.dto.migrationmodel.AttachmentRef
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.data.Active
import com.quadient.migration.data.Deployed
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.data.Error
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.ResourceId
import com.quadient.migration.api.dto.migrationmodel.StatusTracking
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.builder.AttachmentBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ImageBuilder
import com.quadient.migration.api.repository.AttachmentRepository
import com.quadient.migration.api.repository.DisplayRuleRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ImageRepository
import com.quadient.migration.api.repository.ParagraphStyleRepository
import com.quadient.migration.api.repository.TextStyleRepository
import com.quadient.migration.api.repository.VariableRepository
import com.quadient.migration.api.repository.VariableStructureRepository
import com.quadient.migration.data.StatusEvent
import com.quadient.migration.service.Storage
import com.quadient.migration.service.deploy.utility.DeploymentError
import com.quadient.migration.service.deploy.utility.DeploymentInfo
import com.quadient.migration.service.deploy.utility.DeploymentResult
import com.quadient.migration.service.deploy.utility.MetadataValidator
import com.quadient.migration.service.deploy.utility.ResourceType
import com.quadient.migration.service.deploy.utility.ResultTrackerImpl
import com.quadient.migration.service.deploy.utility.ValidationResult
import com.quadient.migration.service.inspirebuilder.DesignerDocumentObjectBuilder
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.service.ipsclient.OperationResult
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.DocumentObjectType.Block
import com.quadient.migration.shared.DocumentObjectType.Template
import com.quadient.migration.shared.IcmFileMetadata
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.MetadataPrimitive
import com.quadient.migration.shared.SkipOptions
import com.quadient.migration.shared.toIcmPath
import com.quadient.migration.tools.aActiveStatus
import com.quadient.migration.tools.aDeployedStatus
import com.quadient.migration.tools.aErrorStatus
import com.quadient.migration.tools.computeIfPresentOrPut
import com.quadient.migration.tools.model.aBlock
import com.quadient.migration.tools.model.aDocObj
import com.quadient.migration.tools.model.aDocumentObjectRef
import com.quadient.migration.tools.model.aAttachment
import com.quadient.migration.tools.model.aImage
import com.quadient.migration.tools.model.aParagraph
import com.quadient.migration.tools.model.aTemplate
import com.quadient.migration.tools.model.aText
import com.quadient.migration.tools.shouldBeEqualTo
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import kotlin.time.Clock
import org.jetbrains.exposed.v1.core.Op
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test
import kotlin.ByteArray
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class DesignerDeployClientTest {
    val documentObjectRepository = mockk<DocumentObjectRepository>()
    val imageRepository = mockk<ImageRepository>()
    val attachmentRepository = mockk<AttachmentRepository>()
    val textStyleRepository = mockk<TextStyleRepository>()
    val paragraphStyleRepository = mockk<ParagraphStyleRepository>()
    val displayRuleRepository = mockk<DisplayRuleRepository>()
    val variableRepository = mockk<VariableRepository>()
    val variableStructureRepository = mockk<VariableStructureRepository>()
    val statusTrackingRepository = mockk<StatusTrackingRepository>()
    val documentObjectBuilder = mockk<DesignerDocumentObjectBuilder>()
    val ipsService = mockk<IpsService>()
    val storage = mockk<Storage>()

    private val subject = DesignerDeployClient(
        documentObjectRepository,
        imageRepository,
        attachmentRepository,
        statusTrackingRepository,
        textStyleRepository,
        paragraphStyleRepository,
        displayRuleRepository,
        variableRepository,
        variableStructureRepository,
        documentObjectBuilder,
        ipsService,
        storage
    )

    @BeforeEach
    fun setupAll() {
        every { documentObjectBuilder.shouldIncludeInternalDependency(any()) } answers {
            val documentObject = firstArg<DocumentObject>()
            (documentObject.internal ?: false) || documentObject.type == DocumentObjectType.Page
        }
        every { ipsService.writeMetadata(any<List<IcmFileMetadata>>()) } just runs
    }

    @Test
    fun `deployDocumentObjects deploys complex structure template with images and attachments`() {
        // given
        val image1 = aImage("I_1").mock()
        val image2 = aImage("I_2").mock()
        val attachment1 = aAttachment("F_1").mock()
        val externalBlock = aDocObj(
            "Txt_Img_Attachment_1", Block, listOf(
                aParagraph(aText(StringValue("Image: "))), ImageRef(image1.id),
                aParagraph(aText(StringValue("Attachment: "))), AttachmentRef(attachment1.id)
            )
        ).mock()
        val internalBlock = aDocObj(
            "Img_2", Block, listOf(ImageRef(image2.id)), internal = true
        ).mock()
        val page = aDocObj(
            "P_1", DocumentObjectType.Page, listOf(
                aDocumentObjectRef(internalBlock.id),
                aDocumentObjectRef(externalBlock.id)
            )
        ).mock()
        val template = aTemplate("1", listOf(aDocumentObjectRef(page.id))).mock()

        every { statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any()) } returns Active()
        every { statusTrackingRepository.deployed(any(), any<Uuid>(), any(), any(), any(), any(), any()) } returns aDeployedStatus("id")
        every { documentObjectRepository.list(any<Op<Boolean>>()) } returns listOf(template, externalBlock)
        every { documentObjectBuilder.getStyleDefinitionPath() } returns "icm://some/path/style.wfd".toIcmPath()
        every { ipsService.fileExists(any<IcmPath>()) } returns false

        // when
        val deploymentResult = subject.deployDocumentObjects()

        // then
        deploymentResult.deployed.size.shouldBeEqualTo(5)
        deploymentResult.errors.shouldBeEqualTo(emptyList())

        verify { ipsService.xml2wfd(any(), "icm://${template.nameOrId()}".toIcmPath()) }
        verify { ipsService.xml2wfd(any(), "icm://${externalBlock.nameOrId()}".toIcmPath()) }
        verify { ipsService.tryUpload("icm://${image1.nameOrId()}".toIcmPath(), any()) }
        verify { ipsService.tryUpload("icm://${image2.nameOrId()}".toIcmPath(), any()) }
        verify { ipsService.tryUpload("icm://${attachment1.nameOrId()}".toIcmPath(), any()) }
    }

    @Test
    fun `deploy list of document objects validates that no document objects are skipped`() {
        val spy = spyk(subject)
        every { spy.deployDocumentObjectsInternal(any(), any(), any(), any(), any(), any()) } returns DeploymentResult(
            Uuid.random()
        )
        every { documentObjectRepository.list(any<Op<Boolean>>()) } returns listOf(
            aBlock(id = "1", skip = SkipOptions(true, null, null)),
            aBlock(id = "2", type = Block),
            aBlock(id = "3", type = DocumentObjectType.Page),
            aBlock(id = "4", type = DocumentObjectType.Template),
            aBlock(id = "5", type = DocumentObjectType.Section),
        )

        val ex = assertThrows<IllegalArgumentException> { spy.deployDocumentObjects(listOf("1", "2", "3")) }

        assertEquals("The following document objects are skipped: [1]. ", ex.message)
        verify(exactly = 1) { documentObjectRepository.list(any<Op<Boolean>>()) }
    }

    @Test
    fun `page objects are skipped in deploy`() {
        val spy = spyk(subject)
        every { spy.deployDocumentObjectsInternal(any(), any(), any(), any(), any(), any()) } returns DeploymentResult(
            Uuid.random()
        )
        every { documentObjectRepository.list(any<Op<Boolean>>()) } returns listOf(
            aBlock(id = "1", type = Block),
            aBlock(id = "2", type = DocumentObjectType.Page),
            aBlock(id = "3", type = DocumentObjectType.Template),
            aBlock(id = "4", type = DocumentObjectType.Section),
        )

        spy.deployDocumentObjects(listOf("1", "2", "3", "4"))

        verify(exactly = 1) { documentObjectRepository.list(any<Op<Boolean>>()) }
        verify {
            spy.deployDocumentObjectsInternal(match { docObjects ->
                docObjects.size == 3 && docObjects.map { it.id }
                    .containsAll(listOf("1", "3", "4"))
            }, any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `deploy list of document objects validates that no document objects are internal`() {
        val spy = spyk(subject)
        every { spy.deployDocumentObjectsInternal(any(), any(), any(), any(), any(), any()) } returns DeploymentResult(
            Uuid.random()
        )
        every { documentObjectRepository.list(any<Op<Boolean>>()) } returns listOf(
            aBlock(id = "1", internal = true),
            aBlock(id = "2", internal = true),
            aBlock(id = "3", internal = false),
        )

        val ex = assertThrows<IllegalArgumentException> { spy.deployDocumentObjects(listOf("1", "2", "3")) }

        assertEquals("The following document objects are internal: [1, 2]. ", ex.message)
        verify(exactly = 1) { documentObjectRepository.list(any<Op<Boolean>>()) }
    }

    @Test
    fun `deploy list of document objects validates that no document are missing`() {
        val spy = spyk(subject)
        every { spy.deployDocumentObjectsInternal(any(), any(), any(), any(), any(), any()) } returns DeploymentResult(
            Uuid.random()
        )
        every { documentObjectRepository.list(any<Op<Boolean>>()) } returns listOf(
            aBlock(id = "1"),
        )

        val ex = assertThrows<IllegalArgumentException> { spy.deployDocumentObjects(listOf("1", "2", "3")) }

        assertEquals("The following document objects were not found: [2, 3]. ", ex.message)
        verify(exactly = 1) { documentObjectRepository.list(any<Op<Boolean>>()) }
    }

    @Test
    fun `deploy list of document objects has all kinds of problems`() {
        val spy = spyk(subject)
        every { spy.deployDocumentObjectsInternal(any(), any(), any(), any(), any(), any()) } returns DeploymentResult(
            Uuid.random()
        )
        every { documentObjectRepository.list(any<Op<Boolean>>()) } returns listOf(
            aBlock(id = "1"),
            aBlock(id = "2", internal = true),
            aBlock(id = "3"),
            aBlock(id = "5"),
            aBlock(id = "6", skip = SkipOptions(true, null, null)),
            aBlock(id = "7"),
        )

        val ex = assertThrows<IllegalArgumentException> {
            spy.deployDocumentObjects(
                listOf(
                    "1", "2", "3", "5", "6", "7", "8"
                )
            )
        }

        assertEquals(
            "The following document objects were not found: [8]. The following document objects are internal: [2]. The following document objects are skipped: [6]. ",
            ex.message
        )
        verify(exactly = 1) { documentObjectRepository.list(any<Op<Boolean>>()) }
    }

    @Test
    fun `deploy list of document objects without dependencies`() {
        val spy = spyk(subject)
        every { spy.deployDocumentObjectsInternal(any(), any(), any(), any(), any(), any()) } returns DeploymentResult(
            Uuid.random()
        )
        val toDeploy = listOf("1", "2", "3")
        val docObjects = listOf(
            aBlock(id = "1", content = listOf(aDocumentObjectRef("4"))),
            aBlock(id = "2", content = listOf(aDocumentObjectRef("5"))),
            aBlock(id = "3", content = listOf(aDocumentObjectRef("6"))),
        )
        every { documentObjectRepository.list(any<Op<Boolean>>()) } returns docObjects

        spy.deployDocumentObjects(toDeploy, true)

        verify(exactly = 1) { documentObjectRepository.list(any<Op<Boolean>>()) }
        verify { spy.deployDocumentObjectsInternal(docObjects, any(), any(), any(), any(), any()) }
    }

    @Test
    fun `deploy list of document objects with recursive dependencies, deduplicates them and skips internal dependencies`() {
        val spy = spyk(subject)
        every { spy.deployDocumentObjectsInternal(any(), any(), any(), any(), any(), any()) } returns DeploymentResult(
            Uuid.random()
        )
        val toDeploy = listOf("1", "2", "3")
        val docObjects = listOf(
            aBlock(id = "1", content = listOf(aDocumentObjectRef("4"))),
            aBlock(id = "2", content = listOf(aDocumentObjectRef("5"))),
            aBlock(id = "3", content = listOf(aDocumentObjectRef("6"))),
        )
        val dependencies = listOf(
            aBlock(id = "4"),
            aBlock(id = "5", content = listOf(aDocumentObjectRef("7"))),
            aBlock(id = "6", content = listOf(aDocumentObjectRef("7"))),
            aBlock(id = "7", content = listOf(aDocumentObjectRef("8"))),
            aBlock(id = "8", internal = true),
        )
        every { documentObjectRepository.list(any<Op<Boolean>>()) } returns docObjects
        for (dependency in dependencies) {
            every { documentObjectRepository.findOrFail(dependency.id) } returns dependency
        }

        spy.deployDocumentObjects(toDeploy)

        verify(exactly = 1) { documentObjectRepository.list(any<Op<Boolean>>()) }
        verify(exactly = 7) { documentObjectRepository.findOrFail(any()) }
        verify {
            spy.deployDocumentObjectsInternal(match { docObjects ->
                docObjects.size == 7 && docObjects.map { it.id }
                    .containsAll(listOf("1", "2", "3", "4", "5", "6", "7"))
            }, any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `deploy list of document objects excludes pages and internal objects but includes their transitive external dependencies`() {
        val spy = spyk(subject)
        every { spy.deployDocumentObjectsInternal(any(), any(), any(), any(), any(), any()) } returns DeploymentResult(
            Uuid.random()
        )

        val block3 = DocumentObjectBuilder("block3", Block).build()
        val block2 = DocumentObjectBuilder("block2", Block).internal(true).documentObjectRef(block3).build()
        val block1 = DocumentObjectBuilder("block1", Block).build()
        val page = DocumentObjectBuilder("page1", DocumentObjectType.Page).documentObjectRef(block1)
            .documentObjectRef("block2").build()
        val template = DocumentObjectBuilder("template1", DocumentObjectType.Template).documentObjectRef(page).build()

        every { documentObjectRepository.list(any<Op<Boolean>>()) } returns listOf(template)
        every { documentObjectRepository.findOrFail("page1") } returns page
        every { documentObjectRepository.findOrFail("block1") } returns block1
        every { documentObjectRepository.findOrFail("block2") } returns block2
        every { documentObjectRepository.findOrFail("block3") } returns block3

        spy.deployDocumentObjects(listOf("template1"), false)

        verify {
            spy.deployDocumentObjectsInternal(match { docObjects ->
                val ids = docObjects.map { it.id }
                ids.containsAll(listOf("template1", "block1", "block3"))
                    && !ids.contains("page1")
                    && !ids.contains("block2")
            }, any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `deploy list of document objects with dependencies when dependency is not found`() {
        val spy = spyk(subject)
        every { spy.deployDocumentObjectsInternal(any(), any(), any(), any(), any(), any()) } returns DeploymentResult(
            Uuid.random()
        )
        val toDeploy = listOf("1")
        val docObjects = listOf(
            aBlock(id = "1", content = listOf(aDocumentObjectRef("4"))),
        )
        every { documentObjectRepository.list(any<Op<Boolean>>()) } returns docObjects
        every { documentObjectRepository.findOrFail(any()) } throws IllegalArgumentException("not found")

        val ex = assertThrows<IllegalArgumentException> { spy.deployDocumentObjects(toDeploy) }

        assertEquals("not found", ex.message)
        verify(exactly = 1) { documentObjectRepository.list(any<Op<Boolean>>()) }
        verify(exactly = 1) { documentObjectRepository.findOrFail(any()) }
    }

    @Test
    fun `deployDocumentObjects continues deployment when there is exception during document build`() {
        // given
        val innerBlock = aDocObj("B_2")
        val block = aDocObj("B_1", Block, listOf(aDocumentObjectRef(innerBlock.id))).mock()
        val template = aDocObj("T_1", Template, listOf(aDocumentObjectRef(block.id))).mock()

        every { documentObjectRepository.find(innerBlock.id) } throws IllegalStateException("Not found")
        every { documentObjectRepository.list(any<Op<Boolean>>()) } returns listOf(template, block)
        every { documentObjectBuilder.buildDocumentObject(block) } throws IllegalStateException("Inner block not found")
        every { statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any()) } returns Active()
        every { statusTrackingRepository.deployed(any(), any<Uuid>(), any(), any(), any(), any(), any()) } returns aDeployedStatus("id")
        every { documentObjectBuilder.getStyleDefinitionPath() } returns "icm://some/path/style.wfd".toIcmPath()
        every { ipsService.fileExists(any<IcmPath>()) } returns false
        every { statusTrackingRepository.error("B_1", any(), any(), any(), any(), any(), any(), any()) } returns aErrorStatus("B_1")

        // when
        val result = subject.deployDocumentObjects()

        // then
        result.deployed.shouldBeEqualTo(
            listOf(
                DeploymentInfo(
                    "T_1", ResourceType.DocumentObject, "icm://${template.nameOrId()}".toIcmPath()
                )
            )
        )
        result.errors.shouldBeEqualTo(
            listOf(
                DeploymentError("B_1", "Not found"), DeploymentError("B_1", "Inner block not found")
            )
        )

        verify(exactly = 1) { ipsService.xml2wfd(any(), "icm://${template.nameOrId()}".toIcmPath()) }
    }

    private fun Image.mock(success: Boolean = true): Image {
        val image = this
        val sourcePath = image.sourcePath
        val imagePath = "icm://${image.nameOrId()}".toIcmPath()

        every { documentObjectBuilder.getImagePath(image) } returns imagePath
        every { imageRepository.find(image.id) } returns image
        if (!sourcePath.isNullOrBlank()) {
            val byteArray = ByteArray(10)
            every { storage.read(sourcePath) } answers {
                if (success) {
                    byteArray
                } else {
                    throw Exception()
                }
            }
            every { ipsService.tryUpload(imagePath, byteArray) } returns OperationResult.Success
        }

        return image
    }

    private fun Attachment.mock(success: Boolean = true): Attachment {
        val attachment = this
        val sourcePath = attachment.sourcePath
        val attachmentPath = "icm://${attachment.nameOrId()}".toIcmPath()

        every { documentObjectBuilder.getAttachmentPath(attachment) } returns attachmentPath
        every { attachmentRepository.find(attachment.id) } returns attachment
        if (!sourcePath.isNullOrBlank()) {
            val byteArray = ByteArray(10)
            every { storage.read(sourcePath) } answers {
                if (success) {
                    byteArray
                } else {
                    throw Exception()
                }
            }
            every { ipsService.tryUpload(attachmentPath, byteArray) } returns OperationResult.Success
        }

        return attachment
    }

    private fun DocumentObject.mock(): DocumentObject {
        val documentObject = this
        every { documentObjectRepository.find(documentObject.id) } returns documentObject
        every { documentObjectRepository.findOrFail(documentObject.id) } returns documentObject

        if (documentObject.internal == false) {
            val xml = "<xml>${documentObject.nameOrId()}</xml>"
            val outputPath = "icm://${documentObject.nameOrId()}".toIcmPath()

            every { documentObjectBuilder.buildDocumentObject(documentObject) } returns xml
            every { documentObjectBuilder.getDocumentObjectPath(documentObject) } returns outputPath
            every { ipsService.xml2wfd(xml, outputPath) } returns OperationResult.Success
        }
        return documentObject
    }


    @Nested
    inner class ConflictValidationTests {
        val states: MutableMap<Pair<String, ResourceType>, List<StatusEvent>> = mutableMapOf()

        @BeforeEach
        fun setupAll() {
            states.clear()
        }

        @Test
        fun `validateConflicts shows conflicts in the same batch`() {
            val t1 = DocumentObjectBuilder("T_1", Template).build().mock().active()
            val b1 = DocumentObjectBuilder("B_1", Block).build().mock().active()
            val b2 = DocumentObjectBuilder("B_2", Block).build().mock().active()
            every { documentObjectRepository.list(any<Op<Boolean>>()) } returns listOf(t1, b1, b2)
            every { documentObjectBuilder.getDocumentObjectPath(b1) } returns "icm://same/path".toIcmPath()
            every { documentObjectBuilder.getDocumentObjectPath(b2) } returns "icm://same/path".toIcmPath()

            val result = subject.validateConflicts()

            assertEquals(1, result.conflictingInBatchResources.size)
            assertEquals(setOf("B_1", "B_2"), result.conflictingInBatchResources.values.single().map { it.id }.toSet())
        }

        @Test
        fun `validateConflicts shows conflicts with previously deployed items`() {
            val t1 = DocumentObjectBuilder("T_1", Template).build().mock().active()
            val b1 = DocumentObjectBuilder("B_1", Block).build().mock().deployed("icm://path1").active()
            val b2 = DocumentObjectBuilder("B_2", Block).build().mock().active()
            every { documentObjectBuilder.getDocumentObjectPath(b2) } returns "icm://path1".toIcmPath()
            every { documentObjectRepository.list(any<Op<Boolean>>()) } returns listOf(t1, b1, b2)

            val result = subject.validateConflicts()

            assertEquals(1, result.conflictingWithPreviousResources.size)
            assertEquals(setOf("B_2"), result.conflictingWithPreviousResources.values.single().map { it.id }.toSet())
        }

        @Test
        fun `validateConflicts returns empty result for unique active resources`() {
            val t1 = DocumentObjectBuilder("T_1", Template).build().mock().active()
            val b1 = DocumentObjectBuilder("B_1", Block).build().mock().active()
            every { documentObjectRepository.list(any<Op<Boolean>>()) } returns listOf(t1, b1)

            val result = subject.validateConflicts()

            assertTrue(result.hasNoConflicts())
            assertTrue(result.conflictingInBatchResources.isEmpty())
            assertTrue(result.conflictingWithPreviousResources.isEmpty())
        }

        @Test
        fun `validateConflicts detects collision even when existing resource is not to be currently deployed`() {
            val b1 = DocumentObjectBuilder("B_1", Block).build().mock().deployed("icm://same/path")
            val b2 = DocumentObjectBuilder("B_2", Block).build().mock().active()
            every { documentObjectBuilder.getDocumentObjectPath(b2) } returns "icm://same/path".toIcmPath()
            every { documentObjectRepository.list(any<Op<Boolean>>()) } returns listOf(b1, b2)

            val result = subject.validateConflicts()

            assertEquals(1, result.conflictingWithPreviousResources.size)
            assertEquals(setOf("B_2"), result.conflictingWithPreviousResources.values.single().map { it.id }.toSet())
        }

        @Test
        fun `validateConflicts ignores previously deployed resources from other outputs`() {
            val b1 = DocumentObjectBuilder("B_1", Block).build().mock().deployed("icm://same/path", InspireOutput.Interactive)
            val b2 = DocumentObjectBuilder("B_2", Block).build().mock().active()
            every { documentObjectBuilder.getDocumentObjectPath(b2) } returns "icm://same/path".toIcmPath()
            every { documentObjectRepository.list(any<Op<Boolean>>()) } returns listOf(b1, b2)

            val result = subject.validateConflicts()

            assertTrue(result.conflictingWithPreviousResources.isEmpty())
        }

        @Test
        fun `validateConflicts uses last deployment for designer output even when newer foreign output deployment exists`() {
            val b1 = DocumentObjectBuilder("B_1", Block).build().mock().deployed("icm://designer-path")
                .deployed("icm://interactive-path", InspireOutput.Interactive)
            val b2 = DocumentObjectBuilder("B_2", Block).build().mock().active()
            every { documentObjectBuilder.getDocumentObjectPath(b2) } returns "icm://designer-path".toIcmPath()
            every { documentObjectRepository.list(any<Op<Boolean>>()) } returns listOf(b1, b2)

            val result = subject.validateConflicts()

            assertEquals(1, result.conflictingWithPreviousResources.size)
            assertEquals(setOf("B_2"), result.conflictingWithPreviousResources.values.single().map { it.id }.toSet())
        }

        @Test
        fun `validateConflicts flags when previously deployed path has extra owners`() {
            val b1 = DocumentObjectBuilder("B_1", Block).build().mock().active()
            DocumentObjectBuilder("B_2", Block).build().mock().deployed("icm://same/path1")
            every { documentObjectBuilder.getDocumentObjectPath(b1) } returns "icm://same/path1".toIcmPath()
            every { documentObjectRepository.list(any<Op<Boolean>>()) } returns listOf(b1)

            val result = subject.validateConflicts()

            assertEquals(1, result.conflictingWithPreviousResources.size)
            assertEquals(setOf("B_1"), result.conflictingWithPreviousResources.values.single().map { it.id }.toSet())
        }

        @Test
        fun `validateConflicts flags when current batch is superset of previous owners`() {
            val b1 = DocumentObjectBuilder("B_1", Block).build().mock().deployed("icm://same/path").active()
            val b2 = DocumentObjectBuilder("B_2", Block).build().mock().active()
            every { documentObjectBuilder.getDocumentObjectPath(b1) } returns "icm://same/path".toIcmPath()
            every { documentObjectBuilder.getDocumentObjectPath(b2) } returns "icm://same/path".toIcmPath()
            every { documentObjectRepository.list(any<Op<Boolean>>()) } returns listOf(b1, b2)

            val result = subject.validateConflicts()

            assertEquals(1, result.conflictingWithPreviousResources.size)
            assertEquals(setOf("B_1", "B_2"), result.conflictingWithPreviousResources.values.single().map { it.id }.toSet())
        }

        @Test
        fun `validateConflicts ignores stale tracked resources that cannot be resolved`() {
            DocumentObjectBuilder("B_1", Block).build().active().deployed("icm://path1")
            DocumentObjectBuilder("B_MISSING", Block).build().active().deployed("icm://path1")
            val b2 = DocumentObjectBuilder("B_2", Block).build().mock().active()
            every { documentObjectBuilder.getDocumentObjectPath(b2) } returns "icm://path1".toIcmPath()
            every { documentObjectRepository.list(any<Op<Boolean>>()) } returns listOf(b2)
            every { documentObjectRepository.findOrFail("B_MISSING") } throws IllegalArgumentException("not found")

            val result = assertDoesNotThrow<ValidationResult> { subject.validateConflicts() }

            assertEquals(1, result.conflictingWithPreviousResources.size)
            assertEquals(setOf("B_2"), result.conflictingWithPreviousResources.values.single().map { it.id }.toSet())
        }

        @Test
        fun `validateConflicts does not flag same resource deploying to the same path again`() {
            val b1 = DocumentObjectBuilder("B_1", Block).build().mock().deployed("icm://same/path").active()
            every { documentObjectBuilder.getDocumentObjectPath(b1) } returns "icm://same/path".toIcmPath()
            every { documentObjectRepository.list(any<Op<Boolean>>()) } returns listOf(b1)

            val result = subject.validateConflicts()

            assertTrue(result.conflictingWithPreviousResources.isEmpty())
        }

        @Test
        fun `validateConflicts detects cross resource in batch collisions`() {
            val i1 = ImageBuilder("I_1").sourcePath("test").build().mock().active()
            val a1 = AttachmentBuilder("A_1").sourcePath("test").build().mock().active()
            val b1 = DocumentObjectBuilder("B_1", Block).imageRef(i1).attachmentRef(a1).build().mock().active()

            every { documentObjectBuilder.getDocumentObjectPath(b1) } returns "icm://same/path".toIcmPath()
            every { documentObjectBuilder.getImagePath(i1) } returns "icm://same/path".toIcmPath()
            every { documentObjectBuilder.getAttachmentPath(a1) } returns "icm://same/path".toIcmPath()
            every { documentObjectRepository.list(any<Op<Boolean>>()) } returns listOf(b1)

            val result = subject.validateConflicts()

            assertEquals(1, result.conflictingInBatchResources.size)
            assertEquals(
                setOf(
                    ResourceId("B_1", ResourceType.DocumentObject),
                    ResourceId("I_1", ResourceType.Image),
                    ResourceId("A_1", ResourceType.Attachment)
                ),
                result.conflictingInBatchResources.values.single()
            )
        }

        @Test
        fun `validateConflicts detects cross resource with previously deployed`() {
            ImageBuilder("I_1").sourcePath("test").build().mock().deployed("icm://image")
            val b1 = DocumentObjectBuilder("B_1", Block).build().mock().active()
            every { documentObjectBuilder.getDocumentObjectPath(b1) } returns "icm://image".toIcmPath()
            every { documentObjectRepository.list(any<Op<Boolean>>()) } returns listOf(b1)

            val result = subject.validateConflicts()

            assertEquals(0, result.conflictingInBatchResources.size)
            assertEquals(1, result.conflictingWithPreviousResources.size)
            assertEquals(
                setOf(ResourceId("B_1", ResourceType.DocumentObject)),
                result.conflictingWithPreviousResources.values.single()
            )
        }

        @Test
        fun `validateConflicts does not report objects which are not to be currently deployed`() {
            val spy = spyk(subject)
            val b1 = DocumentObjectBuilder("B_1", Block).build().mock().active()
            val b2 = DocumentObjectBuilder("B_2", Block).build().mock().active()
            val b3 = DocumentObjectBuilder("B_3", Block).build().mock().active()

            every { spy.getDocumentObjectsToDeploy(listOf("B_1", "B_2")) } returns listOf(b1, b2)
            every { documentObjectBuilder.getDocumentObjectPath(b1) } returns "icm://same/path".toIcmPath()
            every { documentObjectBuilder.getDocumentObjectPath(b2) } returns "icm://same/path".toIcmPath()
            every { documentObjectBuilder.getDocumentObjectPath(b3) } returns "icm://same/path".toIcmPath()

            val result = spy.validateConflicts(listOf("B_1", "B_2"))

            assertFalse(result.hasNoConflicts())
            assertEquals(setOf("B_1", "B_2"), result.conflictingInBatchResources.values.single().map { it.id }.toSet())
            verify(exactly = 1) { spy.getDocumentObjectsToDeploy(listOf("B_1", "B_2")) }
        }

        @Test
        fun `validateConflicts by ids validates not found internal and skipped objects`() {
            every { documentObjectRepository.list(any<Op<Boolean>>()) } returns listOf(
                aBlock(id = "1", internal = true),
                aBlock(id = "2", skip = SkipOptions(true, null, null)),
            )

            val ex = assertThrows<IllegalArgumentException> { subject.validateConflicts(listOf("1", "2", "3")) }

            assertEquals(
                "The following document objects were not found: [3]. The following document objects are internal: [1]. The following document objects are skipped: [2]. ",
                ex.message
            )
        }

        private fun Image.active(): Image {
            val ev = Active()
            val events = states.computeIfPresentOrPut(id to ResourceType.Image, listOf(ev)) { it + ev }
            every { statusTrackingRepository.findLastEventRelevantToOutput(id, any(), any()) } returns ev
            every { statusTrackingRepository.findEventsRelevantToOutput(id, any(), any()) } returns events
            every { statusTrackingRepository.listAll() } returns states.map { (k, v) -> StatusTracking(k.first, "", k.second, v) }
            return this
        }

        private fun Attachment.active(): Attachment {
            val ev = Active()
            val events = states.computeIfPresentOrPut(id to ResourceType.Attachment, listOf(ev)) { it + ev }
            every { statusTrackingRepository.findLastEventRelevantToOutput(id, any(), any()) } returns ev
            every { statusTrackingRepository.findEventsRelevantToOutput(id, any(), any()) } returns events
            every { statusTrackingRepository.listAll() } returns states.map { (k, v) -> StatusTracking(k.first, "", k.second, v) }
            return this
        }

        private fun DocumentObject.active(): DocumentObject {
            val ev = Active()
            val events = states.computeIfPresentOrPut(id to ResourceType.DocumentObject, listOf(ev)) { it + ev }
            every { statusTrackingRepository.findLastEventRelevantToOutput(id, any(), any()) } returns ev
            every { statusTrackingRepository.findEventsRelevantToOutput(id, any(), any()) } returns events
            every { statusTrackingRepository.listAll() } returns states.map { (k, v) -> StatusTracking(k.first, "", k.second, v) }
            return this
        }

        private fun DocumentObject.deployed(path: String?, output: InspireOutput = InspireOutput.Designer): DocumentObject {
            val ev = Deployed(Uuid.random(), Clock.System.now(), output, path?.toIcmPath())
            val events = states.computeIfPresentOrPut(id to ResourceType.DocumentObject, listOf(ev)) { it + ev }
            every { statusTrackingRepository.findLastEventRelevantToOutput(id, any(), any()) } returns ev
            every { statusTrackingRepository.findEventsRelevantToOutput(id, any(), any()) } returns events
            every { statusTrackingRepository.listAll() } returns states.map { (k, v) -> StatusTracking(k.first, "", k.second, v) }
            return this
        }

        private fun Image.deployed(path: String?, output: InspireOutput = InspireOutput.Designer): Image {
            val ev = Deployed(Uuid.random(), Clock.System.now(), output, path?.toIcmPath())
            val events = states.computeIfPresentOrPut(id to ResourceType.Image, listOf(ev)) { it + ev }
            every { statusTrackingRepository.findLastEventRelevantToOutput(id, any(), any()) } returns ev
            every { statusTrackingRepository.findEventsRelevantToOutput(id, any(), any()) } returns events
            every { statusTrackingRepository.listAll() } returns states.map { (k, v) -> StatusTracking(k.first, "", k.second, v) }
            return this
        }
    }

    @Nested
    inner class StatusTrackingTests {
        @BeforeEach
        fun setup() {
            every { documentObjectBuilder.buildDocumentObject(any()) } returns ""
            every { statusTrackingRepository.deployed(any(), any<Uuid>(), any(), any(), any(), any(), any()) } returns  aDeployedStatus("id")
            every { statusTrackingRepository.error(any(), any(), any(), any(), any(), any(), any(), any()) } returns aErrorStatus("id")
            every { statusTrackingRepository.active(any(), any()) } returns aActiveStatus("id")
            every { documentObjectBuilder.getDocumentObjectPath(any()) } returns "icm://path".toIcmPath()
            every { imageRepository.find(any()) } returns null
        }

        @Test
        fun `deployDocumentObjects does not do anything when all objects are deployed`() {
            // given
            val docObjects = listOf(
                aDocObj("D_1", content = listOf(ImageRef("I_1"))),
                aDocObj("D_2", content = listOf(ImageRef("I_2"))),
                aDocObj("D_3", content = listOf(ImageRef("I_3"))),
            )
            givenObjectIsDeployed("D_1")
            givenObjectIsDeployed("I_1")
            givenObjectIsDeployed("D_2")
            givenObjectIsDeployed("I_2")
            givenObjectIsDeployed("D_3")
            givenObjectIsDeployed("I_3")

            // when
            subject.runDeploy(docObjects)

            // then
            verify(exactly = 0) { documentObjectBuilder.buildDocumentObject(any()) }
            verify(exactly = 3) { imageRepository.find(any()) }
        }

        @Test
        fun `deployDocumentObjects skips deployed but deploys active and error`() {
            // given
            val docObjects = listOf(
                aDocObj("D_1", content = listOf(ImageRef("I_1"))),
                aDocObj("D_2", content = listOf(ImageRef("I_2"))),
                aDocObj("D_3", content = listOf(ImageRef("I_3"))),
            )
            aImage("I_1").mock()
            aImage("I_2").mock()
            givenObjectIsDeployed("D_1")
            givenObjectIsError("I_1")
            givenObjectIsError("D_2")
            givenObjectIsActive("I_2")
            givenObjectIsActive("D_3")
            givenObjectIsDeployed("I_3")
            every { ipsService.xml2wfd(any(), any<IcmPath>()) } returns OperationResult.Success
            every { documentObjectBuilder.getStyleDefinitionPath() } returns "icm://some/path/style.wfd".toIcmPath()
            every { ipsService.fileExists(any<IcmPath>()) } returns false


            // when
            subject.runDeploy(docObjects)

            // then
            verify(exactly = 2) { documentObjectBuilder.buildDocumentObject(any()) }
            verify(exactly = 1) { statusTrackingRepository.deployed("D_2", any<Uuid>(), any(), any(), any(), any(), any()) }
            verify(exactly = 1) { statusTrackingRepository.deployed("D_3", any<Uuid>(), any(), any(), any(), any(), any()) }
            verify(exactly = 1) { statusTrackingRepository.deployed("I_1", any<Uuid>(), any(), any(), any(), any(), any()) }
            verify(exactly = 1) { statusTrackingRepository.deployed("I_2", any<Uuid>(), any(), any(), any(), any(), any()) }
        }

        @Test
        fun `deployDocumentObjects records errors`() {
            // given
            val docObjects = listOf(aDocObj("D_1", content = listOf(ImageRef("I_1"))))
            givenObjectIsActive("D_1")
            givenObjectIsActive("I_1")
            aImage("I_1").mock(success = false)
            every { ipsService.xml2wfd(any(), any<IcmPath>()) } returns OperationResult.Failure("oops")
            every { documentObjectBuilder.getStyleDefinitionPath() } returns "icm://some/path/style.wfd".toIcmPath()
            every { ipsService.fileExists(any<IcmPath>()) } returns false

            // when
            subject.runDeploy(docObjects)

            // then
            verify(exactly = 1) { documentObjectBuilder.buildDocumentObject(any()) }
            verify(exactly = 1) { statusTrackingRepository.error("D_1", any(), any(), any(), any(), any(), "oops", any()) }
            verify(exactly = 1) { statusTrackingRepository.error("I_1", any(), any(), any(), any(), any(), any(), any()) }
        }

        @Test
        fun `deployDocumentObjects disallows system metadata`() {
            var count = 0
            for (key in MetadataValidator.DISALLOWED_METADATA) {
                // given
                val docObjects = listOf(aDocObj("D_1", metadata = mapOf(key to listOf(MetadataPrimitive.Str("value")))))
                givenObjectIsActive("D_1")

                // when
                val result = subject.runDeploy(docObjects)

                // then
                assertEquals(
                    listOf(DeploymentError("D_1", "Metadata of document object 'D_1' contains invalid keys: [${key}]")),
                    result.errors
                )

                count++
            }

            assertEquals(MetadataValidator.DISALLOWED_METADATA.size, count)
        }

        @Test
        fun `deployDocumentObjects allows other metadata`() {
            // given
            val docObjects = listOf(aDocObj("D_1", metadata = mapOf("other" to listOf(MetadataPrimitive.Str("value")))))
            givenObjectIsActive("D_1")
            every { documentObjectBuilder.getStyleDefinitionPath() } returns "icm://some/path/style.wfd".toIcmPath()
            every { ipsService.fileExists(any<IcmPath>()) } returns false
            every { ipsService.xml2wfd(any(), any<IcmPath>()) } returns OperationResult.Success

            // when
            val result = subject.runDeploy(docObjects)

            // then
            assertEquals(listOf(DeploymentInfo("D_1", ResourceType.DocumentObject, "icm://path".toIcmPath())), result.deployed)
        }

        @Test
        fun `deployStyles creates style definition and sets production approval state`() {
            // given
            every { documentObjectBuilder.buildStyles(any(), any()) } returns "<xml />"
            every { documentObjectBuilder.buildStyleLayoutDelta(any(), any()) } returns "<xml />"

            every { statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any()) } returns Active()
            every { statusTrackingRepository.deployed(any(), any<Uuid>(), any(), any(), any(), any()) } returns aDeployedStatus("id")
            every { textStyleRepository.listAll() } returns emptyList()
            every { paragraphStyleRepository.listAll() } returns emptyList()
            every { ipsService.xml2wfd(any(), any<IcmPath>()) } returns OperationResult.Success

            val definitionPathWfd = "icm://defaultFolder/CompanyStyles.wfd"

            every { documentObjectBuilder.getStyleDefinitionPath() } returns definitionPathWfd.toIcmPath()

            // when
            subject.deployStyles()

            // then
            verify { ipsService.xml2wfd(eq("<xml />"), eq(definitionPathWfd.toIcmPath())) }
        }

        @Test
        fun `deployStyles does not continue when wfd creation fails`() {
            // given
            every { documentObjectBuilder.buildStyles(any(), any()) } returns "<xml />"

            every { statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any()) } returns Active()
            every { statusTrackingRepository.deployed(any(), any<Uuid>(), any(), any(), any(), any()) } returns aDeployedStatus("id")
            every { textStyleRepository.listAll() } returns emptyList()
            every { paragraphStyleRepository.listAll() } returns emptyList()
            every { ipsService.xml2wfd(any(), any<IcmPath>()) } returns OperationResult.Failure("Problem")

            val definitionPath = "icm://defaultFolder/CompanyStyles.wfd"

            every { documentObjectBuilder.getStyleDefinitionPath() } returns definitionPath.toIcmPath()

            // when
            subject.deployStyles()

            // then
            verify { ipsService.xml2wfd(eq("<xml />"), eq(definitionPath.toIcmPath())) }
            verify(exactly = 0) { ipsService.setProductionApprovalState(any<List<IcmPath>>()) }
        }

        private fun givenObjectIsActive(id: String) {
            every {
                statusTrackingRepository.findLastEventRelevantToOutput(id, any(), any())
            } returns Active()
        }

        private fun givenObjectIsError(id: String) {
            every {
                statusTrackingRepository.findLastEventRelevantToOutput(id, any(), any())
            } returns Error(
                output = InspireOutput.Designer,
                deploymentId = Uuid.random(),
                timestamp = Clock.System.now(),
                icmPath = "icm://path".toIcmPath(),
                error = "oops"
            )
        }

        private fun givenObjectIsDeployed(id: String) {
            every {
                statusTrackingRepository.findLastEventRelevantToOutput(id, any(), any())
            } returns Deployed(
                output = InspireOutput.Designer,
                deploymentId = Uuid.random(),
                timestamp = Clock.System.now(),
                icmPath = "icm://path".toIcmPath()
            )
        }
    }

    private fun DesignerDeployClient.runDeploy(documentObjects: List<DocumentObject>): DeploymentResult {
        return subject.deployDocumentObjectsInternal(
            documentObjects,
            ResultTrackerImpl(statusTrackingRepository, InspireOutput.Designer),
            subject::uploadDocumentObject,
            subject::uploadImage,
            subject::uploadAttachment,
            subject::uploadDisplayRule
        )
    }
}
