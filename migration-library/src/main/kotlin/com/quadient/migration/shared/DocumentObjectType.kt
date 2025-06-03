package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
enum class DocumentObjectType {
    Template, Page, Block, Section, Unsupported;

    fun toInteractiveFolder(): String = when (this) {
        Template -> "Templates"
        Page -> "Templates"
        Block -> "Blocks"
        Section -> "Blocks"
        Unsupported -> error("Unsupported is not expected in this case.")
    }

    fun toRunCommandType(): String = when (this) {
        Template -> "template"
        Page -> "template"
        Block -> "block"
        Section -> "block"
        Unsupported -> error("Unsupported is not expected in this case.")
    }
}