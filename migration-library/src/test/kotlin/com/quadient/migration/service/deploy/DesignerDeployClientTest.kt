package com.quadient.migration.service.deploy

import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.data.ImageModel
import com.quadient.migration.data.ImageModelRef
import com.quadient.migration.data.StringModel
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.repository.ImageInternalRepository
import com.quadient.migration.service.Storage
import com.quadient.migration.service.inspirebuilder.DesignerDocumentObjectBuilder
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.service.ipsclient.OperationResult
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.tools.model.aBlock
import com.quadient.migration.tools.model.aDocObj
import com.quadient.migration.tools.model.aDocumentObjectRef
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test
import kotlin.ByteArray

class DesignerDeployClientTest {
    val documentObjectRepository = mockk<DocumentObjectInternalRepository>()
    val imageRepository = mockk<ImageInternalRepository>()
    val documentObjectBuilder = mockk<DesignerDocumentObjectBuilder>()
    val ipsService = mockk<IpsService>()
    val storage = mockk<Storage>()

    private val subject =
        DesignerDeployClient(documentObjectRepository, imageRepository, documentObjectBuilder, ipsService, storage)

    @Test
    fun `deployDocumentObjects deploys complex structure template`() {
        // given
        val image1 = mockImg(aImage("I_1"))
        val image2 = mockImg(aImage("I_2"))
        val externalBlock = mockObj(
            aDocObj(
                "Txt_Img_1", DocumentObjectType.Block, listOf(
                    aParagraph(aText(StringModel("Image: "))), ImageModelRef(image1.id)
                )
            )
        )
        val internalBlock = mockObj(
            aDocObj(
                "Img_2", DocumentObjectType.Block, listOf(ImageModelRef(image2.id)), internal = true
            )
        )
        val page = mockObj(
            aDocObj(
                "P_1", DocumentObjectType.Page, listOf(
                    aDocumentObjectRef(internalBlock.id),
                    aDocumentObjectRef(externalBlock.id)
                )
            )
        )
        val template = mockObj(aTemplate("1", listOf(aDocumentObjectRef(page.id))))

        every { documentObjectRepository.list(any()) } returns listOf(template, externalBlock)
        every { ipsService.close() } just runs

        // when
        subject.deployDocumentObjects()

        // then
        verify { ipsService.xml2wfd(any(), "icm://${template.nameOrId()}") }
        verify { ipsService.xml2wfd(any(), "icm://${externalBlock.nameOrId()}") }
        verify { ipsService.tryUpload("icm://${image1.nameOrId()}", any()) }
        verify { ipsService.tryUpload("icm://${image2.nameOrId()}", any()) }
        verify { ipsService.close() }
    }

    @Test
    fun `deploy list of document objects validates that no document objects are unsupported`() {
        val spy = spyk(subject)
        every { spy.deployDocumentObjectsInternal(any()) } returns DeploymentResult()
        every { documentObjectRepository.list(any()) } returns listOf(
            aBlock(id = "1", type = DocumentObjectType.Unsupported),
            aBlock(id = "2", type = DocumentObjectType.Block),
            aBlock(id = "3", type = DocumentObjectType.Page),
            aBlock(id = "4", type = DocumentObjectType.Template),
            aBlock(id = "5", type = DocumentObjectType.Section),
        )

        val ex = assertThrows<IllegalArgumentException> { spy.deployDocumentObjects(listOf("1", "2", "3")) }

        assertEquals("The following document objects cannot be deployed due to their type: [1]. ", ex.message)
        verify(exactly = 1) { documentObjectRepository.list(any()) }
    }

    @Test
    fun `page objects are skipped in deploy`() {
        val spy = spyk(subject)
        every { spy.deployDocumentObjectsInternal(any()) } returns DeploymentResult()
        every { documentObjectRepository.list(any()) } returns listOf(
            aBlock(id = "1", type = DocumentObjectType.Block),
            aBlock(id = "2", type = DocumentObjectType.Page),
            aBlock(id = "3", type = DocumentObjectType.Template),
            aBlock(id = "4", type = DocumentObjectType.Section),
        )

        spy.deployDocumentObjects(listOf("1", "2", "3", "4"))

        verify(exactly = 1) { documentObjectRepository.list(any()) }
        verify {
            spy.deployDocumentObjectsInternal(match { docObjects ->
                docObjects.size == 3 && docObjects.map { it.id }
                    .containsAll(listOf("1", "3", "4"))
            })
        }
    }

    @Test
    fun `deploy list of document objects validates that no document objects are internal`() {
        val spy = spyk(subject)
        every { spy.deployDocumentObjectsInternal(any()) } returns DeploymentResult()
        every { documentObjectRepository.list(any()) } returns listOf(
            aBlock(id = "1", internal = true),
            aBlock(id = "2", internal = true),
            aBlock(id = "3", internal = false),
        )

        val ex = assertThrows<IllegalArgumentException> { spy.deployDocumentObjects(listOf("1", "2", "3")) }

        assertEquals("The following document objects are internal: [1, 2]. ", ex.message)
        verify(exactly = 1) { documentObjectRepository.list(any()) }
    }

    @Test
    fun `deploy list of document objects validates that no document are missing`() {
        val spy = spyk(subject)
        every { spy.deployDocumentObjectsInternal(any()) } returns DeploymentResult()
        every { documentObjectRepository.list(any()) } returns listOf(
            aBlock(id = "1"),
        )

        val ex = assertThrows<IllegalArgumentException> { spy.deployDocumentObjects(listOf("1", "2", "3")) }

        assertEquals("The following document objects were not found: [2, 3]. ", ex.message)
        verify(exactly = 1) { documentObjectRepository.list(any()) }
    }

