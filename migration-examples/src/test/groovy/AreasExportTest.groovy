import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ImageBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.AreaBuilder
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.example.common.mapping.AreasExport
import com.quadient.migration.shared.BaseTemplateArea
import com.quadient.migration.shared.BaseTemplatePage
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
            templateId,templateName (read-only),pageId,pageName (read-only),pageWidth (read-only),pageHeight (read-only),interactiveFlowName,flowToNextPage,x (read-only),y (read-only),width (read-only),height (read-only),type,targetId,contentPreview (read-only)
            full tmpl,,full page,,,,test flow2,false,0mm,0mm,0mm,0mm,Standard,,
            full tmpl,,full page,,,,test flow3,true,0mm,0mm,0mm,0mm,Standard,,
            full tmpl,,full page,,,,,false,0mm,0mm,0mm,0mm,Standard,,
            full tmpl,,full page,,,,test flow5,false,0mm,0mm,0mm,0mm,Standard,,
            ,,unreferenced page,,,,test flow,true,0mm,0mm,0mm,0mm,Standard,,
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }

    @Test
    void exportTemplateDirectAreas() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        when(migration.mappingRepository.getAreaMapping(any())).thenReturn(new MappingItem.Area(null, [:], [:]))
        when((migration.documentObjectRepository as DocumentObjectRepository).list(any())).thenReturn([
            new DocumentObjectBuilder("tmpl with areas", DocumentObjectType.Template)
                    .content([createArea("Address Content"), createArea(null, true), createArea("Footer")])
                    .build(),
        ])

        AreasExport.run(migration, mappingFile)

        def expected = """\
            templateId,templateName (read-only),pageId,pageName (read-only),pageWidth (read-only),pageHeight (read-only),interactiveFlowName,flowToNextPage,x (read-only),y (read-only),width (read-only),height (read-only),type,targetId,contentPreview (read-only)
            tmpl with areas,,,,,,Address Content,false,0mm,0mm,0mm,0mm,Standard,,
            tmpl with areas,,,,,,,true,0mm,0mm,0mm,0mm,Standard,,
            tmpl with areas,,,,,,Footer,false,0mm,0mm,0mm,0mm,Standard,,
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }

    @Test
    void exportUsesPageOrTemplateBaseTemplateAsTargetId() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        when(migration.mappingRepository.getAreaMapping(any())).thenReturn(new MappingItem.Area(null, [:], [:]))
        when((migration.documentObjectRepository as DocumentObjectRepository).list(any())).thenReturn([
            new DocumentObjectBuilder("tmpl with base", DocumentObjectType.Template)
                    .baseTemplateRef("G2")
                    .documentObjectRef("page with own base")
                    .build(),
            new DocumentObjectBuilder("page with own base", DocumentObjectType.Page)
                    .baseTemplateRef("G1")
                    .content([createArea("test flow")])
                    .build(),
        ])

        AreasExport.run(migration, mappingFile)

        def expected = """\
            templateId,templateName (read-only),pageId,pageName (read-only),pageWidth (read-only),pageHeight (read-only),interactiveFlowName,flowToNextPage,x (read-only),y (read-only),width (read-only),height (read-only),type,targetId,contentPreview (read-only)
            tmpl with base,,page with own base,,,,test flow,false,0mm,0mm,0mm,0mm,Standard,\$G1,
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }

    @Test
    void exportIncludesBaseTemplatesImportedViaAreasImport() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        when(migration.mappingRepository.getAreaMapping(any())).thenReturn(new MappingItem.Area(null, [:], [:]))
        when((migration.documentObjectRepository as DocumentObjectRepository).list(any())).thenReturn([])

        def baseTemplate = new BaseTemplate("bt-1", "Base template 1", [], new CustomFieldMap(new HashMap<String, String>()), null, [
            new BaseTemplatePage("Page 1", Size.ofMillimeters(210), Size.ofMillimeters(297), [
                new BaseTemplateArea("address", new Position(Size.ofCentimeters(1), Size.ofCentimeters(1), Size.ofMillimeters(190), Size.ofMillimeters(20)), false),
                new BaseTemplateArea("Area 2", new Position(Size.ofCentimeters(1), Size.ofMillimeters(30), Size.ofMillimeters(190), Size.ofMillimeters(50)), true),
            ]),
            new BaseTemplatePage("Page 2", Size.ofMillimeters(210), Size.ofMillimeters(99), [
                new BaseTemplateArea("Area 1", new Position(Size.ofMillimeters(0), Size.ofMillimeters(0), Size.ofMillimeters(210), Size.ofMillimeters(99)), false),
            ]),
        ], null, null)
        when(migration.baseTemplateRepository.listAll()).thenReturn([baseTemplate])

        AreasExport.run(migration, mappingFile)

        def expected = """\
            templateId,templateName (read-only),pageId,pageName (read-only),pageWidth (read-only),pageHeight (read-only),interactiveFlowName,flowToNextPage,x (read-only),y (read-only),width (read-only),height (read-only),type,targetId,contentPreview (read-only)
            bt-1,Base template 1,page-1,Page 1,210mm,297mm,address,false,1cm,1cm,190mm,20mm,Base,,
            bt-1,Base template 1,page-1,Page 1,210mm,297mm,Area 2,true,1cm,30mm,190mm,50mm,Base,,
            bt-1,Base template 1,page-2,Page 2,210mm,99mm,Area 1,false,0mm,0mm,210mm,99mm,Base,,
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }

    @Test
    void exportTruncatesAreaContentPreviewAfterThreeParts() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        when(migration.mappingRepository.getAreaMapping(any())).thenReturn(new MappingItem.Area(null, [:], [:]))
        when((migration.documentObjectRepository as DocumentObjectRepository).list(any()))
                .thenReturn([new DocumentObjectBuilder("page with preview", DocumentObjectType.Page)
                                     .content([createArea("test flow",
                                             false,
                                             [new DocumentObjectRef("doc-1"),
                                              new ImageRef("img-1"),
                                              new DocumentObjectRef("doc-2"),
                                              new ImageRef("img-2"),
                                              new DocumentObjectRef("doc-3")] as List<DocumentContent>)])
                                     .build()])
        when(migration.documentObjectRepository.find("doc-1")).thenReturn(new DocumentObjectBuilder("doc-1", DocumentObjectType.Block).name("Block One").build())
        when(migration.documentObjectRepository.find("doc-2")).thenReturn(new DocumentObjectBuilder("doc-2", DocumentObjectType.Block).name("Block Two").build())
        when(migration.documentObjectRepository.find("doc-3")).thenReturn(new DocumentObjectBuilder("doc-3", DocumentObjectType.Block).name("Block Three").build())
        when(migration.imageRepository.find("img-1")).thenReturn(new ImageBuilder("img-1").name("Image One").build())
        when(migration.imageRepository.find("img-2")).thenReturn(new ImageBuilder("img-2").name("Image Two").build())

        AreasExport.run(migration, mappingFile)

        def expected = """\
            templateId,templateName (read-only),pageId,pageName (read-only),pageWidth (read-only),pageHeight (read-only),interactiveFlowName,flowToNextPage,x (read-only),y (read-only),width (read-only),height (read-only),type,targetId,contentPreview (read-only)
            ,,page with preview,,,,test flow,false,0mm,0mm,0mm,0mm,Standard,,docRef: Block One;imageRef: Image One;docRef: Block Two;(+2 more)
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }

    static Area createArea(String flowName, Boolean flowToNextPage = false, List<DocumentContent> content = null) {
        def areaBuilder = new AreaBuilder()
                .position(new Position(Size.ofMillimeters(0), Size.ofMillimeters(0), Size.ofMillimeters(0), Size.ofMillimeters(0)))
                .flowToNextPage(flowToNextPage)

        if (content != null) {
            areaBuilder.content(content)
        }

        if (flowName != null) {
            areaBuilder.interactiveFlowName(flowName)
        }

        return areaBuilder.build()
    }
}