@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.service.deploy

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.data.Active
import com.quadient.migration.data.Deployed
import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.data.Error
import com.quadient.migration.data.ImageModel
import com.quadient.migration.data.ImageModelRef
import com.quadient.migration.data.ParagraphModel
import com.quadient.migration.data.ParagraphModel.TextModel
import com.quadient.migration.data.StringModel
import com.quadient.migration.data.VariableModelRef
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.repository.ImageInternalRepository
import com.quadient.migration.persistence.repository.ParagraphStyleInternalRepository
import com.quadient.migration.persistence.repository.TextStyleInternalRepository
import com.quadient.migration.service.Storage
import com.quadient.migration.service.inspirebuilder.InteractiveDocumentObjectBuilder
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.service.ipsclient.OperationResult
import com.quadient.migration.service.resolveTargetDir
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.ImageType
import com.quadient.migration.tools.aActiveStatus
import com.quadient.migration.tools.aBlockModel
import com.quadient.migration.tools.aDeployedStatus
import com.quadient.migration.tools.aErrorStatus
import com.quadient.migration.tools.aProjectConfig
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
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class InteractiveDeployClientTest {
    val documentObjectRepository = mockk<DocumentObjectInternalRepository>()
    val imageRepository = mockk<ImageInternalRepository>()
    val textStyleRepository = mockk<TextStyleInternalRepository>()
    val paragraphStyleRepository = mockk<ParagraphStyleInternalRepository>()
    val statusTrackingRepository = mockk<StatusTrackingRepository>()
    val documentObjectBuilder = mockk<InteractiveDocumentObjectBuilder>()
    val ipsService = mockk<IpsService>()
    val storage = mockk<Storage>()
    val config = aProjectConfig(targetDefaultFolder = "defaultFolder")
    val tenant = config.interactiveTenant

    private val subject = InteractiveDeployClient(
        documentObjectRepository,
        imageRepository,
        statusTrackingRepository,
        textStyleRepository,
        paragraphStyleRepository,
        documentObjectBuilder,
        ipsService,
        storage,
        config
    )

    @BeforeEach
    fun setupAll() {
        every { documentObjectBuilder.shouldIncludeInternalDependency(any()) } answers {
            firstArg<DocumentObjectModel>().internal
        }
        every { ipsService.writeMetadata(any()) } just runs
        every { ipsService.setProductionApprovalState(any()) } returns OperationResult.Success
    }

    @Test
    fun `deployDocumentObjects deploys external document objects as standalone jld files and approves them`() {
        // given
        mockBasicDocumentObjects()
        mockBasicSuccessfulIpsOperations()
        every { documentObjectBuilder.buildDocumentObject(any(), any()) } returns "<xml />"
        every { documentObjectRepository.findModel(any()) } returns aBlock("99", internal = false)
        every { statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any()) } returns Active()
        every { statusTrackingRepository.deployed(any(), any<Uuid>(), any(), any(), any(), any(), any()) } returns aDeployedStatus("id")

        // when
        subject.deployDocumentObjects()

        // then
        verifyBasicIpsOperations(
            listOf(
                "icm://Interactive/$tenant/Blocks/defaultFolder/0.jld",
                "icm://Interactive/$tenant/Blocks/defaultFolder/1.jld",
                "icm://Interactive/$tenant/Templates/defaultFolder/0.jld"
            )
        )
        verify(exactly = 3) { documentObjectBuilder.buildDocumentObject(any(), any()) }
    }

    @Test
    fun `deployDocumentObjects omits internal document object from direct deploy and approval`() {
        // given
        val externalBlock = mockDocumentObject(
            aBlock(
                "1", listOf(
                    ParagraphModel(
                        listOf(TextModel(listOf(StringModel("some text"), VariableModelRef("V_1")), null, null)),
                        null,
                        null
                    )
                ), internal = false
            )
        )
        mockDocumentObject(
            aBlock(
                "2", listOf(
                    ParagraphModel(
                        listOf(TextModel(listOf(StringModel("inner text"), VariableModelRef("V_2")), null, null)),
                        null,
                        null
                    )
                ), internal = true
            )
        )
        val template =
            mockDocumentObject(aTemplate("3", listOf(aDocumentObjectRef("block1"), aDocumentObjectRef("block2"))))

        every { documentObjectRepository.list(any()) } returns listOf(externalBlock, template)
        every { documentObjectBuilder.buildDocumentObject(any(), any()) } returns "<xml />"
        every { documentObjectRepository.findModel(any()) } returns aBlock("99", internal = false)
        every { statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any()) } returns Active()
        every { statusTrackingRepository.deployed(any(), any<Uuid>(), any(), any(), any(), any(), any()) } returns aDeployedStatus("id")

        mockBasicSuccessfulIpsOperations()

        // when
        subject.deployDocumentObjects()

        // then
        verifyBasicIpsOperations(
            listOf(
                "icm://Interactive/$tenant/Blocks/defaultFolder/1.jld",
                "icm://Interactive/$tenant/Templates/defaultFolder/3.jld"
            )
        )
        verify(exactly = 2) { documentObjectBuilder.buildDocumentObject(any(), any()) }
    }

    @Test
    fun `deployDocumentObjects keeps running even if deployment of one document object fails`() {
        // given
        mockBasicDocumentObjects()
        mockBasicSuccessfulIpsOperations()
        every {
            ipsService.deployJld(
                any(), any(), any(), any(), "icm://Interactive/$tenant/Blocks/defaultFolder/0.jld"
            )
        } returns OperationResult.Failure("Problem")
        every { documentObjectBuilder.buildDocumentObject(any(), any()) } returns "<xml />"
        every { documentObjectRepository.findModel(any()) } returns aBlock("99", internal = false)
        every { statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any()) } returns Active()
        every { statusTrackingRepository.deployed(any(), any<Uuid>(), any(), any(), any(), any(), any()) } returns aDeployedStatus("id")
        every { statusTrackingRepository.error(any(), any(), any(), any(), any(), any(), any(), any()) } returns aErrorStatus("id")

        // when
        subject.deployDocumentObjects()

        // then
        verify(exactly = 3) { ipsService.deployJld(any(), any(), any(), any(), any()) }
        verify {
            ipsService.setProductionApprovalState(
                listOf(
                    "icm://Interactive/$tenant/Blocks/defaultFolder/1.jld",
                    "icm://Interactive/$tenant/Templates/defaultFolder/0.jld"
                )
            )
        }
        verify(exactly = 3) { documentObjectBuilder.buildDocumentObject(any(), any()) }
    }

    @Test
    fun `deployDocumentObjects omits unsupported document objects`() {
        // given
        val block = mockDocumentObject(
            aBlock(
                "1", listOf(
                    ParagraphModel(
                        listOf(TextModel(listOf(StringModel("some text"), VariableModelRef("V_1")), null, null)),
                        null,
                        null
                    )
                )
            )
        )
        aBlock("2", type = DocumentObjectType.Unsupported)
        val template =
            mockDocumentObject(aTemplate("3", listOf(aDocumentObjectRef("block1"), aDocumentObjectRef("block2"))))

        every { documentObjectRepository.list(any()) } returns listOf(block, template)
        every { documentObjectBuilder.buildDocumentObject(any(), any()) } returns "<xml />"
        every { documentObjectRepository.findModel(any()) } returns aBlock("99", internal = false)
        every { statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any()) } returns Active()
        every { statusTrackingRepository.deployed(any(), any<Uuid>(), any(), any(), any(), any(), any()) } returns aDeployedStatus("id")

        mockBasicSuccessfulIpsOperations()

        // when
        subject.deployDocumentObjects()

        // then
        verifyBasicIpsOperations(
            listOf(
                "icm://Interactive/$tenant/Blocks/defaultFolder/1.jld",
                "icm://Interactive/$tenant/Templates/defaultFolder/3.jld"
            )
        )
        verify(exactly = 2) { documentObjectBuilder.buildDocumentObject(any(), any()) }
    }

    @Test
    fun `deployDocumentObjects deploys images when used in document objects`() {
        // given
        val image = mockImage(aImage("Bunny"))
        val block = mockDocumentObject(aBlock(id = "1", listOf(ImageModelRef(image.id))))

        every { documentObjectRepository.list(any()) } returns listOf(block)
        every { documentObjectBuilder.buildDocumentObject(any(), any()) } returns "<xml />"
        every { ipsService.tryUpload(any(), any()) } returns OperationResult.Success
        every { statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any()) } returns Active()
        every { statusTrackingRepository.deployed(any(), any<Uuid>(), any(), any(), any(), any(), any()) } returns aDeployedStatus("id")

        mockBasicSuccessfulIpsOperations()
        val expectedImageIcmPath = "icm://Interactive/$tenant/Resources/Images/defaultFolder/${image.sourcePath}"

        // when
        subject.deployDocumentObjects()

        // then
        verify { ipsService.tryUpload(expectedImageIcmPath, any()) }
        verifyBasicIpsOperations(
            listOf(
                expectedImageIcmPath, "icm://Interactive/$tenant/Blocks/defaultFolder/${block.id}.jld"
            ), 1
        )
    }

    @Test
    fun `Images with unknown type or missing source path are omitted from deployment`() {
        // given
        val catImage = mockImage(aImage("Cat", imageType = ImageType.Unknown))
        val dogImage = mockImage(aImage("Dog", sourcePath = null))

        val block = mockDocumentObject(
            aBlock("1", listOf(ImageModelRef(catImage.id), ImageModelRef(dogImage.id)))
        )

        every { documentObjectRepository.list(any()) } returns listOf(block)
        every { documentObjectBuilder.buildDocumentObject(any(), any()) } returns "<xml />"
        every { ipsService.upload(any(), any()) } just runs
        every { statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any()) } returns Active()
        every { statusTrackingRepository.deployed(any(), any<Uuid>(), any(), any(), any(), any(), any()) } returns aDeployedStatus("id")
        every { statusTrackingRepository.error(any(), any(), any(), any(), any(), any(), any(), any()) } returns aErrorStatus("id")

        mockBasicSuccessfulIpsOperations()

        // when
        subject.deployDocumentObjects()

        // then
        verify(exactly = 0) { ipsService.upload(any(), any()) }
        verifyBasicIpsOperations(listOf("icm://Interactive/$tenant/Blocks/defaultFolder/${block.id}.jld"))
    }

    @Test
    fun `Multiple times used image is deployed only once`() {
        // given
        val image = mockImage(aImage("Bunny"))
        val innerBlock = aBlock("10", listOf(ImageModelRef(image.id)), internal = true)
        val block = mockDocumentObject(
            aBlock(
                "1", listOf(
                    aDocumentObjectRef(innerBlock.id),
                    aParagraph(aText(ImageModelRef(image.id)))
                )
            )
        )

        every { documentObjectRepository.list(any()) } returns listOf(block)
        every { documentObjectRepository.findModel(innerBlock.id) } returns innerBlock
        every { documentObjectBuilder.buildDocumentObject(any(), any()) } returns "<xml />"
        every { statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any()) } returns Active()
        every { statusTrackingRepository.deployed(any(), any<Uuid>(), any(), any(), any(), any(), any()) } returns aDeployedStatus("id")
        every { ipsService.tryUpload(any(), any()) } returns OperationResult.Success

        mockBasicSuccessfulIpsOperations()
        val expectedImageIcmPath = "icm://Interactive/$tenant/Resources/Images/defaultFolder/${image.sourcePath}"

        // when
        subject.deployDocumentObjects()

        // then
        verify(exactly = 1) { ipsService.tryUpload(expectedImageIcmPath, any()) }
        verifyBasicIpsOperations(
            listOf(
                expectedImageIcmPath, "icm://Interactive/$tenant/Blocks/defaultFolder/${block.id}.jld"
            ), 1
        )
    }

    @Test
    fun `deployStyles creates style definition and sets production approval state`() {
        // given
        every { documentObjectBuilder.buildStyles(any(), any()) } returns "<xml />"

        every { statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any()) } returns Active()
        every { statusTrackingRepository.deployed(any(), any<Uuid>(), any(), any(), any(), any()) } returns aDeployedStatus("id")
        every { textStyleRepository.listAllModel() } returns emptyList()
        every { paragraphStyleRepository.listAllModel() } returns emptyList()
        every { ipsService.xml2wfd(any(), any()) } returns OperationResult.Success
        every { ipsService.setProductionApprovalState(any()) } returns OperationResult.Success

        val definitionPath =
            "icm://Interactive/${config.interactiveTenant}/CompanyStyles/defaultFolder/${config.name}Styles.wfd"

        every { documentObjectBuilder.getStyleDefinitionPath() } returns definitionPath

        // when
        subject.deployStyles()

        // then
        verify { ipsService.xml2wfd(eq("<xml />"), eq(definitionPath)) }
        verify { ipsService.setProductionApprovalState(eq(listOf(definitionPath))) }
    }

    @Test
    fun `deployStyles does not continue when wfd creation fails`() {
        // given
        every { documentObjectBuilder.buildStyles(any(), any()) } returns "<xml />"

        every { statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any()) } returns Active()
        every { statusTrackingRepository.deployed(any(), any<Uuid>(), any(), any(), any(), any()) } returns aDeployedStatus("id")
        every { textStyleRepository.listAllModel() } returns emptyList()
        every { paragraphStyleRepository.listAllModel() } returns emptyList()
        every { ipsService.xml2wfd(any(), any()) } returns OperationResult.Failure("Problem")

        val definitionPath =
            "icm://Interactive/${config.interactiveTenant}/CompanyStyles/defaultFolder/${config.name}Styles.wfd"

        every { documentObjectBuilder.getStyleDefinitionPath() } returns definitionPath

        // when
        subject.deployStyles()

        // then
        verify { ipsService.xml2wfd(eq("<xml />"), eq(definitionPath)) }
        verify(exactly = 0) { ipsService.setProductionApprovalState(any()) }
    }

    @Test
    fun `deployOrder is correct`() {
        val list = listOf(
            aBlockModel("a", content = listOf(aDocumentObjectRef("b"))),
            aBlockModel("a2", content = listOf(aDocumentObjectRef("b"))),
            aBlockModel("a3", content = listOf(aDocumentObjectRef("b"))),
            aBlockModel("b", content = listOf(aDocumentObjectRef("c"))),
            aBlockModel("c", content = listOf()),
            aBlockModel("d", content = listOf(aDocumentObjectRef("f"))),
            aBlockModel("e", content = listOf()),
            aBlockModel("f", content = listOf()),
        )

        val result = subject.deployOrder(list)

        result.map { it.id }.shouldBeEqualTo(listOf("c", "e", "f", "b", "d", "a", "a2", "a3"))
    }

    @Test
    fun `deployOrder has missing object`() {
        val list = listOf(
            aBlockModel("a", content = listOf(aDocumentObjectRef("b"))),
            aBlockModel("a2", content = listOf(aDocumentObjectRef("b"))),
            aBlockModel("a3", content = listOf(aDocumentObjectRef("b"))),
            aBlockModel("b", content = listOf(aDocumentObjectRef("c"))),
            // c is missing
            aBlockModel("d", content = listOf(aDocumentObjectRef("f"))),
            aBlockModel("e", content = listOf()),
            aBlockModel("f", content = listOf()),
        )

        val result = subject.deployOrder(list)

        result.map { it.id }.shouldBeEqualTo(listOf("b", "e", "f", "a", "a2", "a3", "d"))
    }

    @Test
    fun `deployOrder fails on recursive dependency`() {
        val list = listOf(
            aBlockModel("a", content = listOf(aDocumentObjectRef("b"))),
            aBlockModel("b", content = listOf(aDocumentObjectRef("c"))),
            aBlockModel("c", content = listOf(aDocumentObjectRef("b"))),
        )

        val result = assertThrows<RuntimeException> { subject.deployOrder(list) }

        result.message.shouldBeEqualTo("Cannot determine deploy order. Either circular reference or some references are missing.")
    }

    @Test
    fun `deploy list of document objects validates that no document objects are unsupported`() {
        val spy = spyk(subject)
        every { spy.deployDocumentObjectsInternal(any()) } returns DeploymentResult(Uuid.random())
        every { documentObjectRepository.list(any()) } returns listOf(
            aBlock(id = "1", type = DocumentObjectType.Unsupported),
            aBlock(id = "2", type = DocumentObjectType.Block),
            aBlock(id = "3", type = DocumentObjectType.Unsupported),
        )

        val ex = assertThrows<IllegalArgumentException> { spy.deployDocumentObjects(listOf("1", "2", "3")) }

        assertEquals("The following document objects are unsupported: [1, 3]. ", ex.message)
        verify(exactly = 1) { documentObjectRepository.list(any()) }
    }

    @Test
    fun `deploy list of document objects validates that no document objects are internal`() {
        val spy = spyk(subject)
        every { spy.deployDocumentObjectsInternal(any()) } returns DeploymentResult(Uuid.random())
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
        every { spy.deployDocumentObjectsInternal(any()) } returns DeploymentResult(Uuid.random())
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
        every { spy.deployDocumentObjectsInternal(any()) } returns DeploymentResult(Uuid.random())
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
            "The following document objects were not found: [8]. The following document objects are unsupported: [6]. The following document objects are internal: [2]. ",
            ex.message
        )
        verify(exactly = 1) { documentObjectRepository.list(any()) }
    }

    @Test
    fun `deploy list of document objects without dependencies`() {
        val spy = spyk(subject)
        every { spy.deployDocumentObjectsInternal(any()) } returns DeploymentResult(Uuid.random())
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
        every { spy.deployDocumentObjectsInternal(any()) } returns DeploymentResult(Uuid.random())
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
        every { spy.deployDocumentObjectsInternal(any()) } returns DeploymentResult(Uuid.random())
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

        mockBasicSuccessfulIpsOperations()
        every { statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any()) } returns Active()
        every { statusTrackingRepository.deployed(any(), any<Uuid>(), any(), any(), any(), any(), any()) } returns aDeployedStatus("id")
        every { documentObjectRepository.findModel(innerBlock.id) } throws IllegalStateException("Not found")
        every { documentObjectRepository.list(any()) } returns listOf(template, block)
        every { documentObjectBuilder.buildDocumentObject(block, any()) } throws IllegalStateException("Inner block not found")

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

        verify(exactly = 1) { ipsService.deployJld(any(), any(), any(), any(), "icm://${template.nameOrId()}") }
    }

    private fun mockBasicDocumentObjects() {
        val blocks = List(2) {
            mockDocumentObject(
                aBlock(
                    it.toString(), listOf(
                        ParagraphModel(
                            listOf(
                                TextModel(
                                    listOf(StringModel("some text $it"), VariableModelRef("some var $it")), null, null
                                )
                            ), null, null
                        )
                    )
                )
            )
        }
        val templates = List(1) {
            mockDocumentObject(aTemplate(it.toString(), listOf(aDocumentObjectRef("block$it"))))
        }
        every { documentObjectRepository.list(any()) } returns blocks + templates
    }

    private fun mockDocumentObject(documentObject: DocumentObjectModel): DocumentObjectModel {
        val interactiveFolder = documentObject.type.toInteractiveFolder()

        val dir = resolveTargetDir(config.defaultTargetFolder, documentObject.targetFolder)
        every { documentObjectBuilder.getDocumentObjectPath(documentObject) } returns "icm://Interactive/$tenant/$interactiveFolder/$dir/${documentObject.nameOrId()}.jld"
        every { documentObjectRepository.findModel(documentObject.id) } returns documentObject

        return documentObject
    }

    private fun mockImage(image: ImageModel, success: Boolean = true): ImageModel {
        val dir = resolveTargetDir(config.defaultTargetFolder)
        every { documentObjectBuilder.getImagePath(image) } returns "icm://Interactive/$tenant/Resources/Images/$dir/${image.sourcePath}"

        every { imageRepository.findModel(image.id) } returns if (success) { image } else { null }
        if (!image.sourcePath.isNullOrBlank()) {
            every { storage.read(image.sourcePath) } returns ByteArray(10)
        }

        return image
    }

    private fun mockBasicSuccessfulIpsOperations() {
        every {
            ipsService.deployJld(any(), any(), any(), any(), any())
        } returns OperationResult.Success
        every { ipsService.setProductionApprovalState(any()) } returns OperationResult.Success
    }

    private fun verifyBasicIpsOperations(expectedOutputPaths: List<String>, deployCount: Int? = null) {
        val deployCountValue = deployCount ?: expectedOutputPaths.size
        verify(exactly = deployCountValue) { ipsService.deployJld(any(), any(), any(), any(), any()) }
        verify { ipsService.setProductionApprovalState(expectedOutputPaths) }
    }

    private fun mockObj(documentObject: DocumentObjectModel): DocumentObjectModel {
        every { documentObjectRepository.findModel(documentObject.id) } returns documentObject

        if (!documentObject.internal) {
            val xml = "<xml>${documentObject.nameOrId()}</xml>"
            val outputPath = "icm://${documentObject.nameOrId()}"

            every { documentObjectBuilder.buildDocumentObject(documentObject, any()) } returns xml
            every { documentObjectBuilder.getDocumentObjectPath(documentObject) } returns outputPath
        }
        return documentObject
    }

    @Nested
    inner class StatusTrackingTests {
        @BeforeEach
        fun setup() {
            every { documentObjectBuilder.buildDocumentObject(any(), any()) } returns ""
            every { statusTrackingRepository.deployed(any(), any<Uuid>(), any(), any(), any(), any(), any()) } returns  aDeployedStatus("id")
            every { statusTrackingRepository.error(any(), any(), any(), any(), any(), any(), any(), any()) } returns aErrorStatus("id")
            every { statusTrackingRepository.active(any(), any()) } returns aActiveStatus("id")
            every { documentObjectBuilder.getDocumentObjectPath(any()) } returns "icm://path"
            every { ipsService.setProductionApprovalState(any()) } returns OperationResult.Success
            every { ipsService.tryUpload(any(), any()) } returns OperationResult.Success
            every { ipsService.deployJld(any(), any(), any(), any(), any()) } returns OperationResult.Success
        }

        @Test
        fun `deployDocumentObjects does not do anything when all objects are deployed`() {
            // given
            val docObjects = listOf(
                aDocObj("D_1", content = listOf(ImageModelRef("I_1"))),
                aDocObj("D_2", content = listOf(ImageModelRef("I_2"))),
                aDocObj("D_3", content = listOf(ImageModelRef("I_3"))),
            )
            givenObjectIsDeployed("D_1")
            givenObjectIsDeployed("I_1")
            givenObjectIsDeployed("D_2")
            givenObjectIsDeployed("I_2")
            givenObjectIsDeployed("D_3")
            givenObjectIsDeployed("I_3")

            // when
            subject.deployDocumentObjectsInternal(docObjects)

            // then
            verify(exactly = 0) { documentObjectBuilder.buildDocumentObject(any(), any()) }
            verify(exactly = 0) { imageRepository.findModel(any()) }
        }

        @Test
        fun `deployDocumentObjects skips deployed but deploys active and error`() {
            // given
            val docObjects = listOf(
                aDocObj("D_1", content = listOf(ImageModelRef("I_1"))),
                aDocObj("D_2", content = listOf(ImageModelRef("I_2"))),
                aDocObj("D_3", content = listOf(ImageModelRef("I_3"))),
            )
            mockImage(aImage("I_1"))
            mockImage(aImage("I_2"))
            givenObjectIsDeployed("D_1")
            givenObjectIsError("I_1")
            givenObjectIsError("D_2")
            givenObjectIsActive("I_2")
            givenObjectIsActive("D_3")
            givenObjectIsDeployed("I_3")
            every { ipsService.xml2wfd(any(), any()) } returns OperationResult.Success


            // when
            subject.deployDocumentObjectsInternal(docObjects)

            // then
            verify(exactly = 2) { documentObjectBuilder.buildDocumentObject(any(), any()) }
            verify(exactly = 1) { statusTrackingRepository.deployed("D_2", any<Uuid>(), any(), any(), any(), any(), any()) }
            verify(exactly = 1) { statusTrackingRepository.deployed("D_3", any<Uuid>(), any(), any(), any(), any(), any()) }
            verify(exactly = 1) { statusTrackingRepository.deployed("I_1", any<Uuid>(), any(), any(), any(), any(), any()) }
            verify(exactly = 1) { statusTrackingRepository.deployed("I_2", any<Uuid>(), any(), any(), any(), any(), any()) }
        }

        @Test
        fun `deployDocumentObjects records errors`() {
            // given
            val docObjects = listOf(aDocObj("D_1", content = listOf(ImageModelRef("I_1"))))
            givenObjectIsActive("D_1")
            givenObjectIsActive("I_1")
            mockImage(aImage("I_1"), success = false)
            every { ipsService.deployJld(any(), any(), any(), any(), any()) } returns
                    OperationResult.Failure("oops")

            // when
            subject.deployDocumentObjectsInternal(docObjects)

            // then
            verify(exactly = 1) { documentObjectBuilder.buildDocumentObject(any(), any()) }
            verify(exactly = 1) { statusTrackingRepository.error("D_1", any(), any(), any(), any(), any(), "oops", any()) }
            verify(exactly = 1) { statusTrackingRepository.error("I_1", any(), any(), any(), any(), any(), any(), any()) }
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
                icmPath = "icm://path",
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
                icmPath = "icm://path"
            )
        }
    }
}