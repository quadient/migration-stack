package com.quadient.migration.service.deploy

import com.quadient.migration.api.MigConfig
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.dto.migrationmodel.Attachment
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.repository.AttachmentRepository
import com.quadient.migration.api.repository.BaseTemplateRepository
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
import com.quadient.migration.service.deploy.utility.FileNameValidator
import com.quadient.migration.service.deploy.utility.PostProcessImpl
import com.quadient.migration.service.ResourcePathProvider
import com.quadient.migration.service.deploy.utility.ConflictDetectorImpl
import com.quadient.migration.service.deploy.utility.DeployOrderImpl
import com.quadient.migration.service.deploy.utility.DeploymentResult
import com.quadient.migration.service.deploy.utility.RefInheritanceServiceImpl
import com.quadient.migration.service.deploy.utility.ProgressReporterImpl
import com.quadient.migration.service.deploy.utility.ResourceType
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder
import com.quadient.migration.service.inspirebuilder.InspireBaseTemplateBuilder
import com.quadient.migration.service.ipsclient.IpsClientException
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.service.ipsclient.OperationResult
import com.quadient.migration.service.ipsclient.Version
import com.quadient.migration.service.resolveTargetDir
import com.quadient.migration.shared.Categorization
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.toIcmPath
import java.util.UUID
import kotlin.time.Clock
import kotlin.uuid.Uuid

