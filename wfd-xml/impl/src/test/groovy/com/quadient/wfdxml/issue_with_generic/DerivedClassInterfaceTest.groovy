package com.quadient.wfdxml.issue_with_generic

import spock.lang.Specification

class DerivedClassInterfaceTest extends Specification {

    def "cast DerivedClass into DerivedInterface"() {
        given:
        DerivedClass derivedClass = new DerivedClass()
        derivedClass.setBase("AA")
        derivedClass.setBase("Base")

        when:
        DerivedInterface derivedInterface = new DerivedClass()

        then:
        noExceptionThrown()
    }
}