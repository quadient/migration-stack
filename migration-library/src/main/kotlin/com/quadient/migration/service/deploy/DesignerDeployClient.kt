package com.quadient.migration.service.deploy

import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.repository.ImageInternalRepository
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.service.Storage
import com.quadient.migration.service.inspirebuilder.DesignerDocumentObjectBuilder
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.service.ipsclient.OperationResult
import com.quadient.migration.shared.DocumentObjectType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and

class DesignerDeployClient(
    documentObjectRepository: DocumentObjectInternalRepository,
    imageRepository: ImageInternalRepository,
    documentObjectBuilder: DesignerDocumentObjectBuilder,
    ipsService: IpsService,
    storage: Storage,
) : DeployClient(documentObjectRepository, imageRepository, documentObjectBuilder, ipsService, storage) {

    override fun shouldIncludeDependency(documentObject: DocumentObjectModel): Boolean {
        return documentObject.type == DocumentObjectType.Page || !documentObject.internal
    }

    override fun shouldIncludeImage(documentObject: DocumentObjectModel): Boolean {
        return documentObject.internal || documentObject.type == DocumentObjectType.Page
    }

    override fun deployDocumentObjects(documentObjectIds: List<String>, skipDependencies: Boolean): DeploymentResult {
        val documentObjects = documentObjectRepository
            .list(DocumentObjectTable.id inList documentObjectIds)
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
            error += "The following document objects cannot be deployed due to their type: [${invalidTypeIds.joinToString(", ")}]. "
        }

        require(error.isEmpty()) { error }

        val documentObjectsWithoutPages = documentObjects.filter { it.type != DocumentObjectType.Page }
        return if (skipDependencies) {
            deployDocumentObjectsInternal(documentObjectsWithoutPages)
        } else {
            val dependencies = documentObjectsWithoutPages.flatMap { it.findDependencies() }.filter { !it.internal }

            deployDocumentObjectsInternal((documentObjectsWithoutPages + dependencies).toSet().toList())
        }
    }

    override fun deployDocumentObjects(): DeploymentResult {
        val documentObjects = documentObjectRepository.list(
            (DocumentObjectTable.type inList listOf(
                DocumentObjectType.Template.toString(),
                DocumentObjectType.Block.toString(),
                DocumentObjectType.Section.toString()
            ) and DocumentObjectTable.internal.eq(false))
        )

        return deployDocumentObjectsInternal(documentObjects)
    }

    fun deployDocumentObjectsInternal(documentObjects: List<DocumentObjectModel>): DeploymentResult {
        val deploymentResult = DeploymentResult()

        val orderedDocumentObject = deployOrder(documentObjects)
        deploymentResult += deployImages(orderedDocumentObject)

        orderedDocumentObject.forEach {
            try {
                val templateWfdXml = documentObjectBuilder.buildDocumentObject(it)
                val targetPath = documentObjectBuilder.getDocumentObjectPath(it)
                val xml2wfdResult = ipsService.xml2wfd(templateWfdXml, targetPath)

                when (xml2wfdResult) {
                    OperationResult.Success -> {
                        logger.debug("Deployment of $targetPath is successful.")
                        deploymentResult.deployed.add(DeploymentInfo(it.id, ResourceType.DocumentObject, targetPath))
                    }

                    is OperationResult.Failure -> {
                        logger.error("Failed to deploy $targetPath.")
                        deploymentResult.errors.add(DeploymentError(it.id, xml2wfdResult.message))
                    }
                }
            } catch (e: IllegalStateException) {
                deploymentResult.errors.add(DeploymentError(it.id, e.message ?: ""))
            }
        }

        ipsService.close()

        return deploymentResult
    }
}