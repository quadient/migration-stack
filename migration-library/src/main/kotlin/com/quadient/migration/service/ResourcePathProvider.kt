package com.quadient.migration.service

import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.dto.migrationmodel.Attachment
import com.quadient.migration.api.dto.migrationmodel.BaseTemplate
import com.quadient.migration.api.dto.migrationmodel.BaseTemplateLocation
import com.quadient.migration.api.dto.migrationmodel.BaseTemplateRef
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.LiteralBaseTemplatePath
import com.quadient.migration.service.inspirebuilder.appendExtensionIfMissing
import com.quadient.migration.shared.AttachmentType
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.orDefault
import com.quadient.migration.shared.toIcmPath

interface ResourcePathProvider {
    fun getDocumentObjectFileName(documentObject: DocumentObject): String
    fun getDocumentObjectPath(documentObject: DocumentObject): IcmPath

    fun getImageFileName(image: Image): String
    fun getImagePath(image: Image): IcmPath

    fun getAttachmentFileName(attachment: Attachment): String
    fun getAttachmentPath(attachment: Attachment): IcmPath

    fun getBaseTemplateFullPath(
        config: ProjectConfig,
        documentObjectBaseTemplate: BaseTemplateLocation?,
        findBaseTemplate: (String) -> BaseTemplate,
    ): IcmPath


    fun getDisplayRuleFileName(rule: DisplayRule): String
    fun getDisplayRulePath(rule: DisplayRule): IcmPath

    fun getBaseTemplatePath(literalPath: String): IcmPath
    fun getBaseTemplateFileName(baseTemplate: BaseTemplate): String
    fun getBaseTemplatePath(baseTemplate: BaseTemplate): IcmPath

    fun getStyleDefinitionPath(): IcmPath

    fun getFontRootFolder(): IcmPath
}

class DesignerResourcePathProvider(private val projectConfig: ProjectConfig) : ResourcePathProvider {
    override fun getDocumentObjectFileName(documentObject: DocumentObject): String {
        return "${documentObject.nameOrId()}.wfd"
    }

    override fun getDocumentObjectPath(documentObject: DocumentObject): IcmPath {
        val fileName = getDocumentObjectFileName(documentObject)
        val targetFolder = documentObject.targetFolder?.let { IcmPath.from(it) }

        if (targetFolder?.isAbsolute() == true) {
            return targetFolder.join(fileName)
        }

        return IcmPath.root()
            .join(resolveTargetDir(projectConfig.defaultTargetFolder, targetFolder))
            .join(fileName)
    }

    override fun getImageFileName(image: Image): String {
        return "${image.nameOrId()}${imageExtension(image.imageType ?: ImageType.Unknown, image.name, image.sourcePath)}"
    }

    override fun getImagePath(image: Image): IcmPath {
        val fileName = getImageFileName(image)
        val targetFolder = image.targetFolder?.let { IcmPath.from(it) }

        if (targetFolder?.isAbsolute() == true) {
            return targetFolder.join(fileName)
        }

        val imageConfigPath = projectConfig.paths.images

        return IcmPath.root()
            .join(imageConfigPath)
            .join(resolveTargetDir(projectConfig.defaultTargetFolder, targetFolder))
            .join(fileName)
    }

    override fun getAttachmentFileName(attachment: Attachment): String {
        return appendExtensionIfMissing(attachment.nameOrId(), attachment.sourcePath)
    }

    override fun getAttachmentPath(attachment: Attachment): IcmPath {
        val fileName = getAttachmentFileName(attachment)
        val targetFolder = attachment.targetFolder?.let { IcmPath.from(it) }

        if (targetFolder?.isAbsolute() == true) {
            return targetFolder.join(fileName)
        }

        val fileConfigPath = when (attachment.attachmentType) {
            AttachmentType.Attachment -> projectConfig.paths.attachments
            AttachmentType.Document -> projectConfig.paths.documents
        }

        return IcmPath.root()
            .join(fileConfigPath)
            .join(resolveTargetDir(projectConfig.defaultTargetFolder, targetFolder))
            .join(fileName)
    }

    override fun getStyleDefinitionPath(): IcmPath {
        val fileName = "${projectConfig.name}Styles.wfd"
        val styleDefinitionPath = projectConfig.styleDefinitionPath

        if (styleDefinitionPath != null && !styleDefinitionPath.isAbsolute()) {
            throw IllegalArgumentException("The configured style definition path '${styleDefinitionPath}' is not absolute.")
        } else if (styleDefinitionPath != null) {
            return styleDefinitionPath
        }

        return IcmPath.root()
            .join(resolveTargetDir(projectConfig.defaultTargetFolder))
            .join(fileName)
    }

    override fun getDisplayRuleFileName(rule: DisplayRule): String {
        error("External display rules are not supported and should not be used for Designer output. Report this as a bug.")
    }

    override fun getDisplayRulePath(rule: DisplayRule): IcmPath {
        error("External display rules are not supported and should not be used for Designer output. Report this as a bug.")
    }

    override fun getBaseTemplateFullPath(
        config: ProjectConfig,
        documentObjectBaseTemplate: BaseTemplateLocation?,
        findBaseTemplate: (String) -> BaseTemplate
    ): IcmPath {
        error("Referencing base templates is not supported for Designer output. Report this as a bug.")
    }

