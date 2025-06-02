package com.quadient.wfdxml.internal.xml.export

import spock.lang.Specification

class IdRegisterTest extends Specification {

    def "test getOrCreateId"() {
        given:
        def obj1 = new ArrayList()
        def obj2 = new ArrayList()
        IdRegister idRegister = new IdRegister()

        when:
        String id1 = idRegister.getOrCreateId(obj1)
        String id2 = idRegister.getOrCreateId(obj2)
        String id3 = idRegister.getOrCreateId(obj1)

        then:
        assert [id1, id2, id3] == ["SR_1", "SR_2", "SR_1"]
    }

    def "null object is not supported"() {
        given:
        IdRegister idRegister = new IdRegister()

        when:
        idRegister.getOrCreateId(null)

        then:
        NullPointerException ex = thrown()
        ex.message == "Its not possible to get Id from null object."
    }

    def "setObjectId basic test"() {
        given:
        IdRegister idRegister = new IdRegister()
        def obj1 = new ArrayList()

        when:
        idRegister.setObjectId(obj1, "My.Custom.Id")

        then:
        assert idRegister.getOrCreateId(obj1) == "My.Custom.Id"
    }

    def "setObjectId with multiple objects"() {
        given:
        IdRegister idRegister = new IdRegister()
        def obj1 = new ArrayList()
        def obj2 = new ArrayList()
        def obj3 = new ArrayList()

        when:
        idRegister.setObjectId(obj1, "My.Custom.Id.1")
        idRegister.getOrCreateId(obj2)
        idRegister.setObjectId(obj3, "My.Custom.Id.2")

        then:
        assert ["My.Custom.Id.1", "SR_1", "My.Custom.Id.2", "SR_2"] ==
                [
                        idRegister.getOrCreateId(obj1),
                        idRegister.getOrCreateId(obj2),
                        idRegister.getOrCreateId(obj3),
                        idRegister.getOrCreateId("New Object")
                ]
    }
}
