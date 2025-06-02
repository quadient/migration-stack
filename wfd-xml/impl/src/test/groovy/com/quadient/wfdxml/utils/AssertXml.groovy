package com.quadient.wfdxml.utils

import org.xmlunit.builder.DiffBuilder
import org.xmlunit.diff.Diff
import org.xmlunit.diff.Difference

class AssertXml {

    static void assertXmlEqualsWrapRoot(String actualXml, String expectedXml) {
        actualXml = "<WRAP>$actualXml</WRAP>"
        expectedXml = "<WRAP>$expectedXml</WRAP>"
        assertXmlEquals(actualXml, expectedXml)
    }

    static void assertXmlEquals(String actualXml, String expectedXml) {
        assertXmlEquals(actualXml, expectedXml, null)
    }

    static void assertXmlEquals(String actualXml, String expectedXml, Closure<DiffBuilder> nodeBuilderAdjustment) {
        DiffBuilder nodeBuilder = DiffBuilder.compare(expectedXml).withTest(actualXml).ignoreWhitespace()
        if (nodeBuilderAdjustment != null) {
            nodeBuilderAdjustment(nodeBuilder)
        }
        Diff diff = nodeBuilder.build()
        Iterable<Difference> differences = diff.getDifferences()

        String errorMessage = "\n It was found ${differences.size()} differences:\n --- ${differences.join("\n --- ")}"

        assert differences.size() == 0: errorMessage
    }

    static void assertXmlFileEquals(String expectedPathFile, String actualXml) {
        assertXmlFileEquals(expectedPathFile, actualXml, null)
    }

    static void assertXmlFileEquals(String expectedPathFile, String actualXml, Closure<DiffBuilder> nodeBuilderAdjustment) {
        String expectedXmlFile = new String(
                AssertXml.class.getClassLoader()
                        .getResourceAsStream(expectedPathFile)
                        .readAllBytes(),
                "UTF-8"
        )
        assertXmlEquals(actualXml, expectedXmlFile, nodeBuilderAdjustment)
    }
}