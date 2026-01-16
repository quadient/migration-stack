package com.quadient.migration.data

import com.quadient.migration.persistence.migrationmodel.DisplayRuleEntityRef
import com.quadient.migration.persistence.migrationmodel.DocumentObjectEntityRef
import com.quadient.migration.persistence.migrationmodel.FirstMatchEntity
import com.quadient.migration.persistence.migrationmodel.ImageEntityRef
import com.quadient.migration.persistence.migrationmodel.ParagraphStyleDefOrRefEntity
import com.quadient.migration.persistence.migrationmodel.ParagraphStyleDefinitionEntity
import com.quadient.migration.persistence.migrationmodel.ParagraphStyleEntityRef
import com.quadient.migration.persistence.migrationmodel.StringEntity
import com.quadient.migration.persistence.migrationmodel.TableEntity
import com.quadient.migration.persistence.migrationmodel.TextContentEntity
import com.quadient.migration.persistence.migrationmodel.TextStyleDefOrRefEntity
import com.quadient.migration.persistence.migrationmodel.TextStyleDefinitionEntity
import com.quadient.migration.persistence.migrationmodel.TextStyleEntityRef
import com.quadient.migration.persistence.migrationmodel.VariableEntityRef
import com.quadient.migration.persistence.migrationmodel.VariableStructureEntityRef

sealed interface RefModel {
    val id: String
}

sealed interface TextContentModel {
    companion object {
        fun fromDb(entity: TextContentEntity): TextContentModel = when (entity) {
            is StringEntity -> StringModel.fromDb(entity)
            is VariableEntityRef -> VariableModelRef.fromDb(entity)
            is TableEntity -> TableModel.fromDb(entity)
            is DocumentObjectEntityRef -> DocumentObjectModelRef.fromDb(entity)
            is ImageEntityRef -> ImageModelRef.fromDb(entity)
            is FirstMatchEntity -> FirstMatchModel.fromDb(entity)
        }
    }
}

sealed interface TextStyleDefOrRefModel {
    companion object {
        fun fromDb(entity: TextStyleDefOrRefEntity) = when (entity) {
            is TextStyleDefinitionEntity -> TextStyleDefinitionModel.fromDb(entity)
            is TextStyleEntityRef -> TextStyleModelRef.fromDb(entity)
        }
    }
}
sealed interface ParagraphStyleDefOrRefModel {
    companion object {
        fun fromDb(entity: ParagraphStyleDefOrRefEntity) = when (entity) {
            is ParagraphStyleDefinitionEntity -> ParagraphStyleDefinitionModel.fromDb(entity)
            is ParagraphStyleEntityRef -> ParagraphStyleModelRef.fromDb(entity)
        }
    }
}

data class DocumentObjectModelRef(override val id: String, val displayRuleRef: DisplayRuleModelRef?) : RefModel,
    DocumentContentModel, TextContentModel {
    override fun collectRefs(): List<RefModel> {
        return listOfNotNull(this, this.displayRuleRef)
    }

    companion object {
        fun fromDb(entity: DocumentObjectEntityRef) =
            DocumentObjectModelRef(entity.id, entity.displayRuleRef?.let { DisplayRuleModelRef.fromDb(it) })
    }
}

data class VariableModelRef(override val id: String) : RefModel, TextContentModel {
    companion object {
        fun fromDb(entity: VariableEntityRef) = VariableModelRef(entity.id)
    }
}

data class TextStyleModelRef(override val id: String) : RefModel, TextStyleDefOrRefModel {
    companion object {
        fun fromDb(entity: TextStyleEntityRef) = TextStyleModelRef(entity.id)
    }
}

data class ParagraphStyleModelRef(override val id: String) : RefModel, ParagraphStyleDefOrRefModel {
    companion object {
        fun fromDb(entity: ParagraphStyleEntityRef) = ParagraphStyleModelRef(entity.id)
    }
}

data class DisplayRuleModelRef(override val id: String) : RefModel {
    companion object {
        fun fromDb(entity: DisplayRuleEntityRef) = DisplayRuleModelRef(entity.id)
    }
}

data class ImageModelRef(override val id: String) : RefModel, DocumentContentModel, TextContentModel {
    override fun collectRefs(): List<RefModel> {
        return listOf(this)
    }

    companion object {
        fun fromDb(entity: ImageEntityRef) = ImageModelRef(entity.id)
    }
}

data class VariableStructureModelRef(override val id: String) : RefModel {
    companion object {
        fun fromDb(entity: VariableStructureEntityRef) = VariableStructureModelRef(entity.id)
    }
}

data class StringModel(val value: String) : TextContentModel {
    companion object {
        fun fromDb(entity: StringEntity) = StringModel(entity.value)
    }
}


