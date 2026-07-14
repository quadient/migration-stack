package com.quadient.migration.service

import com.quadient.migration.api.dto.migrationmodel.AttachmentRef
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.builder.AttachmentBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ImageBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.VariableBuilder
import com.quadient.migration.api.repository.AttachmentRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ImageRepository
import com.quadient.migration.api.repository.VariableRepository
import com.quadient.migration.shared.DataType
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.tools.shouldBeEqualTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class PreviewProviderTest {
    private val docObjectRepo = mockk<DocumentObjectRepository>()
    private val imageRepo = mockk<ImageRepository>()
    private val attachmentRepo = mockk<AttachmentRepository>()
    private val variableRepo = mockk<VariableRepository>()

    private val provider = PreviewProvider(docObjectRepo, imageRepo, attachmentRepo, variableRepo)

    @Test
    fun `getPreview resolves DocumentObjectRef name`() {
        every { docObjectRepo.find("doc-1") } returns DocumentObjectBuilder("doc-1", DocumentObjectType.Block).name("My Block").build()

        provider.getPreview(DocumentObjectRef("doc-1")).shouldBeEqualTo("docRef: My Block")
    }

    @Test
    fun `getPreview falls back to id when DocumentObjectRef has no name`() {
        every { docObjectRepo.find("doc-1") } returns DocumentObjectBuilder("doc-1", DocumentObjectType.Block).build()

        provider.getPreview(DocumentObjectRef("doc-1")).shouldBeEqualTo("docRef: doc-1")
    }

    @Test
    fun `getPreview falls back to id when DocumentObjectRef not found`() {
        every { docObjectRepo.find("doc-1") } returns null

        provider.getPreview(DocumentObjectRef("doc-1")).shouldBeEqualTo("docRef: doc-1")
    }

    @Test
    fun `getPreview resolves ImageRef name`() {
        every { imageRepo.find("img-1") } returns ImageBuilder("img-1").name("My Image").build()

        provider.getPreview(ImageRef("img-1")).shouldBeEqualTo("imageRef: My Image")
    }

    @Test
    fun `getPreview resolves AttachmentRef name`() {
        every { attachmentRepo.find("att-1") } returns AttachmentBuilder("att-1").name("My Attachment").build()

        provider.getPreview(AttachmentRef("att-1")).shouldBeEqualTo("attachRef: My Attachment")
    }

    @Test
    fun `getPreview resolves VariableRef name`() {
        every { variableRepo.find("var-1") } returns VariableBuilder("var-1").name("My Variable").dataType(DataType.String).build()

        provider.getPreview(VariableRef("var-1")).shouldBeEqualTo($$"$My Variable$")
    }

    @Test
    fun `getPreview falls back to id for unresolvable VariableRef`() {
        every { variableRepo.find("var-1") } returns null

        provider.getPreview(VariableRef("var-1")).shouldBeEqualTo($$"$var-1$")
    }

    @Test
    fun `getPreview returns plain value for StringValue`() {
        provider.getPreview(StringValue("hello world")).shouldBeEqualTo("hello world")
    }

    @Test
    fun `buildDocumentContentListPreview joins previews with semicolon`() {
        every { docObjectRepo.find(any()) } returns null

        val result = provider.buildDocumentContentListPreview(
            listOf(DocumentObjectRef("a"), DocumentObjectRef("b"), DocumentObjectRef("c"))
        )

        result.shouldBeEqualTo("docRef: a;docRef: b;docRef: c")
    }

    @Test
    fun `buildDocumentContentListPreview truncates after limit and appends count`() {
        every { docObjectRepo.find(any()) } returns null

        val content = listOf(
            DocumentObjectRef("a"), DocumentObjectRef("b"), DocumentObjectRef("c"),
            DocumentObjectRef("d"), DocumentObjectRef("e")
        )

        provider.buildDocumentContentListPreview(content, limit = 3)
            .shouldBeEqualTo("docRef: a;docRef: b;docRef: c;(+2 more)")
    }

    @Test
    fun `buildDocumentContentListPreview strips commas from previews`() {
        every { docObjectRepo.find("x") } returns DocumentObjectBuilder("x", DocumentObjectType.Block).name("Foo, Bar").build()

        provider.buildDocumentContentListPreview(listOf(DocumentObjectRef("x")))
            .shouldBeEqualTo("docRef: Foo  Bar")
    }
}