class EvolveDeployClient(
    private val projectConfig: ProjectConfig,
    private val migConfig: MigConfig,
    private val caClient: CaApiClient,
    resourcePathProvider: ResourcePathProvider,
    metadataValidator: MetadataValidatorImpl,
    fileNameValidator: FileNameValidator,
    postProcess: PostProcessImpl,
    conflictDetector: ConflictDetectorImpl,
    progressReporter: ProgressReporterImpl,
    deployOrder: DeployOrderImpl,
    refInheritanceService: RefInheritanceServiceImpl,
    documentObjectRepository: DocumentObjectRepository,
    imageRepository: ImageRepository,
    attachmentRepository: AttachmentRepository,
    statusTrackingRepository: StatusTrackingRepository,
    textStyleRepository: TextStyleRepository,
    paragraphStyleRepository: ParagraphStyleRepository,
    displayRuleRepository: DisplayRuleRepository,
    variableRepository: VariableRepository,
    variableStructureRepository: VariableStructureRepository,
    baseTemplateRepository: BaseTemplateRepository,
    documentObjectBuilder: InspireDocumentObjectBuilder,
    baseTemplateBuilder: InspireBaseTemplateBuilder,
    ipsService: IpsService,
    storage: Storage,
) : InteractiveDeployClient(
    projectConfig,
    resourcePathProvider,
    metadataValidator,
    fileNameValidator,
    postProcess,
    conflictDetector,
    progressReporter,
    deployOrder,
    refInheritanceService,
    documentObjectRepository,
    imageRepository,
    attachmentRepository,
    statusTrackingRepository,
    textStyleRepository,
    paragraphStyleRepository,
    displayRuleRepository,
    variableRepository,
    variableStructureRepository,
    baseTemplateRepository,
    documentObjectBuilder,
    baseTemplateBuilder,
    ipsService,
    storage,
) {
    private companion object {
        val STYLE_DEFINITION_MASTER = "icm://Interactive/StandardPackage/CompanyStyles/StyleDefinition.wfd".toIcmPath()
    }

    init {
        // Clear existing postprocessors for Interactive output since
        // they are not valid for Evolve output
        clearPostProcessors()
    }

    private val canCategorize by lazy { canCategorize() }

    private val evolveConfig by lazy {
        requireNotNull(migConfig.inspireConfig.evolveConfig) {
            "migrationConfig.inspireConfig.evolveConfig must be set to use Evolve inspireOutput"
        }
    }

    private val baseTemplateCopyFailures = mutableMapOf<IcmPath, OperationResult.Failure>()

    override fun prepareDocumentObjectsDeployment(documentObjects: List<DocumentObject>) {
        baseTemplateCopyFailures.clear()
        val baseTemplatePaths = documentObjects.map { obj ->
            resourcePathProvider.getBaseTemplateFullPath(obj.baseTemplate, baseTemplateRepository::findOrFail)
        }.distinct()

        val dependenciesByBaseTemplate = mutableMapOf<IcmPath, List<IcmPath>>()
        for (path in baseTemplatePaths) {
            val result = copyFileToLocalIcm(path)
            if (result is OperationResult.Failure) {
                logger.error(result.message)
                baseTemplateCopyFailures[path] = result
                continue
            }

            val dependencies = try {
                ipsService.listDependencies(path)
            } catch (e: IpsClientException) {
                val failure = OperationResult.Failure("Failed to list dependencies of base template '$path': ${e.message}")
                logger.error(failure.message)
                baseTemplateCopyFailures[path] = failure
                continue
            }
            dependenciesByBaseTemplate[path] = dependencies.map(String::toIcmPath)
        }

        val dependencies = dependenciesByBaseTemplate.values
            .flatten()
            .distinct()
            // Do not download the color profiles, download is blocked on Evolve side,
            // and they should already be present in the local StandardPackage
            .filter { !it.startsWith("icm://Interactive/StandardPackage/CompanyStyles/ICCProfiles") }
        val dependencyCopyFailures = mutableMapOf<IcmPath, OperationResult.Failure>()
        for (path in dependencies) {
            val result = copyFileToLocalIcm(path)
            if (result is OperationResult.Failure) {
                logger.error(result.message)
                dependencyCopyFailures[path] = result
            }
        }

        approveInLocalIcm((dependenciesByBaseTemplate.keys + dependencies.filterNot(dependencyCopyFailures::containsKey)).toList())

        for ((baseTemplatePath, baseTemplateDependencies) in dependenciesByBaseTemplate) {
            baseTemplateDependencies.firstNotNullOfOrNull { dependencyCopyFailures[it] }?.let {
                baseTemplateCopyFailures[baseTemplatePath] = it
            }
        }
    }


    override fun uploadDocumentObject(obj: DocumentObject, targetPath: IcmPath, wfdXml: String): OperationResult {
        val baseTemplatePath = resourcePathProvider.getBaseTemplateFullPath(obj.baseTemplate, baseTemplateRepository::findOrFail)
        baseTemplateCopyFailures[baseTemplatePath]?.let { return it }

        val ipsMemLocation = "memory://${UUID.randomUUID()}"
        try {
            val runCommandType = obj.type.toRunCommandType()
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

                    val publishResult = caClient.executeAction(
                        evolveConfig.publishTemplateActionId,
                        result.response.draft.guid,
                        ObjectType.TemplateDraft,
                    )
                    if (publishResult !is HttpResult.Success) {
                        return publishResult.toOperationResult()
                    }

                    if (canCategorize) {
                        obj.metadata.filterIsInstance<Categorization>().executeHttp(targetPath)
                    } else {
                        publishResult
                    }.toOperationResult()
                }

                DocumentObjectType.Snippet -> OperationResult.Failure("Snippets are not currently supported in Evolve output")
                else -> {
                    val result = caClient.createBlockDraft(obj.nameOrId(), resolvedFolder, baseTemplatePath, jld)
                    if (result !is HttpResult.Success) {
                        return result.toOperationResult()
                    }
                    val publishResult = caClient.executeAction(
                        evolveConfig.publishBlockActionId,
                        result.response.draft.guid,
                        ObjectType.BlockDraft,
                    )

                    if (publishResult !is HttpResult.Success) {
                        return publishResult.toOperationResult()
                    }

                    if (canCategorize) {
                        obj.metadata.filterIsInstance<Categorization>().executeHttp(targetPath)
                    } else {
                        publishResult
                    }.toOperationResult()
                }
            }
        } finally {
            runCatching { ipsService.delete(ipsMemLocation) }.getOrElse {
                logger.error("Failed to delete deployed JLD from '$ipsMemLocation': ${it.message}")
            }
        }
    }

    override fun uploadImage(img: Image, targetPath: IcmPath, data: ByteArray): OperationResult {
        when (val result = ipsService.tryUpload(targetPath, data)) {
            is OperationResult.Success -> {}
            is OperationResult.Failure -> return result
        }

        val result = caClient.uploadResource(targetPath, data)
        if (result !is HttpResult.Success) {
            return result.toOperationResult()
        }

        return if (canCategorize) {
            img.metadata.filterIsInstance<Categorization>().executeHttp(targetPath)
        } else {
            result
        }.toOperationResult()
    }

    override fun uploadAttachment(att: Attachment, targetPath: IcmPath, data: ByteArray): OperationResult {
        val result = caClient.uploadResource(targetPath, data)
        if (result !is HttpResult.Success) {
            return result.toOperationResult()
        }

        return if (canCategorize) {
            att.metadata.filterIsInstance<Categorization>().executeHttp(targetPath)
        } else {
            result
        }.toOperationResult()
    }

    override fun uploadDisplayRule(rule: DisplayRule, targetPath: IcmPath, data: ByteArray): OperationResult {
        val targetFolder = rule.targetFolder?.toIcmPath()
        if (targetFolder?.isAbsolute() == true) {
            return OperationResult.Failure("TargetFolder '$targetFolder' for display rule '${rule.id}' cannot be absolute for Evolve output")
        }
        val resolvedFolder = resolveTargetDir(projectConfig.defaultTargetFolder, rule.targetFolder?.toIcmPath())

        val baseTemplatePath = try {
            resourcePathProvider.getBaseTemplateFullPath(rule.baseTemplate, baseTemplateRepository::findOrFail)
        } catch (e: IllegalStateException) {
            return OperationResult.Failure(e.message ?: "Failed to resolve base template for display rule '${rule.id}'.")
        }
        val result =  caClient.createRuleDraft(rule.nameOrId(), resolvedFolder, baseTemplatePath, data)
        if (result !is HttpResult.Success) return result.toOperationResult()

        val publishResult = caClient.executeAction(
            evolveConfig.publishRuleActionId,
            result.response.guid,
            ObjectType.RuleDraft,
        )
        if (publishResult !is HttpResult.Success) {
            return publishResult.toOperationResult()
        }

        return if (canCategorize) {
            rule.metadata.filterIsInstance<Categorization>().executeHttp(targetPath)
        } else {
            publishResult
        }.toOperationResult()
    }

    override fun deployStyles() {
        if (projectConfig.styleDefinitionPath != null) {
            error("Configured styleDefinitionPath '${projectConfig.styleDefinitionPath}' is not supported for Evolve output")
        }
        val publishActionId = evolveConfig.publishStyleDefinitionActionId
            ?: error("publishStyleDefinitionActionId must be set in migration-config to deploy styles to Evolve output")

        val deploymentId = Uuid.random()
        val deploymentTimestamp = Clock.System.now()
        val targetPath = resourcePathProvider.getStyleDefinitionPath()

        val textStyles = textStyleRepository.listAll().filter { it.targetId == null }
        val paragraphStyles = paragraphStyleRepository.listAll().filter { it.targetId == null }

        val result = run {
            val copyResult = copyFileToLocalIcm(STYLE_DEFINITION_MASTER)
            if (copyResult is OperationResult.Failure) {
                return@run copyResult
            }
            approveInLocalIcm(listOf(STYLE_DEFINITION_MASTER))

            val ipsMemLocation = "memory://${UUID.randomUUID()}"
            try {
                val styleLayoutDeltaXml = documentObjectBuilder.buildStyleLayoutDelta(
                    textStyles = textStyles,
                    paragraphStyles = paragraphStyles
                )

                val deployResult = ipsService.deployStyleJld(
                    baseTemplate = STYLE_DEFINITION_MASTER.toString(),
                    xmlContent = styleLayoutDeltaXml,
                    outputPath = ipsMemLocation,
                )
                if (deployResult is OperationResult.Failure) {
                    return@run deployResult
                }

                val jld = runCatching { ipsService.download(ipsMemLocation) }.getOrElse {
                    return@run OperationResult.Failure("Failed to download deployed style definition from '$ipsMemLocation': ${it.message}")
                }

                val name = targetPath.filename().removeSuffix(".jld")
                val draftResult = caClient.createStyleDefinitionDraft(name, resolveTargetDir(projectConfig.defaultTargetFolder), jld)
                if (draftResult !is HttpResult.Success) {
                    return@run draftResult.toOperationResult()
                }

                caClient.executeAction(
                    publishActionId,
                    draftResult.response.draft.guid,
                    ObjectType.CompanyStyleDraft,
                ).toOperationResult()
            } finally {
                runCatching { ipsService.delete(ipsMemLocation) }.getOrElse {
                    logger.error("Failed to delete deployed style definition from '$ipsMemLocation': ${it.message}")
                }
            }
        }

        when (result) {
            OperationResult.Success -> {
                logger.debug("Deployment of style definition '$targetPath' is successful.")
                textStyles.forEach {
                    statusTrackingRepository.deployed(
                        id = it.id,
                        deploymentId = deploymentId,
                        timestamp = deploymentTimestamp,
                        resourceType = ResourceType.TextStyle,
                        icmPath = targetPath,
                        output = projectConfig.inspireOutput
                    )
                }
                paragraphStyles.forEach {
                    statusTrackingRepository.deployed(
                        id = it.id,
                        deploymentId = deploymentId,
                        timestamp = deploymentTimestamp,
                        resourceType = ResourceType.ParagraphStyle,
                        icmPath = targetPath,
                        output = projectConfig.inspireOutput
                    )
                }
            }

            is OperationResult.Failure -> {
                logger.error("Failed to deploy style definition '$targetPath': ${result.message}")
                textStyles.forEach {
                    statusTrackingRepository.error(
                        it.id,
                        deploymentId,
                        deploymentTimestamp,
                        ResourceType.TextStyle,
                        targetPath,
                        projectConfig.inspireOutput,
                        result.message
                    )
                }
                paragraphStyles.forEach {
                    statusTrackingRepository.error(
                        it.id,
                        deploymentId,
                        deploymentTimestamp,
                        ResourceType.ParagraphStyle,
                        targetPath,
                        projectConfig.inspireOutput,
                        result.message
                    )
                }
            }
        }
    }

    override fun deployBaseTemplates(): DeploymentResult {
        error("Base template deployment is not currently supported in Evolve output")
    }

    private fun HttpResult<*, ApiBadRequestException>.toOperationResult(): OperationResult = when (this) {
        is HttpResult.Success -> OperationResult.Success
        is HttpResult.Failure -> OperationResult.Failure("CA API error ${error.status}: ${error.title} - ${error.detail}")
        is HttpResult.Exception -> OperationResult.Failure(cause.message ?: cause.toString())
    }

    private fun canCategorize(): Boolean {
        val targetVersion = caClient.targetVersion
        if (targetVersion == null) {
            logger.error("Failed to retrieve target CA version, skipping categorization")
            return false
        }

        if (targetVersion < Version(26, 6, 0, 0)) {
            logger.warn("Target CA version $targetVersion does not support categorization API, skipping categorization for deployed objects")
            return false
        }

        return true
    }

    private fun List<Categorization>.executeHttp(targetPath: IcmPath): HttpResult<Unit, ApiBadRequestException> {
        if (this.isEmpty()) {
            return HttpResult.Success(Unit)
        }

        return caClient.setCategorization(
            CategorizationUpdateDto(targetPath.toString(), this.map {
                CategorizationDto(it.name, null, it.fields.map { field ->
                    CategorizationFieldDto(field.key, field.value.map { value -> value.serialize() })
                })
            })
        )
    }

    private fun copyFileToLocalIcm(path: IcmPath): OperationResult {
        val data = when (val result = caClient.downloadFile(path)) {
            is HttpResult.Success if result.response.isEmpty() -> return OperationResult.Failure("Failed to download '$path' from Evolve: empty response. Does the file exist?")
            is HttpResult.Success -> result.response
            is HttpResult.Failure -> return OperationResult.Failure("Failed to download '$path' from Evolve: ${result.error}")
            is HttpResult.Exception -> return OperationResult.Failure("Failed to download '$path' from Evolve: ${result.cause.message}")
        }

        return when (val uploadResult = ipsService.tryUpload(path, data)) {
            is OperationResult.Success -> uploadResult
            is OperationResult.Failure -> OperationResult.Failure("Failed to copy '$path' to local ICM: ${uploadResult.message}")
        }
    }

    private fun approveInLocalIcm(paths: List<IcmPath>) {
        if (paths.isEmpty()) return
        val result = ipsService.setProductionApprovalState(paths)
        if (result is OperationResult.Failure) {
            logger.error("Failed to set production approval state in local ICM for $paths: ${result.message}")
        }
    }
}