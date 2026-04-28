@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.service.deploy.utility

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.shared.DocumentObjectType
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class DeploymentResult(
    val deploymentId: Uuid,
    val deployed: MutableList<DeploymentInfo> = mutableListOf(),
    val errors: MutableList<DeploymentError> = mutableListOf(),
    val warnings: MutableList<DeploymentWarning> = mutableListOf(),
) {
    fun mergeWith(result: DeploymentResult) {
        this.deployed.addAll(result.deployed)
        this.errors.addAll(result.errors)
        this.warnings.addAll(result.warnings)
    }

    operator fun plusAssign(result: DeploymentResult) = mergeWith(result)
}

data class DeploymentInfo(
    val id: String,
    val type: ResourceType,
    val targetPath: String,
)

enum class ResourceType {
    DocumentObject, Image, Attachment, TextStyle, ParagraphStyle, DisplayRule
}

data class DeploymentError(val id: String, val message: String)
data class DeploymentWarning(val id: String, val message: String)

interface ResultTracker {
    val deploymentResult: DeploymentResult
    val deploymentId: Uuid
    val timestamp: Instant

    fun deployedDocumentObject(id: String, icmPath: String, type: DocumentObjectType)
    fun deployedImage(id: String, icmPath: String)
    fun errorDocumentObject(id: String, icmPath: String, type: DocumentObjectType, message: String)
    fun errorImage(id: String, icmPath: String?, message: String)
    fun warningImage(id: String, icmPath: String?, message: String)
    fun deployedAttachment(id: String, icmPath: String)
    fun errorAttachment(id: String, icmPath: String?, message: String)
    fun warningAttachment(id: String, icmPath: String?, message: String)
    fun deployedDisplayRule(id: String, targetPath: String)
    fun warningDisplayRule(id: String, path: String, message: String)
    fun errorDisplayRule(id: String, path: String, message: String)
}

class ResultTrackerImpl(
    private val statusTrackingRepository: StatusTrackingRepository?,
    private val inspireOutput: InspireOutput,
    override val deploymentResult: DeploymentResult,
    override val deploymentId: Uuid = Uuid.random(),
    override val timestamp: Instant = Clock.System.now(),
) : ResultTracker{
    constructor(
        statusTrackingRepository: StatusTrackingRepository?,
        inspireOutput: InspireOutput,
        deploymentId: Uuid = Uuid.random(),
        timestamp: Instant = Clock.System.now(),
    ) : this(statusTrackingRepository, inspireOutput, DeploymentResult(deploymentId), deploymentId, timestamp) {
    }

    override fun deployedDocumentObject(id: String, icmPath: String, type: DocumentObjectType) {
        statusTrackingRepository?.deployed(
            id = id,
            deploymentId = deploymentId,
            timestamp = timestamp,
            resourceType = ResourceType.DocumentObject,
            output = inspireOutput,
            icmPath = icmPath,
            data = mapOf("type" to type.toString())
        )
        deploymentResult.deployed.add(DeploymentInfo(id, ResourceType.DocumentObject, icmPath))
    }

    override fun deployedImage(id: String, icmPath: String) {
        statusTrackingRepository?.deployed(
            id = id,
            deploymentId = deploymentId,
            timestamp = timestamp,
            resourceType = ResourceType.Image,
            output = inspireOutput,
            icmPath = icmPath,
        )
        deploymentResult.deployed.add(DeploymentInfo(id, ResourceType.Image, icmPath))
    }

    override fun errorDocumentObject(id: String, icmPath: String, type: DocumentObjectType, message: String) {
        statusTrackingRepository?.error(
            id = id,
            deploymentId = deploymentId,
            timestamp = timestamp,
            resourceType = ResourceType.DocumentObject,
            output = inspireOutput,
            icmPath = icmPath,
            message = message,
            data = mapOf("type" to type.toString())
        )
        deploymentResult.errors.add(DeploymentError(id, message))
    }

    override fun errorImage(id: String, icmPath: String?, message: String) {
        statusTrackingRepository?.error(
            id = id,
            deploymentId = deploymentId,
            timestamp = timestamp,
            resourceType = ResourceType.Image,
            output = inspireOutput,
            icmPath = icmPath,
            message = message,
        )
        deploymentResult.errors.add(DeploymentError(id, message))
    }

    override fun warningImage(id: String, icmPath: String?, message: String) {
        statusTrackingRepository?.error(
            id = id,
            deploymentId = deploymentId,
            timestamp = timestamp,
            resourceType = ResourceType.Image,
            output = inspireOutput,
            icmPath = icmPath,
            message = message,
        )
        deploymentResult.warnings.add(DeploymentWarning(id, message))
    }

    override fun deployedAttachment(id: String, icmPath: String) {
        statusTrackingRepository?.deployed(
            id = id,
            deploymentId = deploymentId,
            timestamp = timestamp,
            resourceType = ResourceType.Attachment,
            output = inspireOutput,
            icmPath = icmPath,
        )
        deploymentResult.deployed.add(DeploymentInfo(id, ResourceType.Attachment, icmPath))
    }

    override fun errorAttachment(id: String, icmPath: String?, message: String) {
        statusTrackingRepository?.error(
            id = id,
            deploymentId = deploymentId,
            timestamp = timestamp,
            resourceType = ResourceType.Attachment,
            output = inspireOutput,
            icmPath = icmPath,
            message = message,
        )
        deploymentResult.errors.add(DeploymentError(id, message))
    }

    override fun warningAttachment(id: String, icmPath: String?, message: String) {
        statusTrackingRepository?.error(
            id = id,
            deploymentId = deploymentId,
            timestamp = timestamp,
            resourceType = ResourceType.Attachment,
            output = inspireOutput,
            icmPath = icmPath,
            message = message,
        )
        deploymentResult.warnings.add(DeploymentWarning(id, message))
    }

    override fun deployedDisplayRule(id: String, targetPath: String) {
        statusTrackingRepository?.deployed(
            id = id,
            deploymentId = deploymentId,
            timestamp = timestamp,
            resourceType = ResourceType.DisplayRule,
            output = inspireOutput,
            icmPath = targetPath,
        )
        deploymentResult.deployed.add(DeploymentInfo(id, ResourceType.DisplayRule, targetPath))
    }

    override fun warningDisplayRule(id: String, path: String, message: String) {
        statusTrackingRepository?.error(
            id = id,
            deploymentId = deploymentId,
            timestamp = timestamp,
            resourceType = ResourceType.DisplayRule,
            output = inspireOutput,
            icmPath = path,
            message = message,
        )
        deploymentResult.warnings.add(DeploymentWarning(id, message))
    }

    override fun errorDisplayRule(id: String, path: String, message: String) {
        statusTrackingRepository?.error(
            id = id,
            deploymentId = deploymentId,
            timestamp = timestamp,
            resourceType = ResourceType.DisplayRule,
            output = inspireOutput,
            icmPath = path,
            message = message,
        )
        deploymentResult.errors.add(DeploymentError(id, message))
    }
}