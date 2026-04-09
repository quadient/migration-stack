package com.quadient.migration.persistence.migrationmodel

import com.quadient.migration.shared.ColumnApplyTo
import com.quadient.migration.shared.ColumnBalancingType
import com.quadient.migration.shared.Size
import kotlinx.serialization.Serializable

@Serializable
data class ColumnLayoutEntity(
    val numberOfColumns: Int,
    val gutterWidth: Size? = null,
    val balancingType: ColumnBalancingType? = null,
    val applyTo: ColumnApplyTo? = null,
    val content: List<DocumentContentEntity>,
) : DocumentContentEntity
