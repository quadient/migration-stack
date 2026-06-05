package com.quadient.migration.service.deploy

import com.quadient.migration.api.MigConfig
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.dto.migrationmodel.Attachment
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Image
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
import com.quadient.migration.service.getBaseTemplateFullPath
import com.quadient.migration.service.ResourcePathProvider
import com.quadient.migration.service.deploy.utility.ConflictDetectorImpl
import com.quadient.migration.service.deploy.utility.ProgressReporterImpl
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.service.ipsclient.OperationResult
import com.quadient.migration.service.resolveTargetDir
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.toIcmPath
import java.util.UUID

class EvolveDeployClient(
    private val projectConfig: ProjectConfig,
    private val migConfig: MigConfig,
    private val caClient: CaApiClient,
    resourcePathProvider: ResourcePathProvider,
    metadataValidator: MetadataValidatorImpl,
    postProcess: PostProcessImpl,
    conflictDetector: ConflictDetectorImpl,
    progressReporter: ProgressReporterImpl,
    documentObjectRepository: DocumentObjectRepository,
    imageRepository: ImageRepository,
    attachmentRepository: AttachmentRepository,
    statusTrackingRepository: StatusTrackingRepository,
    textStyleRepository: TextStyleRepository,
    paragraphStyleRepository: ParagraphStyleRepository,
    displayRuleRepository: DisplayRuleRepository,
    variableRepository: VariableRepository,
    variableStructureRepository: VariableStructureRepository,
    documentObjectBuilder: InspireDocumentObjectBuilder,
    ipsService: IpsService,
    storage: Storage,
) : InteractiveDeployClient(
    projectConfig,
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
) {
    init {
        // Clear existing postprocessors for Interactive output, there are
        // currently no postprocessors needed for evolve
        clearPostProcessors()
    }

    private val evolveConfig by lazy {
        requireNotNull(migConfig.evolveConfig) {
            "migrationConfig.evolveConfig must be set to use Evolve inspireOutput"
        }
    }

    override fun uploadDocumentObject(obj: DocumentObject, targetPath: IcmPath, wfdXml: String): OperationResult {
        val ipsMemLocation = "memory://${UUID.randomUUID()}"
        try {
            val runCommandType = obj.type.toRunCommandType()
            val baseTemplatePath = getBaseTemplateFullPath(projectConfig, obj.baseTemplate)
            val deployResult = ipsService.deployJld(
                baseTemplate = baseTemplatePath,
                type = runCommandType,
                moduleName = "DocumentLayout",
                xmlContent = wfdXml,
                outputPath = ipsMemLocation
            )

            if (deployResult is OperationResult.Failure) {
                return deployResult
            }

            val jld = runCatching { ipsService.download(ipsMemLocation) }.getOrElse {
                return OperationResult.Failure("Failed to download deployed JLD from '$ipsMemLocation': ${it.message}")
            }

            val targetFolder = obj.targetFolder?.toIcmPath()
            if (targetFolder?.isAbsolute() == true) {
                return OperationResult.Failure("TargetFolder '$targetFolder' for document object '${obj.id}' cannot be absolute for Evolve output")
            }
            val resolvedFolder = resolveTargetDir(projectConfig.defaultTargetFolder, obj.targetFolder?.toIcmPath())

            return when (obj.type) {
                DocumentObjectType.Template, DocumentObjectType.Page -> {
                    val result = caClient.createTemplateDraft(
                        obj.nameOrId(),
                        resolvedFolder,
                        baseTemplatePath,
                        jld
                    )
                    if (result !is HttpResult.Success) {
                        return result.toOperationResult()
                    }

                    caClient.executeAction(
                        evolveConfig.publishTemplateActionId,
                        result.response.draft.guid,
                        ObjectType.TemplateDraft,
                    ).toOperationResult()
                }

                DocumentObjectType.Snippet -> OperationResult.Failure("Snippets are not currently supported in Evolve output")
                else -> {
                    val result = caClient.createBlockDraft(obj.nameOrId(), resolvedFolder, baseTemplatePath, jld)
                    if (result !is HttpResult.Success) {
                        return result.toOperationResult()
                    }
                    caClient.executeAction(
                        evolveConfig.publishBlockActionId,
                        result.response.draft.guid,
                        ObjectType.BlockDraft,
                    ).toOperationResult()
                }
            }
        } finally {
            runCatching { ipsService.delete(ipsMemLocation) }.getOrElse {
                logger.error("Failed to delete deployed JLD from '$ipsMemLocation': ${it.message}")
            }
        }
    }

    override fun uploadImage(img: Image, targetPath: IcmPath, data: ByteArray): OperationResult {
        return caClient.uploadResource(targetPath, data).toOperationResult()
    }

    override fun uploadAttachment(att: Attachment, targetPath: IcmPath, data: ByteArray): OperationResult {
        return caClient.uploadResource(targetPath, data).toOperationResult()
    }

    override fun uploadDisplayRule(rule: DisplayRule, targetPath: IcmPath, data: ByteArray): OperationResult {
        val targetFolder = rule.targetFolder?.toIcmPath()
        if (targetFolder?.isAbsolute() == true) {
            return OperationResult.Failure("TargetFolder '$targetFolder' for display rule '${rule.id}' cannot be absolute for Evolve output")
        }
        val resolvedFolder = resolveTargetDir(projectConfig.defaultTargetFolder, rule.targetFolder?.toIcmPath())

        val baseTemplatePath = getBaseTemplateFullPath(projectConfig, rule.baseTemplate)
        val result =  caClient.createRuleDraft(rule.nameOrId(), resolvedFolder, baseTemplatePath, data)
        if (result !is HttpResult.Success) return result.toOperationResult()

        return caClient.executeAction(
            evolveConfig.publishRuleActionId,
            result.response.guid,
            ObjectType.RuleDraft,
        ).toOperationResult()
    }

    override fun deployStyles() {
        error("Styles deployment is not currently supported in Evolve output")
    }

    private fun HttpResult<*, ApiBadRequestException>.toOperationResult(): OperationResult = when (this) {
        is HttpResult.Success -> OperationResult.Success
        is HttpResult.Failure -> OperationResult.Failure("CA API error ${error.status}: ${error.title} - ${error.detail}")
        is HttpResult.Exception -> OperationResult.Failure(cause.message ?: cause.toString())
    }
}