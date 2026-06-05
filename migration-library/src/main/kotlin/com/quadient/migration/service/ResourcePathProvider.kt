package com.quadient.migration.service

import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.dto.migrationmodel.Attachment
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.service.inspirebuilder.appendExtensionIfMissing
import com.quadient.migration.shared.AttachmentType
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.orDefault

interface ResourcePathProvider {
    fun getDocumentObjectPath(nameOrId: String, type: DocumentObjectType, targetFolder: IcmPath?): IcmPath
    fun getDocumentObjectPath(documentObject: DocumentObject): IcmPath {
        return getDocumentObjectPath(
            documentObject.nameOrId(),
            documentObject.type,
            documentObject.targetFolder?.let { IcmPath.from(it) })
    }

    fun getImagePath(id: String, imageType: ImageType, name: String?, targetFolder: IcmPath?, sourcePath: String?): IcmPath
    fun getImagePath(image: Image): IcmPath {
        return getImagePath(
            image.id,
            image.imageType ?: ImageType.Unknown,
            image.name,
            image.targetFolder?.let { IcmPath.from(it) },
            image.sourcePath
        )
    }

    fun getAttachmentPath(id: String, name: String?, targetFolder: IcmPath?, sourcePath: String?, attachmentType: AttachmentType): IcmPath
    fun getAttachmentPath(attachment: Attachment): IcmPath {
        return getAttachmentPath(
            attachment.id,
            attachment.name,
            attachment.targetFolder?.let { IcmPath.from(it) },
            attachment.sourcePath,
            attachment.attachmentType
        )
    }

    fun getDisplayRulePath(rule: DisplayRule): IcmPath

    fun getStyleDefinitionPath(): IcmPath

    fun getFontRootFolder(): IcmPath
}

class DesignerResourcePathProvider(private val projectConfig: ProjectConfig) : ResourcePathProvider {
    override fun getDocumentObjectPath(nameOrId: String, type: DocumentObjectType, targetFolder: IcmPath?): IcmPath {
        val fileName = "$nameOrId.wfd"

        if (targetFolder?.isAbsolute() == true) {
            return targetFolder.join(fileName)
        }

        return IcmPath.root().join(resolveTargetDir(projectConfig.defaultTargetFolder, targetFolder)).join(fileName)
    }

    override fun getImagePath(
        id: String, imageType: ImageType, name: String?, targetFolder: IcmPath?, sourcePath: String?
    ): IcmPath {
        val fileName = "${name ?: id}${imageExtension(imageType, name, sourcePath)}"

        if (targetFolder?.isAbsolute() == true) {
            return targetFolder.join(fileName)
        }

        val imageConfigPath = projectConfig.paths.images

        return IcmPath.root().join(imageConfigPath)
            .join(resolveTargetDir(projectConfig.defaultTargetFolder, targetFolder)).join(fileName)
    }

    override fun getAttachmentPath(
        id: String, name: String?, targetFolder: IcmPath?, sourcePath: String?, attachmentType: AttachmentType
    ): IcmPath {
        val baseAttachmentName = name ?: id
        val attachmentName = appendExtensionIfMissing(baseAttachmentName, sourcePath)

        if (targetFolder?.isAbsolute() == true) {
            return targetFolder.join(attachmentName)
        }

        val fileConfigPath = when (attachmentType) {
            AttachmentType.Attachment -> projectConfig.paths.attachments
            AttachmentType.Document -> projectConfig.paths.documents
        }

        return IcmPath.root().join(fileConfigPath)
            .join(resolveTargetDir(projectConfig.defaultTargetFolder, targetFolder)).join(attachmentName)
    }

    override fun getStyleDefinitionPath(): IcmPath {
        val styleDefinitionPath = projectConfig.styleDefinitionPath

        if (styleDefinitionPath != null && !styleDefinitionPath.isAbsolute()) {
            throw IllegalArgumentException("The configured style definition path '${styleDefinitionPath}' is not absolute.")
        } else if (styleDefinitionPath != null) {
            return styleDefinitionPath
        }

        return IcmPath.root().join(resolveTargetDir(projectConfig.defaultTargetFolder))
            .join("${projectConfig.name}Styles.wfd")
    }

    override fun getDisplayRulePath(rule: DisplayRule): IcmPath {
        error("External display rules are not supported and should not be used for Designer output. Report this as a bug.")
    }

