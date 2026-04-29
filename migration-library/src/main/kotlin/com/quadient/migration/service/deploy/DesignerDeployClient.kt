@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.service.deploy

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.dto.migrationmodel.Attachment
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.repository.DisplayRuleRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ParagraphStyleRepository
import com.quadient.migration.api.repository.Repository
import com.quadient.migration.api.repository.TextStyleRepository
import com.quadient.migration.api.repository.VariableRepository
import com.quadient.migration.api.repository.VariableStructureRepository
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.service.Storage
import com.quadient.migration.service.deploy.utility.DeploymentResult
import com.quadient.migration.service.deploy.utility.ResourceType
import com.quadient.migration.service.deploy.utility.ResultTracker
import com.quadient.migration.service.inspirebuilder.DesignerDocumentObjectBuilder
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.service.ipsclient.OperationResult
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.IcmPath
import kotlin.time.Clock
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.json.extract
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class DesignerDeployClient(
    documentObjectRepository: DocumentObjectRepository,
    imageRepository: Repository<Image>,
    attachmentRepository: Repository<Attachment>,
    statusTrackingRepository: StatusTrackingRepository,
    textStyleRepository: TextStyleRepository,
    paragraphStyleRepository: ParagraphStyleRepository,
    displayRuleRepository: DisplayRuleRepository,
    variableRepository: VariableRepository,
    variableStructureRepository: VariableStructureRepository,
    documentObjectBuilder: DesignerDocumentObjectBuilder,
    ipsService: IpsService,
    storage: Storage,
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
    InspireOutput.Designer,
) {
    override fun shouldIncludeDependency(documentObject: DocumentObject): Boolean {
        return documentObject.type != DocumentObjectType.Page && documentObject.internal != true
    }

    override fun uploadDocumentObject(obj: DocumentObject, targetPath: IcmPath, wfdXml: String): OperationResult {
        return ipsService.xml2wfd(wfdXml, targetPath)
    }

    override fun uploadImage(img: Image, targetPath: IcmPath, data: ByteArray): OperationResult {
        return ipsService.tryUpload(targetPath, data)
    }

    override fun uploadAttachment(att: Attachment, targetPath: IcmPath, data: ByteArray): OperationResult {
        return ipsService.tryUpload(targetPath, data)
    }

    override fun uploadDisplayRule(rule: DisplayRule, targetPath: IcmPath, data: ByteArray): OperationResult {
        return OperationResult.Success
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
                val templateWfdXml  = documentObjectBuilder.buildDocumentObject(it)
                when (val xml2wfdResult = uploadDocumentObject(it, targetPath, templateWfdXml)) {
                    OperationResult.Success -> {
                        logger.debug("Deployment of $targetPath is successful.")
                        tracker.deployedDocumentObject(it.id, targetPath, it.type)
                    }

                    is OperationResult.Failure -> {
                        logger.error("Failed to deploy $targetPath.")
                        tracker.errorDocumentObject(it.id, targetPath, it.type, xml2wfdResult.message)
                    }
                }
            } catch (e: Exception) {
                tracker.errorDocumentObject(it.id, targetPath, it.type, e.message ?: "")
            }
        }

        return tracker.deploymentResult
    }

    override fun getDocumentObjectsToDeploy(documentObjectIds: List<String>): List<DocumentObject> {
        val documentObjects = documentObjectRepository.list(DocumentObjectTable.id inList documentObjectIds)
        val skippedIds = mutableListOf<String>()
        val internal = mutableListOf<String>()
        for (documentObject in documentObjects) {
            if (documentObject.skip.skipped == true) {
                skippedIds.add(documentObject.id)
            } else if (documentObject.internal == true) {
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
        if (skippedIds.isNotEmpty()) {
            error += "The following document objects are skipped: [${skippedIds.joinToString(", ")}]. "
        }

        require(error.isEmpty()) { error }

        val documentObjectsWithoutPages = documentObjects
            .filter { it.type != DocumentObjectType.Page && it.type != DocumentObjectType.Snippet }

        return documentObjectsWithoutPages
    }

    override fun getAllDocumentObjectsToDeploy(): List<DocumentObject> {
        return documentObjectRepository.list(
            (DocumentObjectTable.type inList listOf(
                DocumentObjectType.Template.toString(),
                DocumentObjectType.Block.toString(),
                DocumentObjectType.Section.toString()
            ) and DocumentObjectTable.internal.eq(false) and (DocumentObjectTable.skip.extract<String>("skipped") eq "false"))
        )
    }

    override fun deployStyles() {
        val deploymentId = Uuid.random()
        val deploymentTimestamp = Clock.System.now()

        val textStyles = textStyleRepository.listAll().filter { it.targetId == null }
        val paragraphStyles = paragraphStyleRepository.listAll().filter { it.targetId == null }
        val outputPath = documentObjectBuilder.getStyleDefinitionPath()
        val xml2wfdResult =
            ipsService.xml2wfd(documentObjectBuilder.buildStyles(textStyles, paragraphStyles), outputPath)

        when (xml2wfdResult) {
            is OperationResult.Success -> {
                logger.debug("Deployment of $outputPath is successful.")
                textStyles.forEach {
                    statusTrackingRepository.deployed(
                        id = it.id,
                        deploymentId = deploymentId,
                        timestamp = deploymentTimestamp,
                        resourceType = ResourceType.TextStyle,
                        icmPath = outputPath,
                        output = output
                    )
                }
                paragraphStyles.forEach {
                    statusTrackingRepository.deployed(
                        id = it.id,
                        deploymentId = deploymentId,
                        timestamp = deploymentTimestamp,
                        resourceType = ResourceType.ParagraphStyle,
                        icmPath = outputPath,
                        output = output
                    )
                }
            }

            is OperationResult.Failure -> {
                logger.error("Failed to deploy $outputPath.")
                textStyles.forEach {
                    statusTrackingRepository.error(
                        it.id,
                        deploymentId,
                        deploymentTimestamp,
                        ResourceType.TextStyle,
                        outputPath,
                        output,
                        xml2wfdResult.message
                    )
                }
                paragraphStyles.forEach {
                    statusTrackingRepository.error(
                        it.id,
                        deploymentId,
                        deploymentTimestamp,
                        ResourceType.ParagraphStyle,
                        outputPath,
                        output,
                        xml2wfdResult.message
                    )
                }
                return
            }
        }
    }
}
