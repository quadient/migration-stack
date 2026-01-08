@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.service.deploy

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.data.Active
import com.quadient.migration.data.Deployed
import com.quadient.migration.data.DisplayRuleModelRef
import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.data.DocumentObjectModelRef
import com.quadient.migration.data.ImageModel
import com.quadient.migration.data.ImageModelRef
import com.quadient.migration.data.ParagraphStyleDefinitionModel
import com.quadient.migration.data.ParagraphStyleModelRef
import com.quadient.migration.data.RefModel
import com.quadient.migration.data.TextStyleDefinitionModel
import com.quadient.migration.data.TextStyleModelRef
import com.quadient.migration.data.VariableModelRef
import com.quadient.migration.data.VariableStructureModelRef
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.repository.ImageInternalRepository
import com.quadient.migration.persistence.repository.ParagraphStyleInternalRepository
import com.quadient.migration.persistence.repository.TextStyleInternalRepository
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

data class DocObjectWithRef(val obj: DocumentObjectModel, val documentObjectRefs: Set<String>)

sealed class DeployClient(
    protected val documentObjectRepository: DocumentObjectInternalRepository,
    protected val imageRepository: ImageInternalRepository,
    protected val statusTrackingRepository: StatusTrackingRepository,
    protected val textStyleRepository: TextStyleInternalRepository,
    protected val paragraphStyleRepository: ParagraphStyleInternalRepository,
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
                            ResourceType.DocumentObject -> documentObjectRepository.findModel(info.id)
                            ResourceType.Image -> imageRepository.findModel(info.id)
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
                                is DocumentObjectModel -> obj.metadata
                                is ImageModel -> obj.metadata
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

    abstract fun getAllDocumentObjectsToDeploy(): List<DocumentObjectModel>
    abstract fun getDocumentObjectsToDeploy(documentObjectIds: List<String>): List<DocumentObjectModel>
    abstract fun deployDocumentObjectsInternal(documentObjects: List<DocumentObjectModel>): DeploymentResult

    protected fun addPostProcessor(processor: (DeploymentResult) -> Unit) {
        postProcessors.add(processor)
    }

    abstract fun shouldIncludeDependency(documentObject: DocumentObjectModel): Boolean

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
            val dependencies = documentObjects.flatMap { it.findDependencies() }.filter { !it.internal }
            deployDocumentObjectsInternal((documentObjects + dependencies).toSet().toList())
        }

        for (processor in postProcessors) {
            processor(result)
        }

        return result
    }

    fun deployStyles() {
        val deploymentId = Uuid.random()
        val deploymentTimestamp = Clock.System.now()

        val textStyles = textStyleRepository.listAllModel().filter { it.definition is TextStyleDefinitionModel }
        val paragraphStyles =
            paragraphStyleRepository.listAllModel().filter { it.definition is ParagraphStyleDefinitionModel }
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

        val approvalResult = ipsService.setProductionApprovalState(listOf(outputPath))
        when (approvalResult) {
            is OperationResult.Failure -> logger.error("Failed to set production approval state to $outputPath.")
            OperationResult.Success -> logger.debug("Setting of production approval state to $outputPath is successful.")
        }
    }

    fun deployOrder(documentObjects: List<DocumentObjectModel>): List<DocumentObjectModel> {
        val documentObjectIds = documentObjects.map { it.id }

        val deployOrder = mutableListOf<DocumentObjectModel>()

        var toCheck = documentObjects.map {
            DocObjectWithRef(
                it, it.collectRefs().filterIsInstance<DocumentObjectModelRef>().map { it -> it.id }.toSet()
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

    protected fun deployImages(documentObjects: List<DocumentObjectModel>, deploymentId: Uuid, deploymentTimestamp: Instant): DeploymentResult {
        val deploymentResult = DeploymentResult(deploymentId)
        val tracker = ResultTracker(statusTrackingRepository, deploymentResult, deploymentId, deploymentTimestamp, output)

        val uniqueImageRefs = documentObjects.flatMap {
            try {
                it.getAllDocumentObjectImageRefs()
            } catch (e: IllegalStateException) {
                deploymentResult.errors.add(DeploymentError(it.id, e.message ?: ""))
                emptyList()
            }
        }.distinct()

        for (imageRef in uniqueImageRefs) {
            if (!shouldDeployObject(imageRef.id, ResourceType.Image, imageRef.id, deploymentResult)) {
                logger.info("Skipping deployment of '${imageRef.id}' as it is not marked for deployment.")
                continue
            }

            val imageModel = imageRepository.findModel(imageRef.id)
            if (imageModel == null) {
                val message = "Image '${imageRef.id}' not found."
                logger.error(message)
                tracker.errorImage(imageRef.id, null, message)
                continue
            }

            val icmImagePath = documentObjectBuilder.getImagePath(imageModel)

            val invalidMetadata = imageModel.getInvalidMetadataKeys()
            if (invalidMetadata.isNotEmpty()) {
                logger.error("Failed to deploy '$icmImagePath' due to invalid metadata.")
                val keys = invalidMetadata.joinToString(", ", prefix = "[", postfix = "]")
                val message = "Metadata of image '${imageModel.id}' contains invalid keys: $keys"
                tracker.errorImage(imageModel.id, icmImagePath, message)
                continue
            }


            if (imageModel.imageType == ImageType.Unknown) {
                val message = "Skipping deployment of image '${imageModel.nameOrId()}' due to unknown image type."
                logger.warn(message)
                tracker.warningImage(imageModel.id, icmImagePath, message)
                continue
            }

            if (imageModel.skip.skipped) {
                val reason = imageModel.skip.reason?.let { " Reason: $it" } ?: ""
                val message = "Image '${imageModel.nameOrId()}' is skipped.$reason"
                logger.warn(message)
                tracker.warningImage(imageModel.id, icmImagePath, message)
                continue
            }


            if (imageModel.sourcePath.isNullOrBlank()) {
                val message = "Skipping deployment of image '${imageModel.nameOrId()}' due to missing source path."
                logger.warn(message)
                tracker.warningImage(imageModel.id, icmImagePath, message)
                continue
            }

            logger.debug("Starting deployment of image '${imageModel.nameOrId()}'.")
            val readResult = readStorageSafely(imageModel.sourcePath)
            if (readResult is ReadResult.Error) {
                val message = "Error while reading image source data: ${readResult.errorMessage}."
                logger.error(message)
                tracker.errorImage(imageModel.id, icmImagePath, message)
                continue
            }

            val imageData = (readResult as ReadResult.Success).result

            logger.trace("Loaded image data of size ${imageData.size} from storage.")

            val uploadResult = ipsService.tryUpload(icmImagePath, imageData)
            if (uploadResult is OperationResult.Failure) {
                tracker.errorImage(imageModel.id, icmImagePath, uploadResult.message)
                continue
            }

            logger.debug("Deployment of image '${imageModel.nameOrId()}' to '${icmImagePath}' is successful.")
            tracker.deployedImage(imageModel.id, icmImagePath)
        }

        return deploymentResult
    }

    fun progressReport(deployId: Uuid? = null): ProgressReport {
        val objects = getAllDocumentObjectsToDeploy()
        return progressReportInternal(objects, deployId)
    }

    fun progressReport(documentObjectIds: List<String>, deployId: Uuid? = null): ProgressReport {
        val objects = getDocumentObjectsToDeploy(documentObjectIds)
        return progressReportInternal(objects, deployId)
    }

    fun progressReportInternal(objects: List<DocumentObjectModel>, deployId: Uuid? = null): ProgressReport {
        val lastDeployment = deployId?.let { LastDeployment(it, Clock.System.now()) } ?: getLastDeployEvent()

        val report = ProgressReport(deployId ?: Uuid.random(), mutableMapOf())

        val queue: MutableList<RefModel> = mutableListOf()
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
            alreadyVisitedRefs.add(Pair(obj.id, DocumentObjectModelRef::class))
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
                is DocumentObjectModelRef -> {
                    val obj = documentObjectRepository.findModelOrFail(ref.id)
                    val nextIcmPath =
                        if (obj.internal || (obj.type == DocumentObjectType.Page && output == InspireOutput.Designer)) {
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

                is ImageModelRef -> {
                    val img = imageRepository.findModelOrFail(ref.id)
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

                is TextStyleModelRef -> null
                is ParagraphStyleModelRef -> null
                is DisplayRuleModelRef -> null
                is VariableModelRef -> null
                is VariableStructureModelRef -> null
            }

            if (resource != null) {
                val refs = resource.collectRefs()
                queue.addAll(refs)
            }
        }

        return report
    }

    protected fun DocumentObjectModel.getInvalidMetadataKeys(): Set<String> {
        return this.metadata.keys.asSequence().filter { key -> DISALLOWED_METADATA.contains(key) }.toSet()
    }

    protected fun ImageModel.getInvalidMetadataKeys(): Set<String> {
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

    fun DocumentObjectModel.findDependencies(): List<DocumentObjectModel> {
        val dependencies = mutableListOf<DocumentObjectModel>()
        this.collectRefs().forEach { ref ->
            when (ref) {
                is DisplayRuleModelRef, is TextStyleModelRef, is ParagraphStyleModelRef, is VariableModelRef, is VariableStructureModelRef -> {}
                is ImageModelRef -> {}
                is DocumentObjectModelRef -> {
                    val model = documentObjectRepository.findModelOrFail(ref.id)
                    if (shouldIncludeDependency(model)) {
                        dependencies.add(model)
                        dependencies.addAll(model.findDependencies())
                    }
                }
            }
        }
        return dependencies
    }

    private fun DocumentObjectModel.getAllDocumentObjectImageRefs(): List<ImageModelRef> {
        return this.collectRefs().flatMap { ref ->
            when (ref) {
                is DisplayRuleModelRef, is TextStyleModelRef, is ParagraphStyleModelRef, is VariableModelRef, is VariableStructureModelRef -> emptyList()
                is ImageModelRef -> listOf(ref)
                is DocumentObjectModelRef -> {
                    val model = documentObjectRepository.findModel(ref.id)
                        ?: error("Unable to collect image references because inner document object '${ref.id}' was not found.")

                    if (documentObjectBuilder.shouldIncludeInternalDependency(model)) {
                        model.getAllDocumentObjectImageRefs()
                    } else {
                        emptyList()
                    }
                }
            }
        }
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

    private fun DocumentObjectModel.getLastStatus(lastDeployment: LastDeployment?): LastStatus {
        return getLastStatus(
            id = this.id,
            lastDeployment = lastDeployment,
            resourceType = ResourceType.DocumentObject,
            output = output,
            internal = this.internal,
            isPage = this.type == DocumentObjectType.Page
        )
    }

    private fun ImageModel.getLastStatus(lastDeployment: LastDeployment?): LastStatus {
        return getLastStatus(
            id = this.id,
            lastDeployment = lastDeployment,
            resourceType = ResourceType.Image,
            output = output,
            internal = false,
            isPage = false
        )
    }

    private fun DocumentObjectModel.getDeployKind(nextIcmPath: String?): DeployKind {
        return getDeployKind(
            this.id,
            ResourceType.DocumentObject,
            output,
            this.internal,
            nextIcmPath,
            this.type == DocumentObjectType.Page
        )
    }

    private fun ImageModel.getDeployKind(nextIcmPath: String?): DeployKind {
        return getDeployKind(this.id, ResourceType.Image, output, false, nextIcmPath)
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