@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.service.deploy

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.repository.ImageInternalRepository
import com.quadient.migration.persistence.repository.ParagraphStyleInternalRepository
import com.quadient.migration.persistence.repository.TextStyleInternalRepository
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.service.Storage
import com.quadient.migration.service.inspirebuilder.InteractiveDocumentObjectBuilder
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.service.ipsclient.OperationResult
import com.quadient.migration.shared.DocumentObjectType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import kotlin.uuid.ExperimentalUuidApi

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

    override fun shouldIncludeDependency(documentObject: DocumentObjectModel): Boolean {
        return !documentObject.internal
    }

    override fun shouldIncludeImage(documentObject: DocumentObjectModel): Boolean {
        return documentObject.internal
    }

    override fun getAllDocumentObjectsToDeploy(): List<DocumentObjectModel> {
        return documentObjectRepository.list(
            DocumentObjectTable.type neq DocumentObjectType.Unsupported.toString() and DocumentObjectTable.internal.eq(
                false
            )
        ).let { deployOrder(it) }
    }

    override fun getDocumentObjectsToDeploy(documentObjectIds: List<String>): List<DocumentObjectModel> {
        val documentObjects = documentObjectRepository.list(DocumentObjectTable.id inList documentObjectIds)
        val unsupported = mutableListOf<String>()
        val internal = mutableListOf<String>()
        for (documentObject in documentObjects) {
            if (documentObject.type == DocumentObjectType.Unsupported) {
                unsupported.add(documentObject.id)
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
        if (unsupported.isNotEmpty()) {
            error += "The following document objects are unsupported: [${unsupported.joinToString(", ")}]. "
        }
        if (internal.isNotEmpty()) {
            error += "The following document objects are internal: [${internal.joinToString(", ")}]. "
        }
        require(error.isEmpty()) { error }

        return documentObjects
    }

    override fun deployDocumentObjectsInternal(documentObjects: List<DocumentObjectModel>): DeploymentResult {
        val deploymentResult = DeploymentResult()

        val orderedDocumentObject = deployOrder(documentObjects)

        deploymentResult += deployImages(orderedDocumentObject)

        for (it in orderedDocumentObject) {
            val targetPath = documentObjectBuilder.getDocumentObjectPath(it)

            if (!shouldDeployObject(it.id, ResourceType.DocumentObject, targetPath, deploymentResult)) {
                logger.info("Skipping deployment of '${it.id}' as it is not marked for deployment.")
                continue
            }

            try {
                val documentObjectXml = documentObjectBuilder.buildDocumentObject(it)
                val runCommandType = it.type.toRunCommandType()

                val editResult = ipsService.deployJld(
                    baseTemplate = it.baseTemplate ?: projectConfig.baseTemplatePath,
                    type = runCommandType,
                    moduleName = "DocumentLayout",
                    xmlContent = documentObjectXml,
                    outputPath = targetPath
                )

                when (editResult) {
                    OperationResult.Success -> {
                        logger.debug("Deployment of '$targetPath' is successful.")
                        deploymentResult.deployed.add(DeploymentInfo(it.id, ResourceType.DocumentObject, targetPath))
                        statusTrackingRepository.deployed(
                            id = it.id,
                            deploymentId = deploymentId,
                            timestamp = deploymentTimestamp,
                            resourceType = ResourceType.DocumentObject,
                            output = output,
                            icmPath = targetPath,
                            data = mapOf("type" to it.type.toString())
                        )
                    }

                    is OperationResult.Failure -> {
                        logger.error("Failed to deploy '$targetPath'.")
                        deploymentResult.errors.add(DeploymentError(it.id, editResult.message))
                        statusTrackingRepository.error(
                            id = it.id,
                            deploymentId = deploymentId,
                            timestamp = deploymentTimestamp,
                            resourceType = ResourceType.DocumentObject,
                            output = output,
                            icmPath = targetPath,
                            message = editResult.message,
                            data = mapOf("type" to it.type.toString())
                        )
                    }
                }
            } catch (e: IllegalStateException) {
                deploymentResult.errors.add(DeploymentError(it.id, e.message ?: ""))
            }
        }

        val approvalResult = ipsService.setProductionApprovalState(deploymentResult.deployed.map { it.targetPath })
        if (approvalResult == OperationResult.Success) {
            logger.debug("Setting of production approval state to ${deploymentResult.deployed.size} document objects is successful.")
        } else {
            logger.error("Failed to set production approval state to document objects.")
        }

        ipsService.close()

        return deploymentResult
    }
}