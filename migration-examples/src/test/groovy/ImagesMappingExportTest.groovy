import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.data.Active
import com.quadient.migration.example.common.mapping.ImagesExport
import com.quadient.migration.shared.ImageType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Paths

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when

class ImagesMappingExportTest {
    @TempDir
    File dir

    @Test
    void allPossibleImages() {
        Path mappingFile = Paths.get(dir.path, "testProject.csv")
        def migration = Utils.mockMigration()

        when(migration.imageRepository.listAll()).thenReturn([
                new Image("empty", null, [], new CustomFieldMap([:]), null, null, null, null),
                new Image("full", "full", ["foo", "bar"], new CustomFieldMap([:]), "sourcePath", null, ImageType.Jpeg, "targetDir"),
                new Image("overridden empty", null, [], new CustomFieldMap([:]), null, null, null, null),
                new Image("overridden full", "full", ["foo", "bar"], new CustomFieldMap([:]), "sourcePath", null, ImageType.Gif, "targetDir"),
        ])

        when(migration.statusTrackingRepository.findLastEventRelevantToOutput(any(), any(), any())).thenReturn(new Active())

        ImagesExport.run(migration, mappingFile)

        def expected = """\
            id,name,sourcePath,imageType,targetFolder,status,originLocation
            empty,,,,,Active,[]
            full,full,sourcePath,Jpeg,targetDir,Active,[foo; bar]
            overridden empty,,,,,Active,[]
            overridden full,full,sourcePath,Gif,targetDir,Active,[foo; bar]
            """.stripIndent()
        Assertions.assertEquals(expected, mappingFile.toFile().text.replaceAll("\\r\\n|\\r", "\n"))
    }
}
