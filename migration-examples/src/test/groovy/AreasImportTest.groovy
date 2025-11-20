import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.example.common.mapping.AreasImport
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.SkipOptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Paths

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when
import static org.mockito.Mockito.times

class AreasImportTest {
    @TempDir
    File dir

    Migration migration

    @BeforeEach
    void setup() {
        migration = Utils.mockMigration()
    }

    @Test
    void importTest() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        when(migration.mappingRepository.getAreaMapping(any())).thenReturn(new MappingItem.Area(null, [:]))
        givenPageExists("full page", ["test flow2", "test flow3", null, "test flow5"])
        givenPageExists("unreferenced page", ["test flow"])
        def input = """\
            templateId,templateName,pageId,pageName,interactiveFlowName,x,y,width,height,contentPreview
            ,,unreferenced page,,test flow,0.0mm,0.0mm,0.0mm,0.0mm,
            full tmpl,,full page,,test flow2,0.0mm,0.0mm,0.0mm,0.0mm,
            full tmpl,,full page,,test flow3,0.0mm,0.0mm,0.0mm,0.0mm,
            full tmpl,,full page,,new flow name,0.0mm,0.0mm,0.0mm,0.0mm,
            full tmpl,,full page,,new test flow5,0.0mm,0.0mm,0.0mm,0.0mm,
            """.stripIndent()
        mappingFile.toFile().write(input)

        AreasImport.run(migration, mappingFile)

        verify(migration.mappingRepository, times(2))
            .upsert("full page", new MappingItem.Area(null, [2: "new flow name", 3: "new test flow5"]))
        verify(migration.mappingRepository).applyAreaMapping("full page")
    }

    static Area createArea(String flowName) {
        return new Area([], new Position(Size.ofMillimeters(0), Size.ofMillimeters(0), Size.ofMillimeters(0), Size.ofMillimeters(0)), flowName)
    }

    void givenPageExists(String pageId, List<String> flowNames) {
        def content = flowNames.collect { flowName -> createArea(flowName) }
        when(migration.documentObjectRepository.find(pageId))
            .thenReturn(new DocumentObject(pageId, null, [], new CustomFieldMap([:]), DocumentObjectType.Page, content, false, null, null, null, null, null, null, null, [:], new SkipOptions(false, null, null)))
    }
}