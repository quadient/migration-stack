package com.quadient.migration.service.deploy.utility

import com.quadient.migration.api.dto.migrationmodel.Attachment
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.repository.AttachmentRepository
import com.quadient.migration.api.repository.DisplayRuleRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ImageRepository
import com.quadient.migration.api.repository.ParagraphStyleRepository
import com.quadient.migration.api.repository.TextStyleRepository
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.shared.Categorization
import com.quadient.migration.shared.IcmFileMetadata
import com.quadient.migration.shared.IcmMetadata
import com.quadient.migration.shared.MetadataPrimitive
import com.quadient.migration.shared.MetadataValue
import com.quadient.migration.tools.partitionByType
import com.quadient.migration.tools.logger

typealias PostProcessor = (DeploymentResult) -> Unit
interface PostProcess {
    val postProcessors: List<PostProcessor>

    fun addPostProcessor(processor: PostProcessor)
    fun runPostProcessors(result: DeploymentResult)
    fun clearPostProcessors(): List<PostProcessor>
}

class PostProcessImpl(
    private val ipsService: IpsService,
    private val documentObjectRepository: DocumentObjectRepository,
    private val imageRepository: ImageRepository,
    private val attachmentRepository: AttachmentRepository,
    private val displayRuleRepository: DisplayRuleRepository,
    private val textStyleRepository: TextStyleRepository,
    private val paragraphStyleRepository: ParagraphStyleRepository,
) : PostProcess {
    private val logger by logger()
    private var _postProcessors: MutableList<PostProcessor> = mutableListOf()

    override val postProcessors: List<PostProcessor>
        get() = _postProcessors.toList()

    init {
        addPostProcessor(::metadataPostProcessor)
    }

    override fun addPostProcessor(processor: PostProcessor) {
        _postProcessors.add(processor)
    }

    override fun runPostProcessors(result: DeploymentResult) {
        for (processor in _postProcessors) {
            processor(result)
        }
    }

    override fun clearPostProcessors(): List<PostProcessor> {
        val old = _postProcessors
        _postProcessors = mutableListOf()
        return old
    }

    fun <T> prepareData(
        name: String,
        deploymentResult: DeploymentResult,
        vararg types: ResourceType,
        transform: (DeploymentInfo, MigrationObject) -> T?
    ): Sequence<T> {
        return deploymentResult.deployed
            .asSequence()
            .filter { info -> info.type in types }
            .mapNotNull { info ->
                val obj = when (info.type) {
                    ResourceType.DocumentObject -> documentObjectRepository.find(info.id)
                    ResourceType.Image -> imageRepository.find(info.id)
                    ResourceType.DisplayRule -> displayRuleRepository.find(info.id)
                    ResourceType.Attachment -> attachmentRepository.find(info.id)
                    ResourceType.TextStyle -> textStyleRepository.find(info.id)
                    ResourceType.ParagraphStyle -> paragraphStyleRepository.find(info.id)
                }
                if (obj == null) {
                    val msg = "Failed to run '$name' post processor, '${info.type}' with id '${info.id}' at path '${info.targetPath}'."
                    deploymentResult.warnings.add(DeploymentWarning(info.id, msg))
                    return@mapNotNull null
                }

                info to obj
            }.mapNotNull { (info, obj) -> transform(info, obj) }
    }

    fun metadataPostProcessor(deploymentResult: DeploymentResult) {
        val allowedTypes = listOf(
            ResourceType.DocumentObject,
            ResourceType.Image,
            ResourceType.Attachment,
            ResourceType.DisplayRule
        ).toTypedArray()
        val meta = prepareData("metadata", deploymentResult, *allowedTypes) { info, obj ->
            val metadata = when (obj) {
                is DocumentObject -> obj.metadata
                is Image -> obj.metadata
                is DisplayRule -> obj.metadata
                is Attachment -> obj.metadata
                else -> emptyList()
            }

            val (icmMetadata, categorizations) = metadata.partitionByType<IcmMetadata, Categorization>()
            val assignedCategorizations = categorizations.map { it.name }

            val cats = categorizations.flatMap { cat ->
                cat.fields.map { field ->
                    "${cat.name}_${field.key}" to MetadataValue(field.value.map { it.toMetadataPrimitive() })
                }
            }.toMap().toMutableMap()

            if (assignedCategorizations.isNotEmpty()) {
                cats["Assigned Categorizations"] = MetadataValue(assignedCategorizations.map(MetadataPrimitive::Str))
            }

            val result = icmMetadata.associate { it.key to MetadataValue(it.value) } + cats
            if (result.isEmpty()) {
                null
            } else {
                IcmFileMetadata(path = info.targetPath.toString(), metadata = result)
            }
        }.toList()

        logger.debug("Writing metadata for ${meta.size} deployed document objects.")
        ipsService.writeMetadata(meta)
        logger.debug("Finished writing metadata for deployed document objects.")
    }
}