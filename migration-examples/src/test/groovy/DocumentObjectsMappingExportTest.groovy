import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.data.Active
import com.quadient.migration.example.common.mapping.DocumentObjectsExport
import com.quadient.migration.shared.DocumentObjectType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.jupiter.api.Test

import static org.mockito.Mockito.*

class DocumentObjectsMappingExportTest {
    @TempDir
    File dir

    @Test
    void allPossibleDocumentObjects() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def migration = Utils.mockMigration()

        when(migration.documentObjectRepository.listAll()).thenReturn([
                new DocumentObjectBuilder("empty", DocumentObjectType.Block).build(),
                new DocumentObjectBuilder("should not be listed because internal", DocumentObjectType.Block).internal(true).build(),
                new DocumentObjectBuilder("full", DocumentObjectType.Page)
                        .name("full")
                        .originLocations(["foo", "bar"])
                        .targetFolder("someDir")
                        .variableStructureRef("struct")
                        .baseTemplate("tmpl.wfd")
                        .skip("placeholder", "reason")
                        .build(),
                new DocumentObjectBuilder("overridden empty", DocumentObjectType.Block).build(),
                new DocumentObjectBuilder("overridden full", DocumentObjectType.Page)
                        .name("full")
                        .originLocations(["foo", "bar"])
                        .customFields(["originalName": "originalFull"])
                        .targetFolder("someDir")
                        .variableStructureRef("struct")
                        .baseTemplateRef("tmplRef")
                        .build(),
        ])

        when(migration.statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any())).thenReturn(new Active())

        DocumentObjectsExport.run(migration, mappingFile)

        def expected = """\
            id,name,type,internal,baseTemplate,targetFolder,variableStructureId,status,skip,skipPlaceholder,skipReason,originalName (read-only),originLocations (read-only)
            empty,,Block,false,,,,Active,false,,,,[]
            full,full,Page,false,tmpl.wfd,someDir,struct,Active,true,placeholder,reason,,[foo; bar]
            overridden empty,,Block,false,,,,Active,false,,,,[]
            overridden full,full,Page,false,\$tmplRef,someDir,struct,Active,false,,,originalFull,[foo; bar]
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }

    @Test
    void exportsOnlySelectedDocumentObjectsAndTheirReferences() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def migration = Utils.mockMigration()

        def referencedBlock = new DocumentObjectBuilder("referenced block", DocumentObjectType.Block).build()
        def selectedTemplate = new DocumentObjectBuilder("selected template", DocumentObjectType.Template)
                .documentObjectRef("referenced block")
                .build()
        def unselectedBlock = new DocumentObjectBuilder("unselected block", DocumentObjectType.Block).build()

        when(migration.projectConfig.getDocumentObjectsToProcess()).thenReturn(["selected template"])
        when(migration.documentObjectRepository.listIds(["selected template"])).thenReturn([selectedTemplate])
        when(migration.referenceCollector.collectAllObjects(selectedTemplate)).thenReturn([referencedBlock] as Set)
        when(migration.documentObjectRepository.listAll()).thenReturn([selectedTemplate, referencedBlock, unselectedBlock])

        when(migration.statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any())).thenReturn(new Active())

        DocumentObjectsExport.run(migration, mappingFile)

        def expected = """\
            id,name,type,internal,baseTemplate,targetFolder,variableStructureId,status,skip,skipPlaceholder,skipReason,originalName (read-only),originLocations (read-only)
            referenced block,,Block,false,,,,Active,false,,,,[]
            selected template,,Template,false,,,,Active,false,,,,[]
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }
}