    override fun getFontRootFolder(): IcmPath {
        val fontConfigPath = projectConfig.paths.fonts

        return IcmPath.root().join(fontConfigPath)
    }
}

open class InteractiveResourcePathProvider(private val projectConfig: ProjectConfig) : ResourcePathProvider {
    override fun getDocumentObjectPath(nameOrId: String, type: DocumentObjectType, targetFolder: IcmPath?): IcmPath {
        val ext = when (type) {
            DocumentObjectType.Snippet -> "jsd"
            else -> "jld"
        }

        val fileName = "$nameOrId.$ext"

        if (targetFolder?.isAbsolute() == true) {
            return targetFolder.join(fileName)
        }

        val tenant = projectConfig.interactiveTenant
        val documentObjectType = type.toInteractiveFolder()

        return IcmPath.root()
            .join("Interactive")
            .join(tenant)
            .join(documentObjectType)
            .join(resolveTargetDir(projectConfig.defaultTargetFolder, targetFolder))
            .join(fileName)
    }

    override fun getImagePath(
        id: String,
        imageType: ImageType,
        name: String?,
        targetFolder: IcmPath?,
        sourcePath: String?
    ): IcmPath {
        val fileName = "${name ?: id}${imageExtension(imageType, name, sourcePath)}"

        if (targetFolder?.isAbsolute() == true) {
            return targetFolder.join(fileName)
        }

        val imageConfigPath = projectConfig.paths.images

        return IcmPath.root()
            .join("Interactive")
            .join(projectConfig.interactiveTenant)
            .join(imageConfigPath.orDefault("Resources/Images"))
            .join(resolveTargetDir(projectConfig.defaultTargetFolder, targetFolder))
            .join(fileName)
    }

    override fun getDisplayRulePath(rule: DisplayRule): IcmPath {
        val fileName = "${rule.name ?: rule.id}.jrd"

        val targetFolder = rule.targetFolder?.let { IcmPath.from(it) }
        if (targetFolder?.isAbsolute() == true) {
            return targetFolder.join(fileName)
        }

        return IcmPath.root()
            .join("Interactive")
            .join(projectConfig.interactiveTenant)
            .join("Rules")
            .join(resolveTargetDir(projectConfig.defaultTargetFolder, rule.targetFolder?.let { IcmPath.from(it) }))
            .join(fileName)
    }

    override fun getAttachmentPath(
        id: String, name: String?, targetFolder: IcmPath?, sourcePath: String?, attachmentType: AttachmentType
    ): IcmPath {
        val baseAttachmentName = name ?: id
        val attachmentName = appendExtensionIfMissing(baseAttachmentName, sourcePath)

        if (targetFolder?.isAbsolute() == true) {
            return targetFolder.join(attachmentName)
        }

        val fileConfigPath = when (attachmentType) {
            AttachmentType.Attachment -> projectConfig.paths.attachments.orDefault("Attachments")
            AttachmentType.Document -> projectConfig.paths.documents.orDefault("Documents")
        }

        return IcmPath.root().join("Interactive").join(projectConfig.interactiveTenant).join(fileConfigPath)
            .join(resolveTargetDir(projectConfig.defaultTargetFolder, targetFolder)).join(attachmentName)
    }

    override fun getStyleDefinitionPath(): IcmPath {
        val styleDefConfigPath = projectConfig.styleDefinitionPath

        if (styleDefConfigPath != null && !styleDefConfigPath.isAbsolute()) {
            throw IllegalArgumentException("The configured style definition path '${styleDefConfigPath}' is not absolute.")
        } else if (styleDefConfigPath != null) {
            val pathString = styleDefConfigPath.toString()
            val base = if (pathString.contains(".")) pathString.substringBeforeLast(".") else pathString
            return IcmPath.from("$base.jld")
        }

        return IcmPath.root()
            .join("Interactive")
            .join(projectConfig.interactiveTenant)
            .join("CompanyStyles")
            .join(resolveTargetDir(projectConfig.defaultTargetFolder))
            .join("${projectConfig.name}Styles.jld")
    }

    override fun getFontRootFolder(): IcmPath {
        val fontConfigPath = projectConfig.paths.fonts

        return IcmPath.root().join("Interactive").join(projectConfig.interactiveTenant)
            .join(fontConfigPath.orDefault("Resources/Fonts"))
    }
}

class EvolveResourcePathProvider(projectConfig: ProjectConfig) : InteractiveResourcePathProvider(projectConfig)