import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.builder.BaseTemplateBuilder
import com.quadient.migration.data.Active
import com.quadient.migration.example.common.mapping.BaseTemplatesImport
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

class BaseTemplatesMappingImportTest {
    @TempDir
    File dir

    @Test
    void overridesAllMappableFields() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,targetFolder,variableStructureRef,status,originLocations (read-only)
            unchanged,,,,Active,[]
            overridden,someName,overriddenFolder,overriddenVarStruct,Active,[]
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingBaseTemplate(migration, "unchanged", null, null, null)
        givenExistingBaseTemplateMapping(migration, "unchanged", null, null, null)
        givenExistingBaseTemplate(migration, "overridden", "previousName", "previousFolder", "previousVarStruct")
        givenExistingBaseTemplateMapping(migration, "overridden", "previousName", "previousFolder", "previousVarStruct")

        BaseTemplatesImport.run(migration, mappingFile.toFile())

        verify(migration.mappingRepository, times(1)).upsertBatch([
            "unchanged" : new MappingItem.BaseTemplate(null, null, [], null),
            "overridden": new MappingItem.BaseTemplate("someName", "overriddenFolder", [], "overriddenVarStruct")
        ])
        verify(migration.mappingRepository, times(1)).applyAllBaseTemplateMappings()
    }

    @Test
    void updatesBaseTemplateStatus() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,targetFolder,variableStructureRef,status,originLocations (read-only)
            activateNew,,,,Active,[]
            keepActive,,,,Active,[]
            deployExisting,,,,Deployed,[]
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingBaseTemplate(migration, "activateNew", null, null, null)
        givenExistingBaseTemplateMapping(migration, "activateNew", null, null, null)
        givenExistingBaseTemplate(migration, "keepActive", null, null, null)
        givenExistingBaseTemplateMapping(migration, "keepActive", null, null, null)
        givenExistingBaseTemplate(migration, "deployExisting", null, null, null)
        givenExistingBaseTemplateMapping(migration, "deployExisting", null, null, null)

        when(migration.statusTrackingRepository.findLastEventRelevantToOutput(eq("activateNew"), any(), any())).thenReturn(null)
        when(migration.statusTrackingRepository.findLastEventRelevantToOutput(eq("keepActive"), any(), any())).thenReturn(new Active())
        when(migration.statusTrackingRepository.findLastEventRelevantToOutput(eq("deployExisting"), any(), any())).thenReturn(new Active())

        BaseTemplatesImport.run(migration, mappingFile.toFile())

        verify(migration.statusTrackingRepository, times(1)).active(eq("activateNew"), eq(ResourceType.BaseTemplate), any(Map.class))
        verify(migration.statusTrackingRepository, never()).active(eq("keepActive"), eq(ResourceType.BaseTemplate), any(Map.class))
        verify(migration.statusTrackingRepository, times(1)).deployed(eq("deployExisting"), anyString(), anyLong(), eq(ResourceType.BaseTemplate), eq((String) null), eq(InspireOutput.Interactive), eq(["reason": "Manual"]))
    }

    @Test
    void ignoresReadOnlyColumns() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,targetFolder,variableStructureRef,status,originLocations (read-only)
            baseTemplate1,myName,myFolder,myVarStruct,Active,[some; location]
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingBaseTemplate(migration, "baseTemplate1", "originalName", null, null)
        givenExistingBaseTemplateMapping(migration, "baseTemplate1", null, null, null)

        BaseTemplatesImport.run(migration, mappingFile.toFile())

        verify(migration.mappingRepository, times(1)).upsertBatch([
            "baseTemplate1": new MappingItem.BaseTemplate("myName", "myFolder", [], "myVarStruct")
        ])
        verify(migration.mappingRepository, times(1)).applyAllBaseTemplateMappings()
    }

    static void givenExistingBaseTemplate(Migration mig,
                                          String id,
                                          String name,
                                          String targetFolder,
                                          String variableStructureRef) {
        def builder = new BaseTemplateBuilder(id)
        if (name != null) builder.name(name)
        if (targetFolder != null) builder.targetFolder(targetFolder)
        if (variableStructureRef != null) builder.variableStructureRef(variableStructureRef)
        when(mig.baseTemplateRepository.find(id)).thenReturn(builder.build())
    }

    static void givenExistingBaseTemplateMapping(Migration mig,
                                                 String id,
                                                 String name,
                                                 String targetFolder,
                                                 String variableStructureRef) {
        when(mig.mappingRepository.getBaseTemplateMapping(id))
                .thenReturn(new MappingItem.BaseTemplate(name, targetFolder, [], variableStructureRef))
    }
}
