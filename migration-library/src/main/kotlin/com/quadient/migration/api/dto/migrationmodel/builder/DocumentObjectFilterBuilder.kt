package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.DocumentObjectFilter
import com.quadient.migration.shared.DocumentObjectType

class DocumentObjectFilterBuilder {
    var ids: List<String>? = null; private set
    var names: List<String>? = null; private set
    var types: List<DocumentObjectType>? = null; private set

    fun ids(ids: List<String>) = apply { this.ids = ids }
    fun names(names: List<String>) = apply { this.names = names }
    fun types(types: List<DocumentObjectType>) = apply { this.types = types }

    fun build(): DocumentObjectFilter {
        return DocumentObjectFilter(ids, names, types)
    }
}