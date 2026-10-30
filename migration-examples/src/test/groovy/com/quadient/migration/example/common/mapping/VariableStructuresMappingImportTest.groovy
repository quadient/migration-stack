package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.builder.VariableStructureBuilder
import com.quadient.migration.data.Active
import com.quadient.migration.example.Utils
import com.quadient.migration.service.deploy.utility.ResourceType
import com.quadient.migration.shared.VariablePathData
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Paths

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.anyLong
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*

class VariableStructuresMappingImportTest {
    @TempDir
    File dir

    @Test
    void overridesAllMappableFields() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,targetFolder,status,originLocations (read-only)
            unchanged,,,Active,[]
            overridden,someName,overriddenFolder,Active,[some; location]
            """.stripIndent()
        mappingFile.toFile().write(input)
        when(migration.variableStructureRepository.find("unchanged")).thenReturn(new VariableStructureBuilder("unchanged").build())
        when(migration.mappingRepository.getVariableStructureMapping("unchanged")).thenReturn(new MappingItem.VariableStructure(null, [:], null, null))
        when(migration.variableStructureRepository.find("overridden"))
                .thenReturn(new VariableStructureBuilder("overridden").name("previousName").targetFolder("previousFolder").build())
        when(migration.mappingRepository.getVariableStructureMapping("overridden"))
                .thenReturn(new MappingItem.VariableStructure("previousName", [:], null, "previousFolder"))

        VariableStructuresImport.run(migration, mappingFile.toFile())

        verify(migration.mappingRepository, times(1)).upsertBatch([
            "unchanged" : new MappingItem.VariableStructure(null, [:], null, null),
            "overridden": new MappingItem.VariableStructure("someName", [:], null, "overriddenFolder")
        ])
        verify(migration.mappingRepository, times(1)).applyAllVariableStructureMappings()
    }

    @Test
    void keepsStructureFromObjectWhenNotMapped() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,targetFolder,status,originLocations (read-only)
            struct,newName,newFolder,Active,[]
            """.stripIndent()
        mappingFile.toFile().write(input)
        when(migration.variableStructureRepository.find("struct")).thenReturn(new VariableStructureBuilder("struct")
                .addVariable("var1", "Data.Clients.Value")
                .languageVariable("var1")
                .build())
        when(migration.mappingRepository.getVariableStructureMapping("struct")).thenReturn(new MappingItem.VariableStructure(null, [:], null, null))

        VariableStructuresImport.run(migration, mappingFile.toFile())

        verify(migration.mappingRepository, times(1)).upsertBatch([
            "struct": new MappingItem.VariableStructure("newName", ["var1": new VariablePathData("Data.Clients.Value", null)], new VariableRef("var1"), "newFolder")
        ])
    }

    @Test
    void keepsStructureFromExistingMapping() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,targetFolder,status,originLocations (read-only)
            struct,newName,newFolder,Active,[]
            """.stripIndent()
        mappingFile.toFile().write(input)
        when(migration.variableStructureRepository.find("struct")).thenReturn(new VariableStructureBuilder("struct")
                .addVariable("objectVar", "Data.Object")
                .languageVariable("objectVar")
                .build())
        when(migration.mappingRepository.getVariableStructureMapping("struct")).thenReturn(new MappingItem.VariableStructure(null,
                ["mappedVar": new VariablePathData("Data.Mapped", null)], new VariableRef("mappedVar"), null))

        VariableStructuresImport.run(migration, mappingFile.toFile())

        verify(migration.mappingRepository, times(1)).upsertBatch([
            "struct": new MappingItem.VariableStructure("newName", ["mappedVar": new VariablePathData("Data.Mapped", null)], new VariableRef("mappedVar"), "newFolder")
        ])
    }

    @Test
    void failsForUnknownVariableStructure() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,targetFolder,status,originLocations (read-only)
            missing,,,Active,[]
            """.stripIndent()
        mappingFile.toFile().write(input)

        def exception = Assertions.assertThrows(Exception) { VariableStructuresImport.run(migration, mappingFile.toFile()) }

        Assertions.assertEquals("VariableStructure with id missing not found", exception.message)
    }

    @Test
    void updatesVariableStructureStatus() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,targetFolder,status,originLocations (read-only)
            activateNew,,,Active,[]
            keepActive,,,Active,[]
            deployExisting,,,Deployed,[]
            """.stripIndent()
        mappingFile.toFile().write(input)
        for (id in ["activateNew", "keepActive", "deployExisting"]) {
            when(migration.variableStructureRepository.find(id)).thenReturn(new VariableStructureBuilder(id).build())
            when(migration.mappingRepository.getVariableStructureMapping(id)).thenReturn(new MappingItem.VariableStructure(null, [:], null, null))
        }

        when(migration.statusTrackingRepository.findLastEventRelevantToOutput(eq("activateNew"), any(), any())).thenReturn(null)
        when(migration.statusTrackingRepository.findLastEventRelevantToOutput(eq("keepActive"), any(), any())).thenReturn(new Active())
        when(migration.statusTrackingRepository.findLastEventRelevantToOutput(eq("deployExisting"), any(), any())).thenReturn(new Active())

        VariableStructuresImport.run(migration, mappingFile.toFile())

        verify(migration.statusTrackingRepository, times(1)).active(eq("activateNew"), eq(ResourceType.VariableStructure), any(Map.class))
        verify(migration.statusTrackingRepository, never()).active(eq("keepActive"), eq(ResourceType.VariableStructure), any(Map.class))
        verify(migration.statusTrackingRepository, times(1)).deployed(eq("deployExisting"), anyString(), anyLong(), eq(ResourceType.VariableStructure), eq((String) null), eq(InspireOutput.Interactive), eq(["reason": "Manual"]))
    }
}