    override fun getBaseTemplatePath(literalPath: String): IcmPath {
        error("Referencing base templates is not supported for Designer output. Report this as a bug.")
    }

    override fun getBaseTemplateFileName(baseTemplate: BaseTemplate): String {
        error("Referencing base templates by id is not supported for Designer output. Report this as a bug.")
    }

    override fun getBaseTemplatePath(baseTemplate: BaseTemplate): IcmPath {
        error("Referencing base templates by id is not supported for Designer output. Report this as a bug.")
    }

    override fun getFontRootFolder(): IcmPath {
        val fontConfigPath = projectConfig.paths.fonts

        return IcmPath.root().join(fontConfigPath)
    }
}

open class InteractiveResourcePathProvider(private val projectConfig: ProjectConfig) : ResourcePathProvider {
    override fun getDocumentObjectFileName(documentObject: DocumentObject): String {
        val ext = when (documentObject.type) {
            DocumentObjectType.Snippet -> "jsd"
            else -> "jld"
        }
        return "${documentObject.nameOrId()}.$ext"
    }

    override fun getDocumentObjectPath(documentObject: DocumentObject): IcmPath {
        val targetFolder = documentObject.targetFolder?.let { IcmPath.from(it) }
        val fileName = getDocumentObjectFileName(documentObject)

        if (targetFolder?.isAbsolute() == true) {
            return targetFolder.join(fileName)
        }

        val tenant = projectConfig.interactiveTenant
        val documentObjectType = documentObject.type.toInteractiveFolder()

        return IcmPath.root()
            .join("Interactive")
            .join(tenant)
            .join(documentObjectType)
            .join(resolveTargetDir(projectConfig.defaultTargetFolder, targetFolder))
            .join(fileName)
    }

    override fun getImageFileName(image: Image): String {
        return "${image.nameOrId()}${imageExtension(image.imageType ?: ImageType.Unknown, image.name, image.sourcePath)}"
    }

    override fun getImagePath(image: Image): IcmPath {
        val fileName = getImageFileName(image)
        val targetFolder = image.targetFolder?.let { IcmPath.from(it) }

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

    override fun getDisplayRuleFileName(rule: DisplayRule): String {
        return "${rule.nameOrId()}.jrd"
    }

    override fun getDisplayRulePath(rule: DisplayRule): IcmPath {
        val fileName = getDisplayRuleFileName(rule)

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

    override fun getAttachmentFileName(attachment: Attachment): String {
        return appendExtensionIfMissing(attachment.nameOrId(), attachment.sourcePath)
    }

    override fun getAttachmentPath(attachment: Attachment): IcmPath {
        val fileName = getAttachmentFileName(attachment)
        val targetFolder = attachment.targetFolder?.let { IcmPath.from(it) }

        if (targetFolder?.isAbsolute() == true) {
            return targetFolder.join(fileName)
        }

        val fileConfigPath = when (attachment.attachmentType) {
            AttachmentType.Attachment -> projectConfig.paths.attachments.orDefault("Attachments")
            AttachmentType.Document -> projectConfig.paths.documents.orDefault("Documents")
        }

        return IcmPath.root().join("Interactive").join(projectConfig.interactiveTenant).join(fileConfigPath)
            .join(resolveTargetDir(projectConfig.defaultTargetFolder, targetFolder)).join(fileName)
    }

    override fun getBaseTemplatePath(literalPath: String): IcmPath {
        return IcmPath.root()
            .join("Interactive")
            .join(projectConfig.interactiveTenant)
            .join("BaseTemplates")
            .join(literalPath)
    }

    override fun getBaseTemplateFullPath(
        config: ProjectConfig,
        documentObjectBaseTemplate: BaseTemplateLocation?,
        findBaseTemplate: (String) -> BaseTemplate,
    ): IcmPath {
        val literalPath = when (documentObjectBaseTemplate) {
            is LiteralBaseTemplatePath -> documentObjectBaseTemplate.path

            is BaseTemplateRef -> {
                val baseTemplate = findBaseTemplate(documentObjectBaseTemplate.id)
                return getBaseTemplatePath(baseTemplate)
            }

            null -> config.baseTemplatePath
        }

        val path = literalPath.toIcmPath()
        if (path.isAbsolute()) return path

        return getBaseTemplatePath(literalPath)
    }

    override fun getBaseTemplateFileName(baseTemplate: BaseTemplate): String {
        return "${baseTemplate.nameOrId()}.wfd"
    }

    override fun getBaseTemplatePath(baseTemplate: BaseTemplate): IcmPath {
        val fileName = getBaseTemplateFileName(baseTemplate)

        val targetFolder = baseTemplate.targetFolder?.let { IcmPath.from(it) }
        if (targetFolder?.isAbsolute() == true) {
            return targetFolder.join(fileName)
        }

        val relativePath = resolveTargetDir(projectConfig.defaultTargetFolder, targetFolder)?.join(fileName) ?: IcmPath.from(fileName)
        return getBaseTemplatePath(relativePath.toString())
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