package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
data class SkipOptions(val skipped: Boolean, val placeholder: String?, val reason: String?)