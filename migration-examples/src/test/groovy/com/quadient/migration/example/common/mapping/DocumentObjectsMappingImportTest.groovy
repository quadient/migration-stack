package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.BaseTemplateRef
import com.quadient.migration.api.dto.migrationmodel.LiteralBaseTemplatePath
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.data.Active
import com.quadient.migration.example.Utils
import com.quadient.migration.service.deploy.utility.ResourceType
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.SkipOptions
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.jupiter.api.Test

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.anyLong
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*

class DocumentObjectsMappingImportTest {
    @TempDir
    File dir

    @Test
    void overridesAllMappableFields() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject-variables.csv")
        def input = """\
            id,name,type,internal,originLocation,baseTemplate,targetFolder,variableStructureId,status,skip,skipPlaceholder,skipReason
            unchanged,,Block,false,[],,,,Active
            overridden,someName,Page,true,[],overriddenTemplate,overriddenFolder,overriddenVarStructure,Active,true,placeholder,reason
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDocumentObject(migration, "unchanged", null, false, null, null, null, null)
        givenExistingDocumentObjectMapping(migration, "unchanged", null, null, null, null, null, null)
        givenExistingDocumentObject(migration, "overridden", "previousName", false, "previousTemplate", "previousFolder", DocumentObjectType.Template, "previousVarStructure")
        givenExistingDocumentObjectMapping(migration, "overridden", "previousName", false, "previousTemplate", "previousFolder", DocumentObjectType.Template, "previousVarStructure")

        DocumentObjectsImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsertBatch([
            "unchanged" : new MappingItem.DocumentObject(null, false, null, null, DocumentObjectType.Block, null, new SkipOptions(false, null, null)),
            "overridden": new MappingItem.DocumentObject("someName", true, new LiteralBaseTemplatePath("overriddenTemplate"), "overriddenFolder", DocumentObjectType.Page, "overriddenVarStructure", new SkipOptions(true, "placeholder", "reason"))
        ])
        verify(migration.mappingRepository, times(1)).applyAllDocumentObjectMappings()
    }

    @Test
    void overridesDocumentObjectBaseTemplateRef() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject-variables.csv")
        def input = """\
            id,name,type,internal,originLocation,baseTemplate,targetFolder,variableStructureId,status
            atPrefixed,,Block,false,[],@someBaseTemplateId,,,Active
            dollarPrefixed,,Block,false,[],\$anotherBaseTemplateId,,,Active
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDocumentObject(migration, "atPrefixed", null, false, null, null, null, null)
        givenExistingDocumentObjectMapping(migration, "atPrefixed", null, null, null, null, null, null)
        givenExistingDocumentObject(migration, "dollarPrefixed", null, false, null, null, null, null)
        givenExistingDocumentObjectMapping(migration, "dollarPrefixed", null, null, null, null, null, null)

        DocumentObjectsImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsertBatch([
            "atPrefixed"    : new MappingItem.DocumentObject(null, false, new BaseTemplateRef("someBaseTemplateId"), null, DocumentObjectType.Block, null, new SkipOptions(false, null, null)),
            "dollarPrefixed": new MappingItem.DocumentObject(null, false, new BaseTemplateRef("anotherBaseTemplateId"), null, DocumentObjectType.Block, null, new SkipOptions(false, null, null))
        ])
        verify(migration.mappingRepository, times(1)).applyAllDocumentObjectMappings()
    }

    @Test
    void clearsVariableStructureRefWhenCsvValueIsBlank() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject-variables.csv")
        def input = """\
            id,name,type,internal,originLocation,baseTemplate,targetFolder,variableStructureId,status
            cleared,,Block,false,[],,,,Active
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDocumentObject(migration, "cleared", null, false, null, null, null, "previousVarStructure")
        givenExistingDocumentObjectMapping(migration, "cleared", null, null, null, null, null, "previousVarStructure")

        DocumentObjectsImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsertBatch([
            "cleared": new MappingItem.DocumentObject(null, false, null, null, DocumentObjectType.Block, null, new SkipOptions(false, null, null))
        ])
        verify(migration.mappingRepository, times(1)).applyAllDocumentObjectMappings()
    }

    @Test
    void updatesDocumentObjectStatus() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,type,internal,baseTemplate,targetFolder,variableStructureId,status
            activateUntracked,,Block,true,,,,
            activateUntrackedExplicit,,Block,true,,,,Active
            deployUntracked,,Block,true,,,,Deployed
            keepActive,,Block,false,,,,Active
            deployExisting,,Block,false,,,,Deployed
            """.stripIndent()
        mappingFile.toFile().write(input)
        ["activateUntracked", "activateUntrackedExplicit", "deployUntracked"].each {
            givenExistingDocumentObject(migration, it, null, true, null, null, null, null)
            givenExistingDocumentObjectMapping(migration, it, null, null, null, null, null, null)
            when(migration.statusTrackingRepository.findLastEventRelevantToOutput(eq(it), any(), any())).thenReturn(null)
        }
        ["keepActive", "deployExisting"].each {
            givenExistingDocumentObject(migration, it, null, false, null, null, null, null)
            givenExistingDocumentObjectMapping(migration, it, null, null, null, null, null, null)
            when(migration.statusTrackingRepository.findLastEventRelevantToOutput(eq(it), any(), any())).thenReturn(new Active())
        }

        DocumentObjectsImport.run(migration, mappingFile)

        verify(migration.statusTrackingRepository, times(1)).active(eq("activateUntracked"), eq(ResourceType.DocumentObject), any(Map.class))
        verify(migration.statusTrackingRepository, times(1)).active(eq("activateUntrackedExplicit"), eq(ResourceType.DocumentObject), any(Map.class))
        verify(migration.statusTrackingRepository, times(1)).deployed(eq("deployUntracked"), anyString(), anyLong(), eq(ResourceType.DocumentObject), eq((IcmPath) null), eq(InspireOutput.Interactive), eq(["reason": "Manual"]))
        verify(migration.statusTrackingRepository, never()).active(eq("keepActive"), eq(ResourceType.DocumentObject), any(Map.class))
        verify(migration.statusTrackingRepository, times(1)).deployed(eq("deployExisting"), anyString(), anyLong(), eq(ResourceType.DocumentObject), eq((IcmPath) null), eq(InspireOutput.Interactive), eq(["reason": "Manual"]))
    }

    @Test
    void convertsInternalDocumentObjectToExternal() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def input = """\
            id,name,type,internal,baseTemplate,targetFolder,variableStructureId,status
            converted,,Block,false,,,,
            """.stripIndent()
        mappingFile.toFile().write(input)
        givenExistingDocumentObject(migration, "converted", null, true, null, null, null, null)
        givenExistingDocumentObjectMapping(migration, "converted", null, null, null, null, null, null)

        DocumentObjectsImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(1)).upsertBatch([
            "converted": new MappingItem.DocumentObject(null, false, null, null, DocumentObjectType.Block, null, new SkipOptions(false, null, null))
        ])
        verify(migration.mappingRepository, times(1)).applyAllDocumentObjectMappings()
    }

    static void givenExistingDocumentObject(Migration mig, String id, String name, Boolean internal, String baseTemplate, String targetFolder, DocumentObjectType type, String varStructureRef) {
        def builder = new DocumentObjectBuilder(id, type ?: DocumentObjectType.Block)
        if (name != null) {
            builder.name(name)
        }
        if (internal != null) {
            builder.internal(internal)
        }
        if (targetFolder != null) {
            builder.targetFolder(targetFolder)
        }
        if (varStructureRef != null) {
            builder.variableStructureRef(varStructureRef)
        }
        if (baseTemplate != null) {
            builder.baseTemplate(baseTemplate)
        }

        when(mig.documentObjectRepository.find(id)).thenReturn(builder.build())
    }

    static void givenExistingDocumentObjectMapping(Migration mig,
                                                   String id,
                                                   String name,
                                                   Boolean internal,
                                                   String baseTemplate,
                                                   String targetFolder,
                                                   DocumentObjectType type,
                                                   String varStructureRef) {
        def baseTemplateLocation = baseTemplate ? new LiteralBaseTemplatePath(baseTemplate) : null
        when(mig.mappingRepository.getDocumentObjectMapping(id))
                .thenReturn(new MappingItem.DocumentObject(name, internal, baseTemplateLocation, targetFolder, type, varStructureRef, null))
    }
}
