package com.quadient.migration.service.deploy.utility

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.dto.migrationmodel.Attachment
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.ResourceId
import com.quadient.migration.api.dto.migrationmodel.StatusTracking
import com.quadient.migration.api.repository.AttachmentRepository
import com.quadient.migration.api.repository.DisplayRuleRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ImageRepository
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.data.Deployed
import com.quadient.migration.service.ResourcePathProvider
import com.quadient.migration.service.ipsclient.OperationResult
import com.quadient.migration.service.resolveTarget
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.tools.computeIfPresentOrPut
import com.quadient.migration.tools.logger
import kotlin.collections.iterator

class ConflictDetectorImpl(
    private val documentObjectRepository: DocumentObjectRepository,
    private val imageRepository: ImageRepository,
    private val attachmentRepository: AttachmentRepository,
    private val displayRuleRepository: DisplayRuleRepository,
    private val statusTrackingRepository: StatusTrackingRepository,
    private val resourcePathProvider: ResourcePathProvider,
    private val output: InspireOutput,
) : ConflictDetector {
    private val logger by logger()

    override fun runConflictValidation(documentObjects: List<DocumentObject>, deployFn: DeployFn): ValidationResult {
        val pathsToResourcesMap: PathToResources = mutableMapOf()
        val trackingFn: (ResourceType) -> ((MigrationObject, IcmPath, Any) -> OperationResult) = { type ->
            { obj, targetPath, _ ->
                val key = ResourceId(obj.id, type)
                pathsToResourcesMap.computeIfPresentOrPut(targetPath, setOf(key)) { it + key }
                OperationResult.Success
            }
        }

        val deploymentResult = deployFn(
            documentObjects,
            ResultTrackerImpl(null, output),
            trackingFn(ResourceType.DocumentObject),
            trackingFn(ResourceType.Image),
            trackingFn(ResourceType.Attachment),
            trackingFn(ResourceType.DisplayRule),
        )

        val previouslyDeployedPaths: PathToResources = mutableMapOf()
        val allStatusTracking = statusTrackingRepository.listAll()

        for (item in allStatusTracking) {
            val lastEvent = item.statusEvents
                .filterIsInstance<Deployed>()
                .lastOrNull { it.output == output } ?: continue
            val path = lastEvent.icmPath ?: resolveTrackedPath(item)
            if (path == null) {
                logger.warn("Could not resolve path for previously deployed resource '${item.id}' of type '${item.resourceType}', skipping conflict check for this resource.")
                continue
            }

            val id = item.resourceId()
            previouslyDeployedPaths.computeIfPresentOrPut(path, setOf(id)) { it + id }
        }

        val conflictingInBatchResources: PathToResources  = mutableMapOf()
        val conflictingWithPreviousResources: MutableMap<IcmPath, PreviousConflict> = mutableMapOf()
        for ((path, resources) in pathsToResourcesMap) {
            if (resources.size > 1) {
                conflictingInBatchResources[path] = resources
            }

            val previouslyDeployed = previouslyDeployedPaths[path]
            if (previouslyDeployed != null && resources.any { it !in previouslyDeployed }) {
                conflictingWithPreviousResources[path] = PreviousConflict(
                    current = resources,
                    previous = previouslyDeployed,
                )
            }
        }

        return ValidationResult(conflictingInBatchResources, conflictingWithPreviousResources, deploymentResult)
    }

    private fun resolveTrackedPath(item: StatusTracking): IcmPath? = runCatching {
        when (item.resourceType) {
            ResourceType.DocumentObject -> resourcePathProvider.getDocumentObjectPath(
                documentObjectRepository.findOrFail(item.id)
            )

            ResourceType.Image -> resourcePathProvider.getImagePath(imageRepository.findOrFail(item.id))
            ResourceType.Attachment -> resourcePathProvider.getAttachmentPath(
                attachmentRepository.findOrFail(item.id)
            )

            ResourceType.DisplayRule -> resourcePathProvider.getDisplayRulePath(
                displayRuleRepository.findOrFail(item.id).resolveTarget(displayRuleRepository::findOrFail)
            )

            ResourceType.TextStyle -> resourcePathProvider.getStyleDefinitionPath()
            ResourceType.ParagraphStyle -> resourcePathProvider.getStyleDefinitionPath()
        }
    }.getOrNull()
}

typealias DeployFn = (
    documentObjects: List<DocumentObject>,
    tracker: ResultTracker,
    deployDocumentObject: (DocumentObject, IcmPath, String) -> OperationResult,
    deployImage: (Image, IcmPath, ByteArray) -> OperationResult,
    deployAttachment: (Attachment, IcmPath, ByteArray) -> OperationResult,
    deployDisplayRule: (DisplayRule, IcmPath, ByteArray) -> OperationResult
) -> DeploymentResult

typealias PathToResources = MutableMap<IcmPath, Set<ResourceId>>

data class PreviousConflict(val current: Set<ResourceId>, val previous: Set<ResourceId>)

data class ValidationResult(
    val conflictingInBatchResources: Map<IcmPath, Set<ResourceId>>,
    val conflictingWithPreviousResources: Map<IcmPath, PreviousConflict>,
    val deploymentResult: DeploymentResult,
) {
    fun hasNoConflicts() = conflictingInBatchResources.isEmpty() && conflictingWithPreviousResources.isEmpty()
}

interface ConflictDetector {
    fun runConflictValidation(documentObjects: List<DocumentObject>, deployFn: DeployFn): ValidationResult
}
