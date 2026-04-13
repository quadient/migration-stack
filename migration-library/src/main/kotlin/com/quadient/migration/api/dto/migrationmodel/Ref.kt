package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.DisplayRuleEntityRef
import com.quadient.migration.persistence.migrationmodel.DocumentObjectEntityRef
import com.quadient.migration.persistence.migrationmodel.FirstMatchEntity
import com.quadient.migration.persistence.migrationmodel.HyperlinkEntity
import com.quadient.migration.persistence.migrationmodel.ImageEntityRef
import com.quadient.migration.persistence.migrationmodel.AttachmentEntityRef
import com.quadient.migration.persistence.migrationmodel.ParagraphStyleEntityRef
import com.quadient.migration.persistence.migrationmodel.ResourceEntityRef
import com.quadient.migration.persistence.migrationmodel.StringEntity
import com.quadient.migration.persistence.migrationmodel.TableEntity
import com.quadient.migration.persistence.migrationmodel.TextContentEntity
import com.quadient.migration.persistence.migrationmodel.TextStyleEntityRef
import com.quadient.migration.persistence.migrationmodel.VariableEntityRef
import com.quadient.migration.persistence.migrationmodel.VariableStructureEntityRef

sealed class Ref(private val _id: String) : RefValidatable {
    open val id: String = _id

    init {
        require(!_id.isEmpty()) { "Ref id can not be empty" }
        require(!_id.isBlank()) { "Id cannot be blank" }
    }

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

data class DocumentObjectRef(override val id: String, val displayRuleRef: DisplayRuleRef? = null) : Ref(id),
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

data class VariableRef(override val id: String) : Ref(id), VariableStringContent, RefValidatable {
    override fun collectRefs(): List<Ref> = listOf(this)

    companion object {
        fun fromDb(entity: VariableEntityRef) = VariableRef(entity.id)
    }

    fun toDb() = VariableEntityRef(id)
}

data class TextStyleRef(override val id: String) : Ref(id), RefValidatable {
    companion object {
        fun fromDb(entity: TextStyleEntityRef) = TextStyleRef(entity.id)
    }

    fun toDb() = TextStyleEntityRef(id)
}

data class ParagraphStyleRef(override val id: String) : Ref(id) {
    companion object {
        fun fromDb(entity: ParagraphStyleEntityRef) = ParagraphStyleRef(entity.id)
    }

    fun toDb() = ParagraphStyleEntityRef(id)
}

data class DisplayRuleRef(override val id: String) : Ref(id) {
    companion object {
        fun fromDb(entity: DisplayRuleEntityRef) = DisplayRuleRef(entity.id)
    }

    fun toDb() = DisplayRuleEntityRef(id)
}

sealed class ResourceRef(override val id: String) : Ref(id), DocumentContent, TextContent, RefValidatable {

    abstract fun toDb(): ResourceEntityRef
}

data class ImageRef(override val id: String) : ResourceRef(id) {
    companion object {
        fun fromDb(entity: ImageEntityRef) = ImageRef(entity.id)
    }

    override fun toDb() = ImageEntityRef(id)
}

data class AttachmentRef(override val id: String) : ResourceRef(id) {
    companion object {
        fun fromDb(entity: AttachmentEntityRef) = AttachmentRef(entity.id)
    }

    override fun toDb() = AttachmentEntityRef(id)
}

data class VariableStructureRef(override val id: String) : Ref(id) {
    fun toDb() = VariableStructureEntityRef(id)
}

data class StringValue(val value: String) : VariableStringContent {
    companion object {
        fun fromDb(entity: StringEntity) = StringValue(entity.value)
    }

    fun toDb() = StringEntity(value)

    override fun collectRefs(): List<Ref> = emptyList()
}
