package com.quadient.migration.shared

import com.quadient.migration.tools.shouldBeEqualTo
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import kotlin.math.round

class SizeTest {

    @TestFactory
    fun equality() = listOf(
        Size.ofMillimeters(1000) to Size.ofMeters(1),
        Size.ofPoints(10) to Size.ofMillimeters(10 / Size.MM_TO_PT),
        Size.ofCentimeters(5) to Size.ofMillimeters(50),
        Size.ofMeters(18) to Size.ofMillimeters(18000),
        Size.ofInches(1) to Size.ofMillimeters(25.4),
    ).map { (input, expected) -> dynamicTest("$input is equal to $expected") { input.toMillimeters().shouldBeEqualTo(expected.toMillimeters()) } }

    @TestFactory
    fun `to value`() = listOf(
        Size.ofMillimeters(100).toMeters() to 0.1,
        Size.ofPoints(10).toCentimeters() to 0.35278,
        Size.ofMeters(8).toPoints() to 22677.1654,
        Size.ofCentimeters(8).toCentimeters() to 8.0,
        Size.ofInches(1).toMillimeters() to 25.4,
    ).map { (input, expected) -> dynamicTest("$input is equal to $expected") { input.shouldBeEqualTo(expected) } }

    @TestFactory
    fun `from string tests`() = listOf(
        Size.fromString("1mm") to Size.ofMillimeters(1),
        Size.fromString("1cm") to Size.ofCentimeters(1),
        Size.fromString("1dm") to Size.ofDecimeters(1),
        Size.fromString("1m") to Size.ofMeters(1),
        Size.fromString("1in") to Size.ofInches(1),
        Size.fromString("1pt") to Size.ofPoints(1),
    ).map { (input, expected) -> dynamicTest("$input is equal to $expected") { input.shouldBeEqualTo(expected) } }


    @Test
    fun `mm converted to pt`() {
        val sizeInMm = Size.ofMillimeters(55.93)

        (round(sizeInMm.toPoints() * 100) / 100).shouldBeEqualTo(158.54)
    }

    @Test
    fun `pt converted to mm`() {
        val sizeInPt = Size.ofPoints(158.54)

        (round(sizeInPt.toMillimeters() * 100) / 100).shouldBeEqualTo(55.93)
    }

    @Test
    fun `dm does not coerce do meters`() {
        val sizeInDm = Size.fromString("10dm")

        sizeInDm.toMeters().shouldBeEqualTo(1.0)
    }

    @Test
    fun `invalid format throws`() {
        val ex = assertThrows<NumberFormatException> { Size.fromString("10mmm") }
        ex.message.shouldBeEqualTo("Invalid size format in 10mmm")
    }

    @Test
    fun `negative number with space in string deserialized correctly`() {
        val negativeSize = Size.fromString("-10 cm")

        negativeSize.shouldBeEqualTo(Size.ofMillimeters(-100))
    }
}