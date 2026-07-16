package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.DocumentObjectOptionsEntity
import com.quadient.migration.persistence.migrationmodel.EmailOptionsEntity
import com.quadient.migration.persistence.migrationmodel.PageOptionsEntity
import com.quadient.migration.persistence.migrationmodel.SmsOptionsEntity
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.Size

sealed interface DocumentObjectOptions {
    companion object {
        fun fromDb(entity: DocumentObjectOptionsEntity): DocumentObjectOptions = when (entity) {
            is PageOptionsEntity -> PageOptions(
                width = entity.width,
                height = entity.height,
            )
            is EmailOptionsEntity -> EmailOptions(
                width = entity.width,
                backgroundFill = entity.backgroundFill,
                from = entity.from.map(VariableStringContent::fromDb),
                fromName = entity.fromName.map(VariableStringContent::fromDb),
                subject = entity.subject.map(VariableStringContent::fromDb),
                to = entity.to.map(VariableStringContent::fromDb),
            )
            is SmsOptionsEntity -> SmsOptions(
                numberTo = entity.numberTo.map(VariableStringContent::fromDb),
            )
        }
    }

    fun toDb(): DocumentObjectOptionsEntity = when (this) {
        is PageOptions -> PageOptionsEntity(
            width = width,
            height = height,
        )
        is EmailOptions -> EmailOptionsEntity(
            width = width,
            backgroundFill = backgroundFill,
            from = from.map(VariableStringContent::toDb),
            fromName = fromName.map(VariableStringContent::toDb),
            subject = subject.map(VariableStringContent::toDb),
            to = to.map(VariableStringContent::toDb),
        )
        is SmsOptions -> SmsOptionsEntity(
            numberTo = numberTo.map(VariableStringContent::toDb),
        )
    }
}


data class PageOptions(
    val width: Size?,
    val height: Size?,
) : DocumentObjectOptions

data class EmailOptions(
    val width: Double?,
    val backgroundFill: Color,
    val from: List<VariableStringContent>,
    val fromName: List<VariableStringContent>,
    val subject: List<VariableStringContent>,
    val to: List<VariableStringContent>,
) : DocumentObjectOptions

data class SmsOptions(
    val numberTo: List<VariableStringContent>,
) : DocumentObjectOptions
