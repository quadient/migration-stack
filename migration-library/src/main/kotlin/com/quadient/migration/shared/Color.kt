package com.quadient.migration.shared

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * @throws When any of the components are outside [0, 255] range
 */
@Serializable(with = ColorHexSerializer::class)
data class Color(
    val red: UByte, val green: UByte, val blue: UByte
) {
    constructor(red: Int, green: Int, blue: Int) : this(red.toUByte(), green.toUByte(), blue.toUByte())
    constructor(red: Double, green: Double, blue: Double) : this(
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )

    /**
     * @throws When invalid hex value is provided
     */
    companion object {
        @JvmStatic
        fun fromHex(hex: String): Color {
            if (hex.length != 7) {
                throw Exception("Color values should be 7 characters long")
            }
            val r = hex.substring(1, 3).toInt(16)
            val g = hex.substring(3, 5).toInt(16)
            val b = hex.substring(5, 7).toInt(16)
            return Color(r.toUByte(), g.toUByte(), b.toUByte())
        }

    }

    fun toHex() = "#${red.toString(16).padStart(length = 2, padChar = '0')}${
        green.toString(16).padStart(length = 2, padChar = '0')
    }${blue.toString(16).padStart(length = 2, padChar = '0')}"

    override fun toString(): String {
        return toHex()
    }

    fun redDouble(): Double = (red / 255.toUByte()).toDouble()
    fun greenDouble(): Double = (green / 255.toUByte()).toDouble()
    fun blueDouble(): Double = (blue / 255.toUByte()).toDouble()
    fun red(): Int = red.toInt()
    fun green(): Int = green.toInt()
    fun blue(): Int = blue.toInt()
}

object ColorHexSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.quadient.migration.shared.Color", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Color) {
        encoder.encodeString(value.toHex())
    }

    override fun deserialize(decoder: Decoder): Color {
        return Color.fromHex(decoder.decodeString())
    }
}