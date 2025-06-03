package com.quadient.migration.shared

import com.quadient.migration.tools.shouldBeEqualTo
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.math.round

class SizeTest {

    @TestFactory
    fun equality() = listOf(
        Size.ofMillimeters(1000) to Size.ofMeters(1),
        Size.ofPoints(10) to Size.ofMillimeters(10 / Size.MM_TO_PT),
        Size.ofCentimeters(5) to Size.ofMillimeters(50),
        Size.ofMeters(18) to Size.ofMillimeters(18000),
    ).map { (input, expected) -> dynamicTest("$input is equal to $expected") { input.shouldBeEqualTo(expected) } }

    @TestFactory
    fun `to value`() = listOf(
        Size.ofMillimeters(100).toMeters() to 0.1,
        Size.ofPoints(10).toCentimeters() to 10 / Size.MM_TO_PT / 10,
        Size.ofMeters(8).toPoints() to 8000 * Size.MM_TO_PT,
        Size.ofCentimeters(8).toCentimeters() to 8.0,
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
}