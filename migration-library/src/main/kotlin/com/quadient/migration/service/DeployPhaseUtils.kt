package com.quadient.migration.service

import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.dto.migrationmodel.Attachment
import com.quadient.migration.api.dto.migrationmodel.AttachmentRef
import com.quadient.migration.api.dto.migrationmodel.BaseTemplate
import com.quadient.migration.api.dto.migrationmodel.BaseTemplateRef
import com.quadient.migration.api.dto.migrationmodel.BaseTemplateLocation
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.LiteralBaseTemplatePath
import com.quadient.migration.api.dto.migrationmodel.ResourceRef
import com.quadient.migration.api.repository.Repository
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.isNullOrBlank
import com.quadient.migration.shared.toIcmPath
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

fun List<DocumentContent>.resolveAliases(
    imageRepository: Repository<Image>,
    attachmentRepository: Repository<Attachment>
): List<DocumentContent> = map { item ->
    when (item) {
        is ResourceRef -> resolveAlias(item, imageRepository, attachmentRepository)
        else -> item
    }
}

fun resolveAlias(
    ref: ResourceRef,
    imageRepository: Repository<Image>,
    attachmentRepository: Repository<Attachment>
): ResourceRef {
    return when (ref) {
        is ImageRef -> {
            val image = imageRepository.find(ref.id) ?: return ref
            image.targetAttachmentId?.let { targetId ->
                logger.info("Resolving image '${ref.id}' to attachment '$targetId' via alias")
                resolveAlias(AttachmentRef(targetId), imageRepository, attachmentRepository)
            } ?: ref
        }
        is AttachmentRef -> {
            val attachment = attachmentRepository.find(ref.id) ?: return ref
            attachment.targetImageId?.let { targetId ->
                logger.info("Resolving attachment '${ref.id}' to image '$targetId' via alias")
                resolveAlias(ImageRef(targetId), imageRepository, attachmentRepository)
            } ?: ref
        }
    }
}

fun resolveTargetDir(defaultTargetFolder: IcmPath? = null, specificTargetFolder: IcmPath? = null): IcmPath? {
    return when {
        !specificTargetFolder.isNullOrBlank() -> specificTargetFolder
        !defaultTargetFolder.isNullOrBlank() -> defaultTargetFolder
        else -> null
    }
}

fun getBaseTemplateFullPath(
    config: ProjectConfig,
    documentObjectBaseTemplate: BaseTemplateLocation?,
    resourcePathProvider: ResourcePathProvider,
    findBaseTemplate: (String) -> BaseTemplate,
): IcmPath {
    val literalPath = when (documentObjectBaseTemplate) {
        is LiteralBaseTemplatePath -> documentObjectBaseTemplate.path

        is BaseTemplateRef -> {
            val baseTemplate = findBaseTemplate(documentObjectBaseTemplate.id)
            val baseTemplatePath = resourcePathProvider.getBaseTemplatePath(baseTemplate)
            logger.info(
                "Base template '$baseTemplatePath' will not be used because referencing base templates by id is not yet supported during deployment. The project config default base template will be used instead."
            )
            config.baseTemplatePath
        }

        null -> config.baseTemplatePath
    }

    val path = literalPath.toIcmPath()
    if (path.isAbsolute()) return path

    return resourcePathProvider.getBaseTemplatePath(literalPath)
}

fun DisplayRule.resolveTarget(findRule: (String) -> DisplayRule): DisplayRule {
    val targetId = this.targetId ?: return this

    val targetRule = findRule(targetId.id)
    return targetRule.resolveTarget(findRule)
}

fun imageExtension(image: Image) = imageExtension(image.imageType!!, image.name, image.sourcePath)

fun imageExtension(imageType: ImageType, name: String?, sourcePath: String?): String {
    return imageType.extension() ?: sourcePath?.split('.')?.last() ?: name?.split('.')?.last() ?: ""
}