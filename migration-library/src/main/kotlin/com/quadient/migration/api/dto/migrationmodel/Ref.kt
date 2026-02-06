package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.DisplayRuleEntityRef
import com.quadient.migration.persistence.migrationmodel.DocumentObjectEntityRef
import com.quadient.migration.persistence.migrationmodel.FirstMatchEntity
import com.quadient.migration.persistence.migrationmodel.HyperlinkEntity
import com.quadient.migration.persistence.migrationmodel.ImageEntityRef
import com.quadient.migration.persistence.migrationmodel.AttachmentEntityRef
import com.quadient.migration.persistence.migrationmodel.ParagraphStyleEntityRef
import com.quadient.migration.persistence.migrationmodel.StringEntity
import com.quadient.migration.persistence.migrationmodel.TableEntity
import com.quadient.migration.persistence.migrationmodel.TextContentEntity
import com.quadient.migration.persistence.migrationmodel.TextStyleEntityRef
import com.quadient.migration.persistence.migrationmodel.VariableEntityRef
import com.quadient.migration.persistence.migrationmodel.VariableStructureEntityRef

sealed interface Ref : RefValidatable {
    val id: String

    override fun collectRefs(): List<Ref> {
        return listOf(this)
    }
}

sealed interface TextContent {
    companion object {
        fun fromDb(entity: TextContentEntity): TextContent = when (entity) {
            is StringEntity -> StringValue.fromDb(entity)
            is VariableEntityRef -> VariableRef.fromDb(entity)
            is TableEntity -> Table.fromDb(entity)
            is DocumentObjectEntityRef -> DocumentObjectRef.fromDb(entity)
            is ImageEntityRef -> ImageRef.fromDb(entity)
            is AttachmentEntityRef -> AttachmentRef.fromDb(entity)
            is FirstMatchEntity -> FirstMatch.fromDb(entity)
            is HyperlinkEntity -> Hyperlink.fromDb(entity)
        }
    }
}

sealed interface TextStyleDefOrRef {
    fun toDb() = when (this) {
        is TextStyleDefinition -> this.toDb()
        is TextStyleRef -> this.toDb()
    }
}

sealed interface ParagraphStyleDefOrRef {
    fun toDb() = when (this) {
        is ParagraphStyleDefinition -> this.toDb()
        is ParagraphStyleRef -> this.toDb()
    }
}

data class DocumentObjectRef(override val id: String, val displayRuleRef: DisplayRuleRef? = null) : Ref,
    DocumentContent, TextContent, RefValidatable {

    constructor(id: String) : this(id, null)

    override fun collectRefs(): List<Ref> {
        return listOfNotNull(this, displayRuleRef)
    }

    companion object {
        fun fromDb(entity: DocumentObjectEntityRef) =
            DocumentObjectRef(entity.id, entity.displayRuleRef?.let { DisplayRuleRef.fromDb(it) })
    }

    fun toDb() = DocumentObjectEntityRef(id, displayRuleRef?.toDb())
}

data class VariableRef(override val id: String) : Ref, TextContent, RefValidatable {
    companion object {
        fun fromDb(entity: VariableEntityRef) = VariableRef(entity.id)
    }

    fun toDb() = VariableEntityRef(id)
}

data class TextStyleRef(override val id: String) : Ref, TextStyleDefOrRef, RefValidatable {
    companion object {
        fun fromDb(entity: TextStyleEntityRef) = TextStyleRef(entity.id)
    }

    override fun toDb() = TextStyleEntityRef(id)
}

data class ParagraphStyleRef(override val id: String) : Ref, ParagraphStyleDefOrRef {
    companion object {
        fun fromDb(entity: ParagraphStyleEntityRef) = ParagraphStyleRef(entity.id)
    }

    override fun toDb() = ParagraphStyleEntityRef(id)
}

data class DisplayRuleRef(override val id: String) : Ref {
    companion object {
        fun fromDb(entity: DisplayRuleEntityRef) = DisplayRuleRef(entity.id)
    }

    fun toDb() = DisplayRuleEntityRef(id)
}

data class ImageRef(override val id: String) : Ref, DocumentContent, TextContent, RefValidatable {
    companion object {
        fun fromDb(entity: ImageEntityRef) = ImageRef(entity.id)
    }

    fun toDb() = ImageEntityRef(id)
}

data class AttachmentRef(override val id: String) : Ref, DocumentContent, TextContent, RefValidatable {
    companion object {
        fun fromDb(entity: AttachmentEntityRef) = AttachmentRef(entity.id)
    }

    fun toDb() = AttachmentEntityRef(id)
}

data class VariableStructureRef(override val id: String) : Ref {
    fun toDb() = VariableStructureEntityRef(id)
}

data class StringValue(val value: String) : TextContent {
    companion object {
        fun fromDb(entity: StringEntity) = StringValue(entity.value)
    }

    fun toDb() = StringEntity(value)
}
