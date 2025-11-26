import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.data.Active
import com.quadient.migration.example.common.mapping.DocumentObjectsExport
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.SkipOptions
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
                new DocumentObject("empty", null, [], new CustomFieldMap([:]), DocumentObjectType.Block, [], false, null, null, null, null, null, null, null, [:], emptySkipOptions()),
                new DocumentObject("should not be listed because internal", null, [], new CustomFieldMap([:]), DocumentObjectType.Block, [], true, null, null, null, null, null, null, null, [:], emptySkipOptions()),
                new DocumentObject("full", "full", ["foo", "bar"], new CustomFieldMap([:]), DocumentObjectType.Page, [], false, "someDir", null, new VariableStructureRef("struct"), "tmpl.wfd", null, null, null, [:], new SkipOptions(true, "placeholder", "reason")),
                new DocumentObject("overridden empty", null, [], new CustomFieldMap([:]), DocumentObjectType.Block, [], false, null, null, null, null, null, null, null, [:], emptySkipOptions()),
                new DocumentObject("overridden full", "full", ["foo", "bar"], new CustomFieldMap([:]), DocumentObjectType.Page, [], false, "someDir", null, new VariableStructureRef("struct"), "tmpl.wfd", null, null, null, [:], emptySkipOptions()),
        ])

        when(migration.statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any())).thenReturn(new Active())

        DocumentObjectsExport.run(migration, mappingFile)

        def expected = """\
            id,name,type,internal,baseTemplate,targetFolder,variableStructureId,status,skip,skipPlaceholder,skipReason,originLocations (read-only)
            empty,,Block,false,,,,Active,false,,,[]
            full,full,Page,false,tmpl.wfd,someDir,struct,Active,true,placeholder,reason,[foo; bar]
            overridden empty,,Block,false,,,,Active,false,,,[]
            overridden full,full,Page,false,tmpl.wfd,someDir,struct,Active,false,,,[foo; bar]
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }

    static SkipOptions emptySkipOptions() {
        return new SkipOptions(false, null, null)
    }
}
