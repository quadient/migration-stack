package com.quadient.migration.service.deploy.utility

import com.quadient.migration.api.dto.migrationmodel.BaseTemplateRef
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeNull
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class RefInheritanceServiceTest {
    val documentObjectRepository = mockk<DocumentObjectRepository>()
    val subject = RefInheritanceServiceImpl(documentObjectRepository)
    val allObjects = mutableListOf<DocumentObject>()

    init {
        every { documentObjectRepository.listAll() } answers { allObjects.toList() }
    }

    @Test
    fun `block inherits baseTemplate and variableStructureRef from its page and template ancestors`() {
        val block = DocumentObjectBuilder("block", DocumentObjectType.Block).mock()
        val page = DocumentObjectBuilder("page", DocumentObjectType.Page)
            .documentObjectRef(block)
            .variableStructureRef("vs1")
            .mock()
        DocumentObjectBuilder("template", DocumentObjectType.Template)
            .documentObjectRef(page)
            .baseTemplateRef("bt1")
            .mock()

        val result = subject.apply(listOf(block))

        result.single().baseTemplate.shouldBeEqualTo(BaseTemplateRef("bt1"))
        result.single().variableStructureRef.shouldBeEqualTo(VariableStructureRef("vs1"))
    }

    @Test
    fun `explicit baseTemplate on the object itself takes priority over inherited value`() {
        val block = DocumentObjectBuilder("block", DocumentObjectType.Block).baseTemplateRef("explicit").mock()
        DocumentObjectBuilder("template", DocumentObjectType.Template)
            .documentObjectRef(block)
            .baseTemplateRef("inherited")
            .mock()

        val result = subject.apply(listOf(block))

        result.single().baseTemplate.shouldBeEqualTo(BaseTemplateRef("explicit"))
    }

    @Test
    fun `standalone block with no ancestors has no resolved baseTemplate`() {
        val block = DocumentObjectBuilder("block", DocumentObjectType.Block).mock()

        val result = subject.apply(listOf(block))

        result.single().baseTemplate.shouldBeNull()
        result.single().variableStructureRef.shouldBeNull()
    }

    private fun DocumentObjectBuilder.mock(): DocumentObject {
        val obj = this.build()
        every { documentObjectRepository.find(obj.id) } returns obj
        allObjects.add(obj)
        return obj
    }
}
