import com.quadient.migration.api.Migration
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ImageRepository
import com.quadient.migration.api.repository.MappingRepository
import com.quadient.migration.api.repository.ParagraphStyleRepository
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.api.repository.TextStyleRepository
import com.quadient.migration.api.repository.VariableRepository
import com.quadient.migration.api.repository.VariableStructureRepository

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

static Migration mockMigration() {
    def migration = mock(Migration.class)

    def projectConfig = mock(ProjectConfig.class)
    when(migration.projectConfig).thenReturn(projectConfig)
    when(migration.projectConfig.name).thenReturn("testProject")

    def varRepo = mock(VariableRepository.class)
    def structureRepo = mock(VariableStructureRepository.class)
    def mappingRepo = mock(MappingRepository.class)
    def docObjectRepo = mock(DocumentObjectRepository.class)
    def imageRepo = mock(ImageRepository.class)
    def statusTrackingRepo = mock(StatusTrackingRepository.class)
    def textStyleRepo = mock(TextStyleRepository.class)
    def paraStyleRepo = mock(ParagraphStyleRepository.class)

    when(migration.paragraphStyleRepository).thenReturn(paraStyleRepo)
    when(migration.textStyleRepository).thenReturn(textStyleRepo)
    when(migration.statusTrackingRepository).thenReturn(statusTrackingRepo)
    when(migration.imageRepository).thenReturn(imageRepo)
    when(migration.documentObjectRepository).thenReturn(docObjectRepo)
    when(migration.variableRepository).thenReturn(varRepo)
    when(migration.variableStructureRepository).thenReturn(structureRepo)
    when(migration.mappingRepository).thenReturn(mappingRepo)

    return migration
}
