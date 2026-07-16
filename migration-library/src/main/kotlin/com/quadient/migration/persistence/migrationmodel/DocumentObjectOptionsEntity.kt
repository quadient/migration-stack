package com.quadient.migration.persistence.migrationmodel

import com.quadient.migration.shared.Color
import com.quadient.migration.shared.Size
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface DocumentObjectOptionsEntity

@Serializable
@SerialName("PageOptionsEntity")
data class PageOptionsEntity(
    val width: Size?,
    val height: Size?,
) : DocumentObjectOptionsEntity

@Serializable
@SerialName("EmailOptionsEntity")
data class EmailOptionsEntity(
    val width: Double?,
    val backgroundFill: Color,
    val from: List<VariableStringContentEntity>,
    val fromName: List<VariableStringContentEntity>,
    val subject: List<VariableStringContentEntity>,
    val to: List<VariableStringContentEntity>,
) : DocumentObjectOptionsEntity

@Serializable
@SerialName("SmsOptionsEntity")
data class SmsOptionsEntity(
    val numberTo: List<VariableStringContentEntity>
) : DocumentObjectOptionsEntity
