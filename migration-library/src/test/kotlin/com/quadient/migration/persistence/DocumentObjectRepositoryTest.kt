package com.quadient.migration.persistence

import com.quadient.migration.Postgres
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.Paragraph
import com.quadient.migration.api.dto.migrationmodel.Paragraph.Text
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.Dsl.table
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphBuilder
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.tools.aBlockDto
import com.quadient.migration.tools.aCell
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.aRow
import com.quadient.migration.tools.aTable
import com.quadient.migration.tools.model.aDocumentObjectInternalRepository
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeOfSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@Postgres
class DocumentObjectRepositoryTest {
    private val internalRepo = aDocumentObjectInternalRepository()
    private val repo = DocumentObjectRepository(internalRepo)

    @Test
    fun `roundtrip is correct`() {
        val table = table {
            row {
                displayRuleRef("someref")
                cell { }
            }
        }
        val dto = DocumentObjectBuilder("id", DocumentObjectType.Section)
            .customFields(mutableMapOf("f1" to "val1"))
            .originLocations(listOf("test1", "test2"))
            .content(listOf(Paragraph("hi"), DocumentObjectRef("var"), table))
            .internal(true)
            .targetFolder("acquired")
            .displayRuleRef("someruleref")
            .baseTemplate("someBaseTemplate")
            .build()

        repo.upsert(dto)
        val result = repo.listAll()

        result.first().content[2].shouldBeEqualTo(dto.content[2])
        result.first().shouldBeEqualTo(dto)
    }

    @Test
    fun `upsert creates a block`() {
        val input = aBlockDto("myblock", listOf())

        repo.upsert(input)

        val result = repo.listAll()
        assertEquals(1, result.size)
        assertEquals(input.id, result.first().id)
    }

    @Test
    fun `upsert updates a block correctly`() {
        val input = aBlockDto("test", listOf(), targetFolder = "text")
        repo.upsert(input)
        val input2 = aBlockDto("test", listOf(), targetFolder = "SomethingElse")

        repo.upsert(input2)

        val result = repo.find(input2.id)
        result!!.targetFolder.shouldBeEqualTo("SomethingElse")
    }

    @Test
    fun `reports usedBy correctly`() {
        repo.upsert(aBlockDto("parent1", listOf(DocumentObjectRef("subjectBlock"))))
        repo.upsert(aBlockDto("parent2", listOf(DocumentObjectRef("subjectBlock"))))
        repo.upsert(aBlockDto("parent3", listOf(DocumentObjectRef("subjectBlock"))))
        repo.upsert(aBlockDto("notParent1", listOf(DocumentObjectRef("nonSubjectBlock"))))
        repo.upsert(aBlockDto("notParent2", listOf(DocumentObjectRef("nonSubjectBlock"))))
        repo.upsert(aBlockDto("subjectBlock", listOf()))

        val result = repo.findUsages("subjectBlock")

        result.shouldBeOfSize(3)
        result.map{ it.id }.shouldBeEqualTo(listOf("parent1", "parent2", "parent3"))
    }

    @Test
    fun `unsupported document object type`() {
        val input = aBlockDto("myblock", listOf(), type = DocumentObjectType.Unsupported)

        repo.upsert(input)

        val result = repo.listAll()
        assertEquals(1, result.size)
        assertEquals(input.id, result.first().id)
        assertEquals(DocumentObjectType.Unsupported, result.first().type)
    }

    @Test
    fun `paragraph roundtrip`() {
        val paragraph = ParagraphBuilder()
        paragraph.styleRef = ParagraphStyleRef("style")
        paragraph.addText().appendContent(StringValue("test"))
        val input = aBlockDto("id", content = listOf(paragraph.build()))

        repo.upsert(input)
        val result = repo.listAll()

        result.first().shouldBeEqualTo(input)
    }

    @Test
    fun `findUsedBy`() {
        val table = aTable(
            rows = listOf(
                aRow(
                    cells = listOf(
                        aCell(
                            content = listOf(
                                DocumentObjectRef("subject1"),
                                DocumentObjectRef("subject1"),
                            )
                        )
                    )
                )
            )
        )
        val tableBlock = aBlockDto("parablock", content = listOf(table))
        repo.upsert(tableBlock)
        repo.upsert(aBlockDto("unused", listOf(aTable())))

        val result = repo.findUsages("subject1")

        result.shouldBeOfSize(1)
        result.first().id.shouldBeEqualTo("parablock")
    }
}

