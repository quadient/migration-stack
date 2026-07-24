package com.quadient.migration.service.deploy.utility

import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.tools.shouldBeEqualTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DeployOrderTest {
    val documentObjectRepository = mockk<DocumentObjectRepository>()
    val subject = DeployOrderImpl(documentObjectRepository)

    @Test
    fun `deployOrder is correct`() {
        val list = listOf(
            DocumentObjectBuilder("a", DocumentObjectType.Block).documentObjectRef("b").mock(),
            DocumentObjectBuilder("a2", DocumentObjectType.Block).documentObjectRef("b").mock(),
            DocumentObjectBuilder("a3", DocumentObjectType.Block).documentObjectRef("b").mock(),
            DocumentObjectBuilder("b", DocumentObjectType.Block).documentObjectRef("c").mock(),
            DocumentObjectBuilder("c", DocumentObjectType.Block).mock(),
            DocumentObjectBuilder("d", DocumentObjectType.Block).documentObjectRef("f").mock(),
            DocumentObjectBuilder("e", DocumentObjectType.Block).mock(),
            DocumentObjectBuilder("f", DocumentObjectType.Block).mock(),
        )

        val result = subject.deployOrder(list)

        result.map { it.id }.shouldBeEqualTo(listOf("c", "e", "f", "b", "d", "a", "a2", "a3"))
    }

    @Test
    fun `deployOrder has missing object`() {
        val list = listOf(
            DocumentObjectBuilder("a", DocumentObjectType.Block).documentObjectRef("b").mock(),
            DocumentObjectBuilder("a2", DocumentObjectType.Block).documentObjectRef("b").mock(),
            DocumentObjectBuilder("a3", DocumentObjectType.Block).documentObjectRef("b").mock(),
            DocumentObjectBuilder("b", DocumentObjectType.Block).documentObjectRef("c").mock(),
            // c is missing
            DocumentObjectBuilder("d", DocumentObjectType.Block).documentObjectRef("f").mock(),
            DocumentObjectBuilder("e", DocumentObjectType.Block).mock(),
            DocumentObjectBuilder("f", DocumentObjectType.Block).mock(),
        )
        every { documentObjectRepository.find("c") } returns null

        val result = subject.deployOrder(list)

        result.map { it.id }.shouldBeEqualTo(listOf("b", "e", "f", "a", "a2", "a3", "d"))
    }

    @Test
    fun `deployOrder works with indirect dependencies`() {
        val external = DocumentObjectBuilder("external", DocumentObjectType.Block).internal(false).mock()
        val internal = DocumentObjectBuilder("internal", DocumentObjectType.Block).internal(true).documentObjectRef(external).mock()
        val tmpl = DocumentObjectBuilder("template", DocumentObjectType.Template).documentObjectRef(internal).mock()
        val list = listOf(tmpl, external)

        val result = subject.deployOrder(list)

        result.map { it.id }.shouldBeEqualTo(listOf("external", "template"))
    }

    @Test
    fun `deployOrder fails on recursive dependency`() {
        val list = listOf(
            DocumentObjectBuilder("a", DocumentObjectType.Block).documentObjectRef("b").mock(),
            DocumentObjectBuilder("b", DocumentObjectType.Block).documentObjectRef("c").mock(),
            DocumentObjectBuilder("c", DocumentObjectType.Block).documentObjectRef("b").mock(),
        )

        val result = assertThrows<RuntimeException> { subject.deployOrder(list) }

        result.message.shouldBeEqualTo("Cannot determine deploy order. Either circular reference or some references are missing.")
    }

    private fun DocumentObjectBuilder.mock(): DocumentObject {
        val obj = this.build()
        every { documentObjectRepository.findOrFail(obj.id) } returns obj
        every { documentObjectRepository.find(obj.id) } returns obj
        return obj
    }
}