package com.quadient.migration.service

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
}