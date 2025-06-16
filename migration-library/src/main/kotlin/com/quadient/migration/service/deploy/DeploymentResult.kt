package com.quadient.migration.service.deploy

data class DeploymentResult(
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
