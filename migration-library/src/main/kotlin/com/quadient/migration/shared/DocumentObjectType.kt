package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
enum class DocumentObjectType {
    Template, Page, Block, Section, Snippet;

    fun toInteractiveFolder(): String = when (this) {
        Template -> "Templates"
        Page -> "Templates"
        Block -> "Blocks"
        Section -> "Blocks"
        Snippet -> "Snippets"
    }

    fun toRunCommandType(): String = when (this) {
        Template -> "template"
        Page -> "template"
        Block -> "block"
        Section -> "block"
        Snippet -> "snippet"
    }
}