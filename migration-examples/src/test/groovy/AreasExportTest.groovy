import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.AreaBuilder
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
    java.io.File dir

    Migration migration

    @BeforeEach
    void setup() {
        migration = Utils.mockMigration()
    }

    @Test
    void export() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        when(migration.mappingRepository.getAreaMapping(any())).thenReturn(new MappingItem.Area(null, [:], [:]))
        when((migration.documentObjectRepository as DocumentObjectRepository).list(any())).thenReturn([
            new DocumentObjectBuilder("empty tmpl", DocumentObjectType.Template).build(),
            new DocumentObjectBuilder("unreferenced page", DocumentObjectType.Page).content([createArea("test flow", true)]).build(),
            new DocumentObjectBuilder("full tmpl", DocumentObjectType.Template).documentObjectRef("full page").build(),
            new DocumentObjectBuilder("full page", DocumentObjectType.Page)
                    .content([createArea("test flow2"), createArea("test flow3", true), createArea(null), createArea("test flow5")])
                    .build(),
        ])

        AreasExport.run(migration, mappingFile)

        def expected = """\
            templateId (read-only),templateName (read-only),pageId,pageName (read-only),interactiveFlowName,flowToNextPage,x (read-only),y (read-only),width (read-only),height (read-only),contentPreview (read-only)
            full tmpl,,full page,,test flow2,false,0.0mm,0.0mm,0.0mm,0.0mm,
            full tmpl,,full page,,test flow3,true,0.0mm,0.0mm,0.0mm,0.0mm,
            full tmpl,,full page,,,false,0.0mm,0.0mm,0.0mm,0.0mm,
            full tmpl,,full page,,test flow5,false,0.0mm,0.0mm,0.0mm,0.0mm,
            ,,unreferenced page,,test flow,true,0.0mm,0.0mm,0.0mm,0.0mm,
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }

    static Area createArea(String flowName, Boolean flowToNextPage = false) {
        def areaBuilder = new AreaBuilder()
                .position(new Position(Size.ofMillimeters(0), Size.ofMillimeters(0), Size.ofMillimeters(0), Size.ofMillimeters(0)))
                .flowToNextPage(flowToNextPage)

        if (flowName != null) {
            areaBuilder.interactiveFlowName(flowName)
        }

        return areaBuilder.build()
    }
}
