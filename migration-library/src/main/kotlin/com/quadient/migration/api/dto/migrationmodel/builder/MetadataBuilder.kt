package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.shared.IcmDateTime
import com.quadient.migration.shared.MetadataPrimitive
import java.time.Instant
import kotlinx.datetime.Instant as KInstant

class MetadataBuilder {
    private val list: MutableList<MetadataPrimitive> = mutableListOf()
    fun add(primitive: MetadataPrimitive) {
        list.add(primitive)
    }

    fun string(value: String) {
        list.add(MetadataPrimitive.Str(value))
    }

    fun boolean(value: Boolean) {
        list.add(MetadataPrimitive.Bool(value))
    }

    fun integer(value: Long) {
        list.add(MetadataPrimitive.Integer(value))
    }

    fun float(value: Double) {
        list.add(MetadataPrimitive.Float(value))
    }

    fun dateTime(value: Instant) {
        list.add(MetadataPrimitive.DateTime(IcmDateTime(KInstant.fromEpochMilliseconds(value.toEpochMilli()))))
    }

    fun build(): List<MetadataPrimitive> = list
}
