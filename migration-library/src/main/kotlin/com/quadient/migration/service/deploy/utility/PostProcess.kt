package com.quadient.migration.service.deploy.utility

import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.repository.DisplayRuleRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.Repository
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.shared.IcmFileMetadata
import com.quadient.migration.shared.toMetadata
import org.slf4j.LoggerFactory
import kotlin.collections.ifEmpty

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
    private val imageRepository: Repository<Image>,
    private val displayRuleRepository: DisplayRuleRepository,
) : PostProcess {
    private val logger = LoggerFactory.getLogger(this::class.java)!!
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

    fun metadataPostProcessor(deploymentResult: DeploymentResult) {
        val meta =
            deploymentResult.deployed
                .filter { it.type == ResourceType.DocumentObject || it.type == ResourceType.Image || it.type == ResourceType.DisplayRule }
                .mapNotNull { info ->
                    val obj = when (info.type) {
                        ResourceType.DocumentObject -> documentObjectRepository.find(info.id)
                        ResourceType.Image -> imageRepository.find(info.id)
                        ResourceType.DisplayRule -> displayRuleRepository.find(info.id)
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
                            is DisplayRule -> obj.metadata
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