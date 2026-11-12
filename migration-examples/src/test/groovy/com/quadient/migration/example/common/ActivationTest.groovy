package com.quadient.migration.example.common

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.StatusTracking
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ImageBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.VariableBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.VariableStructureBuilder
import com.quadient.migration.example.Utils
import com.quadient.migration.example.common.util.Activation
import com.quadient.migration.service.deploy.utility.ResourceType
import com.quadient.migration.shared.DataType
import com.quadient.migration.shared.DocumentObjectType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.anyMap
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.never
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class ActivationTest {
    private static final List<StatusTracking> STATUSES = [
        status("root", ResourceType.DocumentObject),
        status("child", ResourceType.DocumentObject),
        status("unrelated", ResourceType.DocumentObject),
        status("img", ResourceType.Image),
        status("unrelatedImg", ResourceType.Image),
        status("structure", ResourceType.VariableStructure),
        status("defaultStructure", ResourceType.VariableStructure),
        status("child", ResourceType.Image), // same id, different type - must not be confused
    ]

    @Test
    void withoutSelectionActivatesEverything() {
        def migration = mockWithStatuses([])

        def count = Activation.activate(migration, true, "Activate")

        Assertions.assertEquals(STATUSES.size(), count)
        STATUSES.each { verify(migration.statusTrackingRepository).activeIfNotActive(it.id, it.resourceType, [reason: "Activate"]) }
    }

    @Test
    void withoutDependenciesAndWithoutSelectionFails() {
        def migration = mockWithStatuses([])

        Assertions.assertThrows(IllegalStateException) { Activation.activate(migration, false, "x") }
        verify(migration.statusTrackingRepository, never()).activeIfNotActive(any(), any(), anyMap())
    }

    @Test
    void withDependenciesActivatesSelectedAndAllReferencedObjects() {
        def migration = mockWithStatuses(["root"])
        def root = new DocumentObjectBuilder("root", DocumentObjectType.Template).build()
        def child = new DocumentObjectBuilder("child", DocumentObjectType.Block)
            .variableStructureRef("structure").build()
        when(migration.documentObjectRepository.listIds(["root"])).thenReturn([root])
        when(migration.referenceCollector.collectAllObjects(root)).thenReturn([
            child,
            new ImageBuilder("img").build(),
            new VariableStructureBuilder("structure").build(),
            new VariableBuilder("var").dataType(DataType.String).build(), // variables are not tracked, must be ignored
        ] as Set)
        when(migration.projectConfig.defaultVariableStructure).thenReturn("defaultStructure")

        def count = Activation.activate(migration, true, "Activate")

        def repo = migration.statusTrackingRepository
        verify(repo).activeIfNotActive("root", ResourceType.DocumentObject, [reason: "Activate"])
        verify(repo).activeIfNotActive("child", ResourceType.DocumentObject, [reason: "Activate"])
        verify(repo).activeIfNotActive("img", ResourceType.Image, [reason: "Activate"])
        verify(repo).activeIfNotActive("structure", ResourceType.VariableStructure, [reason: "Activate"])
        verify(repo).activeIfNotActive("defaultStructure", ResourceType.VariableStructure, [reason: "Activate"])
        verify(repo, never()).activeIfNotActive(eq("unrelated"), any(), anyMap())
        verify(repo, never()).activeIfNotActive(eq("unrelatedImg"), any(), anyMap())
        verify(repo, never()).activeIfNotActive("child", ResourceType.Image, [reason: "Activate"])
        Assertions.assertEquals(5, count)
    }

    @Test
    void defaultVariableStructureIsSkippedWhenAllDocumentObjectsHaveExplicitOne() {
        def migration = mockWithStatuses(["root"])
        def root = new DocumentObjectBuilder("root", DocumentObjectType.Template)
            .variableStructureRef("structure").build()
        when(migration.documentObjectRepository.listIds(["root"])).thenReturn([root])
        when(migration.referenceCollector.collectAllObjects(root)).thenReturn([new VariableStructureBuilder("structure").build()] as Set)
        when(migration.projectConfig.defaultVariableStructure).thenReturn("defaultStructure")

        Activation.activate(migration, true, "Activate")

        verify(migration.statusTrackingRepository, never()).activeIfNotActive(eq("defaultStructure"), any(), anyMap())
    }

    private static Migration mockWithStatuses(List<String> selected, List<String> alreadyActive = []) {
        def migration = Utils.mockMigration()
        when(migration.projectConfig.getDocumentObjectsToProcess()).thenReturn(selected)
        when(migration.statusTrackingRepository.listAll()).thenReturn(STATUSES)
        when(migration.statusTrackingRepository.activeIfNotActive(any(), any(), anyMap()))
            .thenAnswer { inv ->
                def id = inv.getArgument(0) as String
                alreadyActive.contains(id) ? null : status(id, inv.getArgument(1) as ResourceType)
            }
        return migration
    }

    private static StatusTracking status(String id, ResourceType type) {
        return new StatusTracking(id, "testProject", type, [])
    }
}
