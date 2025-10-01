@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.service.deploy

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.repository.ImageInternalRepository
import com.quadient.migration.persistence.repository.ParagraphStyleInternalRepository
import com.quadient.migration.persistence.repository.TextStyleInternalRepository
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.service.Storage
import com.quadient.migration.service.inspirebuilder.DesignerDocumentObjectBuilder
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.service.ipsclient.OperationResult
import com.quadient.migration.shared.DocumentObjectType
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.inList
import org.jetbrains.exposed.v1.core.and
import kotlin.uuid.ExperimentalUuidApi

class DesignerDeployClient(
    documentObjectRepository: DocumentObjectInternalRepository,
    imageRepository: ImageInternalRepository,
    statusTrackingRepository: StatusTrackingRepository,
    textStyleRepository: TextStyleInternalRepository,
    paragraphStyleRepository: ParagraphStyleInternalRepository,
    documentObjectBuilder: DesignerDocumentObjectBuilder,
    ipsService: IpsService,
    storage: Storage,
) : DeployClient(
    documentObjectRepository,
    imageRepository,
    statusTrackingRepository,
    textStyleRepository,
    paragraphStyleRepository,
    documentObjectBuilder,
    ipsService,
    storage,
    InspireOutput.Designer,
) {

    override fun shouldIncludeDependency(documentObject: DocumentObjectModel): Boolean {
        return documentObject.type == DocumentObjectType.Page || !documentObject.internal
    }

    override fun shouldIncludeImage(documentObject: DocumentObjectModel): Boolean {
        return documentObject.internal || documentObject.type == DocumentObjectType.Page
    }

    override fun deployDocumentObjectsInternal(documentObjects: List<DocumentObjectModel>): DeploymentResult {
        val deploymentResult = DeploymentResult(deploymentId)

        val orderedDocumentObject = deployOrder(documentObjects)
        deploymentResult += deployImages(orderedDocumentObject)

        for (it in orderedDocumentObject) {
            val targetPath = documentObjectBuilder.getDocumentObjectPath(it)

            if (!shouldDeployObject(it.id, ResourceType.DocumentObject, targetPath, deploymentResult)) {
                logger.info("Skipping deployment of '${it.id}' as it is not marked for deployment.")
                continue
            }

            val styleDefPath = try {
                val styleDefPath = documentObjectBuilder.getStyleDefinitionPath()
                if (ipsService.fileExists(styleDefPath)) {
                    styleDefPath
                } else {
                    null
                }
            } catch (e: Exception) {
                throw RuntimeException("Failed to check for style definition existence", e)
            }

            try {
                val templateWfdXml = documentObjectBuilder.buildDocumentObject(it, styleDefPath)
                val xml2wfdResult = ipsService.xml2wfd(templateWfdXml, targetPath)

                when (xml2wfdResult) {
                    OperationResult.Success -> {
                        logger.debug("Deployment of $targetPath is successful.")
                        deploymentResult.deployed.add(DeploymentInfo(it.id, ResourceType.DocumentObject, targetPath))
                        statusTrackingRepository.deployed(
                            id = it.id,
                            deploymentId = deploymentId,
                            timestamp = deploymentTimestamp,
                            resourceType = ResourceType.DocumentObject,
                            output = output,
                            icmPath = targetPath,
                            data = mapOf("type" to it.type.toString()),
                        )
                    }

                    is OperationResult.Failure -> {
                        logger.error("Failed to deploy $targetPath.")
                        deploymentResult.errors.add(DeploymentError(it.id, xml2wfdResult.message))
                        statusTrackingRepository.error(
                            id = it.id,
                            deploymentId = deploymentId,
                            timestamp = deploymentTimestamp,
                            resourceType = ResourceType.DocumentObject,
                            output = output,
                            icmPath = targetPath,
                            message = xml2wfdResult.message,
                            data = mapOf("type" to it.type.toString()),
                        )
                    }
                }
            } catch (e: Exception) {
                deploymentResult.errors.add(DeploymentError(it.id, e.message ?: ""))
            }
        }

        ipsService.close()

        return deploymentResult
    }

    override fun getDocumentObjectsToDeploy(documentObjectIds: List<String>): List<DocumentObjectModel> {
        val documentObjects = documentObjectRepository.list(DocumentObjectTable.id inList documentObjectIds)
        val invalidTypeIds = mutableListOf<String>()
        val internal = mutableListOf<String>()
        for (documentObject in documentObjects) {
            when (documentObject.type) {
                DocumentObjectType.Unsupported -> invalidTypeIds.add(documentObject.id)
                DocumentObjectType.Page, DocumentObjectType.Template, DocumentObjectType.Block, DocumentObjectType.Section -> {}
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
        if (internal.isNotEmpty()) {
            error += "The following document objects are internal: [${internal.joinToString(", ")}]. "
        }
        if (invalidTypeIds.isNotEmpty()) {
            error += "The following document objects cannot be deployed due to their type: [${
                invalidTypeIds.joinToString(
                    ", "
                )
            }]. "
        }

        require(error.isEmpty()) { error }

        val documentObjectsWithoutPages = documentObjects.filter { it.type != DocumentObjectType.Page }

        return documentObjectsWithoutPages
    }

    override fun getAllDocumentObjectsToDeploy(): List<DocumentObjectModel> {
        return documentObjectRepository.list(
            (DocumentObjectTable.type inList listOf(
                DocumentObjectType.Template.toString(),
                DocumentObjectType.Block.toString(),
                DocumentObjectType.Section.toString()
            ) and DocumentObjectTable.internal.eq(false))
        )
    }
}