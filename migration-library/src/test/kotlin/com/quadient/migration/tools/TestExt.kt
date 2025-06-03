package com.quadient.migration.tools

import org.junit.jupiter.api.Assertions.assertEquals

fun <T> Collection<T>.shouldBeEmpty() = assertEquals(this.count(), 0)
fun <T> Collection<T>.shouldBeOfSize(size: Int) = assertEquals(size, this.count())

fun <T> T.shouldBeEqualTo(expected: T) = assertEquals(expected, this)
fun <T> T.shouldNotBeEqualTo(expected: T) = assertEquals(true, this != expected)
