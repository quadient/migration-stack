package com.quadient.migration.example.docx

import com.quadient.migration.api.Migration
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.repository.*
import com.quadient.migration.service.Storage

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class DocxTestSupport {
    /** Creates fresh, ordinary mocks for the services used by the DOCX parser. */
    static Migration mockMigration(Map<String, Object> context = [:]) {
        def migration = mock(Migration)
        def variables = mock(VariableRepository)
        def displayRules = mock(DisplayRuleRepository)
        def documentObjects = mock(DocumentObjectRepository)
        def textStyles = mock(TextStyleRepository)
        def paragraphStyles = mock(ParagraphStyleRepository)
        def images = mock(ImageRepository)
        def storage = mock(Storage)
        def projectConfig = mock(ProjectConfig)

        when(migration.variableRepository).thenReturn(variables)
        when(migration.displayRuleRepository).thenReturn(displayRules)
        when(migration.documentObjectRepository).thenReturn(documentObjects)
        when(migration.textStyleRepository).thenReturn(textStyles)
        when(migration.paragraphStyleRepository).thenReturn(paragraphStyles)
        when(migration.imageRepository).thenReturn(images)
        when(migration.storage).thenReturn(storage)
        when(migration.projectConfig).thenReturn(projectConfig)
        when(projectConfig.context).thenReturn(context)
        return migration
    }
}
