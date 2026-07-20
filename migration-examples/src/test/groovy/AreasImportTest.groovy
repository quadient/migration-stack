import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.AreaBuilder
import com.quadient.migration.example.common.mapping.AreasImport
import com.quadient.migration.shared.BaseTemplateArea
import com.quadient.migration.shared.BaseTemplatePage
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.SkipOptions
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.ArgumentCaptor

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

        verify(migration.mappingRepository).upsertBatch([
            "page1": new MappingItem.Area(null, [0: "flow1", 1: "new flow2", 2: "flow3"], [0: false, 1: false, 2: true]),
            "page2": new MappingItem.Area(null, [0: "flowA", 1: "modified flowB"], [0: true, 1: false]),
            "page3": new MappingItem.Area(null, [0: "new alpha", 1: "beta", 2: "new gamma", 3: "modified delta"], [0: false, 1: true, 2: false, 3: true])
        ])
        verify(migration.mappingRepository).applyAllAreaMappings()
    }

    @Test
    void importTemplateDirectAreas() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")

        when(migration.mappingRepository.getAreaMapping("tmpl1")).thenReturn(new MappingItem.Area(null, [:], [:]))

        when(migration.documentObjectRepository.find("tmpl1")).thenReturn(
            new DocumentObjectBuilder("tmpl1", DocumentObjectType.Template)
                    .content([createArea("Address Content", false), createArea(null, false), createArea("Footer", false)])
                    .build()
        )

        def input = """\
            templateId,templateName,pageId,pageName,interactiveFlowName,flowToNextPage,x,y,width,height,contentPreview
            tmpl1,,,,Updated Address,true,0.0mm,0.0mm,0.0mm,0.0mm,
            tmpl1,,,,New Header,false,0.0mm,0.0mm,0.0mm,0.0mm,
            tmpl1,,,,Footer,true,0.0mm,0.0mm,0.0mm,0.0mm,
            """.stripIndent()
        mappingFile.toFile().write(input)

        AreasImport.run(migration, mappingFile)

        verify(migration.mappingRepository).upsertBatch([
            "tmpl1": new MappingItem.Area(null, [0: "Updated Address", 1: "New Header", 2: "Footer"], [0: true, 1: false, 2: true])
        ])
        verify(migration.mappingRepository).applyAllAreaMappings()
    }

    @Test
    void importSetsPageBaseTemplateFromTargetIdOnStandardRows() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")

        when(migration.mappingRepository.getAreaMapping("page1")).thenReturn(new MappingItem.Area(null, [:], [:]))
        when(migration.mappingRepository.getDocumentObjectMapping("page1")).thenReturn(
            new MappingItem.DocumentObject(null, null, null, null, null, null, null)
        )
        when(migration.mappingRepository.getDocumentObjectMapping("tmpl1")).thenReturn(
            new MappingItem.DocumentObject(null, null, null, null, null, null, null)
        )
        givenPageExists("page1", ["flow1", "flow2"], [false, false])

        def input = """\
            templateId,templateName,pageId,pageName,interactiveFlowName,flowToNextPage,x,y,width,height,type,targetId,contentPreview
            tmpl1,,page1,,flow1,false,0.0mm,0.0mm,0.0mm,0.0mm,Standard,\$G1,
            tmpl1,,page1,,flow2,false,0.0mm,0.0mm,0.0mm,0.0mm,Standard,\$G1,
            """.stripIndent()
        mappingFile.toFile().write(input)

        AreasImport.run(migration, mappingFile)

        verify(migration.mappingRepository).upsertBatch([
            "page1": new MappingItem.DocumentObject(null, false, new BaseTemplateRef("G1"), null, null, null, new SkipOptions(false, null, null)),
            "tmpl1": new MappingItem.DocumentObject(null, null, new BaseTemplateRef("G1"), null, null, null, null)
        ])
        verify(migration.mappingRepository).applyAllDocumentObjectMappings()
    }

    @Test
    void importCreatesNewBaseTemplateFromBaseRows() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")

        when(migration.mappingRepository.getAreaMapping("page1")).thenReturn(new MappingItem.Area(null, [:], [:]))
        when(migration.mappingRepository.getDocumentObjectMapping("page1")).thenReturn(
            new MappingItem.DocumentObject(null, null, null, null, null, null, null)
        )
        when(migration.mappingRepository.getDocumentObjectMapping("tmpl1")).thenReturn(
            new MappingItem.DocumentObject(null, null, null, null, null, null, null)
        )
        givenPageExists("page1", ["flow1"], [false])
        when(migration.baseTemplateRepository.find("G1")).thenReturn(null)
        when(migration.mappingRepository.getBaseTemplateMapping("G1")).thenReturn(new MappingItem.BaseTemplate(null, null, []))

        def input = """\
            templateId,templateName,pageId,pageName,pageWidth,pageHeight,interactiveFlowName,flowToNextPage,x,y,width,height,type,targetId,contentPreview
            tmpl1,,page1,,,,flow1,false,0.0mm,0.0mm,0.0mm,0.0mm,Standard,\$G1,
            G1,Base template 1,G1-P1,Page group 1,210mm,297mm,G1-P1.Area1,false,1cm,1cm,190mm,20mm,Base,,
            G1,Base template 1,G1-P1,Page group 1,210mm,297mm,G1-P1.Area2,true,1cm,30mm,190mm,50mm,Base,,
            """.stripIndent()
        mappingFile.toFile().write(input)

        AreasImport.run(migration, mappingFile)

        def baseTemplateCaptor = ArgumentCaptor.forClass(BaseTemplate.class)
        verify(migration.baseTemplateRepository).upsert(baseTemplateCaptor.capture())
        Assertions.assertEquals("G1", baseTemplateCaptor.value.id)

        def created = new BaseTemplatePage("Page group 1", Size.ofMillimeters(210), Size.ofMillimeters(297), [
            new BaseTemplateArea("G1-P1.Area1", new Position(Size.ofCentimeters(1), Size.ofCentimeters(1), Size.ofMillimeters(190), Size.ofMillimeters(20)), false),
            new BaseTemplateArea("G1-P1.Area2", new Position(Size.ofCentimeters(1), Size.ofMillimeters(30), Size.ofMillimeters(190), Size.ofMillimeters(50)), true),
        ])
        verify(migration.mappingRepository).upsertBatch([
            "G1": new MappingItem.BaseTemplate("Base template 1", null, [created])
        ])
        verify(migration.mappingRepository).applyAllBaseTemplateMappings()
    }

    @Test
    void importUpdatesExistingBaseTemplateMappingButKeepsItsOtherFields() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")

        def existing = new BaseTemplate("G1", "Old name", ["origin.wfd"], new CustomFieldMap(new HashMap<String, String>()), "target/folder", [], null, null)
        when(migration.baseTemplateRepository.find("G1")).thenReturn(existing)
        when(migration.mappingRepository.getBaseTemplateMapping("G1")).thenReturn(new MappingItem.BaseTemplate(null, null, []))

        def input = """\
            templateId,templateName,pageId,pageName,pageWidth,pageHeight,interactiveFlowName,flowToNextPage,x,y,width,height,type,targetId,contentPreview
            G1,,G1-P1,Page group 1,210mm,297mm,G1-P1.Area1,false,1cm,1cm,190mm,20mm,Base,,
            """.stripIndent()
        mappingFile.toFile().write(input)

        AreasImport.run(migration, mappingFile)

        def created = new BaseTemplatePage("Page group 1", Size.ofMillimeters(210), Size.ofMillimeters(297), [
            new BaseTemplateArea("G1-P1.Area1", new Position(Size.ofCentimeters(1), Size.ofCentimeters(1), Size.ofMillimeters(190), Size.ofMillimeters(20)), false),
        ])
        verify(migration.mappingRepository).upsertBatch([
            "G1": new MappingItem.BaseTemplate("Old name", "target/folder", [created])
        ])
        verify(migration.mappingRepository).applyAllBaseTemplateMappings()
    }

    static Area createArea(String flowName, boolean flowToNextPage) {
        def areaBuilder = new AreaBuilder()
                .position(new Position(Size.ofMillimeters(0), Size.ofMillimeters(0), Size.ofMillimeters(0), Size.ofMillimeters(0)))
                .flowToNextPage(flowToNextPage)

        if (flowName != null) {
            areaBuilder.interactiveFlowName(flowName)
        }

        return areaBuilder.build()
    }

    void givenPageExists(String pageId, List<String> flowNames, List<Boolean> flowToNextPageValues = null) {
        def values = flowToNextPageValues ?: flowNames.collect { false }
        def content = [flowNames, values].transpose()
                .collect { String flowName, Boolean flowToNextPage -> createArea(flowName, flowToNextPage) }
        when(migration.documentObjectRepository.find(pageId)).thenReturn(new DocumentObjectBuilder(pageId, DocumentObjectType.Page).content(content).build())
    }
}
