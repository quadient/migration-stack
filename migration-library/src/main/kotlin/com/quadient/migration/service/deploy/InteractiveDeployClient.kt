@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.service.deploy

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.data.ParagraphStyleDefinitionModel
import com.quadient.migration.data.TextStyleDefinitionModel
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.repository.ImageInternalRepository
import com.quadient.migration.persistence.repository.ParagraphStyleInternalRepository
import com.quadient.migration.persistence.repository.TextStyleInternalRepository
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.service.Storage
import com.quadient.migration.service.getBaseTemplateFullPath
import com.quadient.migration.service.inspirebuilder.InteractiveDocumentObjectBuilder
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.service.ipsclient.OperationResult
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.inList
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.json.extract
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class InteractiveDeployClient(
    documentObjectRepository: DocumentObjectInternalRepository,
    imageRepository: ImageInternalRepository,
    statusTrackingRepository: StatusTrackingRepository,
    textStyleRepository: TextStyleInternalRepository,
    paragraphStyleRepository: ParagraphStyleInternalRepository,
    documentObjectBuilder: InteractiveDocumentObjectBuilder,
    ipsService: IpsService,
    storage: Storage,
    private val projectConfig: ProjectConfig,
) : DeployClient(
    documentObjectRepository,
    imageRepository,
    statusTrackingRepository,
    textStyleRepository,
    paragraphStyleRepository,
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

    override fun shouldIncludeDependency(documentObject: DocumentObjectModel): Boolean {
        return !documentObject.internal
    }

    override fun getAllDocumentObjectsToDeploy(): List<DocumentObjectModel> {
        return documentObjectRepository.list(
            DocumentObjectTable.skip.extract<String>("skipped") eq "false" and DocumentObjectTable.internal.eq(
                false
            )
        ).let { deployOrder(it) }
    }

    override fun getDocumentObjectsToDeploy(documentObjectIds: List<String>): List<DocumentObjectModel> {
        val documentObjects = documentObjectRepository.list(DocumentObjectTable.id inList documentObjectIds)
        val skipped = mutableListOf<String>()
        val internal = mutableListOf<String>()
        for (documentObject in documentObjects) {
            if (documentObject.skip.skipped) {
                skipped.add(documentObject.id)
            }
            if (documentObject.internal) {
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

    override fun deployDocumentObjectsInternal(documentObjects: List<DocumentObjectModel>): DeploymentResult {
        val deploymentId = Uuid.random()
        val deploymentTimestamp = Clock.System.now()
        val deploymentResult = DeploymentResult(deploymentId)

        val orderedDocumentObject = deployOrder(documentObjects)

        deploymentResult += deployImages(orderedDocumentObject, deploymentId, deploymentTimestamp)
        val tracker = ResultTracker(statusTrackingRepository, deploymentResult, deploymentId, deploymentTimestamp, output)

        for (it in orderedDocumentObject) {
            val targetPath = documentObjectBuilder.getDocumentObjectPath(it)

            if (!shouldDeployObject(it.id, ResourceType.DocumentObject, targetPath, deploymentResult)) {
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
                val documentObjectXml = documentObjectBuilder.buildDocumentObject(it, null)
                val runCommandType = it.type.toRunCommandType()

                val editResult = ipsService.deployJld(
                    baseTemplate = getBaseTemplateFullPath(projectConfig, it.baseTemplate).toString(),
                    type = runCommandType,
                    moduleName = "DocumentLayout",
                    xmlContent = documentObjectXml,
                    outputPath = targetPath
                )

                when (editResult) {
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

        return deploymentResult
    }

    override fun deployStyles() {
        val deploymentId = Uuid.random()
        val deploymentTimestamp = Clock.System.now()

        val outputPathWfd = documentObjectBuilder.getStyleDefinitionPath(extension = "wfd")
        val outputPathJld = documentObjectBuilder.getStyleDefinitionPath(extension = "jld")

        val xml2wfdResult = ipsService.xml2wfd(
            documentObjectBuilder.buildStyles(emptyList(), emptyList(), withDeltaStyles = true),
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

        val textStyles = textStyleRepository.listAllModel().filter { it.definition is TextStyleDefinitionModel }
        val paragraphStyles =
            paragraphStyleRepository.listAllModel().filter { it.definition is ParagraphStyleDefinitionModel }

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
            is OperationResult.Failure -> logger.error("Failed to set production approval state to $outputPathWfd.")
            OperationResult.Success -> logger.debug("Setting of production approval state to $outputPathWfd is successful.")
        }
    }
}