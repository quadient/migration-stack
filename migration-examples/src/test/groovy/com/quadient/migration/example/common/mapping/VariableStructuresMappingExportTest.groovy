package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.VariableStructureBuilder
import com.quadient.migration.data.Active
import com.quadient.migration.example.Utils
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.shared.DocumentObjectType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Paths

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when

class VariableStructuresMappingExportTest {
    @TempDir
    File dir

    @Test
    void allPossibleFields() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def migration = Utils.mockMigration()

        when(migration.variableStructureRepository.listAll()).thenReturn([
            new VariableStructureBuilder("empty").build(),
            new VariableStructureBuilder("full").name("full").targetFolder("targetFolder1").originLocations(["foo", "bar"]).build(),
        ])

        when(migration.statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any())).thenReturn(new Active())

        VariableStructuresExport.run(migration, mappingFile.toFile())

        def expected = """\
            id,name,targetFolder,status,originLocations (read-only)
            empty,,,Active,[]
            full,full,targetFolder1,Active,[foo; bar]
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }

    @Test
    void exportsOnlySelectedVariableStructures() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def migration = Utils.mockMigration()

        def referencedStructure = new VariableStructureBuilder("referenced").build()
        def unselectedStructure = new VariableStructureBuilder("unselected").build()
        def selectedDocObject = new DocumentObjectBuilder("doc1", DocumentObjectType.Block).build()

        when(migration.projectConfig.getDocumentObjectsToProcess()).thenReturn(["doc1"])
        when(migration.documentObjectRepository.listIds(["doc1"])).thenReturn([selectedDocObject])
        when(migration.referenceCollector.collectAllObjects(selectedDocObject)).thenReturn([referencedStructure] as Set)
        when(migration.variableStructureRepository.listAll()).thenReturn([referencedStructure, unselectedStructure])

        when(migration.statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any())).thenReturn(new Active())

        VariableStructuresExport.run(migration, mappingFile.toFile())

        def expected = """\
            id,name,targetFolder,status,originLocations (read-only)
            referenced,,,Active,[]
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }

    @Test
    void csvFileIsNotPickedUpByVariablesImport() {
        def fileName = "${Mapping.namePrefix("testProject", null)}-variable-structures.csv"

        Assertions.assertFalse(fileName.startsWith(Mapping.variableStructureFileNamePrefix("testProject", null)))
    }
}
