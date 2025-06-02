package com.quadient.wfdxml.internal.layoutnodes.data

import spock.lang.Specification

import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType
import static com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.typeToString

class WorkFlowTreeEnumsTest extends Specification {
    def "test typeToString"() {
        expect:
        typeToString(nodeType) == expectedResult

        where:
        expectedResult | _
        "String"       | _
        "Int"          | _
        "Int64"        | _
        "Currency"     | _
        "Double"       | _
        "DateTime"     | _
        "Bool"         | _
        "SubTree"      | _
        "Binary"       | _

        nodeType << NodeType.values().toList()
    }
}