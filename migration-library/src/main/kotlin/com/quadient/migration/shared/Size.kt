package com.quadient.migration.shared

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SizeSerializer::class)
class Size {
    val millimeters: Double

    enum class Unit {
        Points, Millimeters, Centimeters, Meters, Inches
    }

    companion object {
        const val MM_TO_PT = 72 / 25.4

        @JvmStatic
        fun ofPoints(value: Double) = Size(value, Unit.Points)
        @JvmStatic
        fun ofPoints(value: Long) = Size(value.toDouble(), Unit.Points)
        @JvmStatic
        fun ofPoints(value: Int) = Size(value.toDouble(), Unit.Points)
        @JvmStatic
        fun ofMillimeters(value: Double) = Size(value, Unit.Millimeters)
        @JvmStatic
        fun ofMillimeters(value: Long) = Size(value.toDouble(), Unit.Millimeters)
        @JvmStatic
        fun ofMillimeters(value: Int) = Size(value.toDouble(), Unit.Millimeters)
        @JvmStatic
        fun ofCentimeters(value: Double) = Size(value, Unit.Centimeters)
        @JvmStatic
        fun ofCentimeters(value: Long) = Size(value.toDouble(), Unit.Centimeters)
        @JvmStatic
        fun ofCentimeters(value: Int) = Size(value.toDouble(), Unit.Centimeters)
        @JvmStatic
        fun ofMeters(value: Double) = Size(value, Unit.Meters)
        @JvmStatic
        fun ofMeters(value: Long) = Size(value.toDouble(), Unit.Meters)
        @JvmStatic
        fun ofMeters(value: Int) = Size(value.toDouble(), Unit.Meters)
        @JvmStatic
        fun ofInches(value: Double)= Size(value, Unit.Inches)
        @JvmStatic
        fun ofInches(value: Long) = Size(value.toDouble(), Unit.Inches)
        @JvmStatic
        fun ofInches(value: Int) = Size(value.toDouble(), Unit.Inches)

        @JvmStatic
        fun fromString(input: String): Size {
            val (value, unit) = when {
                input.endsWith("mm") -> Pair(input.substringBefore("mm"), Unit.Millimeters)
                input.endsWith("cm") -> Pair(input.substringBefore("cm"), Unit.Centimeters)
                input.endsWith("m") -> Pair(input.substringBefore("m"), Unit.Meters)
                input.endsWith("pt") -> Pair(input.substringBefore("pt"), Unit.Points)
                input.endsWith("in") -> Pair(input.substringBefore("in"), Unit.Inches)
                else -> throw NumberFormatException("Invalid size format in $input")
            }
            return Size(value.toDouble(), unit)
        }

    }

    constructor(value: Double, unit: Unit) {
        millimeters = when (unit) {
            Unit.Points -> value / MM_TO_PT
            Unit.Millimeters -> value
            Unit.Centimeters -> value * 10.0
            Unit.Meters -> value * 1000.0
            Unit.Inches -> value * 25.4
        }
    }

    fun to(unit: Unit): Double {
        return when (unit) {
            Unit.Points -> millimeters * MM_TO_PT
            Unit.Millimeters -> millimeters
            Unit.Centimeters -> millimeters / 10.0
            Unit.Meters -> millimeters / 1000.0
            Unit.Inches -> millimeters / 25.4
        }
    }

    fun to(unit: Long) = to(unit.toDouble())

    fun toMillimeters() = to(Unit.Millimeters)
    fun toCentimeters() = to(Unit.Centimeters)
    fun toMeters() = to(Unit.Meters)
    fun toPoints() = to(Unit.Points)
    fun toInches() = to(Unit.Inches)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Size) return false

        if (millimeters != other.millimeters) return false

        return true
    }

    override fun toString(): String {
        return toString(Unit.Millimeters)
    }

    fun toString(unit: Unit): String {
        return when (unit) {
            Unit.Points -> "${toPoints()}pt"
            Unit.Millimeters -> "${toMillimeters()}mm"
            Unit.Centimeters -> "${toCentimeters()}cm"
            Unit.Meters -> "${toMeters()}m"
            Unit.Inches -> "${toInches()}in"
        }
    }

    override fun hashCode(): Int {
        return millimeters.hashCode()
    }

    operator fun plus(other: Size): Size {
        return Size(millimeters + other.millimeters, Unit.Millimeters)
    }

    operator fun minus(other: Size): Size {
        val res = millimeters - other.millimeters
        if (res < 0) {
            throw IllegalArgumentException("Size cannot be negative")
        }

        return Size(res, Unit.Millimeters)
    }

    operator fun times(other: Double): Size {
        return Size(millimeters * other, Unit.Millimeters)
    }

    operator fun div(other: Double): Size {
        return Size(millimeters / other, Unit.Millimeters)
    }
}

fun Long.points(): Size = Size.ofPoints(this)
fun Int.points(): Size = Size.ofPoints(this)
fun Double.points(): Size = Size.ofPoints(this)
fun Long.millimeters(): Size = Size.ofMillimeters(this)
fun Int.millimeters(): Size = Size.ofMillimeters(this)
fun Double.millimeters(): Size = Size.ofMillimeters(this)
fun Long.centimeters(): Size = Size.ofCentimeters(this)
fun Int.centimeters(): Size = Size.ofCentimeters(this)
fun Double.centimeters(): Size = Size.ofCentimeters(this)
fun Long.meters(): Size = Size.ofMeters(this)
fun Int.meters(): Size = Size.ofMeters(this)
fun Double.meters(): Size = Size.ofMeters(this)
fun Long.inches(): Size = Size.ofInches(this)
fun Int.inches(): Size = Size.ofInches(this)
fun Double.inches(): Size = Size.ofInches(this)

object SizeSerializer : KSerializer<Size> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.quadient.migration.shared.Size", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Size) {
        encoder.encodeString(value.toString(Size.Unit.Millimeters))
    }

    override fun deserialize(decoder: Decoder): Size {
        return Size.fromString(decoder.decodeString())
    }
}
