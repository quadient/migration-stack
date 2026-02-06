package com.quadient.migration.service

import com.quadient.migration.Postgres
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.tools.aBlockDto
import com.quadient.migration.tools.aDisplayRuleRepository
import com.quadient.migration.tools.aDocumentObjectRepository
import com.quadient.migration.tools.aAttachmentRepository
import com.quadient.migration.tools.aImageRepository
import com.quadient.migration.tools.aParaStyleRepository
import com.quadient.migration.tools.aParagraphStyle
import com.quadient.migration.tools.aTextStyle
import com.quadient.migration.tools.aTextStyleRepository
import com.quadient.migration.tools.aVariableRepository
import com.quadient.migration.tools.aVariableStructureRepository
import com.quadient.migration.tools.model.aBlock
import com.quadient.migration.tools.shouldBeEmpty
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeOfSize
import org.junit.jupiter.api.Test

@Postgres
class ReferenceValidatorTest {
    val documentObjectRepository = aDocumentObjectRepository()
    val variableRepository = aVariableRepository()
    val paraStyleRepository = aParaStyleRepository()
    val textStyleRepository = aTextStyleRepository()
    val dataStructureRepository = aVariableStructureRepository()
    val displayRuleRepository = aDisplayRuleRepository()
    val imageRepository = aImageRepository()
    val attachmentRepository = aAttachmentRepository()

    val docRepo = aDocumentObjectRepository()
    val paraStyleRepo = aParaStyleRepository()
    val textStyleRepo = aTextStyleRepository()

    val subject = ReferenceValidator(
        documentObjectRepository,
        variableRepository,
        textStyleRepository,
        paraStyleRepository,
        dataStructureRepository,
        displayRuleRepository,
        imageRepository,
        attachmentRepository,
    )

    @Test
    fun `validates leaf block`() {
        val input = aBlock(id = "1", type = DocumentObjectType.Block)

        val result = subject.validate(input, mutableSetOf())

        result.validatedRefs.shouldBeEmpty()
        result.missingRefs.shouldBeEmpty()
    }

    @Test
    fun `validates block with missing ref`() {
        val missingRef = DocumentObjectRef("obj1")
        val input = aBlock(
            id = "1", type = DocumentObjectType.Block, content = listOf(missingRef)
        )

        val result = subject.validate(input, mutableSetOf())

        result.validatedRefs.shouldBeEmpty()
        result.missingRefs.shouldBeOfSize(1)
        result.missingRefs.first().shouldBeEqualTo(missingRef)
    }

    @Test
    fun `valid block with nested dependencies`() {
        val blockRef1 = DocumentObjectRef("obj1")
        val blockRef2 = DocumentObjectRef("obj2")
        val blockRef11 = DocumentObjectRef("obj11")
        val blockRef21 = DocumentObjectRef("obj21")
        val input = aBlock(
            id = "1", type = DocumentObjectType.Block, content = listOf(blockRef1, blockRef2)
        )
        docRepo.upsert(aBlockDto("obj2"))
        docRepo.upsert(aBlockDto("obj11"))
        docRepo.upsert(aBlockDto("obj21"))
        docRepo.upsert(
            aBlockDto(
                "obj1", content = listOf(
                    blockRef11, 
                    blockRef21
                )
            )
        )

        val result = subject.validate(input, mutableSetOf())

        result.validatedRefs.shouldBeOfSize(4)
        result.missingRefs.shouldBeEmpty()
        result.validatedRefs.shouldBeEqualTo(listOf(blockRef1, blockRef2, blockRef11, blockRef21))
    }

    @Test
    fun `valid block with nested dependencies and one missing in the end`() {
        val blockRef1 = DocumentObjectRef("obj1")
        val blockRef2 = DocumentObjectRef("obj2")
        val blockRef11 = DocumentObjectRef("obj11")
        val blockRef21 = DocumentObjectRef("obj21")
        val missingVarRef = DocumentObjectRef("obj111")
        val input = aBlock(
            id = "1", type = DocumentObjectType.Block, content = listOf(blockRef1, blockRef2)
        )

        docRepo.upsert(aBlockDto("obj2"))
        docRepo.upsert(aBlockDto("obj21"))
        docRepo.upsert(
            aBlockDto(
                "obj1", content = listOf(
                    blockRef11, 
                    blockRef21
                )
            )
        )
        docRepo.upsert(
            aBlockDto(
                "obj11", content = listOf(missingVarRef)
            )
        )

        val result = subject.validate(input, mutableSetOf())

        result.validatedRefs.shouldBeOfSize(4)
        result.missingRefs.shouldBeOfSize(1)
        result.validatedRefs.shouldBeEqualTo(listOf(blockRef1, blockRef2, blockRef11, blockRef21))
        result.missingRefs.shouldBeEqualTo(listOf(missingVarRef))
    }

    @Test
    fun `takes published if draft is missing`() {
        val blockRef1 = DocumentObjectRef("block1")
        val missingRef = DocumentObjectRef("missing")
        docRepo.upsert(
            aBlockDto(
                "block1", content = listOf(missingRef)
            )
        )
        val input = aBlock(id = "1", type = DocumentObjectType.Block, content = listOf(blockRef1))

        val result = subject.validate(input, mutableSetOf())

        result.validatedRefs.shouldBeOfSize(1)
        result.missingRefs.shouldBeOfSize(1)
        result.validatedRefs.first().shouldBeEqualTo(blockRef1)
        result.missingRefs.first().shouldBeEqualTo(missingRef)
    }

    @Test
    fun `validates text and paragraph style refs`() {
        textStyleRepo.upsert(aTextStyle("text1", definition = TextStyleRef("textref1")))
        textStyleRepo.upsert(aTextStyle("textref1", definition = TextStyleRef("textref2")))
        textStyleRepo.upsert(aTextStyle("textref2"))
        paraStyleRepo.upsert(aParagraphStyle("para1", definition = ParagraphStyleRef("pararef1")))
        paraStyleRepo.upsert(aParagraphStyle("pararef1", definition = ParagraphStyleRef("pararef2")))
        paraStyleRepo.upsert(aParagraphStyle("pararef2"))

        val result = subject.validateAll()

        result.missingRefs.shouldBeEmpty()
    }

    @Test
    fun `validates text and paragraph style refs when refs are missing`() {
        textStyleRepo.upsert(aTextStyle("text1", definition = TextStyleRef("textref1")))
        textStyleRepo.upsert(aTextStyle("textref1", definition = TextStyleRef("textref2")))
        paraStyleRepo.upsert(aParagraphStyle("para1", definition = ParagraphStyleRef("pararef1")))
        paraStyleRepo.upsert(aParagraphStyle("pararef1", definition = ParagraphStyleRef("pararef2")))

        val result = subject.validateAll()

        result.missingRefs.shouldBeOfSize(2)
        result.missingRefs[0].id.shouldBeEqualTo("pararef2")
        result.missingRefs[1].id.shouldBeEqualTo("textref2")
    }
}