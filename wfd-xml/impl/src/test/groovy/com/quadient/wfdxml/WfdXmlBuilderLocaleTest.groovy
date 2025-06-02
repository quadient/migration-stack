package com.quadient.wfdxml

import spock.lang.Specification

class WfdXmlBuilderLocaleTest extends Specification {
    private Locale defaultLocale

    void setup() {
        defaultLocale = Locale.getDefault()
    }

    void cleanup() {
        restoreLocale()
    }

    void restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    def "WfdXmlBuilder works even when system locale is not en_US"() {
        when:
        Locale.setDefault(Locale.GERMANY)

        then:
        new WfdXmlBuilder().build()
    }
}