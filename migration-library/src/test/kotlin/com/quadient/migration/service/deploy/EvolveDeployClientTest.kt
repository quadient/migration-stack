@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.service.deploy

import com.quadient.migration.api.EvolveConfig
import com.quadient.migration.api.InspireConfig
import com.quadient.migration.api.MigConfig
import com.quadient.migration.api.dto.migrationmodel.builder.AttachmentBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.DisplayRuleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ImageBuilder
import com.quadient.migration.api.repository.AttachmentRepository
import com.quadient.migration.api.repository.DisplayRuleRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ImageRepository
import com.quadient.migration.api.repository.ParagraphStyleRepository
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.api.repository.TextStyleRepository
import com.quadient.migration.api.repository.VariableRepository
import com.quadient.migration.api.repository.VariableStructureRepository
import com.quadient.migration.service.Storage
import com.quadient.migration.service.deploy.utility.DeploymentInfo
import com.quadient.migration.service.deploy.utility.DeploymentResult
import com.quadient.migration.service.deploy.utility.MetadataValidatorImpl
import com.quadient.migration.service.deploy.utility.PostProcessImpl
import com.quadient.migration.service.deploy.utility.PostProcessor
import com.quadient.migration.service.deploy.utility.ConflictDetectorImpl
import com.quadient.migration.service.deploy.utility.ProgressReporterImpl
import com.quadient.migration.service.deploy.utility.ResourceType
import com.quadient.migration.service.inspirebuilder.InteractiveDocumentObjectBuilder
import com.quadient.migration.service.InteractiveResourcePathProvider
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.service.ipsclient.OperationResult
import com.quadient.migration.service.ipsclient.Version
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.toIcmPath
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.shouldBeEmpty
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeOfInstance
import com.quadient.migration.tools.shouldBeOfSize
import com.quadient.migration.tools.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class EvolveDeployClientTest {
    val metadataValidator = MetadataValidatorImpl()
    val documentObjectRepository = mockk<DocumentObjectRepository>()
    val imageRepository = mockk<ImageRepository>()
    val attachmentRepository = mockk<AttachmentRepository>()
    val textStyleRepository = mockk<TextStyleRepository>()
    val paragraphStyleRepository = mockk<ParagraphStyleRepository>()
    val displayRuleRepository = mockk<DisplayRuleRepository>()
    val variableRepository = mockk<VariableRepository>()
    val variableStructureRepository = mockk<VariableStructureRepository>()
    val statusTrackingRepository = mockk<StatusTrackingRepository>()
    val documentObjectBuilder = mockk<InteractiveDocumentObjectBuilder>()
    val ipsService = mockk<IpsService>()
    val storage = mockk<Storage>()
    val caClient = mockk<CaApiClient>()
    val resourcePathProvider = mockk<InteractiveResourcePathProvider>()
    val postProcess = mockk<PostProcessImpl>(relaxed = true)

    val evolveConfig = EvolveConfig(
        apiRetryDelayMs = 0L,
        contentAuthorApiKey = "test-api-key",
        companyUrl = "http://localhost:8080",
        holder = "testHolder",
        holderType = "testHolderType",
        publishBlockActionId = "publishBlock",
        publishTemplateActionId = "publishTemplate",
        publishRuleActionId = "publishRule",
    )

    val migConfig = MigConfig(inspireConfig = InspireConfig(evolveConfig = evolveConfig))

    val projectConfig = aProjectConfig(
        baseTemplatePath = "icm://Interactive/tenant/BaseTemplates/templ.wfd",
        interactiveTenant = "tenant",
        targetDefaultFolder = "defaultFolder"
    )

    val conflictDetector = ConflictDetectorImpl(documentObjectRepository, imageRepository, attachmentRepository, displayRuleRepository, statusTrackingRepository, resourcePathProvider, projectConfig.inspireOutput)
    val progressReporter = ProgressReporterImpl(documentObjectRepository, imageRepository, attachmentRepository, displayRuleRepository, documentObjectBuilder, statusTrackingRepository, resourcePathProvider, projectConfig.inspireOutput)

    private val subject = EvolveDeployClient(
        projectConfig,
        migConfig,
        caClient,
        resourcePathProvider,
        metadataValidator,
        postProcess,
        conflictDetector,
        progressReporter,
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
        storage,
    )

    private val jld = byteArrayOf(1, 2, 3)
    private val draftGuid = "draft-guid-123"
    private val ruleGuid = "rule-guid-456"
    private val baseTemplatePath = "icm://Interactive/tenant/BaseTemplates/templ.wfd".toIcmPath()

    @BeforeEach
    fun setup() {
        every { documentObjectBuilder.shouldIncludeInternalDependency(any()) } answers {
            val documentObject = firstArg<com.quadient.migration.api.dto.migrationmodel.DocumentObject>()
            (documentObject.internal ?: false) || documentObject.type == DocumentObjectType.Page
        }
        every { ipsService.deployJld(any<IcmPath>(), any<String>(), any<String>(), any<String>(), any<String>()) } returns OperationResult.Success
        every { ipsService.download(any<String>()) } returns jld
        every { ipsService.delete(any<String>()) } returns true
    }

    @Test
    fun `deployStyles throws IllegalStateException`() {
        assertThrows<IllegalStateException> { subject.deployStyles() }
    }

    @Test
    fun `uploadImage delegates to caClient and returns Success`() {
        val image = ImageBuilder("img1").build()
        val targetPath = "icm://path/to/img1.jpg".toIcmPath()
        val data = byteArrayOf(1, 2, 3)
        every { caClient.uploadResource(targetPath, data) } returns HttpResult.Success(Unit)
        every { ipsService.tryUpload(targetPath, data) } returns OperationResult.Success

        val result = subject.uploadImage(image, targetPath, data)

        result.shouldBeOfInstance<OperationResult.Success>()
        verify { caClient.uploadResource(targetPath, data) }
    }

    @Test
    fun `uploadImage returns Failure when caClient returns Failure`() {
        val image = ImageBuilder("img1").build()
        val targetPath = "icm://path/to/img1.jpg".toIcmPath()
        val data = byteArrayOf(1, 2, 3)
        val error = ApiBadRequestException(status = 400, title = "Bad Request", detail = "Invalid image")
        every { caClient.uploadResource(targetPath, data) } returns HttpResult.Failure(error)
        every { ipsService.tryUpload(targetPath, data) } returns OperationResult.Success

        val result = subject.uploadImage(image, targetPath, data)

        result.shouldBeOfInstance<OperationResult.Failure>()
        (result as OperationResult.Failure).message.shouldBeEqualTo("CA API error 400: Bad Request - Invalid image")
    }

    @Test
    fun `uploadImage returns Failure when caClient returns Exception`() {
        val image = ImageBuilder("img1").build()
        val targetPath = "icm://path/to/img1.jpg".toIcmPath()
        val data = byteArrayOf(1, 2, 3)
        every { caClient.uploadResource(targetPath, data) } returns HttpResult.Exception(RuntimeException("connection refused"))
        every { ipsService.tryUpload(targetPath, data) } returns OperationResult.Success

        val result = subject.uploadImage(image, targetPath, data)

        result.shouldBeOfInstance<OperationResult.Failure>()
        (result as OperationResult.Failure).message.shouldBeEqualTo("connection refused")
    }

    @Test
    fun `uploadAttachment delegates to caClient and returns Success`() {
        val attachment = AttachmentBuilder("att1").build()
        val targetPath = "icm://path/to/att1.pdf".toIcmPath()
        val data = byteArrayOf(4, 5, 6)
        every { caClient.uploadResource(targetPath, data) } returns HttpResult.Success(Unit)

        val result = subject.uploadAttachment(attachment, targetPath, data)

        result.shouldBeOfInstance<OperationResult.Success>()
        verify { caClient.uploadResource(targetPath, data) }
    }

    @Test
    fun `uploadAttachment returns Failure when caClient returns Failure`() {
        val attachment = AttachmentBuilder("att1").build()
        val targetPath = "icm://path/to/att1.pdf".toIcmPath()
        val data = byteArrayOf(4, 5, 6)
        val error = ApiBadRequestException(status = 500, title = "Internal Server Error", detail = "Something went wrong")
        every { caClient.uploadResource(targetPath, data) } returns HttpResult.Failure(error)

        val result = subject.uploadAttachment(attachment, targetPath, data)

        result.shouldBeOfInstance<OperationResult.Failure>()
        (result as OperationResult.Failure).message.shouldBeEqualTo("CA API error 500: Internal Server Error - Something went wrong")
    }

    @Test
    fun `uploadDocumentObject for Template creates template draft and executes publish action`() {
        val template = DocumentObjectBuilder("T1", DocumentObjectType.Template).build()
        val targetPath = "icm://Interactive/tenant/T1.jld".toIcmPath()
        val draftResult = DraftJsonIpsResult(
            draft = CreateDraftResult(guid = draftGuid, url = "http://example.com"),
            result = CreateIcmObjectResult(valid = true)
        )
        every { caClient.createTemplateDraft(any(), any(), any(), any()) } returns HttpResult.Success(draftResult)
        every { caClient.executeAction(evolveConfig.publishTemplateActionId, draftGuid, ObjectType.TemplateDraft) } returns HttpResult.Success(Unit)

        val result = subject.uploadDocumentObject(template, targetPath, "<xml/>")

        result.shouldBeOfInstance<OperationResult.Success>()
        verify { caClient.createTemplateDraft(template.nameOrId(), any(), baseTemplatePath, jld) }
        verify { caClient.executeAction(evolveConfig.publishTemplateActionId, draftGuid, ObjectType.TemplateDraft) }
    }

    @Test
    fun `uploadDocumentObject for Page creates template draft and executes publish template action`() {
        val page = DocumentObjectBuilder("P1", DocumentObjectType.Page).build()
        val targetPath = "icm://Interactive/tenant/P1.jld".toIcmPath()
        val draftResult = DraftJsonIpsResult(
            draft = CreateDraftResult(guid = draftGuid, url = "http://example.com"),
            result = CreateIcmObjectResult(valid = true)
        )
        every { caClient.createTemplateDraft(any(), any(), any(), any()) } returns HttpResult.Success(draftResult)
        every { caClient.executeAction(evolveConfig.publishTemplateActionId, draftGuid, ObjectType.TemplateDraft) } returns HttpResult.Success(Unit)

        val result = subject.uploadDocumentObject(page, targetPath, "<xml/>")

        result.shouldBeOfInstance<OperationResult.Success>()
        verify { caClient.createTemplateDraft(page.nameOrId(), any(), baseTemplatePath, jld) }
        verify { caClient.executeAction(evolveConfig.publishTemplateActionId, draftGuid, ObjectType.TemplateDraft) }
    }

    @Test
    fun `uploadDocumentObject for Block creates block draft and executes publish block action`() {
        val block = DocumentObjectBuilder("B1", DocumentObjectType.Block).build()
        val targetPath = "icm://Interactive/tenant/B1.jld".toIcmPath()
        val draftResult = DraftJsonIpsResult(
            draft = CreateDraftResult(guid = draftGuid, url = "http://example.com"),
            result = CreateIcmObjectResult(valid = true)
        )
        every { caClient.createBlockDraft(any(), any(), any(), any()) } returns HttpResult.Success(draftResult)
        every { caClient.executeAction(evolveConfig.publishBlockActionId, draftGuid, ObjectType.BlockDraft) } returns HttpResult.Success<Unit, ApiBadRequestException>(Unit)

        val result = subject.uploadDocumentObject(block, targetPath, "<xml/>")

        result.shouldBeOfInstance<OperationResult.Success>()
        verify { caClient.createBlockDraft(block.nameOrId(), any(), baseTemplatePath, jld) }
        verify { caClient.executeAction(evolveConfig.publishBlockActionId, draftGuid, ObjectType.BlockDraft) }
    }

    @Test
    fun `uploadDocumentObject for Snippet returns Failure`() {
        val snippet = DocumentObjectBuilder("S1", DocumentObjectType.Snippet).build()
        val targetPath = "icm://Interactive/tenant/S1.jld".toIcmPath()

        val result = subject.uploadDocumentObject(snippet, targetPath, "<xml/>")

        result.shouldBeOfInstance<OperationResult.Failure>()
        (result as OperationResult.Failure).message.shouldBeEqualTo("Snippets are not currently supported in Evolve output")
    }

    @Test
    fun `uploadDocumentObject returns Failure when IPS deployJld fails`() {
        val block = DocumentObjectBuilder("B1", DocumentObjectType.Block).build()
        val targetPath = "icm://Interactive/tenant/B1.jld".toIcmPath()
        every { ipsService.deployJld(any<IcmPath>(), any<String>(), any<String>(), any<String>(), any<String>()) } returns OperationResult.Failure("IPS deploy failed")

        val result = subject.uploadDocumentObject(block, targetPath, "<xml/>")

        result.shouldBeOfInstance<OperationResult.Failure>()
        (result as OperationResult.Failure).message.shouldBeEqualTo("IPS deploy failed")
    }

    @Test
    fun `uploadDocumentObject returns Failure when JLD download fails`() {
        val block = DocumentObjectBuilder("B1", DocumentObjectType.Block).build()
        val targetPath = "icm://Interactive/tenant/B1.jld".toIcmPath()
        every { ipsService.download(any<String>()) } throws RuntimeException("download error")

        val result = subject.uploadDocumentObject(block, targetPath, "<xml/>")

        result.shouldBeOfInstance<OperationResult.Failure>()
        (result as OperationResult.Failure).message.shouldStartWith("Failed to download deployed JLD")
    }

    @Test
    fun `uploadDocumentObject for Template returns Failure when createTemplateDraft fails`() {
        val template = DocumentObjectBuilder("T1", DocumentObjectType.Template).build()
        val targetPath = "icm://Interactive/tenant/T1.jld".toIcmPath()
        val error = ApiBadRequestException(status = 422, title = "Unprocessable", detail = "Invalid template")
        every { caClient.createTemplateDraft(any(), any(), any(), any()) } returns HttpResult.Failure(error)

        val result = subject.uploadDocumentObject(template, targetPath, "<xml/>")

        result.shouldBeOfInstance<OperationResult.Failure>()
        (result as OperationResult.Failure).message.shouldBeEqualTo("CA API error 422: Unprocessable - Invalid template")
    }

    @Test
    fun `uploadDocumentObject for Block returns Failure when createBlockDraft fails`() {
        val block = DocumentObjectBuilder("B1", DocumentObjectType.Block).build()
        val targetPath = "icm://Interactive/tenant/B1.jld".toIcmPath()
        val error = ApiBadRequestException(status = 400, title = "Bad Request", detail = "Invalid block")
        every { caClient.createBlockDraft(any(), any(), any(), any()) } returns HttpResult.Failure(error)

        val result = subject.uploadDocumentObject(block, targetPath, "<xml/>")

        result.shouldBeOfInstance<OperationResult.Failure>()
        (result as OperationResult.Failure).message.shouldBeEqualTo("CA API error 400: Bad Request - Invalid block")
    }

    @Test
    fun `uploadDocumentObject for Template returns Failure when executeAction fails`() {
        val template = DocumentObjectBuilder("T1", DocumentObjectType.Template).build()
        val targetPath = "icm://Interactive/tenant/T1.jld".toIcmPath()
        val draftResult = DraftJsonIpsResult(
            draft = CreateDraftResult(guid = draftGuid, url = "http://example.com"),
            result = CreateIcmObjectResult(valid = true)
        )
        every { caClient.createTemplateDraft(any(), any(), any(), any()) } returns HttpResult.Success(draftResult)
        val error = ApiBadRequestException(status = 500, title = "Server Error", detail = "Action failed")
        every { caClient.executeAction(any(), any(), any()) } returns HttpResult.Failure(error)

        val result = subject.uploadDocumentObject(template, targetPath, "<xml/>")

        result.shouldBeOfInstance<OperationResult.Failure>()
        (result as OperationResult.Failure).message.shouldBeEqualTo("CA API error 500: Server Error - Action failed")
    }

    @Test
    fun `uploadDocumentObject for Block uses custom baseTemplate when set on document object`() {
        val customBaseTemplate = "icm://Interactive/tenant/BaseTemplates/custom.wfd"
        val block = DocumentObjectBuilder("B1", DocumentObjectType.Block).baseTemplate(customBaseTemplate).build()
        val targetPath = "icm://Interactive/tenant/B1.jld".toIcmPath()
        val draftResult = DraftJsonIpsResult(
            draft = CreateDraftResult(guid = draftGuid, url = "http://example.com"),
            result = CreateIcmObjectResult(valid = true)
        )
        every { caClient.createBlockDraft(any(), any(), any(), any()) } returns HttpResult.Success(draftResult)
        every { caClient.executeAction(any(), any(), any()) } returns HttpResult.Success(Unit)

        subject.uploadDocumentObject(block, targetPath, "<xml/>")

        verify { caClient.createBlockDraft(any(), any(), customBaseTemplate.toIcmPath(), any()) }
    }

    @Test
    fun `uploadDocumentObject for Template returns Failure when targetFolder is absolute`() {
        val template = DocumentObjectBuilder("T1", DocumentObjectType.Template).targetFolder("icm://absolute/path").build()
        val targetPath = "icm://Interactive/tenant/T1.jld".toIcmPath()

        val result = subject.uploadDocumentObject(template, targetPath, "<xml/>")

        result.shouldBeOfInstance<OperationResult.Failure>()
        (result as OperationResult.Failure).message.shouldStartWith("TargetFolder")
    }

    @Test
    fun `uploadDocumentObject for Block returns Failure when targetFolder is absolute`() {
        val block = DocumentObjectBuilder("B1", DocumentObjectType.Block).targetFolder("icm://absolute/path").build()
        val targetPath = "icm://Interactive/tenant/B1.jld".toIcmPath()

        val result = subject.uploadDocumentObject(block, targetPath, "<xml/>")

        result.shouldBeOfInstance<OperationResult.Failure>()
        (result as OperationResult.Failure).message.shouldStartWith("TargetFolder")
    }

    @Test
    fun `uploadDocumentObject for Template uses defaultTargetFolder when no specific targetFolder`() {
        val template = DocumentObjectBuilder("T1", DocumentObjectType.Template).build()
        val targetPath = "icm://Interactive/tenant/T1.jld".toIcmPath()
        val draftResult = DraftJsonIpsResult(
            draft = CreateDraftResult(guid = draftGuid, url = "http://example.com"),
            result = CreateIcmObjectResult(valid = true)
        )
        every { caClient.createTemplateDraft(any(), any(), any(), any()) } returns HttpResult.Success(draftResult)
        every { caClient.executeAction(any(), any(), any()) } returns HttpResult.Success(Unit)

        subject.uploadDocumentObject(template, targetPath, "<xml/>")

        verify { caClient.createTemplateDraft(any(), projectConfig.defaultTargetFolder, any(), any()) }
    }

    @Test
    fun `uploadDisplayRule creates rule draft and executes publish rule action`() {
        val rule = DisplayRuleBuilder("R1").name("RuleOne").build()
        val targetPath = "icm://path/to/R1.jrd".toIcmPath()
        val data = byteArrayOf(10, 20, 30)
        val draftResult = CreateDraftWithVFFResult(guid = ruleGuid, url = "http://example.com/rule")
        every { caClient.createRuleDraft(any(), any(), any(), any()) } returns HttpResult.Success(draftResult)
        every { caClient.executeAction(evolveConfig.publishRuleActionId, ruleGuid, ObjectType.RuleDraft) } returns HttpResult.Success<Unit, ApiBadRequestException>(Unit)

        val result = subject.uploadDisplayRule(rule, targetPath, data)

        result.shouldBeOfInstance<OperationResult.Success>()
        verify { caClient.createRuleDraft(rule.nameOrId(), any(), any(), data) }
        verify { caClient.executeAction(evolveConfig.publishRuleActionId, ruleGuid, ObjectType.RuleDraft) }
    }

    @Test
    fun `uploadDisplayRule returns Failure when createRuleDraft fails`() {
        val rule = DisplayRuleBuilder("R1").build()
        val targetPath = "icm://path/to/R1.jrd".toIcmPath()
        val data = byteArrayOf(10, 20, 30)
        val error = ApiBadRequestException(status = 400, title = "Bad Request", detail = "Invalid rule")
        every { caClient.createRuleDraft(any(), any(), any(), any()) } returns HttpResult.Failure(error)

        val result = subject.uploadDisplayRule(rule, targetPath, data)

        result.shouldBeOfInstance<OperationResult.Failure>()
        (result as OperationResult.Failure).message.shouldBeEqualTo("CA API error 400: Bad Request - Invalid rule")
    }

    @Test
    fun `uploadDisplayRule returns Failure when executeAction fails`() {
        val rule = DisplayRuleBuilder("R1").build()
        val targetPath = "icm://path/to/R1.jrd".toIcmPath()
        val data = byteArrayOf(10, 20, 30)
        val draftResult = CreateDraftWithVFFResult(guid = ruleGuid, url = "http://example.com/rule")
        every { caClient.createRuleDraft(any(), any(), any(), any()) } returns HttpResult.Success(draftResult)
        val error = ApiBadRequestException(status = 503, title = "Service Unavailable", detail = "Try later")
        every { caClient.executeAction(any(), any(), any()) } returns HttpResult.Failure(error)

        val result = subject.uploadDisplayRule(rule, targetPath, data)

        result.shouldBeOfInstance<OperationResult.Failure>()
        (result as OperationResult.Failure).message.shouldBeEqualTo("CA API error 503: Service Unavailable - Try later")
    }

    @Test
    fun `uploadDisplayRule returns Failure when targetFolder is absolute`() {
        val rule = DisplayRuleBuilder("R1").targetFolder("icm://absolute/path").build()
        val targetPath = "icm://path/to/R1.jrd".toIcmPath()
        val data = byteArrayOf(10, 20, 30)

        val result = subject.uploadDisplayRule(rule, targetPath, data)

        result.shouldBeOfInstance<OperationResult.Failure>()
        (result as OperationResult.Failure).message.shouldStartWith("TargetFolder")
    }

    @Test
    fun `uploadDisplayRule uses defaultTargetFolder when no specific targetFolder`() {
        val rule = DisplayRuleBuilder("R1").build()
        val targetPath = "icm://path/to/R1.jrd".toIcmPath()
        val data = byteArrayOf(10, 20, 30)
        val draftResult = CreateDraftWithVFFResult(guid = ruleGuid, url = "http://example.com/rule")
        every { caClient.createRuleDraft(any(), any(), any(), any()) } returns HttpResult.Success(draftResult)
        every { caClient.executeAction(any(), any(), any()) } returns HttpResult.Success(Unit)

        subject.uploadDisplayRule(rule, targetPath, data)

        verify { caClient.createRuleDraft(any(), projectConfig.defaultTargetFolder, any(), any()) }
    }

    @Test
    fun `uploadDisplayRule returns Failure on network Exception`() {
        val rule = DisplayRuleBuilder("R1").build()
        val targetPath = "icm://path/to/R1.jrd".toIcmPath()
        val data = byteArrayOf(10, 20, 30)
        every { caClient.createRuleDraft(any(), any(), any(), any()) } returns HttpResult.Exception(RuntimeException("network failure"))

        val result = subject.uploadDisplayRule(rule, targetPath, data)

        result.shouldBeOfInstance<OperationResult.Failure>()
        (result as OperationResult.Failure).message.shouldBeEqualTo("network failure")
    }

    @Nested
    inner class CategorizationPostProcessorTest {
        val realPostProcess = PostProcessImpl(
            ipsService = ipsService,
            documentObjectRepository = documentObjectRepository,
            imageRepository = imageRepository,
            attachmentRepository = attachmentRepository,
            displayRuleRepository = displayRuleRepository,
            textStyleRepository = textStyleRepository,
            paragraphStyleRepository = paragraphStyleRepository,
        )

        private val innerSubject = EvolveDeployClient(
            projectConfig,
            migConfig,
            caClient,
            resourcePathProvider,
            MetadataValidatorImpl(),
            realPostProcess,
            conflictDetector,
            progressReporter,
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
            storage,
        )

        private val categorizationPostProcessor: PostProcessor = innerSubject.clearPostProcessors().single()

        private fun aDeploymentResult(vararg infos: DeploymentInfo) = DeploymentResult(
            deploymentId = Uuid.random(),
            deployed = infos.toMutableList(),
        )

        @Test
        fun `post processor does nothing when targetVersion is null`() {
            every { caClient.targetVersion } returns null

            val result = aDeploymentResult(
                DeploymentInfo("doc1", ResourceType.DocumentObject, "icm://path/doc1.jld".toIcmPath())
            )
            categorizationPostProcessor(result)

            verify(exactly = 0) { caClient.setCategorization(any()) }
            result.warnings.shouldBeEmpty()
        }

        @Test
        fun `post processor does nothing when targetVersion is less than 26_6_0_0`() {
            every { caClient.targetVersion } returns Version(26, 5, 9, 9)

            val result = aDeploymentResult(
                DeploymentInfo("doc1", ResourceType.DocumentObject, "icm://path/doc1.jld".toIcmPath())
            )
            categorizationPostProcessor(result)

            verify(exactly = 0) { caClient.setCategorization(any()) }
            result.warnings.shouldBeEmpty()
        }

        @Test
        fun `post processor does nothing when deployed DocumentObject has no categorization metadata`() {
            every { caClient.targetVersion } returns Version(26, 6, 0, 0)
            val doc = DocumentObjectBuilder("doc1", DocumentObjectType.Template).build()
            every { documentObjectRepository.find("doc1") } returns doc

            val result = aDeploymentResult(
                DeploymentInfo("doc1", ResourceType.DocumentObject, "icm://path/doc1.jld".toIcmPath())
            )
            categorizationPostProcessor(result)

            verify(exactly = 0) { caClient.setCategorization(any()) }
        }

        @Test
        fun `post processor calls setCategorization for DocumentObject with categorization`() {
            every { caClient.targetVersion } returns Version(26, 6, 0, 0)
            val targetPath = "icm://Interactive/tenant/defaultFolder/doc1.jld".toIcmPath()
            val doc = DocumentObjectBuilder("doc1", DocumentObjectType.Template)
                .categorization("MyCat") { string("fieldA", "value1") }
                .build()
            every { documentObjectRepository.find("doc1") } returns doc
            every { caClient.setCategorization(any()) } returns HttpResult.Success(Unit)

            val result = aDeploymentResult(DeploymentInfo("doc1", ResourceType.DocumentObject, targetPath))
            categorizationPostProcessor(result)

            verify {
                caClient.setCategorization(
                    CategorizationUpdateDto(
                        path = targetPath.toString(),
                        categorizations = listOf(
                            CategorizationDto("MyCat", null, listOf(CategorizationFieldDto("fieldA", listOf("value1"))))
                        )
                    )
                )
            }
        }

        @Test
        fun `post processor calls setCategorization for Image with categorization`() {
            every { caClient.targetVersion } returns Version(26, 6, 0, 0)
            val targetPath = "icm://path/img1.jpg".toIcmPath()
            val img = ImageBuilder("img1")
                .categorization("ImgCat") { string("color", "red") }
                .build()
            every { imageRepository.find("img1") } returns img
            every { caClient.setCategorization(any()) } returns HttpResult.Success(Unit)

            val result = aDeploymentResult(DeploymentInfo("img1", ResourceType.Image, targetPath))
            categorizationPostProcessor(result)

            verify {
                caClient.setCategorization(
                    CategorizationUpdateDto(
                        path = targetPath.toString(),
                        categorizations = listOf(
                            CategorizationDto("ImgCat", null, listOf(CategorizationFieldDto("color", listOf("red"))))
                        )
                    )
                )
            }
        }

        @Test
        fun `post processor calls setCategorization for Attachment with categorization`() {
            every { caClient.targetVersion } returns Version(26, 6, 0, 0)
            val targetPath = "icm://path/att1.pdf".toIcmPath()
            val att = AttachmentBuilder("att1")
                .categorization("AttCat") { bool("active", true) }
                .build()
            every { attachmentRepository.find("att1") } returns att
            every { caClient.setCategorization(any()) } returns HttpResult.Success(Unit)

            val result = aDeploymentResult(DeploymentInfo("att1", ResourceType.Attachment, targetPath))
            categorizationPostProcessor(result)

            verify {
                caClient.setCategorization(
                    CategorizationUpdateDto(
                        path = targetPath.toString(),
                        categorizations = listOf(
                            CategorizationDto("AttCat", null, listOf(CategorizationFieldDto("active", listOf("true"))))
                        )
                    )
                )
            }
        }

        @Test
        fun `post processor calls setCategorization for DisplayRule with categorization`() {
            every { caClient.targetVersion } returns Version(26, 6, 0, 0)
            val targetPath = "icm://path/rule1.jrd".toIcmPath()
            val rule = DisplayRuleBuilder("rule1")
                .categorization("RuleCat") { string("type", "discount") }
                .build()
            every { displayRuleRepository.find("rule1") } returns rule
            every { caClient.setCategorization(any()) } returns HttpResult.Success(Unit)

            val result = aDeploymentResult(DeploymentInfo("rule1", ResourceType.DisplayRule, targetPath))
            categorizationPostProcessor(result)

            verify {
                caClient.setCategorization(
                    CategorizationUpdateDto(
                        path = targetPath.toString(),
                        categorizations = listOf(
                            CategorizationDto("RuleCat", null, listOf(CategorizationFieldDto("type", listOf("discount"))))
                        )
                    )
                )
            }
        }

        @Test
        fun `post processor skips TextStyle and ParagraphStyle resources`() {
            every { caClient.targetVersion } returns Version(26, 6, 0, 0)

            val result = aDeploymentResult(
                DeploymentInfo("ts1", ResourceType.TextStyle, "icm://path/ts1".toIcmPath()),
                DeploymentInfo("ps1", ResourceType.ParagraphStyle, "icm://path/ps1".toIcmPath()),
            )
            categorizationPostProcessor(result)

            verify(exactly = 0) { caClient.setCategorization(any()) }
            result.warnings.shouldBeEmpty()
        }

        @Test
        fun `post processor adds warning and skips setCategorization when repository returns null`() {
            every { caClient.targetVersion } returns Version(26, 6, 0, 0)
            every { documentObjectRepository.find("doc1") } returns null

            val result = aDeploymentResult(
                DeploymentInfo("doc1", ResourceType.DocumentObject, "icm://path/doc1.jld".toIcmPath())
            )
            categorizationPostProcessor(result)

            verify(exactly = 0) { caClient.setCategorization(any()) }
            result.warnings.shouldBeOfSize(1)
        }

        @Test
        fun `post processor calls setCategorization once per deployed item with categorization`() {
            every { caClient.targetVersion } returns Version(26, 6, 0, 0)
            val path1 = "icm://path/doc1.jld".toIcmPath()
            val path2 = "icm://path/doc2.jld".toIcmPath()
            val doc1 = DocumentObjectBuilder("doc1", DocumentObjectType.Template)
                .categorization("Cat") { string("key", "val1") }
                .build()
            val doc2 = DocumentObjectBuilder("doc2", DocumentObjectType.Template)
                .categorization("Cat") { string("key", "val2") }
                .build()
            every { documentObjectRepository.find("doc1") } returns doc1
            every { documentObjectRepository.find("doc2") } returns doc2
            every { caClient.setCategorization(any()) } returns HttpResult.Success(Unit)

            val result = aDeploymentResult(
                DeploymentInfo("doc1", ResourceType.DocumentObject, path1),
                DeploymentInfo("doc2", ResourceType.DocumentObject, path2),
            )
            categorizationPostProcessor(result)

            verify(exactly = 2) { caClient.setCategorization(any()) }
        }

        @Test
        fun `post processor handles multiple categorizations on the same object`() {
            every { caClient.targetVersion } returns Version(26, 6, 0, 0)
            val targetPath = "icm://path/doc1.jld".toIcmPath()
            val doc = DocumentObjectBuilder("doc1", DocumentObjectType.Template)
                .categorization("CatA") { string("fieldA", "valueA") }
                .categorization("CatB") { string("fieldB", "valueB") }
                .build()
            every { documentObjectRepository.find("doc1") } returns doc
            every { caClient.setCategorization(any()) } returns HttpResult.Success(Unit)

            val result = aDeploymentResult(DeploymentInfo("doc1", ResourceType.DocumentObject, targetPath))
            categorizationPostProcessor(result)

            verify {
                caClient.setCategorization(
                    CategorizationUpdateDto(
                        path = targetPath.toString(),
                        categorizations = listOf(
                            CategorizationDto("CatA", null, listOf(CategorizationFieldDto("fieldA", listOf("valueA")))),
                            CategorizationDto("CatB", null, listOf(CategorizationFieldDto("fieldB", listOf("valueB")))),
                        )
                    )
                )
            }
        }
    }
}
