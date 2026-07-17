import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.BaseTemplateRef
import com.quadient.migration.api.dto.migrationmodel.LiteralBaseTemplatePath
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.builder.DisplayRuleBuilder
import com.quadient.migration.data.Active
import com.quadient.migration.example.common.mapping.DisplayRulesImport
import com.quadient.migration.service.deploy.utility.ResourceType
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
    void overridesAllMappableFields() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,internal,baseTemplate,targetFolder,targetId,variableStructureRef,status,originalName (read-only),originLocations (read-only)
            unchanged,,true,,,,,Active,,[]
            overridden,someName,false,overriddenTemplate,overriddenFolder,overriddenId,overriddenVarStruct,Active,,[]
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDisplayRule(migration, "unchanged", null, true, null, null, null, null)
        givenExistingDisplayRuleMapping(migration, "unchanged", null, null, null, null, null, null)
        givenExistingDisplayRule(migration, "overridden", "previousName", true, "previousId", "previousTemplate", "previousFolder", "previousVarStruct")
        givenExistingDisplayRuleMapping(migration, "overridden", "previousName", true, "previousId", "previousTemplate", "previousFolder", "previousVarStruct")

        DisplayRulesImport.run(migration, mappingFile.toFile())

        verify(migration.mappingRepository, times(1)).upsertBatch([
            "unchanged" : new MappingItem.DisplayRule(null, null, null, null, null, true),
            "overridden": new MappingItem.DisplayRule("someName", "overriddenFolder", "overriddenId", new LiteralBaseTemplatePath("overriddenTemplate"), "overriddenVarStruct", false)
        ])
        verify(migration.mappingRepository, times(1)).applyAllDisplayRuleMappings()
    }

    @Test
    void overridesDisplayRuleBaseTemplateRef() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,internal,baseTemplate,targetFolder,targetId,variableStructureRef,status,originalName (read-only),originLocations (read-only)
            atPrefixed,,true,@someBaseTemplateId,,,,Active,,[]
            dollarPrefixed,,true,\$anotherBaseTemplateId,,,,Active,,[]
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDisplayRule(migration, "atPrefixed", null, true, null, null, null, null)
        givenExistingDisplayRuleMapping(migration, "atPrefixed", null, null, null, null, null, null)
        givenExistingDisplayRule(migration, "dollarPrefixed", null, true, null, null, null, null)
        givenExistingDisplayRuleMapping(migration, "dollarPrefixed", null, null, null, null, null, null)

        DisplayRulesImport.run(migration, mappingFile.toFile())

        verify(migration.mappingRepository, times(1)).upsertBatch([
            "atPrefixed"    : new MappingItem.DisplayRule(null, null, null, new BaseTemplateRef("someBaseTemplateId"), null, true),
            "dollarPrefixed": new MappingItem.DisplayRule(null, null, null, new BaseTemplateRef("anotherBaseTemplateId"), null, true)
        ])
        verify(migration.mappingRepository, times(1)).applyAllDisplayRuleMappings()
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

        verify(migration.mappingRepository, times(1)).upsertBatch([
            "rule1": new MappingItem.DisplayRule("myName", "myFolder", "myId", new LiteralBaseTemplatePath("myTemplate"), "myVarStruct", false)
        ])
        verify(migration.mappingRepository, times(1)).applyAllDisplayRuleMappings()
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
        def baseTemplateLocation = baseTemplate ? new LiteralBaseTemplatePath(baseTemplate) : null
        when(mig.mappingRepository.getDisplayRuleMapping(id))
                .thenReturn(new MappingItem.DisplayRule(name, targetFolder, targetId, baseTemplateLocation, variableStructureRef, internal))
    }
}
