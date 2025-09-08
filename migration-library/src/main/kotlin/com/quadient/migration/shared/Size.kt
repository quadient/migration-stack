package com.quadient.migration.shared

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal
import java.math.RoundingMode

@Serializable(with = SizeSerializer::class)
class Size {
    val millimeters: BigDecimal

    enum class Unit {
        Points, Millimeters, Centimeters, Decimeters, Meters, Inches
    }

    companion object {
        const val MM_TO_PT = 72.0 / 25.4
        private const val DIV_SCALE = 20

        @JvmStatic
        fun ofPoints(value: Double) = Size(value, Unit.Points)
        @JvmStatic
        fun ofPoints(value: Long) = Size(value.toBigDecimal(), Unit.Points)
        @JvmStatic
        fun ofPoints(value: Int) = Size(value.toBigDecimal(), Unit.Points)
        @JvmStatic
        fun ofMillimeters(value: Double) = Size(value, Unit.Millimeters)
        @JvmStatic
        fun ofMillimeters(value: Long) = Size(value.toBigDecimal(), Unit.Millimeters)
        @JvmStatic
        fun ofMillimeters(value: Int) = Size(value.toBigDecimal(), Unit.Millimeters)
        @JvmStatic
        fun ofDecimeters(value: Double) = Size(value, Unit.Decimeters)
        @JvmStatic
        fun ofDecimeters(value: Long) = Size(value.toBigDecimal(), Unit.Decimeters)
        @JvmStatic
        fun ofDecimeters(value: Int) = Size(value.toBigDecimal(), Unit.Decimeters)
        @JvmStatic
        fun ofCentimeters(value: Double) = Size(value, Unit.Centimeters)
        @JvmStatic
        fun ofCentimeters(value: Long) = Size(value.toBigDecimal(), Unit.Centimeters)
        @JvmStatic
        fun ofCentimeters(value: Int) = Size(value.toBigDecimal(), Unit.Centimeters)
        @JvmStatic
        fun ofMeters(value: Double) = Size(value, Unit.Meters)
        @JvmStatic
        fun ofMeters(value: Long) = Size(value.toBigDecimal(), Unit.Meters)
        @JvmStatic
        fun ofMeters(value: Int) = Size(value.toBigDecimal(), Unit.Meters)
        @JvmStatic
        fun ofInches(value: Double)= Size(value, Unit.Inches)
        @JvmStatic
        fun ofInches(value: Long) = Size(value.toBigDecimal(), Unit.Inches)
        @JvmStatic
        fun ofInches(value: Int) = Size(value.toBigDecimal(), Unit.Inches)

        @JvmStatic
        fun fromString(input: String): Size {
            val cleansedInput = input.replace("\\s".toRegex(), "")

            val len = cleansedInput.length
            val (value, unit) = when {
                cleansedInput.endsWith("mm") -> Pair(cleansedInput.take(len - 2), Unit.Millimeters)
                cleansedInput.endsWith("cm") -> Pair(cleansedInput.take(len - 2), Unit.Centimeters)
                cleansedInput.endsWith("dm") -> Pair(cleansedInput.take(len - 2), Unit.Decimeters)
                cleansedInput.endsWith("m") -> Pair(cleansedInput.take(len - 1), Unit.Meters)
                cleansedInput.endsWith("pt") -> Pair(cleansedInput.take(len - 2), Unit.Points)
                cleansedInput.endsWith("in") -> Pair(cleansedInput.take(len - 2), Unit.Inches)
                else -> throw NumberFormatException("Invalid size format in $input")
            }

            if (!value.matches("^-?[\\d.]+$".toRegex())) {
                throw NumberFormatException("Invalid size format in $input")
            }

            return Size(value.toDouble(), unit)
        }
    }

