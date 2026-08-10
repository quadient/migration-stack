package com.quadient.migration.service

import com.quadient.migration.service.inspirebuilder.appendExtensionIfMissing
import com.quadient.migration.service.inspirebuilder.extractExtensionFromPath
import com.quadient.migration.api.dto.migrationmodel.BaseTemplate
import com.quadient.migration.api.dto.migrationmodel.BaseTemplateRef
import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.LiteralBaseTemplatePath
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.shouldBeEqualTo
import org.junit.jupiter.api.Test

class DeployPhaseUtilsTest {
    val projectConfig = aProjectConfig(
        "vcs:\\\\Interactive\\StandardPackage\\BaseTemplates\\BaseTemplate.wfd", interactiveTenant = "StandardPackage"
    )
    val resourcePathProvider = InteractiveResourcePathProvider(projectConfig)
    val findBaseTemplate: (String) -> BaseTemplate = { error("Unexpected base template lookup for '$it'") }

    @Test
    fun `project config base template is used and normalized`() {
        val result = resourcePathProvider.getBaseTemplateFullPath(projectConfig, null, findBaseTemplate).toString()

        result.shouldBeEqualTo("icm://Interactive/StandardPackage/BaseTemplates/BaseTemplate.wfd")
    }

    @Test
    fun `specific base template path is preferred over the project config one`() {
        val baseTemplatePath = "icm://Interactive/Vital/BaseTemplates/MyBaseTemplate.wfd"
        val result = resourcePathProvider.getBaseTemplateFullPath(
            projectConfig, LiteralBaseTemplatePath(baseTemplatePath), findBaseTemplate
        ).toString()

        result.shouldBeEqualTo(baseTemplatePath)
    }

    @Test
    fun `path not starting with icm is handled as relative`() {
        val result = resourcePathProvider.getBaseTemplateFullPath(
            projectConfig, LiteralBaseTemplatePath("/projectA/AddressBT.wfd"), findBaseTemplate
        ).toString()

        result.shouldBeEqualTo("icm://Interactive/${projectConfig.interactiveTenant}/BaseTemplates/projectA/AddressBT.wfd")
    }

    @Test
    fun `only base template name in project config is correctly translated to full path`() {
        val config = aProjectConfig("myBT.wfd", interactiveTenant = "StandardPackage")
        val result = InteractiveResourcePathProvider(config).getBaseTemplateFullPath(
            config, null, findBaseTemplate
        ).toString()

        result.shouldBeEqualTo("icm://Interactive/StandardPackage/BaseTemplates/myBT.wfd")
    }

    @Test
    fun `base template referenced by id fails because it is not yet supported`() {
        val baseTemplate = BaseTemplate(
            id = "bt-1",
            name = "AddressBaseTemplate",
            customFields = CustomFieldMap(),
        )

        try {
            resourcePathProvider.getBaseTemplateFullPath(
                projectConfig, BaseTemplateRef(baseTemplate.id)
            ) { id -> if (id == baseTemplate.id) baseTemplate else error("Unexpected id '$id'") }
            error("Expected an exception to be thrown")
        } catch (e: IllegalStateException) {
            e.message.shouldBeEqualTo(
                "Base template 'icm://Interactive/StandardPackage/BaseTemplates/AddressBaseTemplate.wfd' cannot be used because referencing base templates by id is not yet supported during deployment."
            )
        }
    }

    @Test
    fun `base template referenced by id fails if it cannot be found`() {
        try {
            resourcePathProvider.getBaseTemplateFullPath(
                projectConfig, BaseTemplateRef("missing")
            ) { error("Record 'missing' not found") }
            error("Expected an exception to be thrown")
        } catch (e: IllegalStateException) {
            e.message.shouldBeEqualTo("Record 'missing' not found")
        }
    }

    @Test
    fun `extractExtensionFromPath handles various path formats correctly`() {
        // Valid extensions
        extractExtensionFromPath("file.pdf").shouldBeEqualTo(".pdf")
        extractExtensionFromPath("C:/folder/file.txt").shouldBeEqualTo(".txt")
        extractExtensionFromPath("folder/subfolder/file.bat").shouldBeEqualTo(".bat")
        extractExtensionFromPath("archive.tar.gz").shouldBeEqualTo(".gz")
        extractExtensionFromPath("C:\\Windows\\Path\\file.docx").shouldBeEqualTo(".docx")

        // Invalid cases
        extractExtensionFromPath(null).shouldBeEqualTo(null)
        extractExtensionFromPath("").shouldBeEqualTo(null)
        extractExtensionFromPath("   ").shouldBeEqualTo(null)
        extractExtensionFromPath("C:/folder/filename").shouldBeEqualTo(null)
        extractExtensionFromPath("folder.ext/filename").shouldBeEqualTo(null)
        extractExtensionFromPath(".gitignore").shouldBeEqualTo(null)
        extractExtensionFromPath("file.").shouldBeEqualTo(null)
    }

    @Test
    fun `appendExtensionIfMissing handles various scenarios correctly`() {
        // Appends extension when missing
        appendExtensionIfMissing("document", "C:/file.pdf").shouldBeEqualTo("document.pdf")
        appendExtensionIfMissing("file", "folder/doc.txt").shouldBeEqualTo("file.txt")
        appendExtensionIfMissing("file", "C:\\folder\\doc.bat").shouldBeEqualTo("file.bat")

        // Preserves existing extension
        appendExtensionIfMissing("report.docx", "C:/file.pdf").shouldBeEqualTo("report.docx")
        appendExtensionIfMissing("archive.tar.gz", "file.txt").shouldBeEqualTo("archive.tar.gz")

        // Handles null/blank/invalid sourcePath gracefully
        appendExtensionIfMissing("file", null).shouldBeEqualTo("file")
        appendExtensionIfMissing("file", "").shouldBeEqualTo("file")
        appendExtensionIfMissing("file", "noext").shouldBeEqualTo("file")
        appendExtensionIfMissing("file", "folder.ext/noext").shouldBeEqualTo("file")
    }
}