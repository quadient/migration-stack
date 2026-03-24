import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.builder.DisplayRuleBuilder
import com.quadient.migration.data.Active
import com.quadient.migration.example.common.mapping.DisplayRulesImport
import com.quadient.migration.service.deploy.ResourceType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Paths

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.anyLong
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*

class DisplayRulesMappingImportTest {
    @TempDir
    File dir

    @Test
    void overridesDisplayRuleName() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,internal,baseTemplate,targetFolder,targetId,variableStructureRef,status,originalName (read-only),originLocations (read-only)
            unchanged,,true,,,,,Active,,[]
            kept,keptName,true,,,,,Active,,[]
            overridden,someName,true,,,,,Active,,[]
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDisplayRule(migration, "unchanged", null, true, null, null, null, null)
        givenExistingDisplayRuleMapping(migration, "unchanged", null, null, null, null, null, null)
        givenExistingDisplayRule(migration, "kept", "someName", true, null, null, null, null)
        givenExistingDisplayRuleMapping(migration, "kept", "keptName", null, null, null, null, null)
        givenExistingDisplayRule(migration, "overridden", "previousName", true, null, null, null, null)
        givenExistingDisplayRuleMapping(migration, "overridden", "previousName", null, null, null, null, null)

        DisplayRulesImport.run(migration, mappingFile.toFile())

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.DisplayRule(null, null, null, null, null, true))
        verify(migration.mappingRepository, times(1)).applyDisplayRuleMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.DisplayRule("keptName", null, null, null, null, true))
        verify(migration.mappingRepository, times(1)).applyDisplayRuleMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.DisplayRule("someName", null, null, null, null, true))
        verify(migration.mappingRepository, times(1)).applyDisplayRuleMapping("overridden")
    }

    @Test
    void overridesDisplayRuleInternal() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,internal,baseTemplate,targetFolder,targetId,variableStructureRef,status,originalName (read-only),originLocations (read-only)
            unchanged,,false,,,,,Active,,[]
            kept,,true,,,,,Active,,[]
            overridden,,true,,,,,Active,,[]
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDisplayRule(migration, "unchanged", null, false, null, null, null, null)
        givenExistingDisplayRuleMapping(migration, "unchanged", null, null, null, null, null, null)
        givenExistingDisplayRule(migration, "kept", null, true, null, null, null, null)
        givenExistingDisplayRuleMapping(migration, "kept", null, true, null, null, null, null)
        givenExistingDisplayRule(migration, "overridden", null, false, null, null, null, null)
        givenExistingDisplayRuleMapping(migration, "overridden", null, false, null, null, null, null)

        DisplayRulesImport.run(migration, mappingFile.toFile())

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.DisplayRule(null, null, null, null, null, false))
        verify(migration.mappingRepository, times(1)).applyDisplayRuleMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.DisplayRule(null, null, null, null, null, true))
        verify(migration.mappingRepository, times(1)).applyDisplayRuleMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.DisplayRule(null, null, null, null, null, true))
        verify(migration.mappingRepository, times(1)).applyDisplayRuleMapping("overridden")
    }

    @Test
    void overridesDisplayRuleBaseTemplate() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,internal,baseTemplate,targetFolder,targetId,variableStructureRef,status,originalName (read-only),originLocations (read-only)
            unchanged,,true,,,,,Active,,[]
            kept,,true,keptTemplate,,,,Active,,[]
            overridden,,true,overriddenTemplate,,,,Active,,[]
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDisplayRule(migration, "unchanged", null, true, null, null, null, null)
        givenExistingDisplayRuleMapping(migration, "unchanged", null, null, null, null, null, null)
        givenExistingDisplayRule(migration, "kept", null, true, null, "keptTemplate", null, null)
        givenExistingDisplayRuleMapping(migration, "kept", null, null, null, "keptTemplate", null, null)
        givenExistingDisplayRule(migration, "overridden", null, true, null, "previousTemplate", null, null)
        givenExistingDisplayRuleMapping(migration, "overridden", null, null, null, "previousTemplate", null, null)

        DisplayRulesImport.run(migration, mappingFile.toFile())

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.DisplayRule(null, null, null, null, null, true))
        verify(migration.mappingRepository, times(1)).applyDisplayRuleMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.DisplayRule(null, null, null, "keptTemplate", null, true))
        verify(migration.mappingRepository, times(1)).applyDisplayRuleMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.DisplayRule(null, null, null, "overriddenTemplate", null, true))
        verify(migration.mappingRepository, times(1)).applyDisplayRuleMapping("overridden")
    }

    @Test
    void overridesDisplayRuleTargetFolder() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,internal,baseTemplate,targetFolder,targetId,variableStructureRef,status,originalName (read-only),originLocations (read-only)
            unchanged,,true,,,,,Active,,[]
            kept,,true,,keptFolder,,,Active,,[]
            overridden,,true,,overriddenFolder,,,Active,,[]
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDisplayRule(migration, "unchanged", null, true, null, null, null, null)
        givenExistingDisplayRuleMapping(migration, "unchanged", null, null, null, null, null, null)
        givenExistingDisplayRule(migration, "kept", null, true, null, null, "keptFolder", null)
        givenExistingDisplayRuleMapping(migration, "kept", null, null, null, null, "keptFolder", null)
        givenExistingDisplayRule(migration, "overridden", null, true, null, null, "previousFolder", null)
        givenExistingDisplayRuleMapping(migration, "overridden", null, null, null, null, "previousFolder", null)

        DisplayRulesImport.run(migration, mappingFile.toFile())

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.DisplayRule(null, null, null, null, null, true))
        verify(migration.mappingRepository, times(1)).applyDisplayRuleMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.DisplayRule(null, "keptFolder", null, null, null, true))
        verify(migration.mappingRepository, times(1)).applyDisplayRuleMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.DisplayRule(null, "overriddenFolder", null, null, null, true))
        verify(migration.mappingRepository, times(1)).applyDisplayRuleMapping("overridden")
    }

    @Test
    void overridesDisplayRuleTargetId() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,internal,baseTemplate,targetFolder,targetId,variableStructureRef,status,originalName (read-only),originLocations (read-only)
            unchanged,,true,,,,,Active,,[]
            kept,,true,,,keptId,,Active,,[]
            overridden,,true,,,overriddenId,,Active,,[]
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDisplayRule(migration, "unchanged", null, true, null, null, null, null)
        givenExistingDisplayRuleMapping(migration, "unchanged", null, null, null, null, null, null)
        givenExistingDisplayRule(migration, "kept", null, true, "keptId", null, null, null)
        givenExistingDisplayRuleMapping(migration, "kept", null, null, "keptId", null, null, null)
        givenExistingDisplayRule(migration, "overridden", null, true, "previousId", null, null, null)
        givenExistingDisplayRuleMapping(migration, "overridden", null, null, "previousId", null, null, null)

        DisplayRulesImport.run(migration, mappingFile.toFile())

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.DisplayRule(null, null, null, null, null, true))
        verify(migration.mappingRepository, times(1)).applyDisplayRuleMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.DisplayRule(null, null, "keptId", null, null, true))
        verify(migration.mappingRepository, times(1)).applyDisplayRuleMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.DisplayRule(null, null, "overriddenId", null, null, true))
        verify(migration.mappingRepository, times(1)).applyDisplayRuleMapping("overridden")
    }

    @Test
    void overridesDisplayRuleVariableStructureRef() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,internal,baseTemplate,targetFolder,targetId,variableStructureRef,status,originalName (read-only),originLocations (read-only)
            unchanged,,true,,,,,Active,,[]
            kept,,true,,,,keptVarStruct,Active,,[]
            overridden,,true,,,,overriddenVarStruct,Active,,[]
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDisplayRule(migration, "unchanged", null, true, null, null, null, null)
        givenExistingDisplayRuleMapping(migration, "unchanged", null, null, null, null, null, null)
        givenExistingDisplayRule(migration, "kept", null, true, null, null, null, "keptVarStruct")
        givenExistingDisplayRuleMapping(migration, "kept", null, null, null, null, null, "keptVarStruct")
        givenExistingDisplayRule(migration, "overridden", null, true, null, null, null, "previousVarStruct")
        givenExistingDisplayRuleMapping(migration, "overridden", null, null, null, null, null, "previousVarStruct")

        DisplayRulesImport.run(migration, mappingFile.toFile())

        verify(migration.mappingRepository, times(1)).upsert("unchanged", new MappingItem.DisplayRule(null, null, null, null, null, true))
        verify(migration.mappingRepository, times(1)).applyDisplayRuleMapping("unchanged")
        verify(migration.mappingRepository, times(1)).upsert("kept", new MappingItem.DisplayRule(null, null, null, null, "keptVarStruct", true))
        verify(migration.mappingRepository, times(1)).applyDisplayRuleMapping("kept")
        verify(migration.mappingRepository, times(1)).upsert("overridden", new MappingItem.DisplayRule(null, null, null, null, "overriddenVarStruct", true))
        verify(migration.mappingRepository, times(1)).applyDisplayRuleMapping("overridden")
    }

    @Test
    void updatesDisplayRuleStatus() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,internal,baseTemplate,targetFolder,targetId,variableStructureRef,status,originalName (read-only),originLocations (read-only)
            activateNew,,true,,,,,Active,,[]
            keepActive,,true,,,,,Active,,[]
            deployExisting,,true,,,,,Deployed,,[]
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDisplayRule(migration, "activateNew", null, true, null, null, null, null)
        givenExistingDisplayRuleMapping(migration, "activateNew", null, null, null, null, null, null)
        givenExistingDisplayRule(migration, "keepActive", null, true, null, null, null, null)
        givenExistingDisplayRuleMapping(migration, "keepActive", null, null, null, null, null, null)
        givenExistingDisplayRule(migration, "deployExisting", null, true, null, null, null, null)
        givenExistingDisplayRuleMapping(migration, "deployExisting", null, null, null, null, null, null)

        when(migration.statusTrackingRepository.findLastEventRelevantToOutput(eq("activateNew"), any(), any())).thenReturn(null)
        when(migration.statusTrackingRepository.findLastEventRelevantToOutput(eq("keepActive"), any(), any())).thenReturn(new Active())
        when(migration.statusTrackingRepository.findLastEventRelevantToOutput(eq("deployExisting"), any(), any())).thenReturn(new Active())

        DisplayRulesImport.run(migration, mappingFile.toFile())

        verify(migration.statusTrackingRepository, times(1)).active(eq("activateNew"), eq(ResourceType.DisplayRule), any(Map.class))
        verify(migration.statusTrackingRepository, never()).active(eq("keepActive"), eq(ResourceType.DisplayRule), any(Map.class))
        verify(migration.statusTrackingRepository, times(1)).deployed(eq("deployExisting"), anyString(), anyLong(), eq(ResourceType.DisplayRule), eq((String) null), eq(InspireOutput.Interactive), eq(["reason": "Manual"]))
    }

    @Test
    void ignoresReadOnlyColumns() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,internal,baseTemplate,targetFolder,targetId,variableStructureRef,status,originalName (read-only),originLocations (read-only)
            rule1,myName,false,myTemplate,myFolder,myId,myVarStruct,Active,ignoredOriginalName,[some; location]
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDisplayRule(migration, "rule1", "originalName", true, null, null, null, null)
        givenExistingDisplayRuleMapping(migration, "rule1", null, null, null, null, null, null)

        DisplayRulesImport.run(migration, mappingFile.toFile())

        verify(migration.mappingRepository, times(1)).upsert("rule1", new MappingItem.DisplayRule("myName", "myFolder", "myId", "myTemplate", "myVarStruct", false))
        verify(migration.mappingRepository, times(1)).applyDisplayRuleMapping("rule1")
    }

    static void givenExistingDisplayRule(Migration mig,
                                         String id,
                                         String name,
                                         Boolean internal,
                                         String targetId,
                                         String baseTemplate,
                                         String targetFolder,
                                         String variableStructureRef) {
        def builder = new DisplayRuleBuilder(id)
        if (name != null) builder.name(name)
        if (internal != null) builder.internal(internal)
        if (targetId != null) builder.targetId(targetId)
        if (baseTemplate != null) builder.baseTemplate(baseTemplate)
        if (targetFolder != null) builder.targetFolder(targetFolder)
        if (variableStructureRef != null) builder.variableStructureRef(variableStructureRef)
        when(mig.displayRuleRepository.find(id)).thenReturn(builder.build())
    }

    static void givenExistingDisplayRuleMapping(Migration mig,
                                                String id,
                                                String name,
                                                Boolean internal,
                                                String targetId,
                                                String baseTemplate,
                                                String targetFolder,
                                                String variableStructureRef) {
        when(mig.mappingRepository.getDisplayRuleMapping(id))
                .thenReturn(new MappingItem.DisplayRule(name, targetFolder, targetId, baseTemplate, variableStructureRef, internal))
    }
}

