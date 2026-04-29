@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.service.deploy

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.data.Active
import com.quadient.migration.data.Deployed
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.Attachment
import com.quadient.migration.api.dto.migrationmodel.AttachmentRef
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.ResourceRef
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.api.repository.DisplayRuleRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ParagraphStyleRepository
import com.quadient.migration.api.repository.Repository
import com.quadient.migration.api.repository.TextStyleRepository
import com.quadient.migration.api.repository.VariableRepository
import com.quadient.migration.api.repository.VariableStructureRepository
import com.quadient.migration.service.ReadResult
import com.quadient.migration.service.Storage
import com.quadient.migration.service.deploy.utility.ConflictDetector
import com.quadient.migration.service.deploy.utility.ConflictDetectorImpl
import com.quadient.migration.service.deploy.utility.DeploymentError
import com.quadient.migration.service.deploy.utility.DeploymentResult
import com.quadient.migration.service.deploy.utility.MetadataValidator
import com.quadient.migration.service.deploy.utility.MetadataValidatorImpl
import com.quadient.migration.service.deploy.utility.PostProcess
import com.quadient.migration.service.deploy.utility.PostProcessImpl
import com.quadient.migration.service.deploy.utility.ProgressReport
import com.quadient.migration.service.deploy.utility.ProgressReporter
import com.quadient.migration.service.deploy.utility.ProgressReporterImpl
import com.quadient.migration.service.deploy.utility.ResourceType
import com.quadient.migration.service.deploy.utility.ResultTracker
import com.quadient.migration.service.deploy.utility.ResultTrackerImpl
import com.quadient.migration.service.deploy.utility.ValidationResult
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.service.ipsclient.OperationResult
import com.quadient.migration.service.readSafely
import com.quadient.migration.service.resolveAlias
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.toIcmPath
import org.slf4j.LoggerFactory
import kotlin.collections.plus
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import com.quadient.migration.data.Error as StatusError

data class DocObjectWithRef(val obj: DocumentObject, val documentObjectRefs: Set<String>)

