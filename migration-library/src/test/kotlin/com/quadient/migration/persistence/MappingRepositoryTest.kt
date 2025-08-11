package com.quadient.migration.persistence

import com.quadient.migration.Postgres
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.api.repository.*
import com.quadient.migration.data.VariableModelRef
import com.quadient.migration.data.VariablePath
import com.quadient.migration.tools.aProjectConfig
import com.quadient.migration.tools.aVariable
import com.quadient.migration.tools.model.aVariableStructureModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

@Postgres
class MappingRepositoryTest {
    val projectConfig = aProjectConfig()
    val documentObjectRepository = mockk<DocumentObjectRepository>()
    val imageRepository = mockk<ImageRepository>()
    val textStyleRepository = mockk<TextStyleRepository>()
    val paraStyleRepository = mockk<ParagraphStyleRepository>()
    val variableRepository = mockk<VariableRepository>()
    val variableStructureRepository = mockk<VariableStructureRepository>()

    private val repo = MappingRepository(
        projectConfig.name,
        documentObjectRepository,
        imageRepository,
        textStyleRepository,
        paraStyleRepository,
        variableRepository,
        variableStructureRepository
    )

    @Test
    fun `apply variable mapping`() {
        every { variableRepository.find("varId") } returns aVariable(id = "varId", name = "Variable Name")
        every { variableStructureRepository.find("varStructId") } returns VariableStructure.fromModel(
            aVariableStructureModel(
                id = "varStructId", name = "Variable Structure Name"
            )
        )
        every { variableStructureRepository.find(any()) } returns null
        every { variableStructureRepository.upsert(any()) } returns Unit
        every { variableRepository.upsert(any()) } returns Unit
        repo.upsert("varId", MappingItem.Variable(name = "new name", dataType = null, inspirePath = "somePath"))

        repo.applyVariableMapping("varId", "name-datastructure")

        verify { variableRepository.upsert(aVariable(id = "varId", name = "new name")) }
        verify {
            variableStructureRepository.upsert(
                VariableStructure.fromModel(
                    aVariableStructureModel(
                        id = "name-datastructure",
                        name = null,
                        structure = mapOf(VariableModelRef("varId") to VariablePath("somePath"))
                    )
                )
            )
        }
    }
}