    @Test
    fun `deploy list of document objects has all kinds of problems`() {
        val spy = spyk(subject)
        every { spy.deployDocumentObjectsInternal(any()) } returns DeploymentResult()
        every { documentObjectRepository.list(any()) } returns listOf(
            aBlock(id = "1"),
            aBlock(id = "2", internal = true),
            aBlock(id = "3"),
            aBlock(id = "5"),
            aBlock(id = "6", type = DocumentObjectType.Unsupported),
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
            "The following document objects were not found: [8]. The following document objects are internal: [2]. The following document objects cannot be deployed due to their type: [6]. ",
            ex.message
        )
        verify(exactly = 1) { documentObjectRepository.list(any()) }
    }

    @Test
    fun `deploy list of document objects without dependencies`() {
        val spy = spyk(subject)
        every { spy.deployDocumentObjectsInternal(any()) } returns DeploymentResult()
        val toDeploy = listOf("1", "2", "3")
        val docObjects = listOf(
            aBlock(id = "1", content = listOf(aDocumentObjectRef("4"))),
            aBlock(id = "2", content = listOf(aDocumentObjectRef("5"))),
            aBlock(id = "3", content = listOf(aDocumentObjectRef("6"))),
        )
        every { documentObjectRepository.list(any()) } returns docObjects

        spy.deployDocumentObjects(toDeploy, true)

        verify(exactly = 1) { documentObjectRepository.list(any()) }
        verify { spy.deployDocumentObjectsInternal(docObjects) }
    }

    @Test
    fun `deploy list of document objects with recursive dependencies, deduplicates them and skips internal dependencies`() {
        val spy = spyk(subject)
        every { spy.deployDocumentObjectsInternal(any()) } returns DeploymentResult()
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
        every { documentObjectRepository.list(any()) } returns docObjects
        for (dependency in dependencies) {
            every { documentObjectRepository.findModelOrFail(dependency.id) } returns dependency
        }

        spy.deployDocumentObjects(toDeploy)

        verify(exactly = 1) { documentObjectRepository.list(any()) }
        verify(exactly = 7) { documentObjectRepository.findModelOrFail(any()) }
        verify {
            spy.deployDocumentObjectsInternal(match { docObjects ->
                docObjects.size == 7 && docObjects.map { it.id }
                    .containsAll(listOf("1", "2", "3", "4", "5", "6", "7"))
            })
        }
    }

    @Test
    fun `deploy list of document objects with dependencies when dependency is not found`() {
        val spy = spyk(subject)
        every { spy.deployDocumentObjectsInternal(any()) } returns DeploymentResult()
        val toDeploy = listOf("1")
        val docObjects = listOf(
            aBlock(id = "1", content = listOf(aDocumentObjectRef("4"))),
        )
        every { documentObjectRepository.list(any()) } returns docObjects
        every { documentObjectRepository.findModelOrFail(any()) } throws IllegalArgumentException("not found")

        val ex = assertThrows<IllegalArgumentException> { spy.deployDocumentObjects(toDeploy) }

        assertEquals("not found", ex.message)
        verify(exactly = 1) { documentObjectRepository.list(any()) }
        verify(exactly = 1) { documentObjectRepository.findModelOrFail(any()) }
    }

    @Test
    fun `deployDocumentObjects continues deployment when there is exception during document build`() {
        // given
        val innerBlock = aDocObj("B_2")
        val block = mockObj(aDocObj("B_1", DocumentObjectType.Block, listOf(aDocumentObjectRef(innerBlock.id))))
        val template = mockObj(aDocObj("T_1", DocumentObjectType.Template, listOf(aDocumentObjectRef(block.id))))

        every { ipsService.close() } just runs
        every { documentObjectRepository.findModel(innerBlock.id) } throws IllegalStateException("Not found")
        every { documentObjectRepository.list(any()) } returns listOf(template, block)
        every { documentObjectBuilder.buildDocumentObject(block) } throws IllegalStateException("Inner block not found")

        // when
        val result = subject.deployDocumentObjects()

        // then
        result.deployed.shouldBeEqualTo(
            listOf(
                DeploymentInfo(
                    "T_1", ResourceType.DocumentObject, "icm://${template.nameOrId()}"
                )
            )
        )
        result.errors.shouldBeEqualTo(
            listOf(
                DeploymentError("B_1", "Not found"), DeploymentError("B_1", "Inner block not found")
            )
        )

        verify(exactly = 1) { ipsService.xml2wfd(any(), "icm://${template.nameOrId()}") }
    }

    private fun mockImg(image: ImageModel): ImageModel {
        val imagePath = "icm://${image.nameOrId()}"

        every { documentObjectBuilder.getImagePath(image) } returns imagePath
        every { imageRepository.findModel(image.id) } returns image
        if (!image.sourcePath.isNullOrBlank()) {
            val byteArray = ByteArray(10)
            every { storage.read(image.sourcePath) } returns byteArray
            every { ipsService.tryUpload(imagePath, byteArray) } returns OperationResult.Success
        }

        return image
    }

    private fun mockObj(documentObject: DocumentObjectModel): DocumentObjectModel {
        every { documentObjectRepository.findModel(documentObject.id) } returns documentObject

        if (documentObject.internal == false) {
            val xml = "<xml>${documentObject.nameOrId()}</xml>"
            val outputPath = "icm://${documentObject.nameOrId()}"

            every { documentObjectBuilder.buildDocumentObject(documentObject) } returns xml
            every { documentObjectBuilder.getDocumentObjectPath(documentObject) } returns outputPath
            every { ipsService.xml2wfd(xml, outputPath) } returns OperationResult.Success
        }
        return documentObject
    }
}