sealed class DeployClient(
    protected val documentObjectRepository: DocumentObjectRepository,
    protected val imageRepository: Repository<Image>,
    protected val attachmentRepository: Repository<Attachment>,
    protected val statusTrackingRepository: StatusTrackingRepository,
    protected val textStyleRepository: TextStyleRepository,
    protected val paragraphStyleRepository: ParagraphStyleRepository,
    protected val displayRuleRepository: DisplayRuleRepository,
    protected val variableRepository: VariableRepository,
    protected val variableStructureRepository: VariableStructureRepository,
    val documentObjectBuilder: InspireDocumentObjectBuilder,
    protected val ipsService: IpsService,
    protected val storage: Storage,
    protected val output: InspireOutput,
) : MetadataValidator by MetadataValidatorImpl(),
    PostProcess by PostProcessImpl(ipsService, documentObjectRepository, imageRepository, displayRuleRepository),
    ConflictDetector by ConflictDetectorImpl(documentObjectRepository, imageRepository, attachmentRepository, displayRuleRepository, documentObjectBuilder, statusTrackingRepository, output),
    ProgressReporter by ProgressReporterImpl( documentObjectRepository, imageRepository, attachmentRepository, displayRuleRepository, documentObjectBuilder, statusTrackingRepository, output)
{
    protected val logger = LoggerFactory.getLogger(this::class.java)!!

    abstract fun uploadDocumentObject(obj: DocumentObject, targetPath: IcmPath, wfdXml: String): OperationResult
    abstract fun uploadImage(img: Image, targetPath: IcmPath, data: ByteArray): OperationResult
    abstract fun uploadAttachment(att: Attachment, targetPath: IcmPath, data: ByteArray): OperationResult
    abstract fun uploadDisplayRule(rule: DisplayRule, targetPath: IcmPath, data: ByteArray): OperationResult

    abstract fun getAllDocumentObjectsToDeploy(): List<DocumentObject>
    abstract fun getDocumentObjectsToDeploy(documentObjectIds: List<String>): List<DocumentObject>
    abstract fun deployDocumentObjectsInternal(
        documentObjects: List<DocumentObject>,
        tracker: ResultTracker,
        uploadDocumentObject: (DocumentObject, IcmPath, String) -> OperationResult,
        uploadImage: (Image, IcmPath, ByteArray) -> OperationResult,
        uploadAttachment: (Attachment, IcmPath, ByteArray) -> OperationResult,
        uploadDisplayRule: (DisplayRule, IcmPath, ByteArray) -> OperationResult,
    ): DeploymentResult
    abstract fun deployStyles()

    abstract fun shouldIncludeDependency(documentObject: DocumentObject): Boolean

    fun deployDocumentObjects(): DeploymentResult {
        val tracker = ResultTrackerImpl(statusTrackingRepository, output)
        val ordered = deployOrder(getAllDocumentObjectsToDeploy())
        val result = deployDocumentObjectsInternal(ordered, tracker, ::uploadDocumentObject, ::uploadImage, ::uploadAttachment, ::uploadDisplayRule)
        runPostProcessors(result)

        return result
    }

    fun deployDocumentObjects(documentObjectIds: List<String>) = deployDocumentObjects(documentObjectIds, false)
    fun deployDocumentObjects(documentObjectIds: List<String>, skipDependencies: Boolean): DeploymentResult {
        val documentObjects = getDocumentObjectsToDeploy(documentObjectIds)
        val tracker = ResultTrackerImpl(statusTrackingRepository, output)
        val result = if (skipDependencies) {
            val ordered = deployOrder(documentObjects)
            deployDocumentObjectsInternal(ordered, tracker, ::uploadDocumentObject, ::uploadImage, ::uploadAttachment, ::uploadDisplayRule)
        } else {
            val dependencies = documentObjects.flatMap { it.findDependencies() }.filter { it.internal != true }
            val ordered = deployOrder((documentObjects + dependencies).toSet().toList())
            deployDocumentObjectsInternal(ordered, tracker, ::uploadDocumentObject, ::uploadImage, ::uploadAttachment, ::uploadDisplayRule)
        }

        runPostProcessors(result)

        return result
    }

    fun deployOrder(documentObjects: List<DocumentObject>): List<DocumentObject> {
        val documentObjectIds = documentObjects.map { it.id }

        val deployOrder = mutableListOf<DocumentObject>()

        var toCheck = documentObjects.map { obj ->
            DocObjectWithRef(obj, obj.collectRefs().filterIsInstance<DocumentObjectRef>().map { it.id }.toSet())
        }
        val deployed = mutableSetOf<String>()

        var lastSize = toCheck.size
        while (deployed.size < documentObjects.size) {
            val (canDeploy, cantDeploy) = toCheck.partition { docObj ->
                docObj.documentObjectRefs.all {
                    deployed.contains(it) || !documentObjectIds.contains(it)
                }
            }

            if (cantDeploy.isEmpty()) {
                for (item in canDeploy) {
                    deployOrder.add(item.obj)
                }
                return deployOrder
            }

            if (lastSize == cantDeploy.size) {
                logger.error(
                    "Cannot determine deploy order. Either circular reference or some references are missing. Deployed: ${
                        deployed.joinToString(
                            separator = ",", prefix = "'[", postfix = "]'"
                        )
                    }, Can deploy: ${
                        canDeploy.joinToString(
                            separator = ",", prefix = "'[", postfix = "]'"
                        )
                    }, Cannot deploy: ${cantDeploy.joinToString(separator = ",", prefix = "'[", postfix = "]'")}"
                )
                throw RuntimeException("Cannot determine deploy order. Either circular reference or some references are missing.")
            }

            for (item in canDeploy) {
                deployOrder.add(item.obj)
                deployed.add(item.obj.id)
            }

            lastSize = cantDeploy.size
            toCheck = cantDeploy
        }

        return deployOrder
    }

    protected fun deployImagesAndAttachments(
        documentObjects: List<DocumentObject>,
        tracker: ResultTracker,
        deployImageCb: (Image, IcmPath, ByteArray) -> OperationResult,
        deployAttachmentCb: (Attachment, IcmPath, ByteArray) -> OperationResult,
    ) {
        val allResourceRefs = documentObjects.flatMap {
            try {
                it.getAllDocumentObjectResourceRefs()
            } catch (e: IllegalStateException) {
                tracker.deploymentResult.errors.add(DeploymentError(it.id, e.message ?: ""))
                emptyList()
            }
        }.map { resolveAlias(it, imageRepository, attachmentRepository) }
         .distinct()

        for (resourceRef in allResourceRefs) {
            when (resourceRef) {
                is ImageRef -> deployImage(resourceRef, tracker, deployImageCb)
                is AttachmentRef -> deployAttachment(resourceRef, tracker, deployAttachmentCb)
            }
        }
    }

    private fun deployImage(
        imageRef: ImageRef,
        tracker: ResultTracker,
        deployImage: (Image, IcmPath, ByteArray) -> OperationResult,
    ) {
        if (!shouldDeployObject(imageRef.id, ResourceType.Image, imageRef.id.toIcmPath(), tracker.deploymentResult)) {
            logger.info("Skipping deployment of '${imageRef.id}' as it is not marked for deployment.")
            return
        }

        val imageModel = imageRepository.find(imageRef.id)
        if (imageModel == null) {
            val message = "Image '${imageRef.id}' not found."
            logger.error(message)
            tracker.errorImage(imageRef.id, null, message)
            return
        }

        val icmImagePath = documentObjectBuilder.getImagePath(imageModel)

        val invalidMetadata = imageModel.getInvalidMetadataKeys()
        if (invalidMetadata.isNotEmpty()) {
            logger.error("Failed to deploy '$icmImagePath' due to invalid metadata.")
            val keys = invalidMetadata.joinToString(", ", prefix = "[", postfix = "]")
            val message = "Metadata of image '${imageModel.id}' contains invalid keys: $keys"
            tracker.errorImage(imageModel.id, icmImagePath, message)
            return
        }

        if (imageModel.imageType == ImageType.Unknown) {
            val message = "Skipping deployment of image '${imageModel.nameOrId()}' due to unknown image type."
            logger.warn(message)
            tracker.warningImage(imageModel.id, icmImagePath, message)
            return
        }

        if (imageModel.skip.skipped) {
            val reason = imageModel.skip.reason?.let { " Reason: $it" } ?: ""
            val message = "Image '${imageModel.nameOrId()}' is skipped.$reason"
            logger.warn(message)
            tracker.warningImage(imageModel.id, icmImagePath, message)
            return
        }

        if (imageModel.sourcePath.isNullOrBlank()) {
            val message = "Skipping deployment of image '${imageModel.nameOrId()}' due to missing source path."
            logger.warn(message)
            tracker.warningImage(imageModel.id, icmImagePath, message)
            return
        }

        logger.debug("Starting deployment of image '${imageModel.nameOrId()}'.")
        val sourcePath = imageModel.sourcePath!!
        val readResult = storage.readSafely(sourcePath)
        if (readResult is ReadResult.Error) {
            val message = "Error while reading image source data: ${readResult.errorMessage}."
            logger.error(message)
            tracker.errorImage(imageModel.id, icmImagePath, message)
            return
        }

        val imageData = (readResult as ReadResult.Success).result
        logger.trace("Loaded image data of size ${imageData.size} from storage.")

        val uploadResult = deployImage(imageModel, icmImagePath, imageData)
        if (uploadResult is OperationResult.Failure) {
            tracker.errorImage(imageModel.id, icmImagePath, uploadResult.message)
            return
        }

        logger.debug("Deployment of image '${imageModel.nameOrId()}' to '${icmImagePath}' is successful.")
        tracker.deployedImage(imageModel.id, icmImagePath)
    }

    private fun deployAttachment(
        attachmentRef: AttachmentRef,
        tracker: ResultTracker,
        deployAttachment: (Attachment, IcmPath, ByteArray) -> OperationResult,
        ) {
        if (!shouldDeployObject(attachmentRef.id, ResourceType.Attachment, attachmentRef.id.toIcmPath(), tracker.deploymentResult)) {
            logger.info("Skipping deployment of attachment '${attachmentRef.id}' as it is not marked for deployment.")
            return
        }

        val attachmentModel = attachmentRepository.find(attachmentRef.id)
        if (attachmentModel == null) {
            val message = "Attachment '${attachmentRef.id}' not found."
            logger.error(message)
            tracker.errorAttachment(attachmentRef.id, null, message)
            return
        }

        val icmFilePath = documentObjectBuilder.getAttachmentPath(attachmentModel)

        if (attachmentModel.skip.skipped) {
            val reason = attachmentModel.skip.reason?.let { " Reason: $it" } ?: ""
            val message = "Attachment '${attachmentModel.nameOrId()}' is skipped.$reason"
            logger.warn(message)
            tracker.warningAttachment(attachmentModel.id, icmFilePath, message)
            return
        }

        if (attachmentModel.sourcePath.isNullOrBlank()) {
            val message = "Skipping deployment of attachment '${attachmentModel.nameOrId()}' due to missing source path."
            logger.warn(message)
            tracker.warningAttachment(attachmentModel.id, icmFilePath, message)
            return
        }

        logger.debug("Starting deployment of attachment '${attachmentModel.nameOrId()}'.")
        val sourcePath = attachmentModel.sourcePath!!
        val readResult = storage.readSafely(sourcePath)
        if (readResult is ReadResult.Error) {
            val message = "Error while reading attachment source data: ${readResult.errorMessage}."
            logger.error(message)
            tracker.errorAttachment(attachmentModel.id, icmFilePath, message)
            return
        }

        val attachmentData = (readResult as ReadResult.Success).result
        logger.trace("Loaded attachment data of size ${attachmentData.size} from storage.")

        val uploadResult = deployAttachment(attachmentModel, icmFilePath, attachmentData)
        if (uploadResult is OperationResult.Failure) {
            tracker.errorAttachment(attachmentModel.id, icmFilePath, uploadResult.message)
            return
        }

        logger.debug("Deployment of attachment '${attachmentModel.nameOrId()}' to '${icmFilePath}' is successful.")
        tracker.deployedAttachment(attachmentModel.id, icmFilePath)
    }

    protected fun shouldDeployObject(
        id: String, resourceType: ResourceType, targetPath: IcmPath?, deploymentResult: DeploymentResult
    ): Boolean {
        val currentStatus = statusTrackingRepository.findLastEventRelevantToOutput(id, resourceType, output)

        return when (currentStatus) {
            null -> {
                logger.error("Not deploying '$targetPath' due to a missing status.")
                deploymentResult.errors.add(DeploymentError(id, "Missing tracked status for $resourceType"))
                false
            }

            is Deployed -> {
                logger.info("Already deployed: '$targetPath'.")
                false
            }

            is Active -> {
                logger.info("Deploying '$targetPath'.")
                true
            }

            is StatusError -> {
                logger.info("Last attempt to deploy '$targetPath' failed with error: '${currentStatus.error}'. Trying again.")
                true
            }
        }
    }

    fun progressReport(deployId: Uuid? = null): ProgressReport {
        val objects = getAllDocumentObjectsToDeploy()
        return createProgressReport(objects, deployId)
    }

    fun progressReport(documentObjectIds: List<String>, deployId: Uuid? = null): ProgressReport {
        val objects = getDocumentObjectsToDeploy(documentObjectIds)
        return createProgressReport(objects, deployId)
    }

    fun validateConflicts(): ValidationResult {
        // Post processors should not run for conflict validation because they modify content in ICM
        val pp = clearPostProcessors()
        try {
            return runConflictValidation(getAllDocumentObjectsToDeploy(), ::deployDocumentObjectsInternal)
        } finally {
            pp.forEach(::addPostProcessor)
        }
    }

    fun validateConflicts(documentObjectIds: List<String>): ValidationResult {
        // Post processors should not run for conflict validation because they modify content in ICM
        val pp = clearPostProcessors()
        try {
            return runConflictValidation(getDocumentObjectsToDeploy(documentObjectIds), ::deployDocumentObjectsInternal)
        } finally {
            pp.forEach(::addPostProcessor)
        }
    }

    fun DocumentObject.findDependencies(): List<DocumentObject> {
        val dependencies = mutableListOf<DocumentObject>()
        this.collectRefs().forEach { ref ->
            when (ref) {
                is DisplayRuleRef, is TextStyleRef, is ParagraphStyleRef, is VariableRef, is VariableStructureRef -> {}
                is ImageRef -> {}
                is AttachmentRef -> {}
                is DocumentObjectRef -> {
                    val model = documentObjectRepository.findOrFail(ref.id)
                    if (shouldIncludeDependency(model)) {
                        dependencies.add(model)
                    }
                    dependencies.addAll(model.findDependencies())
                }
            }
        }
        return dependencies
    }

    private fun DocumentObject.getAllDocumentObjectResourceRefs(): List<ResourceRef> {
        val resources = mutableListOf<ResourceRef>()

        this.collectRefs().forEach { ref ->
            when (ref) {
                is DisplayRuleRef, is TextStyleRef, is ParagraphStyleRef, is VariableRef, is VariableStructureRef -> {}
                is ResourceRef -> resources.add(ref)
                is DocumentObjectRef -> {
                    val model = documentObjectRepository.find(ref.id)
                        ?: error("Unable to collect resource references because inner document object '${ref.id}' was not found.")

                    if (documentObjectBuilder.shouldIncludeInternalDependency(model)) {
                        resources.addAll(model.getAllDocumentObjectResourceRefs())
                    }
                }
            }
        }

        return resources
    }
}