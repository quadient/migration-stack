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
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.Ref
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ParagraphStyleRepository
import com.quadient.migration.api.repository.Repository
import com.quadient.migration.api.repository.TextStyleRepository
import com.quadient.migration.service.Storage
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.service.ipsclient.OperationResult
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.IcmFileMetadata
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.toMetadata
import com.quadient.migration.tools.caseInsensitiveSetOf
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.InvalidPathException
import kotlin.reflect.KClass
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
    val documentObjectBuilder: InspireDocumentObjectBuilder,
    protected val ipsService: IpsService,
    protected val storage: Storage,
    protected val output: InspireOutput,
) {
    protected val logger = LoggerFactory.getLogger(this::class.java)!!
    val postProcessors: MutableList<(DeploymentResult) -> Unit> = mutableListOf()

    companion object {
        val DISALLOWED_METADATA = caseInsensitiveSetOf(
            "Type",
            "Dependencies",
            "Tags",
            "Subject",
            "PublicTemplate",
            "StateId",
            "BusinessProcess",
            "TicketTitle",
            "TicketDescription",
            "TicketIcon",
            "UserAttachment",
            "GlobalStorageAttachment",
            "Languages",
            "MailMergeModule",
            "MailMergePath",
            "Guid",
            "IM_Path",
            "IM_PM_Name",
            "IM_PM_Location",
            "IM_Context",
            "ModuleNames",
            "EM_Paths",
            "ResultType",
            "ParamTypes",
            "Channels",
            "Master Template",
            "ProductionActions",
            "Html Content",
            "Responsive Html Content",
            "Brand",
            "WFDType",
            "OutputType",
            "PreviewTypes",
            "SupportedChannels",
            "InteractiveFlowsNames",
            "InteractiveFlowsTypes ",
        )
        val IMAGE_DISALLOWED_METADATA = DISALLOWED_METADATA - "Subject"
    }

    init {
        addPostProcessor { deploymentResult ->
            val meta =
                deploymentResult.deployed.filter { it.type == ResourceType.DocumentObject || it.type == ResourceType.Image }
                    .mapNotNull { info ->
                        val obj = when (info.type) {
                            ResourceType.DocumentObject -> documentObjectRepository.find(info.id)
                            ResourceType.Image -> imageRepository.find(info.id)
                            else -> null
                        }
                        if (obj == null) {
                            deploymentResult.warnings.add(
                                DeploymentWarning(
                                    info.id,
                                    "Failed to set metadata for ${info.type} object '${info.id}' at path '${info.targetPath}'."
                                )
                            )
                            null
                        } else {
                            val metadata = when (obj) {
                                is DocumentObject -> obj.metadata
                                is Image -> obj.metadata
                                else -> emptyMap()
                            }
                            info to metadata
                        }
                    }.mapNotNull { (info, metadata) ->
                        metadata.ifEmpty { null }?.let {
                            IcmFileMetadata(path = info.targetPath, metadata = metadata.toMetadata())
                        }
                    }

            logger.debug("Writing metadata for ${meta.size} deployed document objects.")
            ipsService.writeMetadata(meta)
            logger.debug("Finished writing metadata for deployed document objects.")
        }
    }

    abstract fun getAllDocumentObjectsToDeploy(): List<DocumentObject>
    abstract fun getDocumentObjectsToDeploy(documentObjectIds: List<String>): List<DocumentObject>
    abstract fun deployDocumentObjectsInternal(documentObjects: List<DocumentObject>): DeploymentResult
    abstract fun deployStyles()

    protected fun addPostProcessor(processor: (DeploymentResult) -> Unit) {
        postProcessors.add(processor)
    }

    abstract fun shouldIncludeDependency(documentObject: DocumentObject): Boolean

    fun deployDocumentObjects(): DeploymentResult {
        val result = deployDocumentObjectsInternal(getAllDocumentObjectsToDeploy())
        for (processor in postProcessors) {
            processor(result)
        }

        return result
    }

    fun deployDocumentObjects(documentObjectIds: List<String>) = deployDocumentObjects(documentObjectIds, false)
    fun deployDocumentObjects(documentObjectIds: List<String>, skipDependencies: Boolean): DeploymentResult {
        val documentObjects = getDocumentObjectsToDeploy(documentObjectIds)
        val result = if (skipDependencies) {
            deployDocumentObjectsInternal(documentObjects)
        } else {
            val dependencies = documentObjects.flatMap { it.findDependencies() }.filter { it.internal != true }
            deployDocumentObjectsInternal((documentObjects + dependencies).toSet().toList())
        }

        for (processor in postProcessors) {
            processor(result)
        }

        return result
    }


    fun deployOrder(documentObjects: List<DocumentObject>): List<DocumentObject> {
        val documentObjectIds = documentObjects.map { it.id }

        val deployOrder = mutableListOf<DocumentObject>()

        var toCheck = documentObjects.map {
            DocObjectWithRef(
                it, it.collectRefs().filterIsInstance<DocumentObjectRef>().map { it -> it.id }.toSet()
            )
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

    protected fun deployImagesAndAttachments(documentObjects: List<DocumentObject>, deploymentId: Uuid, deploymentTimestamp: Instant): DeploymentResult {
        val deploymentResult = DeploymentResult(deploymentId)
        val tracker = ResultTracker(statusTrackingRepository, deploymentResult, deploymentId, deploymentTimestamp, output)

        val allRefs = documentObjects.map {
            try {
                it.getAllDocumentObjectImageAndAttachmentRefs()
            } catch (e: IllegalStateException) {
                deploymentResult.errors.add(DeploymentError(it.id, e.message ?: ""))
                Pair(emptyList(), emptyList())
            }
        }
        
        val imageRefs = allRefs.flatMap { pair -> pair.first }.distinct()
        val attachmentRefs = allRefs.flatMap { pair -> pair.second }.distinct()

        for (imageRef in imageRefs) {
            deployImage(imageRef, deploymentResult, tracker)
        }

        for (attachmentRef in attachmentRefs) {
            deployAttachment(attachmentRef, deploymentResult, tracker)
        }

        return deploymentResult
    }

    private fun deployImage(imageRef: ImageRef, deploymentResult: DeploymentResult, tracker: ResultTracker) {
        if (!shouldDeployObject(imageRef.id, ResourceType.Image, imageRef.id, deploymentResult)) {
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
        val readResult = readStorageSafely(sourcePath)
        if (readResult is ReadResult.Error) {
            val message = "Error while reading image source data: ${readResult.errorMessage}."
            logger.error(message)
            tracker.errorImage(imageModel.id, icmImagePath, message)
            return
        }

        val imageData = (readResult as ReadResult.Success).result
        logger.trace("Loaded image data of size ${imageData.size} from storage.")

        val uploadResult = ipsService.tryUpload(icmImagePath, imageData)
        if (uploadResult is OperationResult.Failure) {
            tracker.errorImage(imageModel.id, icmImagePath, uploadResult.message)
            return
        }

        logger.debug("Deployment of image '${imageModel.nameOrId()}' to '${icmImagePath}' is successful.")
        tracker.deployedImage(imageModel.id, icmImagePath)
    }

    private fun deployAttachment(attachmentRef: AttachmentRef, deploymentResult: DeploymentResult, tracker: ResultTracker) {
        if (!shouldDeployObject(attachmentRef.id, ResourceType.Attachment, attachmentRef.id, deploymentResult)) {
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
        val readResult = readStorageSafely(sourcePath)
        if (readResult is ReadResult.Error) {
            val message = "Error while reading attachment source data: ${readResult.errorMessage}."
            logger.error(message)
            tracker.errorAttachment(attachmentModel.id, icmFilePath, message)
            return
        }

        val attachmentData = (readResult as ReadResult.Success).result
        logger.trace("Loaded attachment data of size ${attachmentData.size} from storage.")

        val uploadResult = ipsService.tryUpload(icmFilePath, attachmentData)
        if (uploadResult is OperationResult.Failure) {
            tracker.errorAttachment(attachmentModel.id, icmFilePath, uploadResult.message)
            return
        }

        logger.debug("Deployment of attachment '${attachmentModel.nameOrId()}' to '${icmFilePath}' is successful.")
        tracker.deployedAttachment(attachmentModel.id, icmFilePath)
    }

    fun progressReport(deployId: Uuid? = null): ProgressReport {
        val objects = getAllDocumentObjectsToDeploy()
        return progressReportInternal(objects, deployId)
    }

    fun progressReport(documentObjectIds: List<String>, deployId: Uuid? = null): ProgressReport {
        val objects = getDocumentObjectsToDeploy(documentObjectIds)
        return progressReportInternal(objects, deployId)
    }

    fun progressReportInternal(objects: List<DocumentObject>, deployId: Uuid? = null): ProgressReport {
        val lastDeployment = deployId?.let { LastDeployment(it, Clock.System.now()) } ?: getLastDeployEvent()

        val report = ProgressReport(deployId ?: Uuid.random(), mutableMapOf())

        val queue: MutableList<Ref> = mutableListOf()
        val alreadyVisitedRefs = mutableSetOf<Pair<String, KClass<*>>>()

        for (obj in objects) {
            val nextIcmPath = documentObjectBuilder.getDocumentObjectPath(obj)
            val deployKind = obj.getDeployKind(nextIcmPath)
            val lastStatus = obj.getLastStatus(lastDeployment)

            report.addDocumentObject(
                id = obj.id,
                deploymentId = lastStatus.deployId,
                deployTimestamp = lastStatus.deployTimestamp,
                documentObject = obj,
                previousIcmPath = lastStatus.icmPath,
                nextIcmPath = nextIcmPath,
                lastStatus = lastStatus,
                deployKind = deployKind,
                errorMessage = lastStatus.errorMessage,
            )
            alreadyVisitedRefs.add(Pair(obj.id, DocumentObjectRef::class))
            val refs = obj.collectRefs()
            queue.addAll(refs)
        }

        while (!queue.isEmpty()) {
            val ref = queue.removeFirst()

            if (alreadyVisitedRefs.contains(Pair(ref.id, ref::class))) {
                continue
            }
            alreadyVisitedRefs.add(Pair(ref.id, ref::class))

            val resource = when (ref) {
                is DocumentObjectRef -> {
                    val obj = documentObjectRepository.findOrFail(ref.id)
                    val nextIcmPath =
                        if (obj.internal == true || (obj.type == DocumentObjectType.Page && output == InspireOutput.Designer)) {
                            null
                        } else {
                            documentObjectBuilder.getDocumentObjectPath(obj)
                        }
                    val deployKind = obj.getDeployKind(nextIcmPath)
                    val lastStatus = obj.getLastStatus(lastDeployment)


                    report.addDocumentObject(
                        id = obj.id,
                        deploymentId = lastStatus.deployId,
                        deployTimestamp = lastStatus.deployTimestamp,
                        documentObject = obj,
                        previousIcmPath = lastStatus.icmPath,
                        nextIcmPath = nextIcmPath,
                        deployKind = deployKind,
                        lastStatus = lastStatus,
                        errorMessage = lastStatus.errorMessage,
                    )
                    obj
                }

                is ImageRef -> {
                    val img = imageRepository.findOrFail(ref.id)
                    val nextIcmPath = documentObjectBuilder.getImagePath(img)
                    val deployKind = img.getDeployKind(nextIcmPath)
                    val lastStatus = img.getLastStatus(lastDeployment)

                    report.addImage(
                        id = img.id,
                        image = img,
                        deploymentId = lastStatus.deployId,
                        deployTimestamp = lastStatus.deployTimestamp,
                        previousIcmPath = lastStatus.icmPath,
                        nextIcmPath = nextIcmPath,
                        lastStatus = lastStatus,
                        deployKind = deployKind,
                        errorMessage = lastStatus.errorMessage,
                    )
                    img
                }

                is AttachmentRef -> {
                    val attachment = attachmentRepository.findOrFail(ref.id)
                    val nextIcmPath = documentObjectBuilder.getAttachmentPath(attachment)
                    val deployKind = attachment.getDeployKind(nextIcmPath)
                    val lastStatus = attachment.getLastStatus(lastDeployment)

                    report.addAttachment(
                        id = attachment.id,
                        attachment = attachment,
                        deploymentId = lastStatus.deployId,
                        deployTimestamp = lastStatus.deployTimestamp,
                        previousIcmPath = lastStatus.icmPath,
                        nextIcmPath = nextIcmPath,
                        lastStatus = lastStatus,
                        deployKind = deployKind,
                        errorMessage = lastStatus.errorMessage,
                    )
                    attachment
                }

                is TextStyleRef -> null
                is ParagraphStyleRef -> null
                is DisplayRuleRef -> null
                is VariableRef -> null
                is VariableStructureRef -> null
            }

            if (resource != null) {
                val refs = resource.collectRefs()
                queue.addAll(refs)
            }
        }

        return report
    }

    protected fun DocumentObject.getInvalidMetadataKeys(): Set<String> {
        return this.metadata.keys.asSequence().filter { key -> DISALLOWED_METADATA.contains(key) }.toSet()
    }

    protected fun Image.getInvalidMetadataKeys(): Set<String> {
        return this.metadata.keys.asSequence().filter { key -> IMAGE_DISALLOWED_METADATA.contains(key) }.toSet()
    }

    protected fun shouldDeployObject(
        id: String, resourceType: ResourceType, targetPath: String?, deploymentResult: DeploymentResult
    ): Boolean {
        val currentStatus = statusTrackingRepository.findLastEventRelevantToOutput(id, resourceType, output)

        return when (currentStatus) {
            null -> {
                logger.error("Not deploying '$targetPath' due to a missing status.")
                deploymentResult.errors.add(DeploymentError(id, "Missing tracked status for document object"))
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
                        dependencies.addAll(model.findDependencies())
                    }
                }
            }
        }
        return dependencies
    }

    private fun DocumentObject.getAllDocumentObjectImageAndAttachmentRefs(): Pair<List<ImageRef>, List<AttachmentRef>> {
        val images = mutableListOf<ImageRef>()
        val attachments = mutableListOf<AttachmentRef>()

        this.collectRefs().forEach { ref ->
            when (ref) {
                is DisplayRuleRef, is TextStyleRef, is ParagraphStyleRef, is VariableRef, is VariableStructureRef -> {}
                is ImageRef -> images.add(ref)
                is AttachmentRef -> attachments.add(ref)
                is DocumentObjectRef -> {
                    val model = documentObjectRepository.find(ref.id)
                        ?: error("Unable to collect image or attachment references because inner document object '${ref.id}' was not found.")

                    if (documentObjectBuilder.shouldIncludeInternalDependency(model)) {
                        val (nestedImages, nestedAttachments) = model.getAllDocumentObjectImageAndAttachmentRefs()
                        images.addAll(nestedImages)
                        attachments.addAll(nestedAttachments)
                    }
                }
            }
        }

        return Pair(images, attachments)
    }

    private fun getLastDeployEvent(): LastDeployment? {
        return statusTrackingRepository.listAll().mapNotNull {
            it.statusEvents.findLast { status ->
                status is Deployed || status is StatusError
            }
        }.maxByOrNull { it.timestamp }?.let {
            when (it) {
                is Deployed -> LastDeployment(it.deploymentId, it.timestamp)
                is StatusError -> LastDeployment(it.deploymentId, it.timestamp)
                is Active -> null
            }
        }
    }

    private fun getLastStatus(
        id: String,
        lastDeployment: LastDeployment?,
        resourceType: ResourceType,
        output: InspireOutput,
        internal: Boolean,
        isPage: Boolean
    ): LastStatus {
        if (internal || (isPage && output == InspireOutput.Designer)) return LastStatus.Inlined

        val objectEvents = statusTrackingRepository.findEventsRelevantToOutput(id, resourceType, output)
            .filter { ev -> lastDeployment?.timestamp?.let { ev.timestamp <= it } ?: true }
        val lastEvent = objectEvents.lastOrNull()
        val lastDeployEvent = objectEvents.lastOrNull { it is Deployed || it is StatusError }

        return when (lastDeployEvent) {
            null -> {
                if (lastEvent is Active) LastStatus.None
                else error("No events tracked for $resourceType with id $id and output $output")
            }
            is StatusError -> LastStatus.Error(
                lastDeployEvent.icmPath, lastDeployEvent.deploymentId, lastDeployEvent.timestamp, lastDeployEvent.error
            )

            else -> {
                val deployEvent = lastDeployEvent as Deployed
                if (deployEvent.deploymentId != lastDeployment?.id) {
                    LastStatus.Unchanged(deployEvent.icmPath, deployEvent.deploymentId, deployEvent.timestamp)
                } else {
                    val hasPreviousSuccessfulDeploy =
                        objectEvents.any { it.timestamp < deployEvent.timestamp && it is Deployed }
                    if (hasPreviousSuccessfulDeploy) {
                        LastStatus.Overwritten(deployEvent.icmPath, deployEvent.deploymentId, deployEvent.timestamp)
                    } else {
                        LastStatus.Created(deployEvent.icmPath, deployEvent.deploymentId, deployEvent.timestamp)
                    }
                }
            }
        }
    }

    private fun DocumentObject.getLastStatus(lastDeployment: LastDeployment?): LastStatus {
        return getLastStatus(
            id = this.id,
            lastDeployment = lastDeployment,
            resourceType = ResourceType.DocumentObject,
            output = output,
            internal = this.internal ?: false,
            isPage = this.type == DocumentObjectType.Page
        )
    }

    private fun Image.getLastStatus(lastDeployment: LastDeployment?): LastStatus {
        return getLastStatus(
            id = this.id,
            lastDeployment = lastDeployment,
            resourceType = ResourceType.Image,
            output = output,
            internal = false,
            isPage = false
        )
    }

    private fun Attachment.getLastStatus(lastDeployment: LastDeployment?): LastStatus {
        return getLastStatus(
            id = this.id,
            lastDeployment = lastDeployment,
            resourceType = ResourceType.Attachment,
            output = output,
            internal = false,
            isPage = false
        )
    }

    private fun DocumentObject.getDeployKind(nextIcmPath: String?): DeployKind {
        return getDeployKind(
            this.id,
            ResourceType.DocumentObject,
            output,
            this.internal ?: false,
            nextIcmPath,
            this.type == DocumentObjectType.Page
        )
    }

    private fun Image.getDeployKind(nextIcmPath: String?): DeployKind {
        return getDeployKind(this.id, ResourceType.Image, output, false, nextIcmPath)
    }

    private fun Attachment.getDeployKind(nextIcmPath: String?): DeployKind {
        return getDeployKind(this.id, ResourceType.Attachment, output, false, nextIcmPath)
    }

    private fun getDeployKind(
        id: String,
        resourceType: ResourceType,
        output: InspireOutput,
        internal: Boolean = false,
        nextIcmPath: String?,
        isPage: Boolean = false
    ): DeployKind {
        if (internal) {
            return DeployKind.Inline
        }

        val objectEvents = statusTrackingRepository.findEventsRelevantToOutput(id, resourceType, output)
        val lastEvent = objectEvents.lastOrNull()
        return if (lastEvent is Active || lastEvent is StatusError) {
            val lastDeployEvent = objectEvents.filterIsInstance<Deployed>().lastOrNull()
            if (lastDeployEvent != null && lastDeployEvent.icmPath != nextIcmPath) {
                DeployKind.Create
            } else if (lastDeployEvent != null) {
                DeployKind.Overwrite
            } else if (isPage && output == InspireOutput.Designer) {
                DeployKind.Inline
            } else {
                DeployKind.Create
            }
        } else {
            DeployKind.Keep
        }
    }

    private sealed interface ReadResult {
        data class Success(val result: ByteArray) : ReadResult
        data class Error(val errorMessage: String) : ReadResult
    }

    private fun readStorageSafely(path: String): ReadResult {
        try {
            val byteArray = storage.read(path)
            return ReadResult.Success(byteArray)
        } catch (_: InvalidPathException) {
            return ReadResult.Error("File path '$path' is invalid.")
        } catch (_: SecurityException) {
            return ReadResult.Error("Access to file '$path' is denied.")
        } catch (_: FileNotFoundException) {
            return ReadResult.Error("File '$path' not found.")
        } catch (_: IOException) {
            return ReadResult.Error("I/O error occurred.")
        } catch (_: Exception) {
            return ReadResult.Error("Unexpected error.")
        }
    }

    data class LastDeployment(val id: Uuid, val timestamp: Instant)
}