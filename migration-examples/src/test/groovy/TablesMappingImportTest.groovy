import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.TableBuilder
import com.quadient.migration.example.common.mapping.TablesImport
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.TableAction
import com.quadient.migration.shared.TablePdfTaggingRule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Paths

import static com.quadient.migration.service.TableUtilKt.computeFingerprint
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class TablesMappingImportTest {
    @TempDir
    File dir

    @Test
    void upsertsSeparateItemsForDifferentDocumentObjects() {
        def migration = Utils.mockMigration()
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def table10 = new TableBuilder().build()
        def table11 = new TableBuilder().build()
        def table20 = new TableBuilder().build()
        when(migration.documentObjectRepository.find("doc1")).thenReturn(new DocumentObjectBuilder("doc1", DocumentObjectType.Block).content([table10, table11]).build())
        when(migration.documentObjectRepository.find("doc2")).thenReturn(new DocumentObjectBuilder("doc2", DocumentObjectType.Block).content([table20]).build())
        def input = """\
            documentObjectId,tableId,action,pdfTaggingRule,pdfAlternateText,tableName
            doc1,table:0,Keep,Table,Alt text,TableOne
            doc1,table:1,,,,
            doc2,table:0,Flatten,Table,Alt,TableTwo
            """.stripIndent()
        mappingFile.toFile().write(input)

        TablesImport.run(migration, mappingFile)

        verify(migration.mappingRepository).upsertBatch([
            "doc1": new MappingItem.Table(null, [
                new MappingItem.Table.TableEntry("table:0", TableAction.Keep, TablePdfTaggingRule.Table, "Alt text", computeFingerprint(table10), "TableOne"),
                new MappingItem.Table.TableEntry("table:1", TableAction.Keep, null, null, computeFingerprint(table11), null)
            ]),
            "doc2": new MappingItem.Table(null, [
                new MappingItem.Table.TableEntry("table:0", TableAction.Flatten, TablePdfTaggingRule.Table, "Alt", computeFingerprint(table20), "TableTwo")
            ])
        ])
    }
}
