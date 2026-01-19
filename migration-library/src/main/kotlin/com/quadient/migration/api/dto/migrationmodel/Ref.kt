package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.data.DisplayRuleModelRef
import com.quadient.migration.data.DocumentObjectModelRef
import com.quadient.migration.data.FirstMatchModel
import com.quadient.migration.data.HyperlinkModel
import com.quadient.migration.data.ImageModelRef
import com.quadient.migration.data.ParagraphStyleDefOrRefModel
import com.quadient.migration.data.ParagraphStyleDefinitionModel
import com.quadient.migration.data.ParagraphStyleModelRef
import com.quadient.migration.data.RefModel
import com.quadient.migration.data.StringModel
import com.quadient.migration.data.TableModel
import com.quadient.migration.data.TextContentModel
import com.quadient.migration.data.TextStyleDefOrRefModel
import com.quadient.migration.data.TextStyleDefinitionModel
import com.quadient.migration.data.TextStyleModelRef
import com.quadient.migration.data.VariableModelRef
import com.quadient.migration.data.VariableStructureModelRef
import com.quadient.migration.persistence.migrationmodel.DisplayRuleEntityRef
import com.quadient.migration.persistence.migrationmodel.DocumentObjectEntityRef
import com.quadient.migration.persistence.migrationmodel.HyperlinkEntity
import com.quadient.migration.persistence.migrationmodel.ImageEntityRef
import com.quadient.migration.persistence.migrationmodel.ParagraphStyleEntityRef
import com.quadient.migration.persistence.migrationmodel.StringEntity
import com.quadient.migration.persistence.migrationmodel.TextStyleEntityRef
import com.quadient.migration.persistence.migrationmodel.VariableEntityRef
import com.quadient.migration.persistence.migrationmodel.VariableStructureEntityRef

sealed interface Ref {
    val id: String

    companion object {
        fun fromModel(model: RefModel) = when (model) {
            is DocumentObjectModelRef -> DocumentObjectRef.fromModel(model)
            is VariableModelRef -> VariableRef.fromModel(model)
            is TextStyleModelRef -> TextStyleRef.fromModel(model)
            is ParagraphStyleModelRef -> ParagraphStyleRef.fromModel(model)
            is DisplayRuleModelRef -> DisplayRuleRef.fromModel(model)
            is ImageModelRef -> ImageRef.fromModel(model)
            is VariableStructureModelRef -> VariableStructureRef.fromModel(model)
        }
    }
}

sealed interface TextContent {
    companion object {
        fun fromModel(model: TextContentModel) = when (model) {
            is DocumentObjectModelRef -> DocumentObjectRef.fromModel(model)
            is ImageModelRef -> ImageRef.fromModel(model)
            is StringModel -> StringValue.fromModel(model)
            is TableModel -> Table.fromModel(model)
            is VariableModelRef -> VariableRef.fromModel(model)
            is FirstMatchModel -> FirstMatch.fromModel(model)
            is HyperlinkModel -> Hyperlink.fromModel(model)
        }
    }
}

sealed interface TextStyleDefOrRef {
    companion object {
        fun fromModel(model: TextStyleDefOrRefModel) = when (model) {
            is TextStyleDefinitionModel -> TextStyleDefinition.fromModel(model)
            is TextStyleModelRef -> TextStyleRef.fromModel(model)
        }
    }

    fun toDb() = when (this) {
        is TextStyleDefinition -> this.toDb()
        is TextStyleRef -> this.toDb()
    }

    fun toModel() = when (this) {
        is TextStyleDefinition -> this.toModel()
        is TextStyleRef -> this.toModel()
    }
}

sealed interface ParagraphStyleDefOrRef {
    companion object {
        fun fromModel(model: ParagraphStyleDefOrRefModel) = when (model) {
            is ParagraphStyleDefinitionModel -> ParagraphStyleDefinition.fromModel(model)
            is ParagraphStyleModelRef -> ParagraphStyleRef.fromModel(model)
        }
    }

    fun toDb() = when (this) {
        is ParagraphStyleDefinition -> this.toDb()
        is ParagraphStyleRef -> this.toDb()
    }

    fun toModel() = when (this) {
        is ParagraphStyleDefinition -> this.toModel()
        is ParagraphStyleRef -> this.toModel()
    }
}

data class DocumentObjectRef(override val id: String, val displayRuleRef: DisplayRuleRef? = null) : Ref,
    DocumentContent, TextContent {

    constructor(id: String) : this(id, null)

    companion object {
        fun fromModel(model: DocumentObjectModelRef) =
            DocumentObjectRef(model.id, model.displayRuleRef?.let { DisplayRuleRef.fromModel(it) })
    }

    fun toDb() = DocumentObjectEntityRef(id, displayRuleRef?.toDb())
    fun toModel() = DocumentObjectModelRef(id, displayRuleRef?.toModel())
}

data class VariableRef(override val id: String) : Ref, TextContent {
    companion object {
        fun fromModel(model: VariableModelRef) = VariableRef(model.id)
    }

    fun toModel() = VariableModelRef(id)
    fun toDb() = VariableEntityRef(id)
}

data class TextStyleRef(override val id: String) : Ref, TextStyleDefOrRef {
    companion object {
        fun fromModel(model: TextStyleModelRef) = TextStyleRef(model.id)
    }

    override fun toModel() = TextStyleModelRef(id)
    override fun toDb() = TextStyleEntityRef(id)
}

data class ParagraphStyleRef(override val id: String) : Ref, ParagraphStyleDefOrRef {
    companion object {
        fun fromModel(model: ParagraphStyleModelRef) = ParagraphStyleRef(model.id)
    }

    override fun toModel() = ParagraphStyleModelRef(id)
    override fun toDb() = ParagraphStyleEntityRef(id)
}

data class DisplayRuleRef(override val id: String) : Ref {
    companion object {
        fun fromModel(model: DisplayRuleModelRef) = DisplayRuleRef(model.id)
    }

    fun toModel() = DisplayRuleModelRef(id)
    fun toDb() = DisplayRuleEntityRef(id)
}

data class ImageRef(override val id: String) : Ref, DocumentContent, TextContent {
    companion object {
        fun fromModel(model: ImageModelRef) = ImageRef(model.id)
    }

    fun toModel() = ImageModelRef(id)
    fun toDb() = ImageEntityRef(id)
}

data class VariableStructureRef(override val id: String) : Ref {
    companion object {
        fun fromModel(model: VariableStructureModelRef) = VariableStructureRef(model.id)
    }

    fun toModel() = VariableStructureModelRef(id)
    fun toDb() = VariableStructureEntityRef(id)
}

data class StringValue(val value: String) : TextContent {
    companion object {
        fun fromModel(model: StringModel) = StringValue(model.value)
    }

    fun toModel() = StringModel(value)
    fun toDb() = StringEntity(value)
}

data class Hyperlink(
    val url: String,
    val displayText: String? = null,
    val alternateText: String? = null
) : TextContent {
    companion object {
        fun fromModel(model: HyperlinkModel) = Hyperlink(
            url = model.url,
            displayText = model.displayText,
            alternateText = model.alternateText
        )
    }

    fun toModel() = HyperlinkModel(
        url = url,
        displayText = displayText,
        alternateText = alternateText
    )

    fun toDb() = HyperlinkEntity(
        url = url,
        displayText = displayText,
        alternateText = alternateText
    )
}
