import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.TableBuilder
import com.quadient.migration.example.common.mapping.TablesExport
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.TableAction
import com.quadient.migration.shared.TablePdfTaggingRule
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Paths

import static org.mockito.Mockito.when

class TablesMappingExportTest {
    @TempDir
    File dir

    @Test
    void exportsMultipleTablesFromSameDocumentObject() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def migration = Utils.mockMigration()
        def table1 = new TableBuilder().name("First").build()
        def table2 = new TableBuilder()
                .name("Second")
                .pdfTaggingRule(TablePdfTaggingRule.Table)
                .pdfAlternateText("Alt text")
                .action(TableAction.Flatten)
                .build()
        when(migration.documentObjectRepository.listAll()).thenReturn([new DocumentObjectBuilder("doc1", DocumentObjectType.Block).name("My Doc").content([table1, table2]).build()])

        TablesExport.run(migration, mappingFile)

        def expected = """\
            documentObjectId,documentObjectName (read-only),tableId,contentPreview (read-only),tableName,pdfTaggingRule,pdfAlternateText,action
            doc1,My Doc,table:0,0 cols | 0 body rows,First,Default,,Keep
            doc1,My Doc,table:1,0 cols | 0 body rows,Second,Table,Alt text,Flatten
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }

    @Test
    void exportsTablesFromMultipleDocumentObjects() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def migration = Utils.mockMigration()
        when(migration.documentObjectRepository.listAll()).thenReturn([new DocumentObjectBuilder("doc1", DocumentObjectType.Block)
                                                                               .name("First Doc")
                                                                               .content([new TableBuilder().name("T1").build()])
                                                                               .build(),
                                                                       new DocumentObjectBuilder("doc2", DocumentObjectType.Block)
                                                                               .name("Second Doc")
                                                                               .content([new TableBuilder().name("T2").build()])
                                                                               .build()])

        TablesExport.run(migration, mappingFile)

        def expected = """\
            documentObjectId,documentObjectName (read-only),tableId,contentPreview (read-only),tableName,pdfTaggingRule,pdfAlternateText,action
            doc1,First Doc,table:0,0 cols | 0 body rows,T1,Default,,Keep
            doc2,Second Doc,table:0,0 cols | 0 body rows,T2,Default,,Keep
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }
}
