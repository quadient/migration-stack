package com.quadient.migration.example.docx.util


import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class DocxUtilsTest {
    @ParameterizedTest
    @CsvSource(['1440,72', '1,0.05', '0,0', '-240,-12'])
    void "twips conversion preserves fractional and negative coordinates"(String value, double expected) {
        // given: a twips value and expected points from the parameterized case
        // when / then: string and numeric inputs produce the same conversion
        assert DocxUtils.twipsToPoints(value) == expected
        assert DocxUtils.twipsToPoints(new BigInteger(value)) == expected
    }

    @Test
    void "missing and invalid dimensions remain absent"() {
        // given: absent or nonnumeric dimensions
        // when / then: conversion leaves them absent
        [null, '', 'auto', '12pt'].each { assert DocxUtils.twipsToPoints(it) == null }
        // given: the unset-size sentinel and valid twips / EMU dimensions
        // when / then: size conversion handles the sentinel and preserves fractional points
        assert DocxUtils.twipsToSize(-1).toPoints() == 0d
        assert DocxUtils.twipsToSize(1440).toPoints() == 72d
        assert DocxUtils.emuToSize(6350).toPoints() == 0.5d
    }

    @Test
    void "hash remains compatible with existing style identifiers"() {
        // given: a known SHA-256 input and ordered attributes with the same concatenated value
        // when / then: hashing preserves the digest and uppercase style-identifier convention
        assert DocxUtils.sha256Hex('abc') == 'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad'
        assert DocxUtils.calculateSHA([first: 'a', second: 'bc']) == 'BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD'
    }
}
