package com.quadient.migration.service

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.PathsConfig
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.service.inspirebuilder.InteractiveDocumentObjectBuilder
import com.quadient.migration.shared.AttachmentType
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.DocumentObjectType.Template
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.toIcmPath
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.model.aAttachment
import com.quadient.migration.tools.model.aDocObj
import com.quadient.migration.tools.model.aImage
import com.quadient.migration.tools.shouldBeEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ResourcePathProviderTest {

    @Nested
    inner class InteractiveResourcePathProviderTest {
        private fun aSubject(config: ProjectConfig) = InteractiveResourcePathProvider(config)

        @ParameterizedTest
        @CsvSource(
            // projectCfg.paths ,targetFolder   ,defaultTargetFolder,expected
            "               ,                   ,                   ,icm://Interactive/tenant/Resources/Images/Image_T_1.jpg",
            "null           ,null               ,null               ,icm://Interactive/tenant/Resources/Images/Image_T_1.jpg",
            "null           ,                   ,                   ,icm://Interactive/tenant/Resources/Images/Image_T_1.jpg",
            "               ,null               ,                   ,icm://Interactive/tenant/Resources/Images/Image_T_1.jpg",
            "               ,                   ,null               ,icm://Interactive/tenant/Resources/Images/Image_T_1.jpg",
            "               ,relative           ,                   ,icm://Interactive/tenant/Resources/Images/relative/Image_T_1.jpg",
            "               ,relative           ,null               ,icm://Interactive/tenant/Resources/Images/relative/Image_T_1.jpg",
            "root           ,relative           ,                   ,icm://Interactive/tenant/root/relative/Image_T_1.jpg",
            "root           ,relative           ,null               ,icm://Interactive/tenant/root/relative/Image_T_1.jpg",
            "               ,icm://absolute/    ,                   ,icm://absolute/Image_T_1.jpg",
            "/root/         ,icm://absolute/    ,                   ,icm://absolute/Image_T_1.jpg",
            "               ,icm://absolute/    ,shouldIgnoreToo    ,icm://absolute/Image_T_1.jpg",
            "shouldIgnore   ,icm://absolute/    ,shouldIgnoreToo    ,icm://absolute/Image_T_1.jpg",
            "imagesConfig   ,                   ,                   ,icm://Interactive/tenant/imagesConfig/Image_T_1.jpg",
            "/imagesConfig/ ,                   ,                   ,icm://Interactive/tenant/imagesConfig/Image_T_1.jpg",
            "imagesConfig   ,subDir             ,                   ,icm://Interactive/tenant/imagesConfig/subDir/Image_T_1.jpg",
            "imagesConfig   ,                   ,default            ,icm://Interactive/tenant/imagesConfig/default/Image_T_1.jpg",
            "imagesConfig   ,subDir             ,default            ,icm://Interactive/tenant/imagesConfig/subDir/Image_T_1.jpg",
            "               ,                   ,default            ,icm://Interactive/tenant/Resources/Images/default/Image_T_1.jpg",
            "               ,subDir             ,shouldIgnore       ,icm://Interactive/tenant/Resources/Images/subDir/Image_T_1.jpg",
        )
        fun testImagesPath(imagesPath: String?, targetFolder: String?, defaultTargetFolder: String?, expected: String) {
            val config = aProjectConfig(
                output = InspireOutput.Interactive,
                paths = PathsConfig(images = imagesPath.nullToNull()?.let(IcmPath::from)),
                targetDefaultFolder = defaultTargetFolder.nullToNull(),
                interactiveTenant = "tenant"
            )
            val pathTestSubject = aSubject(config)
            val image = aImage("T_1", targetFolder = targetFolder.nullToNull())

            val path = pathTestSubject.getImagePath(image)

            path.toString().shouldBeEqualTo(expected)
        }

        @ParameterizedTest
        @CsvSource(
            // targetFolder   ,defaultTargetFolder  ,expected
            "                   ,                   ,icm://Interactive/tenant/Templates/T_1name.jld",
            "null               ,                   ,icm://Interactive/tenant/Templates/T_1name.jld",
            "                   ,null               ,icm://Interactive/tenant/Templates/T_1name.jld",
            "null               ,null               ,icm://Interactive/tenant/Templates/T_1name.jld",
            "relative           ,                   ,icm://Interactive/tenant/Templates/relative/T_1name.jld",
            "relative           ,null               ,icm://Interactive/tenant/Templates/relative/T_1name.jld",
            "relative           ,default            ,icm://Interactive/tenant/Templates/relative/T_1name.jld",
            "icm://absolute/    ,                   ,icm://absolute/T_1name.jld",
            "icm://absolute/    ,default            ,icm://absolute/T_1name.jld",
            "                   ,default            ,icm://Interactive/tenant/Templates/default/T_1name.jld",
            "null               ,default            ,icm://Interactive/tenant/Templates/default/T_1name.jld",
        )
        fun testDocumentObjectPath(targetFolder: String?, defaultTargetFolder: String?, expected: String) {
            val config = aProjectConfig(
                output = InspireOutput.Interactive,
                targetDefaultFolder = defaultTargetFolder.nullToNull(),
                interactiveTenant = "tenant"
            )
            val pathTestSubject = aSubject(config)
            val image = aDocObj("T_1", type = Template, targetFolder = targetFolder.nullToNull())

            val path = pathTestSubject.getDocumentObjectPath(image)

            path.toString().shouldBeEqualTo(expected)
        }

        @ParameterizedTest
        @CsvSource(
            // styleDefPath        ,defaultTargetFolder  ,expected
            "                      ,                       ,icm://Interactive/tenant/CompanyStyles/projectNameStyles.jld",
            "                      ,null                   ,icm://Interactive/tenant/CompanyStyles/projectNameStyles.jld",
            "                      ,default                ,icm://Interactive/tenant/CompanyStyles/default/projectNameStyles.jld",
            "icm://some/path/f.jld ,default                ,icm://some/path/f.jld",
            "icm://some/path/f.wfd ,default                ,icm://some/path/f.jld",
        )
        fun testCompanyStylesPath(styleDefPath: String?, defaultTargetFolder: String?, expected: String) {
            val config = aProjectConfig(
                name = "projectName",
                output = InspireOutput.Interactive,
                targetDefaultFolder = defaultTargetFolder.nullToNull(),
                interactiveTenant = "tenant",
                styleDefinitionPath = styleDefPath?.toIcmPath()
            )
            val pathTestSubject = aSubject(config)

            val path = pathTestSubject.getStyleDefinitionPath()

            path.toString().shouldBeEqualTo(expected)
        }

        @ParameterizedTest
        @CsvSource(
            ",icm://Interactive/tenant/Resources/Fonts", "Fonts,icm://Interactive/tenant/Fonts"
        )
        fun testFontRootFolder(cfgFontsPath: String?, expected: String) {
            val config = aProjectConfig(
                output = InspireOutput.Interactive,
                interactiveTenant = "tenant",
                paths = PathsConfig(fonts = cfgFontsPath?.toIcmPath())
            )
            val pathTestSubject = aSubject(config)

            val path = pathTestSubject.getFontRootFolder()

            path.toString().shouldBeEqualTo(expected)
        }

        @Test
        fun companyStylePathMustBeAbsolute() {
            val config = aProjectConfig(
                name = "projectName",
                output = InspireOutput.Interactive,
                styleDefinitionPath = "somePath.wfd".toIcmPath()
            )
            val pathTestSubject = aSubject(config)

            assertThrows<IllegalArgumentException> { pathTestSubject.getStyleDefinitionPath() }
        }


        private fun String?.nullToNull() = when (this?.trim()) {
            "null" -> null
            null -> null
            else -> this.trim()
        }

        @ParameterizedTest
        @CsvSource(
            // attachmentType,paths.documents,paths.attachments,targetFolder,defaultTargetFolder,expected
            "Document,,,,                   ,icm://Interactive/tenant/Documents/Attachment_F1.pdf",
            "Document,,,relative,           ,icm://Interactive/tenant/Documents/relative/Attachment_F1.pdf",
            "Document,Docs,,relative,       ,icm://Interactive/tenant/Docs/relative/Attachment_F1.pdf",
            "Document,,,icm://absolute/,    ,icm://absolute/Attachment_F1.pdf",
            "Document,,,                ,def,icm://Interactive/tenant/Documents/def/Attachment_F1.pdf",
            "Attachment,,,,                 ,icm://Interactive/tenant/Attachments/Attachment_F1.pdf",
            "Attachment,,Attach,relative,   ,icm://Interactive/tenant/Attach/relative/Attachment_F1.pdf",
            "Attachment,,,icm://absolute/,  ,icm://absolute/Attachment_F1.pdf",
        )
        fun testAttachmentPath(
            attachmentType: String,
            documentsPath: String?,
            attachmentsPath: String?,
            targetFolder: String?,
            defaultTargetFolder: String?,
            expected: String
        ) {
            val config = aProjectConfig(
                output = InspireOutput.Interactive,
                interactiveTenant = "tenant",
                paths = PathsConfig(
                    documents = documentsPath.nullToNull()?.let(IcmPath::from),
                    attachments = attachmentsPath.nullToNull()?.let(IcmPath::from)
                ),
                targetDefaultFolder = defaultTargetFolder.nullToNull(),
            )
            val pathTestSubject = aSubject(config)
            val attachment = aAttachment("F1", targetFolder = targetFolder.nullToNull(), attachmentType = AttachmentType.valueOf(attachmentType))

            val path = pathTestSubject.getAttachmentPath(attachment)

            path.toString().shouldBeEqualTo(expected)
        }

        @Test
        fun `attachment path appends extension from sourcePath when attachmentName lacks one`() {
            val config = aProjectConfig(output = InspireOutput.Interactive, interactiveTenant = "tenant")
            val pathTestSubject = aSubject(config)
            val attachment = aAttachment("F1", name = "document", sourcePath = "C:/attachments/doc.pdf")

            val path = pathTestSubject.getAttachmentPath(attachment)

            path.toString().shouldBeEqualTo("icm://Interactive/tenant/Documents/document.pdf")
        }
    }

    @Nested
    inner class DesignerResourcePathProviderTest {
        private fun aSubject(config: ProjectConfig) = DesignerResourcePathProvider(config)

        @ParameterizedTest
        @CsvSource(
            // projectCfg.paths ,targetFolder   ,defaultTargetFolder,expected
            "               ,                   ,                   ,icm://Image_T_1.jpg",
            "null           ,null               ,null               ,icm://Image_T_1.jpg",
            "null           ,                   ,                   ,icm://Image_T_1.jpg",
            "               ,null               ,                   ,icm://Image_T_1.jpg",
            "               ,                   ,null               ,icm://Image_T_1.jpg",
            "               ,relative           ,                   ,icm://relative/Image_T_1.jpg",
            "               ,relative           ,null               ,icm://relative/Image_T_1.jpg",
            "root           ,relative           ,                   ,icm://root/relative/Image_T_1.jpg",
            "root           ,relative           ,null               ,icm://root/relative/Image_T_1.jpg",
            "               ,icm://absolute/    ,                   ,icm://absolute/Image_T_1.jpg",
            "/root/         ,icm://absolute/    ,                   ,icm://absolute/Image_T_1.jpg",
            "               ,icm://absolute/    ,shouldIgnoreToo    ,icm://absolute/Image_T_1.jpg",
            "shouldIgnore   ,icm://absolute/    ,shouldIgnoreToo    ,icm://absolute/Image_T_1.jpg",
            "imagesConfig   ,                   ,                   ,icm://imagesConfig/Image_T_1.jpg",
            "/imagesConfig/ ,                   ,                   ,icm://imagesConfig/Image_T_1.jpg",
            "imagesConfig   ,subDir             ,                   ,icm://imagesConfig/subDir/Image_T_1.jpg",
            "imagesConfig   ,                   ,default            ,icm://imagesConfig/default/Image_T_1.jpg",
            "imagesConfig   ,subDir             ,default            ,icm://imagesConfig/subDir/Image_T_1.jpg",
            "               ,                   ,default            ,icm://default/Image_T_1.jpg",
            "               ,subDir             ,shouldIgnore       ,icm://subDir/Image_T_1.jpg",
        )
        fun testImagesPath(imagesPath: String?, targetFolder: String?, defaultTargetFolder: String?, expected: String) {
            val config = aProjectConfig(
                output = InspireOutput.Designer,
                paths = PathsConfig(images = imagesPath.nullToNull()?.let(IcmPath::from)),
                targetDefaultFolder = defaultTargetFolder.nullToNull(),
            )
            val pathTestSubject = aSubject(config)
            val image = aImage("T_1", targetFolder = targetFolder.nullToNull())

            val path = pathTestSubject.getImagePath(image)

            path.toString().shouldBeEqualTo(expected)
        }

        @ParameterizedTest
        @CsvSource(
            // targetFolder   ,defaultTargetFolder  ,expected
            "                   ,                   ,icm://T_1name.wfd",
            "null               ,                   ,icm://T_1name.wfd",
            "                   ,null               ,icm://T_1name.wfd",
            "null               ,null               ,icm://T_1name.wfd",
            "relative           ,                   ,icm://relative/T_1name.wfd",
            "relative           ,null               ,icm://relative/T_1name.wfd",
            "relative           ,default            ,icm://relative/T_1name.wfd",
            "icm://absolute/    ,                   ,icm://absolute/T_1name.wfd",
            "icm://absolute/    ,default            ,icm://absolute/T_1name.wfd",
            "                   ,default            ,icm://default/T_1name.wfd",
            "null               ,default            ,icm://default/T_1name.wfd",
        )
        fun testDocumentObjectPath(targetFolder: String?, defaultTargetFolder: String?, expected: String) {
            val config = aProjectConfig(
                output = InspireOutput.Designer,
                targetDefaultFolder = defaultTargetFolder.nullToNull(),
            )
            val pathTestSubject = aSubject(config)
            val image = aDocObj("T_1", type = DocumentObjectType.Template, targetFolder = targetFolder.nullToNull())

            val path = pathTestSubject.getDocumentObjectPath(image)

            path.toString().shouldBeEqualTo(expected)
        }

        @ParameterizedTest
        @CsvSource(
            // styleDefPath        ,defaultTargetFolder  ,expected
            "                      ,                       ,icm://projectNameStyles.wfd",
            "                      ,null                   ,icm://projectNameStyles.wfd",
            "                      ,default                ,icm://default/projectNameStyles.wfd",
            "                      ,default                ,icm://default/projectNameStyles.wfd",
            "icm://some/path/f.wfd ,default                ,icm://some/path/f.wfd",
        )
        fun testCompanyStylesPath(styleDefPath: String?, defaultTargetFolder: String?, expected: String) {
            val config = aProjectConfig(
                name = "projectName",
                output = InspireOutput.Designer,
                targetDefaultFolder = defaultTargetFolder.nullToNull(),
                styleDefinitionPath = styleDefPath?.toIcmPath()
            )
            val pathTestSubject = aSubject(config)

            val path = pathTestSubject.getStyleDefinitionPath()

            path.toString().shouldBeEqualTo(expected)
        }

        @ParameterizedTest
        @CsvSource(
            ",icm://", "Resources/Fonts,icm://Resources/Fonts"
        )
        fun testFontRootFolder(cfgFontsPath: String?, expected: String) {
            val config =
                aProjectConfig(output = InspireOutput.Designer, paths = PathsConfig(fonts = cfgFontsPath?.toIcmPath()))
            val pathTestSubject = aSubject(config)

            val path = pathTestSubject.getFontRootFolder()

            path.toString().shouldBeEqualTo(expected)
        }

        @Test
        fun companyStylePathMustBeAbsolute() {
            val config = aProjectConfig(
                name = "projectName",
                output = InspireOutput.Designer,
                styleDefinitionPath = "somePath.wfd".toIcmPath()
            )
            val pathTestSubject = aSubject(config)

            assertThrows<IllegalArgumentException> { pathTestSubject.getStyleDefinitionPath() }
        }

        private fun String?.nullToNull() = when (this?.trim()) {
            "null" -> null
            null -> ""
            else -> this.trim()
        }

        @ParameterizedTest
        @CsvSource(
            // attachmentType,paths.documents,paths.attachments,targetFolder,defaultTargetFolder,expected
            "Document,,,,                   ,icm://Attachment_F1.pdf",
            "Document,,,relative,           ,icm://relative/Attachment_F1.pdf",
            "Document,Docs,,relative,       ,icm://Docs/relative/Attachment_F1.pdf",
            "Document,,,icm://absolute/,    ,icm://absolute/Attachment_F1.pdf",
            "Document,,,                ,def,icm://def/Attachment_F1.pdf",
            "Attachment,,,,                 ,icm://Attachment_F1.pdf",
            "Attachment,,Attach,relative,   ,icm://Attach/relative/Attachment_F1.pdf",
            "Attachment,,,icm://absolute/,  ,icm://absolute/Attachment_F1.pdf",
        )
        fun testAttachmentPath(
            attachmentType: String,
            documentsPath: String?,
            attachmentsPath: String?,
            targetFolder: String?,
            defaultTargetFolder: String?,
            expected: String
        ) {
            val config = aProjectConfig(
                output = InspireOutput.Designer,
                paths = PathsConfig(
                    documents = documentsPath.nullToNull()?.let(IcmPath::from),
                    attachments = attachmentsPath.nullToNull()?.let(IcmPath::from)
                ),
                targetDefaultFolder = defaultTargetFolder.nullToNull(),
            )
            val pathTestSubject = aSubject(config)
            val attachment = aAttachment("F1", targetFolder = targetFolder.nullToNull(), attachmentType = AttachmentType.valueOf(attachmentType))

            val path = pathTestSubject.getAttachmentPath(attachment)

            path.toString().shouldBeEqualTo(expected)
        }

        @Test
        fun `attachment path appends extension from sourcePath when attachmentName lacks one`() {
            val config = aProjectConfig(output = InspireOutput.Designer)
            val pathTestSubject = aSubject(config)
            val attachment = aAttachment("F1", name = "document", sourcePath = "C:/attachments/doc.pdf")

            val path = pathTestSubject.getAttachmentPath(attachment)

            path.toString().shouldBeEqualTo("icm://document.pdf")
        }

        @Test
        fun `attachment path preserves attachmentName extension when present`() {
            val config = aProjectConfig(output = InspireOutput.Designer)
            val pathTestSubject = aSubject(config)
            val attachment = aAttachment("F1", name = "report.docx", sourcePath = "attachment.pdf")

            val path = pathTestSubject.getAttachmentPath(attachment)

            path.toString().shouldBeEqualTo("icm://report.docx")
        }
    }
}