@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.service.deploy

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.shared.DocumentObjectType
import kotlinx.datetime.Instant
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
    DocumentObject, Image, TextStyle, ParagraphStyle
}

data class DeploymentError(val id: String, val message: String)
data class DeploymentWarning(val id: String, val message: String)

class ResultTracker(
    private val statusTrackingRepository: StatusTrackingRepository,
    private val deploymentResult: DeploymentResult,
    private val deploymentId: Uuid,
    private val timestamp: Instant,
    private val inspireOutput: InspireOutput,
) {
    fun deployedDocumentObject(id: String, icmPath: String, type: DocumentObjectType) {
        statusTrackingRepository.deployed(
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

    fun deployedImage(id: String, icmPath: String) {
        statusTrackingRepository.deployed(
            id = id,
            deploymentId = deploymentId,
            timestamp = timestamp,
            resourceType = ResourceType.Image,
            output = inspireOutput,
            icmPath = icmPath,
        )
        deploymentResult.deployed.add(DeploymentInfo(id, ResourceType.Image, icmPath))
    }

    fun errorDocumentObject(id: String, icmPath: String, type: DocumentObjectType, message: String) {
        statusTrackingRepository.error(
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

    fun errorImage(id: String, icmPath: String?, message: String) {
        statusTrackingRepository.error(
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

    fun warningImage(id: String, icmPath: String?, message: String) {
        statusTrackingRepository.error(
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
}