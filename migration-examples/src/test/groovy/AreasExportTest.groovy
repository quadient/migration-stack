import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.example.common.mapping.AreasExport
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Paths

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when

class AreasExportTest {
    @TempDir
    File dir

    Migration migration

    @BeforeEach
    void setup() {
        migration = Utils.mockMigration()
    }

    @Test
    void export() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        when(migration.mappingRepository.getAreaMapping(any())).thenReturn(new MappingItem.Area(null, [:]))
        when((migration.documentObjectRepository as DocumentObjectRepository).list(any())).thenReturn([
            new DocumentObject("empty tmpl", null, [], new CustomFieldMap([:]), DocumentObjectType.Template, [], false, null, null, null, null, null, null, null),
            new DocumentObject("unreferenced page", null, [], new CustomFieldMap([:]), DocumentObjectType.Page, [createArea("test flow")], false, null, null, null, null, null, null, null),
            new DocumentObject("full tmpl", null, [], new CustomFieldMap([:]), DocumentObjectType.Template, [new DocumentObjectRef("full page")], false, null, null, null, null, null, null, null),
            new DocumentObject("full page", null, [], new CustomFieldMap([:]), DocumentObjectType.Page, [createArea("test flow2"), createArea("test flow3"), createArea(null), createArea("test flow5")], false, null, null, null, null, null, null, null),
        ])

        AreasExport.run(migration, mappingFile)

        def expected = """\
            templateId,templateName,pageId,pageName,interactiveFlowName,x,y,width,height,contentPreview
            full tmpl,,full page,,test flow2,0.0mm,0.0mm,0.0mm,0.0mm,
            full tmpl,,full page,,test flow3,0.0mm,0.0mm,0.0mm,0.0mm,
            full tmpl,,full page,,,0.0mm,0.0mm,0.0mm,0.0mm,
            full tmpl,,full page,,test flow5,0.0mm,0.0mm,0.0mm,0.0mm,
            ,,unreferenced page,,test flow,0.0mm,0.0mm,0.0mm,0.0mm,
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text)
    }

    static Area createArea(String flowName) {
        return new Area([], new Position(Size.ofMillimeters(0), Size.ofMillimeters(0), Size.ofMillimeters(0), Size.ofMillimeters(0)), flowName)
    }
}