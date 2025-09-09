package com.quadient.migration.dto

import kotlinx.serialization.Serializable

@Serializable
data class StatisticsResponse(
    val unsupportedCount: Int?, val supportedCount: Int?
)