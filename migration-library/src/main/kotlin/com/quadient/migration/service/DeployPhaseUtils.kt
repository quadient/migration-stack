package com.quadient.migration.service

import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.dto.migrationmodel.Attachment
import com.quadient.migration.api.dto.migrationmodel.AttachmentRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.ResourceRef
import com.quadient.migration.api.repository.Repository
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.isNullOrBlank
import com.quadient.migration.shared.toIcmPath
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.quadient.migration.service.DeployPhaseUtils")

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
                AttachmentRef(targetId)
            } ?: ref
        }
        is AttachmentRef -> {
            val attachment = attachmentRepository.find(ref.id) ?: return ref
            attachment.targetImageId?.let { targetId ->
                logger.info("Resolving attachment '${ref.id}' to image '$targetId' via alias")
                ImageRef(targetId)
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

fun getBaseTemplateFullPath(config: ProjectConfig, documentObjectBaseTemplatePath: String?): IcmPath {
    val baseTemplatePath = documentObjectBaseTemplatePath ?: config.baseTemplatePath
    val path = baseTemplatePath.toIcmPath()

    if (path.isAbsolute()) return path

    return "icm://Interactive".toIcmPath()
        .join(config.interactiveTenant)
        .join("BaseTemplates")
        .join(path)
}

fun imageExtension(image: Image) = imageExtension(image.imageType!!, image.name, image.sourcePath)

fun imageExtension(imageType: ImageType, name: String?, sourcePath: String?): String {
    return imageType.extension() ?: sourcePath?.split('.')?.last() ?: name?.split('.')?.last() ?: ""
}