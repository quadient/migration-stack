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

import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

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

        when(migration.mappingRepository.getAreaMapping("page1")).thenReturn(new MappingItem.Area(null, [:], [:]))
        when(migration.mappingRepository.getAreaMapping("page2")).thenReturn(new MappingItem.Area(null, [:], [:]))
        when(migration.mappingRepository.getAreaMapping("page3")).thenReturn(new MappingItem.Area(null, [:], [:]))

        givenPageExists("page1", ["flow1", "flow2", "flow3"], [false, false, false])
        givenPageExists("page2", ["flowA", "flowB"], [false, false])
        givenPageExists("page3", [null, "beta", null, "delta"])

        def input = """\
            templateId,templateName,pageId,pageName,interactiveFlowName,flowToNextPage,x,y,width,height,contentPreview
            ,,page1,,flow1,false,0.0mm,0.0mm,0.0mm,0.0mm,
            ,,page1,,new flow2,false,0.0mm,0.0mm,0.0mm,0.0mm,
            ,,page1,,flow3,true,0.0mm,0.0mm,0.0mm,0.0mm,
            tmpl2,,page2,,flowA,true,0.0mm,0.0mm,0.0mm,0.0mm,
            tmpl2,,page2,,modified flowB,false,0.0mm,0.0mm,0.0mm,0.0mm,
            tmpl3,,page3,,new alpha,false,0.0mm,0.0mm,0.0mm,0.0mm,
            tmpl3,,page3,,beta,true,0.0mm,0.0mm,0.0mm,0.0mm,
            tmpl3,,page3,,new gamma,false,0.0mm,0.0mm,0.0mm,0.0mm,
            tmpl3,,page3,,modified delta,true,0.0mm,0.0mm,0.0mm,0.0mm,
            """.stripIndent()
        mappingFile.toFile().write(input)

        AreasImport.run(migration, mappingFile)

        verify(migration.mappingRepository).upsert("page1", new MappingItem.Area(null, [0: "flow1", 1: "new flow2", 2: "flow3"], [0: false, 1: false, 2: true]))
        verify(migration.mappingRepository).applyAreaMapping("page1")
        verify(migration.mappingRepository).upsert("page2", new MappingItem.Area(null, [0: "flowA", 1: "modified flowB"], [0: true, 1: false]))
        verify(migration.mappingRepository).applyAreaMapping("page2")
        verify(migration.mappingRepository).upsert("page3", new MappingItem.Area(null, [0: "new alpha", 1: "beta", 2: "new gamma", 3: "modified delta"], [0: false, 1: true, 2: false, 3: true]))
        verify(migration.mappingRepository).applyAreaMapping("page3")
    }

    static Area createArea(String flowName, boolean flowToNextPage) {
        return new Area([], new Position(Size.ofMillimeters(0), Size.ofMillimeters(0), Size.ofMillimeters(0), Size.ofMillimeters(0)), flowName, flowToNextPage)
    }

    void givenPageExists(String pageId, List<String> flowNames, List<Boolean> flowToNextPageValues = null) {
        def values = flowToNextPageValues ?: flowNames.collect { false }
        def content = [flowNames, values].transpose()
                .collect { String flowName, Boolean flowToNextPage -> createArea(flowName, flowToNextPage) }
        when(migration.documentObjectRepository.find(pageId))
                .thenReturn(new DocumentObject(pageId, null, [], new CustomFieldMap([:]), DocumentObjectType.Page, content, false, null, null, null, null, null, null, null, [:], new SkipOptions(false, null, null), null))
    }
}