package com.quadient.migration.shared

import com.quadient.migration.tools.unreachable
import kotlinx.serialization.Serializable

@Serializable
enum class DocumentObjectType {
    Template, Page, Block, Section, Snippet, Email, Sms;

    fun toInteractiveFolder(): String = when (this) {
        Template -> "Templates"
        Page -> "Templates"
        Block -> "Blocks"
        Section -> "Blocks"
        Snippet -> "Snippets"
        Email -> unreachable("Email should never be created as an external object")
        Sms -> unreachable("SMS should never be created as an external object")
    }

    fun toRunCommandType(): String = when (this) {
        Template -> "template"
        Page -> "template"
        Block -> "block"
        Section -> "block"
        Snippet -> "snippet"
        Email -> unreachable("Email should never be deployed as an external object")
        Sms -> unreachable("SMS should never be deployed as an external object")
    }
}