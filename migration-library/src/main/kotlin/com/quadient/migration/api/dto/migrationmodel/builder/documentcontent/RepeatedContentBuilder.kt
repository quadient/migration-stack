package com.quadient.migration.api.dto.migrationmodel.builder.documentcontent

import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.RepeatedContent
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentContentBuilderBase
import com.quadient.migration.shared.VariablePath

class RepeatedContentBuilder(private val variablePath: VariablePath) : DocumentContentBuilderBase<RepeatedContentBuilder> {
    override val content = mutableListOf<DocumentContent>()

    /**
     * Builds the [RepeatedContent] instance.
     * @return The constructed [RepeatedContent] instance.
     */
    fun build(): RepeatedContent = RepeatedContent(
        variablePath = variablePath,
        content = content,
    )
}
