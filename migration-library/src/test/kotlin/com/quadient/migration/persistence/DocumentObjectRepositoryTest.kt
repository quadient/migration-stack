package com.quadient.migration.persistence

import com.quadient.migration.Postgres
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.Paragraph
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.Dsl.table
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphBuilder
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.data.Active
import com.quadient.migration.service.deploy.ResourceType
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.tools.aBlockDto
import com.quadient.migration.tools.aCell
import com.quadient.migration.tools.aDocumentObjectRepository
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.aRow
import com.quadient.migration.tools.aTable
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeOfSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

@Postgres
class DocumentObjectRepositoryTest {
    private val projectName = aProjectConfig().name
    private val repo = aDocumentObjectRepository()
    private val statusRepo = StatusTrackingRepository(projectName)

    @Test
    fun `roundtrip is correct`() {
        val table = table {
            pdfTaggingRule(com.quadient.migration.shared.TablePdfTaggingRule.Table)
            pdfAlternateText("Table alt text")
            row {
                displayRuleRef("someref")
                cell { }
            }
        }
        val dto = DocumentObjectBuilder("id", DocumentObjectType.Section)
            .customFields(mutableMapOf("f1" to "val1"))
            .originLocations(listOf("test1", "test2"))
            .content(listOf(Paragraph("hi"), DocumentObjectRef("var"), table))
            .metadata("test") {
                string("value")
                integer(123)
                boolean(true)
            }
            .internal(true)
            .targetFolder("acquired")
            .displayRuleRef("someruleref")
            .baseTemplate("someBaseTemplate")
            .skip("reason", "placeholder")
            .variableStructureRef("someVarStructRef")
            .subject("somesubject")
            .build()

        repo.upsert(dto)
        val result = repo.listAll()

        val updatedDto = dto.copy(lastUpdated = result.first().lastUpdated, created = result.first().created)
        result.first().content[2].shouldBeEqualTo(updatedDto.content[2])
        result.first().shouldBeEqualTo(updatedDto)
    }

    @Test
    fun `upsertBatch roundtrip`() {
        // given
        val block1 = DocumentObjectBuilder("block1", DocumentObjectType.Block)
            .internal(true)
            .baseTemplate("baseTemplate1")
            .customFields(mutableMapOf("field1" to "value1"))
            .originLocations(listOf("origin1"))
            .content(listOf(Paragraph("content1")))
            .internal(false)
            .targetFolder("folder1")
            .displayRuleRef("rule1")
            .variableStructureRef("varref1")
            .metadata("meta1") { string("data1") }
            .subject("sub1")
            .area { interactiveFlowName("flow1") }
            .build()

        val block2 = DocumentObjectBuilder("block2", DocumentObjectType.Block)
            .internal(false)
            .baseTemplate("baseTemplate2")
            .customFields(mutableMapOf("field2" to "value2"))
            .originLocations(listOf("origin2"))
            .content(listOf(Paragraph("content2")))
            .internal(true)
            .targetFolder("folder2")
            .displayRuleRef("rule2")
            .variableStructureRef("varref2")
            .metadata("meta2") { string("data2") }
            .subject("sub2")
            .area { interactiveFlowName("flow1") }
            .build()

        // when
        repo.upsertBatch(listOf(block1, block2))

        // then
        val result = repo.listAll()
        result.shouldBeOfSize(2)

        val resultBlock1 = result.first { it.id == "block1" }
        val resultBlock2 = result.first { it.id == "block2" }

        block1.lastUpdated = resultBlock1.lastUpdated
        block1.created = resultBlock1.created
        block2.lastUpdated = resultBlock2.lastUpdated
        block2.created = resultBlock2.created

        resultBlock1.shouldBeEqualTo(block1)
        resultBlock2.shouldBeEqualTo(block2)

        // Update the already existing blocks and assert upsert again
        resultBlock1.targetFolder = "updatedFolder1"
        resultBlock2.targetFolder = "updatedFolder2"

        repo.upsertBatch(listOf(resultBlock1, resultBlock2))

        val updatedResult = repo.listAll()
        updatedResult.shouldBeOfSize(2)

        val updatedBlock1 = updatedResult.first { it.id == "block1" }
        val updatedBlock2 = updatedResult.first { it.id == "block2" }

        updatedBlock1.targetFolder.shouldBeEqualTo("updatedFolder1")
        updatedBlock2.targetFolder.shouldBeEqualTo("updatedFolder2")
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
    fun `upsert tracks active status for new objects and does not insert it again for existing objects`() {
        val input = aBlockDto("myblock", listOf())

        repo.upsert(input)
        repo.upsert(input)

        val result = statusRepo.find(input.id, ResourceType.DocumentObject)

        result?.statusEvents?.shouldBeOfSize(1)
        assertInstanceOf(Active::class.java, result?.statusEvents?.last())
    }

    @Test
    fun `upsert tracks active status for objects that changed from internal to external`() {
        val input = aBlockDto("blockarstarst", internal = true, content = listOf())
        repo.upsert(input)

        input.internal = false
        repo.upsert(input)
        val result = statusRepo.find(input.id, ResourceType.DocumentObject)

        result?.statusEvents?.shouldBeOfSize(1)
        assertInstanceOf(Active::class.java, result?.statusEvents?.last())
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
    fun `paragraph roundtrip`() {
        val paragraph = ParagraphBuilder()
        paragraph.styleRef("style")
        paragraph.addText().appendContent(StringValue("test"))
        val input = aBlockDto("id", content = listOf(paragraph.build()))

        repo.upsert(input)
        val result = repo.listAll()

        val updatedInput = input.copy(lastUpdated = result.first().lastUpdated, created = result.first().created)
        result.first().shouldBeEqualTo(updatedInput)
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
