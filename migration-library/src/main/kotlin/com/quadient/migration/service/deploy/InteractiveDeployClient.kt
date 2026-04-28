@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.service.deploy

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Attachment
import com.quadient.migration.api.dto.migrationmodel.AttachmentRef
import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.api.repository.DisplayRuleRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ParagraphStyleRepository
import com.quadient.migration.api.repository.Repository
import com.quadient.migration.api.repository.TextStyleRepository
import com.quadient.migration.api.repository.VariableRepository
import com.quadient.migration.api.repository.VariableStructureRepository
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.service.Storage
import com.quadient.migration.service.deploy.utility.DeploymentError
import com.quadient.migration.service.deploy.utility.DeploymentResult
import com.quadient.migration.service.deploy.utility.ResourceType
import com.quadient.migration.service.deploy.utility.ResultTracker
import com.quadient.migration.service.getBaseTemplateFullPath
import com.quadient.migration.service.inspirebuilder.InteractiveDocumentObjectBuilder
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.service.ipsclient.OperationResult
import com.quadient.migration.service.resolveTarget
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.Jrd
import kotlin.time.Clock
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.json.extract
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class InteractiveDeployClient(
    documentObjectRepository: DocumentObjectRepository,
    imageRepository: Repository<Image>,
    attachmentRepository: Repository<Attachment>,
    statusTrackingRepository: StatusTrackingRepository,
    textStyleRepository: TextStyleRepository,
    paragraphStyleRepository: ParagraphStyleRepository,
    displayRuleRepository: DisplayRuleRepository,
    variableRepository: VariableRepository,
    variableStructureRepository: VariableStructureRepository,
    documentObjectBuilder: InteractiveDocumentObjectBuilder,
    ipsService: IpsService,
    storage: Storage,
    private val projectConfig: ProjectConfig,
) : DeployClient(
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
    InspireOutput.Interactive,
) {
    init {
        addPostProcessor { deploymentResult ->
            val approvalResult = ipsService.setProductionApprovalState(deploymentResult.deployed.map { it.targetPath })
            if (approvalResult == OperationResult.Success) {
                logger.debug("Setting of production approval state to ${deploymentResult.deployed.size} document objects is successful.")
            } else {
                logger.error("Failed to set production approval state to document objects.")
            }
        }
    }

    override fun shouldIncludeDependency(documentObject: DocumentObject): Boolean {
        return documentObject.internal != true
    }

    override fun getAllDocumentObjectsToDeploy(): List<DocumentObject> {
        return documentObjectRepository.list(
            DocumentObjectTable.skip.extract<String>("skipped") eq "false" and DocumentObjectTable.internal.eq(
                false
            )
        )
    }

    override fun getDocumentObjectsToDeploy(documentObjectIds: List<String>): List<DocumentObject> {
        val documentObjects = documentObjectRepository.list(DocumentObjectTable.id inList documentObjectIds)
        val skipped = mutableListOf<String>()
        val internal = mutableListOf<String>()
        for (documentObject in documentObjects) {
            if (documentObject.skip.skipped) {
                skipped.add(documentObject.id)
            }
            if (documentObject.internal == true) {
                internal.add(documentObject.id)
            }
        }

        var error = ""
        val notFoundDocObjects = documentObjectIds.filter { id ->
            documentObjects.none { it.id == id }
        }
        if (notFoundDocObjects.isNotEmpty()) {
            error += "The following document objects were not found: [${notFoundDocObjects.joinToString(", ")}]. "
        }
        if (skipped.isNotEmpty()) {
            error += "The following document objects are skipped: [${skipped.joinToString(", ")}]. "
        }
        if (internal.isNotEmpty()) {
            error += "The following document objects are internal: [${internal.joinToString(", ")}]. "
        }
        require(error.isEmpty()) { error }

        return documentObjects
    }

    private fun deployDisplayRules(
        documentObjects: List<DocumentObject>,
        tracker: ResultTracker,
        deployDisplayRule: (DisplayRule, IcmPath, ByteArray) -> OperationResult,
    ) {
        val rules = documentObjects
            .flatMap {
                try {
                    it.getAllExternalDisplayRules()
                } catch (e: IllegalStateException) {
                    tracker.deploymentResult.errors.add(DeploymentError(it.id, e.message ?: ""))
                    emptyList()
                }
            }
            .distinctBy { it.id }

        for (r in rules) {
            val rule = r.resolveTarget(displayRuleRepository::findOrFail)
            val targetPath = documentObjectBuilder.getDisplayRulePath(rule)

            if (!shouldDeployObject(rule.id, ResourceType.DisplayRule, targetPath, tracker.deploymentResult)) {
                logger.info("Skipping deployment of '${rule.id}' as it is not marked for deployment.")
                continue
            }

            val invalidMetadata = rule.getInvalidMetadataKeys()
            if (invalidMetadata.isNotEmpty()) {
                logger.error("Failed to deploy '$targetPath' due to invalid metadata.")
                val keys = invalidMetadata.joinToString(", ", prefix = "[", postfix = "]")
                val message = "Metadata of display rule '${rule.id}' contains invalid keys: $keys"
                tracker.errorDisplayRule(rule.id, targetPath, message)
                continue
            }

            val variableStructureId = rule.variableStructureRef?.id ?: projectConfig.defaultVariableStructure
            val variableStructure=
                variableStructureId?.let { variableStructureRepository.findOrFail(it) } ?: VariableStructure(
                    id = "defaultVariableStructure",
                    lastUpdated = Clock.System.now(),
                    created = Clock.System.now(),
                    structure = mutableMapOf(),
                    customFields = CustomFieldMap(),
                    languageVariable = null,
                )

            val findVar = { id: String ->
                variableRepository.find(id) ?: error("Unable to find variable '$id' referenced from display rule '${rule.id}'")
            }

            if (rule.definition?.containsFunction() == true) {
                val message = "External display rule '${rule.id}' contains functions. Will fallback to internal display rule"
                logger.warn(message)
                tracker.warningDisplayRule(rule.id, targetPath, message)
                continue
            }

            val jrd = Jrd.fromDisplayRule(rule, projectConfig, variableStructure, findVar)

            val uploadResult = deployDisplayRule(rule, targetPath, jrd.toByteArray())
            if (uploadResult is OperationResult.Failure) {
                tracker.errorDisplayRule(rule.id, targetPath, uploadResult.message)
                continue
            }

            logger.debug("Deployment of display rule '${rule.nameOrId()}' to '${targetPath}' is successful.")
            tracker.deployedDisplayRule(rule.id, targetPath)
        }
    }

    override fun uploadDocumentObject(obj: DocumentObject, targetPath: IcmPath, wfdXml: String): OperationResult {
        val runCommandType = obj.type.toRunCommandType()
        return ipsService.deployJld(
            baseTemplate = getBaseTemplateFullPath(projectConfig, obj.baseTemplate),
            type = runCommandType,
            moduleName = "DocumentLayout",
            xmlContent = wfdXml,
            outputPath = targetPath
        )

    }

    override fun uploadImage(img: Image, targetPath: IcmPath, data: ByteArray): OperationResult {
        return ipsService.tryUpload(targetPath, data)
    }

    override fun uploadAttachment(att: Attachment, targetPath: IcmPath, data: ByteArray): OperationResult {
        return ipsService.tryUpload(targetPath, data)
    }

    override fun uploadDisplayRule(rule: DisplayRule, targetPath: IcmPath, data: ByteArray): OperationResult {
        return ipsService.tryUpload(targetPath, data)
    }

    override fun deployDocumentObjectsInternal(
        documentObjects: List<DocumentObject>,
        tracker: ResultTracker,
        uploadDocumentObject: (DocumentObject, IcmPath, String) -> OperationResult,
        uploadImage: (Image, IcmPath, ByteArray) -> OperationResult,
        uploadAttachment: (Attachment, IcmPath, ByteArray) -> OperationResult,
        uploadDisplayRule: (DisplayRule, IcmPath, ByteArray) -> OperationResult,
    ): DeploymentResult {
        deployImagesAndAttachments(documentObjects, tracker, uploadImage, uploadAttachment)
        deployDisplayRules(documentObjects, tracker, uploadDisplayRule)

        for (it in documentObjects) {
            val targetPath = documentObjectBuilder.getDocumentObjectPath(it)

            if (!shouldDeployObject(it.id, ResourceType.DocumentObject, targetPath, tracker.deploymentResult)) {
                logger.info("Skipping deployment of '${it.id}' as it is not marked for deployment.")
                continue
            }

            val invalidMetadata = it.getInvalidMetadataKeys()
            if (invalidMetadata.isNotEmpty()) {
                logger.error("Failed to deploy '$targetPath' due to invalid metadata.")
                val keys = invalidMetadata.joinToString(", ", prefix = "[", postfix = "]")
                val message = "Metadata of document object '${it.id}' contains invalid keys: $keys"
                tracker.errorDocumentObject(it.id, targetPath, it.type, message)
                continue
            }

            try {
                val documentObjectXml = documentObjectBuilder.buildDocumentObject(it)
                when (val editResult = uploadDocumentObject(it, targetPath, documentObjectXml)) {
                    OperationResult.Success -> {
                        logger.debug("Deployment of '$targetPath' is successful.")
                        tracker.deployedDocumentObject(it.id, targetPath, it.type)
                    }

                    is OperationResult.Failure -> {
                        logger.error("Failed to deploy '$targetPath'.")
                        tracker.errorDocumentObject(it.id, targetPath, it.type, editResult.message)
                    }
                }
            } catch (e: IllegalStateException) {
                tracker.errorDocumentObject(it.id, targetPath, it.type, e.message ?: "")
            }
        }

        return tracker.deploymentResult
    }

    override fun deployStyles() {
        val deploymentId = Uuid.random()
        val deploymentTimestamp = Clock.System.now()

        val outputPathJld = documentObjectBuilder.getStyleDefinitionPath()
        val outputPathWfd = outputPathJld.extension(".wfd")

        val xml2wfdResult = ipsService.xml2wfd(
            documentObjectBuilder.buildStyles(emptyList(), emptyList()),
            outputPathWfd
        )

        when (xml2wfdResult) {
            is OperationResult.Success -> {
                logger.debug("Deployment of $outputPathWfd is successful. Will deploy style $outputPathJld next.")
            }

            is OperationResult.Failure -> {
                logger.error("Failed to deploy $outputPathWfd. Will not attempt to deploy style $outputPathJld.")
                return
            }
        }

        val textStyles = textStyleRepository.listAll().filter { it.targetId == null }
        val paragraphStyles = paragraphStyleRepository.listAll().filter { it.targetId == null }

        val styleLayoutDeltaXml = documentObjectBuilder.buildStyleLayoutDelta(
            textStyles = textStyles,
            paragraphStyles = paragraphStyles
        )

        val editResult = ipsService.deployStyleJld(
            baseTemplate = outputPathWfd,
            xmlContent = styleLayoutDeltaXml,
            outputPath = outputPathJld,
        )

        when (editResult) {
            OperationResult.Success -> {
                logger.debug("Deployment of $outputPathJld is successful.")
                textStyles.forEach {
                    statusTrackingRepository.deployed(
                        id = it.id,
                        deploymentId = deploymentId,
                        timestamp = deploymentTimestamp,
                        resourceType = ResourceType.TextStyle,
                        icmPath = outputPathWfd,
                        output = output
                    )
                }
                paragraphStyles.forEach {
                    statusTrackingRepository.deployed(
                        id = it.id,
                        deploymentId = deploymentId,
                        timestamp = deploymentTimestamp,
                        resourceType = ResourceType.ParagraphStyle,
                        icmPath = outputPathWfd,
                        output = output
                    )
                }
            }

            is OperationResult.Failure -> {
                logger.error("Failed to deploy $outputPathJld.")
                textStyles.forEach {
                    statusTrackingRepository.error(
                        it.id,
                        deploymentId,
                        deploymentTimestamp,
                        ResourceType.TextStyle,
                        outputPathWfd,
                        output,
                        ""
                    )
                }
                paragraphStyles.forEach {
                    statusTrackingRepository.error(
                        it.id,
                        deploymentId,
                        deploymentTimestamp,
                        ResourceType.ParagraphStyle,
                        outputPathWfd,
                        output,
                        ""
                    )
                }
                return
            }
        }

        val approvalResult = ipsService.setProductionApprovalState(listOf(outputPathWfd, outputPathJld))
        when (approvalResult) {
            is OperationResult.Failure -> logger.error("Failed to set production approval state to $outputPathWfd and $outputPathJld.")
            OperationResult.Success -> logger.debug("Setting of production approval state to $outputPathWfd and $outputPathJld is successful.")
        }
    }

    private fun DocumentObject.getAllExternalDisplayRules(): List<DisplayRule> {
        val resources = mutableListOf<DisplayRule>()

        this.collectRefs().forEach {
            when (it) {
                is DisplayRuleRef -> {
                    val model = displayRuleRepository.find(it.id)
                        ?: error("Unable to collect resource references because display rule '${it.id}' was not found.")

                    val resolvedModel = model.resolveTarget(displayRuleRepository::findOrFail)
                    if (!resolvedModel.internal) {
                        resources.add(model)
                    }
                }
                is DocumentObjectRef -> {
                    val model = documentObjectRepository.find(it.id)
                        ?: error("Unable to collect resource references because inner document object '${it.id}' was not found.")

                    resources.addAll(model.getAllExternalDisplayRules())
                }
                is ParagraphStyleRef, is AttachmentRef, is ImageRef, is TextStyleRef, is VariableRef, is VariableStructureRef -> {}
            }
        }

        return resources
    }
}
