import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ImageBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.AreaBuilder
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.example.common.mapping.LayoutExport
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

class LayoutExportTest {
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

        LayoutExport.run(migration, mappingFile)

        def expected = """\
            templateId,templateName,pageId,pageName,type,baseTemplateTargetId,interactiveFlowName,flowToNextPage,areaIndex,x,y,width,height,pageWidth,pageHeight,contentPreview (read-only)
            full tmpl,,full page,,Standard,,test flow2,false,0,0mm,0mm,0mm,0mm,,,
            full tmpl,,full page,,Standard,,test flow3,true,1,0mm,0mm,0mm,0mm,,,
            full tmpl,,full page,,Standard,,,false,2,0mm,0mm,0mm,0mm,,,
            full tmpl,,full page,,Standard,,test flow5,false,3,0mm,0mm,0mm,0mm,,,
            ,,unreferenced page,,Standard,,test flow,true,0,0mm,0mm,0mm,0mm,,,
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

        LayoutExport.run(migration, mappingFile)

        def expected = """\
            templateId,templateName,pageId,pageName,type,baseTemplateTargetId,interactiveFlowName,flowToNextPage,areaIndex,x,y,width,height,pageWidth,pageHeight,contentPreview (read-only)
            tmpl with areas,,,,Standard,,Address Content,false,0,0mm,0mm,0mm,0mm,,,
            tmpl with areas,,,,Standard,,,true,1,0mm,0mm,0mm,0mm,,,
            tmpl with areas,,,,Standard,,Footer,false,2,0mm,0mm,0mm,0mm,,,
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

        LayoutExport.run(migration, mappingFile)

        def expected = """\
            templateId,templateName,pageId,pageName,type,baseTemplateTargetId,interactiveFlowName,flowToNextPage,areaIndex,x,y,width,height,pageWidth,pageHeight,contentPreview (read-only)
            tmpl with base,,page with own base,,Standard,\$G1,test flow,false,0,0mm,0mm,0mm,0mm,,,
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }

    @Test
    void exportIncludesBaseTemplatesImportedViaLayoutImport() {
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
        ], null, null, null)
        when(migration.baseTemplateRepository.listAll()).thenReturn([baseTemplate])

        LayoutExport.run(migration, mappingFile)

        def expected = """\
            templateId,templateName,pageId,pageName,type,baseTemplateTargetId,interactiveFlowName,flowToNextPage,areaIndex,x,y,width,height,pageWidth,pageHeight,contentPreview (read-only)
            bt-1,Base template 1,page-1,Page 1,Base,,address,false,0,1cm,1cm,190mm,20mm,210mm,297mm,
            bt-1,Base template 1,page-1,Page 1,Base,,Area 2,true,1,1cm,30mm,190mm,50mm,210mm,297mm,
            bt-1,Base template 1,page-2,Page 2,Base,,Area 1,false,0,0mm,0mm,210mm,99mm,210mm,99mm,
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

        LayoutExport.run(migration, mappingFile)

        def expected = """\
            templateId,templateName,pageId,pageName,type,baseTemplateTargetId,interactiveFlowName,flowToNextPage,areaIndex,x,y,width,height,pageWidth,pageHeight,contentPreview (read-only)
            ,,page with preview,,Standard,,test flow,false,0,0mm,0mm,0mm,0mm,,,docRef: Block One;imageRef: Image One;docRef: Block Two;(+2 more)
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }

    @Test
    void exportsOnlySelectedTemplatesPagesAndTheirReferences() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        when(migration.mappingRepository.getAreaMapping(any())).thenReturn(new MappingItem.Area(null, [:], [:]))

        def selectedPage = new DocumentObjectBuilder("selected page", DocumentObjectType.Page)
                .content([createArea("selected flow")])
                .build()
        def selectedTemplate = new DocumentObjectBuilder("selected tmpl", DocumentObjectType.Template)
                .documentObjectRef("selected page")
                .build()
        def unselectedTemplate = new DocumentObjectBuilder("unselected tmpl", DocumentObjectType.Template)
                .content([createArea("unselected flow")])
                .build()

        when(migration.projectConfig.getDocumentObjectsToProcess()).thenReturn(["selected tmpl"])
        when(migration.documentObjectRepository.listIds(["selected tmpl"])).thenReturn([selectedTemplate])
        when(migration.referenceCollector.collectAllObjects(selectedTemplate)).thenReturn([selectedPage] as Set)
        when((migration.documentObjectRepository as DocumentObjectRepository).list(any())).thenReturn([
            selectedTemplate, selectedPage, unselectedTemplate,
        ])

        LayoutExport.run(migration, mappingFile)

        def expected = """\
            templateId,templateName,pageId,pageName,type,baseTemplateTargetId,interactiveFlowName,flowToNextPage,areaIndex,x,y,width,height,pageWidth,pageHeight,contentPreview (read-only)
            selected tmpl,,selected page,,Standard,,selected flow,false,0,0mm,0mm,0mm,0mm,,,
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }

    @Test
    void exportIncludesAllBaseTemplatesEvenWhenDocumentObjectsAreSelected() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        when(migration.mappingRepository.getAreaMapping(any())).thenReturn(new MappingItem.Area(null, [:], [:]))

        def selectedTemplate = new DocumentObjectBuilder("selected tmpl", DocumentObjectType.Template).build()

        when(migration.projectConfig.getDocumentObjectsToProcess()).thenReturn(["selected tmpl"])
        when(migration.documentObjectRepository.listIds(["selected tmpl"])).thenReturn([selectedTemplate])
        when(migration.referenceCollector.collectAllObjects(selectedTemplate)).thenReturn([] as Set)

        def baseTemplate = new BaseTemplate("bt-1", "Base template 1", [], new CustomFieldMap(new HashMap<String, String>()), null, [
            new BaseTemplatePage("Page 1", Size.ofMillimeters(210), Size.ofMillimeters(297), [
                new BaseTemplateArea("address", new Position(Size.ofCentimeters(1), Size.ofCentimeters(1), Size.ofMillimeters(190), Size.ofMillimeters(20)), false),
            ]),
        ], null, null, null)
        when(migration.baseTemplateRepository.listAll()).thenReturn([baseTemplate])

        LayoutExport.run(migration, mappingFile)

        def expected = """\
            templateId,templateName,pageId,pageName,type,baseTemplateTargetId,interactiveFlowName,flowToNextPage,areaIndex,x,y,width,height,pageWidth,pageHeight,contentPreview (read-only)
            bt-1,Base template 1,page-1,Page 1,Base,,address,false,0,1cm,1cm,190mm,20mm,210mm,297mm,
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