    constructor(value: Double, unit: Unit) {
        millimeters = when (unit) {
            Unit.Points -> value.toBigDecimal().divide(MM_TO_PT.toBigDecimal(), DIV_SCALE, RoundingMode.HALF_EVEN)
            Unit.Millimeters -> value.toBigDecimal()
            Unit.Centimeters -> (value * 10.0).toBigDecimal()
            Unit.Decimeters -> (value * 100.0).toBigDecimal()
            Unit.Meters -> (value * 1000.0).toBigDecimal()
            Unit.Inches -> (value * 25.4).toBigDecimal()
        }
    }

    constructor(value: BigDecimal, unit: Unit) {
        millimeters = when (unit) {
            Unit.Points -> value.divide(MM_TO_PT.toBigDecimal(), DIV_SCALE, RoundingMode.HALF_EVEN)
            Unit.Millimeters -> value
            Unit.Centimeters -> value * 10.0.toBigDecimal()
            Unit.Decimeters -> value * 100.0.toBigDecimal()
            Unit.Meters -> value * 1000.0.toBigDecimal()
            Unit.Inches -> value * 25.4.toBigDecimal()
        }
    }

    fun to(unit: Unit): Double {
        return when (unit) {
            Unit.Points -> (millimeters * MM_TO_PT.toBigDecimal()).setScale(4, RoundingMode.HALF_EVEN)
            Unit.Millimeters -> millimeters.setScale(4, RoundingMode.HALF_EVEN)
            Unit.Centimeters -> millimeters.divide(10.toBigDecimal(), DIV_SCALE, RoundingMode.HALF_EVEN).setScale(5, RoundingMode.HALF_EVEN)
            Unit.Decimeters -> millimeters.divide(100.toBigDecimal(), DIV_SCALE, RoundingMode.HALF_EVEN).setScale(6, RoundingMode.HALF_EVEN)
            Unit.Meters -> millimeters.divide(1000.0.toBigDecimal(), DIV_SCALE, RoundingMode.HALF_EVEN).setScale(7, RoundingMode.HALF_EVEN)
            Unit.Inches -> millimeters.divide(25.4.toBigDecimal(), DIV_SCALE, RoundingMode.HALF_EVEN).setScale(5, RoundingMode.HALF_EVEN)
        }.toDouble()
    }

    fun to(unit: Long) = to(unit.toDouble())

    fun toMillimeters() = to(Unit.Millimeters)
    fun toCentimeters() = to(Unit.Centimeters)
    fun toDecimeters() = to(Unit.Decimeters)
    fun toMeters() = to(Unit.Meters)
    fun toPoints() = to(Unit.Points)
    fun toInches() = to(Unit.Inches)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Size) return false

        if (millimeters.compareTo(other.millimeters) != 0) return false

        return true
    }

    override fun toString(): String {
        return toString(Unit.Millimeters)
    }

    fun toString(unit: Unit): String {
        return when (unit) {
            Unit.Points -> "${toPoints()}pt"
            Unit.Millimeters -> "${toMillimeters()}mm"
            Unit.Decimeters -> "${toDecimeters()}dm"
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
        if (res < BigDecimal.ZERO) {
            throw IllegalArgumentException("Size cannot be negative")
        }

        return Size(res, Unit.Millimeters)
    }

    operator fun times(other: Double): Size {
        return Size(millimeters * other.toBigDecimal(), Unit.Millimeters)
    }

    operator fun div(other: Double): Size {
        return Size(millimeters.divide(other.toBigDecimal(), DIV_SCALE, RoundingMode.HALF_EVEN), Unit.Millimeters)
    }
}

fun Long.points(): Size = Size.ofPoints(this)
fun Int.points(): Size = Size.ofPoints(this)
fun Double.points(): Size = Size.ofPoints(this)
fun Long.millimeters(): Size = Size.ofMillimeters(this)
fun Int.millimeters(): Size = Size.ofMillimeters(this)
fun Double.millimeters(): Size = Size.ofMillimeters(this)
fun Long.decimeters(): Size = Size.ofDecimeters(this)
fun Int.decimeters(): Size = Size.ofDecimeters(this)
fun Double.decimeters(): Size = Size.ofDecimeters(this)
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
