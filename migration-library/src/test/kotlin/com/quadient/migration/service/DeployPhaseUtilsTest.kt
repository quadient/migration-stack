package com.quadient.migration.service

import com.quadient.migration.service.inspirebuilder.appendExtensionIfMissing
import com.quadient.migration.service.inspirebuilder.extractExtensionFromPath
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.shouldBeEqualTo
import org.junit.jupiter.api.Test

class DeployPhaseUtilsTest {
    val projectConfig = aProjectConfig(
        "vcs:\\\\Interactive\\StandardPackage\\BaseTemplates\\BaseTemplate.wfd", interactiveTenant = "StandardPackage"
    )

    @Test
    fun `project config base template is used and normalized`() {
        val result = getBaseTemplateFullPath(projectConfig, null).toString()

        result.shouldBeEqualTo("icm://Interactive/StandardPackage/BaseTemplates/BaseTemplate.wfd")
    }

    @Test
    fun `specific base template path is preferred over the project config one`() {
        val baseTemplatePath = "icm://Interactive/Vital/BaseTemplates/MyBaseTemplate.wfd"
        val result = getBaseTemplateFullPath(projectConfig, baseTemplatePath).toString()

        result.shouldBeEqualTo(baseTemplatePath)
    }

    @Test
    fun `path not starting with icm is handled as relative`() {
        val result = getBaseTemplateFullPath(projectConfig, "/projectA/AddressBT.wfd").toString()

        result.shouldBeEqualTo("icm://Interactive/${projectConfig.interactiveTenant}/BaseTemplates/projectA/AddressBT.wfd")
    }

    @Test
    fun `only base template name in project config is correctly translated to full path`() {
        val result = getBaseTemplateFullPath(aProjectConfig("myBT.wfd", interactiveTenant = "StandardPackage"), null).toString()

        result.shouldBeEqualTo("icm://Interactive/StandardPackage/BaseTemplates/myBT.wfd")
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