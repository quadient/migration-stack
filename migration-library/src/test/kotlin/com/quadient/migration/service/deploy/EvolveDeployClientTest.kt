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
import com.quadient.migration.service.deploy.utility.MetadataValidatorImpl
import com.quadient.migration.service.deploy.utility.PostProcessImpl
import com.quadient.migration.service.deploy.utility.ConflictDetectorImpl
import com.quadient.migration.service.deploy.utility.ProgressReporterImpl
import com.quadient.migration.service.inspirebuilder.InteractiveDocumentObjectBuilder
import com.quadient.migration.service.InteractiveResourcePathProvider
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.service.ipsclient.OperationResult
import com.quadient.migration.service.ipsclient.Version
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.toIcmPath
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeOfInstance
import com.quadient.migration.tools.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
        every { caClient.targetVersion } returns null
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
    inner class CategorizationTest {

        @BeforeEach
        fun setup() {
            every { caClient.targetVersion } returns Version(26, 6, 0, 0)
        }

        @Test
        fun `categorization is skipped when targetVersion is null`() {
            every { caClient.targetVersion } returns null
            val img = ImageBuilder("img1").categorization("Cat") { string("key", "val") }.build()
            val targetPath = "icm://path/img1.jpg".toIcmPath()
            val data = byteArrayOf(1, 2, 3)
            every { ipsService.tryUpload(targetPath, data) } returns OperationResult.Success
            every { caClient.uploadResource(targetPath, data) } returns HttpResult.Success(Unit)

            subject.uploadImage(img, targetPath, data)

            verify(exactly = 0) { caClient.setCategorization(any()) }
        }

        @Test
        fun `categorization is skipped when targetVersion is less than 26_6_0_0`() {
            every { caClient.targetVersion } returns Version(26, 5, 9, 9)
            val img = ImageBuilder("img1").categorization("Cat") { string("key", "val") }.build()
            val targetPath = "icm://path/img1.jpg".toIcmPath()
            val data = byteArrayOf(1, 2, 3)
            every { ipsService.tryUpload(targetPath, data) } returns OperationResult.Success
            every { caClient.uploadResource(targetPath, data) } returns HttpResult.Success(Unit)

            subject.uploadImage(img, targetPath, data)

            verify(exactly = 0) { caClient.setCategorization(any()) }
        }

        @Test
        fun `categorization is skipped when DocumentObject has no categorization metadata`() {
            val doc = DocumentObjectBuilder("doc1", DocumentObjectType.Template).build()
            val targetPath = "icm://path/doc1.jld".toIcmPath()
            val draftResult = DraftJsonIpsResult(
                draft = CreateDraftResult(guid = draftGuid, url = "http://example.com"),
                result = CreateIcmObjectResult(valid = true)
            )
            every { caClient.createTemplateDraft(any(), any(), any(), any()) } returns HttpResult.Success(draftResult)
            every { caClient.executeAction(any(), any(), any()) } returns HttpResult.Success(Unit)

            subject.uploadDocumentObject(doc, targetPath, "<xml/>")

            verify(exactly = 0) { caClient.setCategorization(any()) }
        }

        @Test
        fun `setCategorization is called for DocumentObject with categorization`() {
            val targetPath = "icm://Interactive/tenant/defaultFolder/doc1.jld".toIcmPath()
            val doc = DocumentObjectBuilder("doc1", DocumentObjectType.Template)
                .categorization("MyCat") { string("fieldA", "value1") }
                .build()
            val draftResult = DraftJsonIpsResult(
                draft = CreateDraftResult(guid = draftGuid, url = "http://example.com"),
                result = CreateIcmObjectResult(valid = true)
            )
            every { caClient.createTemplateDraft(any(), any(), any(), any()) } returns HttpResult.Success(draftResult)
            every { caClient.executeAction(any(), any(), any()) } returns HttpResult.Success(Unit)
            every { caClient.setCategorization(any()) } returns HttpResult.Success(Unit)

            subject.uploadDocumentObject(doc, targetPath, "<xml/>")

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
        fun `setCategorization is called for Image with categorization`() {
            val targetPath = "icm://path/img1.jpg".toIcmPath()
            val img = ImageBuilder("img1")
                .categorization("ImgCat") { string("color", "red") }
                .build()
            val data = byteArrayOf(1, 2, 3)
            every { ipsService.tryUpload(targetPath, data) } returns OperationResult.Success
            every { caClient.uploadResource(targetPath, data) } returns HttpResult.Success(Unit)
            every { caClient.setCategorization(any()) } returns HttpResult.Success(Unit)

            subject.uploadImage(img, targetPath, data)

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
        fun `setCategorization is called for Attachment with categorization`() {
            val targetPath = "icm://path/att1.pdf".toIcmPath()
            val att = AttachmentBuilder("att1")
                .categorization("AttCat") { bool("active", true) }
                .build()
            val data = byteArrayOf(4, 5, 6)
            every { caClient.uploadResource(targetPath, data) } returns HttpResult.Success(Unit)
            every { caClient.setCategorization(any()) } returns HttpResult.Success(Unit)

            subject.uploadAttachment(att, targetPath, data)

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
        fun `setCategorization is called for DisplayRule with categorization`() {
            val targetPath = "icm://path/rule1.jrd".toIcmPath()
            val rule = DisplayRuleBuilder("rule1")
                .categorization("RuleCat") { string("type", "discount") }
                .build()
            val data = byteArrayOf(10, 20, 30)
            every { caClient.createRuleDraft(any(), any(), any(), any()) } returns HttpResult.Success(CreateDraftWithVFFResult(guid = ruleGuid, url = "http://example.com/rule"))
            every { caClient.executeAction(any(), any(), any()) } returns HttpResult.Success(Unit)
            every { caClient.setCategorization(any()) } returns HttpResult.Success(Unit)

            subject.uploadDisplayRule(rule, targetPath, data)

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
        fun `categorization is skipped for resources without categorization metadata`() {
            val img = ImageBuilder("img1").build()
            val targetPath = "icm://path/img1.jpg".toIcmPath()
            val data = byteArrayOf(1, 2, 3)
            every { ipsService.tryUpload(targetPath, data) } returns OperationResult.Success
            every { caClient.uploadResource(targetPath, data) } returns HttpResult.Success(Unit)

            subject.uploadImage(img, targetPath, data)

            verify(exactly = 0) { caClient.setCategorization(any()) }
        }

        @Test
        fun `categorization is skipped when DocumentObject has no categorization metadata and upload succeeds`() {
            val doc = DocumentObjectBuilder("doc1", DocumentObjectType.Template).build()
            val targetPath = "icm://path/doc1.jld".toIcmPath()
            val draftResult = DraftJsonIpsResult(
                draft = CreateDraftResult(guid = draftGuid, url = "http://example.com"),
                result = CreateIcmObjectResult(valid = true)
            )
            every { caClient.createTemplateDraft(any(), any(), any(), any()) } returns HttpResult.Success(draftResult)
            every { caClient.executeAction(any(), any(), any()) } returns HttpResult.Success(Unit)

            subject.uploadDocumentObject(doc, targetPath, "<xml/>")

            verify(exactly = 0) { caClient.setCategorization(any()) }
        }

        @Test
        fun `setCategorization is called once per uploaded item with categorization`() {
            val path1 = "icm://path/doc1.jld".toIcmPath()
            val path2 = "icm://path/doc2.jld".toIcmPath()
            val doc1 = DocumentObjectBuilder("doc1", DocumentObjectType.Template)
                .categorization("Cat") { string("key", "val1") }
                .build()
            val doc2 = DocumentObjectBuilder("doc2", DocumentObjectType.Template)
                .categorization("Cat") { string("key", "val2") }
                .build()
            val draftResult = DraftJsonIpsResult(
                draft = CreateDraftResult(guid = draftGuid, url = "http://example.com"),
                result = CreateIcmObjectResult(valid = true)
            )
            every { caClient.createTemplateDraft(any(), any(), any(), any()) } returns HttpResult.Success(draftResult)
            every { caClient.executeAction(any(), any(), any()) } returns HttpResult.Success(Unit)
            every { caClient.setCategorization(any()) } returns HttpResult.Success(Unit)

            subject.uploadDocumentObject(doc1, path1, "<xml/>")
            subject.uploadDocumentObject(doc2, path2, "<xml/>")

            verify(exactly = 2) { caClient.setCategorization(any()) }
        }

        @Test
        fun `setCategorization is called with all categorizations for the same object`() {
            val targetPath = "icm://path/doc1.jld".toIcmPath()
            val doc = DocumentObjectBuilder("doc1", DocumentObjectType.Template)
                .categorization("CatA") { string("fieldA", "valueA") }
                .categorization("CatB") { string("fieldB", "valueB") }
                .build()
            val draftResult = DraftJsonIpsResult(
                draft = CreateDraftResult(guid = draftGuid, url = "http://example.com"),
                result = CreateIcmObjectResult(valid = true)
            )
            every { caClient.createTemplateDraft(any(), any(), any(), any()) } returns HttpResult.Success(draftResult)
            every { caClient.executeAction(any(), any(), any()) } returns HttpResult.Success(Unit)
            every { caClient.setCategorization(any()) } returns HttpResult.Success(Unit)

            subject.uploadDocumentObject(doc, targetPath, "<xml/>")

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
