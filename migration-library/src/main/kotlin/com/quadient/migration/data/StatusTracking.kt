@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.data

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.shared.IcmPath
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
sealed class StatusEvent() {
    abstract val timestamp: Instant
}

@Serializable
@SerialName("Active")
class Active(
    override val timestamp: Instant = Clock.System.now(),
    val data: Map<String, String> = emptyMap()
) : StatusEvent()

@Serializable
@SerialName("Deployed")
class Deployed(
    val deploymentId: Uuid,
    override val timestamp: Instant,
    val output: InspireOutput,
    val icmPath: IcmPath?,
    val data: Map<String, String> = emptyMap(),
) : StatusEvent()

@Serializable
@SerialName("Error")
data class Error(
    val deploymentId: Uuid,
    override val timestamp: Instant,
    val output: InspireOutput,
    val icmPath: IcmPath?,
    val error: String,
    val data: Map<String, String> = emptyMap(),
) : StatusEvent()