package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.shared.DocumentObjectType
import groovy.transform.Field

@Field
static final int MAX_BLOCK_NAME_LENGTH = 60

// Stores a piece of content as an internal block and returns the reference to place in the flow instead.
static DocumentObjectRef upsertBlock(Migration migration, String id, String name, List<DocumentContent> content, String fileName,
                                     String displayRuleId = null) {
    migration.documentObjectRepository.upsert(new DocumentObjectBuilder(id, DocumentObjectType.Block)
            .name(name)
            .content(content)
            .internal(true)
            .originLocations([fileName])
            .build())
    return new DocumentObjectRef(id, displayRuleId ? new DisplayRuleRef(displayRuleId) : null)
}

// Derives a readable block name from source text (a heading, a table's first cell, ...), shortened at a word boundary.
static String blockName(String text, String suffix, String fallback) {
    String normalized = text?.replaceAll(/\s+/, " ")?.trim()
    if (!normalized) {
        return fallback
    }
    if (normalized.length() > MAX_BLOCK_NAME_LENGTH) {
        normalized = normalized.substring(0, MAX_BLOCK_NAME_LENGTH).replaceFirst(/\s+\S*$/, "")
    }
    return suffix ? "${normalized} ${suffix}" : normalized
}
