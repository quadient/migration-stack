package com.quadient.wfdxml.internal.layoutnodes.tables

import com.quadient.wfdxml.api.layoutnodes.data.Variable
import com.quadient.wfdxml.api.layoutnodes.tables.GeneralRowSet
import com.quadient.wfdxml.api.layoutnodes.tables.HeaderFooterRowSet
import com.quadient.wfdxml.internal.layoutnodes.data.VariableImpl
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.api.layoutnodes.tables.RowSet.Type.HEADER_AND_FOOTER
import static com.quadient.wfdxml.api.layoutnodes.tables.RowSet.Type.MULTIPLE_ROWS
import static com.quadient.wfdxml.api.layoutnodes.tables.RowSet.Type.REPEATED
import static com.quadient.wfdxml.api.layoutnodes.tables.RowSet.Type.SELECT_BY_CONDITION
import static com.quadient.wfdxml.api.layoutnodes.tables.RowSet.Type.SELECT_BY_INLINE_CONDITION
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class RowSetImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "export elementName"() {
        when:
        String xmlElementName = new RowSetImpl().xmlElementName

        then:
        assert xmlElementName == "RowSet"
    }

    def "export empty RowSet"() {
        given:
        RowSetImpl rowSet = new RowSetImpl()

        when:
        rowSet.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
            <RowSetType>Row</RowSetType>
            <VariableId/>
            <VariableId/>
            <VariableId/>
            <VariableId/>
         """)
    }

    def "export RowSet with cells"() {
        given:
        GeneralRowSet rowSet = new RowSetImpl() as GeneralRowSet
        rowSet.addCell(new CellImpl())
        rowSet.addCell(new CellImpl())

        when:
        (rowSet as RowSetImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
            <RowSetType>Row</RowSetType>
            <SubRowId>SR_1</SubRowId>
            <SubRowId>SR_2</SubRowId>
            <VariableId/>
            <VariableId/>
            <VariableId/>
            <VariableId/>
         """)
    }

    def "export RowSet with empty cell"() {
        given:
        GeneralRowSet rowSet = new RowSetImpl() as GeneralRowSet
        rowSet.addEmptyCell()
        rowSet.addCell(new CellImpl())

        when:
        (rowSet as RowSetImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
                <RowSetType>Row</RowSetType>
                <SubRowId/>
                <SubRowId>SR_1</SubRowId>
                <VariableId/>
                <VariableId/>
                <VariableId/>
                <VariableId/>
             """)
    }

    def "export RowSet of RowSet type"() {
        given:
        GeneralRowSet rowSet = new RowSetImpl().setType(MULTIPLE_ROWS) as GeneralRowSet
        rowSet.addRowSet(new RowSetImpl())
        rowSet.addRowSet(new RowSetImpl())

        when:
        rowSet.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
            <RowSetType>RowSet</RowSetType>
            <SubRowId>SR_1</SubRowId>
            <SubRowId>SR_2</SubRowId>
         """)
    }

    def "export RowSet of HEADER_FOOTER defined by general"() {
        given:
        GeneralRowSet rowSet = new RowSetImpl() as GeneralRowSet
        rowSet.setType(HEADER_AND_FOOTER)
                .addRowSet(new RowSetImpl())
                .addRowSet(new RowSetImpl())
                .addRowSet(new RowSetImpl())
                .addRowSet(new RowSetImpl())
                .addRowSet(new RowSetImpl())

        when:
        (rowSet as RowSetImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
            <RowSetType>HeaderFooter</RowSetType>
            <SubRowId>SR_1</SubRowId>
            <SubRowId>SR_2</SubRowId>
            <SubRowId>SR_3</SubRowId>
            <SubRowId>SR_4</SubRowId>
            <SubRowId>SR_5</SubRowId>
             """)
    }

    def "export RowSet of HEADER_FOOTER type"() {
        given:
        HeaderFooterRowSet rowSet = new RowSetImpl().initAsHeaderFooterRowSet()
        rowSet.setFirstHeader(new RowSetImpl())
                .setHeader(new RowSetImpl())
                .setBody(new RowSetImpl())
                .setFooter(new RowSetImpl())
                .setLastFooter(new RowSetImpl())

        when:
        (rowSet as RowSetImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
            <RowSetType>HeaderFooter</RowSetType>
            <SubRowId>SR_1</SubRowId>
            <SubRowId>SR_2</SubRowId>
            <SubRowId>SR_3</SubRowId>
            <SubRowId>SR_4</SubRowId>
            <SubRowId>SR_5</SubRowId>
             """)
    }

    def "export RowSet of HEADER_FOOTER type with empty RowSets"() {
        given:
        HeaderFooterRowSet rowSet = new RowSetImpl().initAsHeaderFooterRowSet()
        rowSet.setHeader(new RowSetImpl())

        when:
        (rowSet as RowSetImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
            <RowSetType>HeaderFooter</RowSetType>
            <SubRowId></SubRowId>
            <SubRowId>SR_1</SubRowId>
            <SubRowId></SubRowId>
            <SubRowId></SubRowId>
            <SubRowId></SubRowId>
             """)
    }

    def "export With Empty RowSets"() {
        given:
        GeneralRowSet rowSet = new RowSetImpl().setType(HEADER_AND_FOOTER) as GeneralRowSet
        rowSet.addRowSet(new RowSetImpl())
                .addEmptyRowSet()
                .addRowSet(new RowSetImpl())
                .addEmptyRowSet()
                .addEmptyRowSet()

        when:
        (rowSet as RowSetImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
                <RowSetType>HeaderFooter</RowSetType>
                <SubRowId>SR_1</SubRowId>
                <SubRowId></SubRowId>
                <SubRowId>SR_2</SubRowId>
                <SubRowId></SubRowId>
                <SubRowId></SubRowId>
                 """)
    }


    def "export repeated RowSet"() {
        given:
        GeneralRowSet rowSet = new RowSetImpl().setType(REPEATED) as GeneralRowSet

        Variable variable = new VariableImpl()
        GeneralRowSet innerRowSet = new RowSetImpl()
        String innerRowSetId = exporter.idRegister.getOrCreateId(innerRowSet)
        String variableId = exporter.idRegister.getOrCreateId(variable)

        rowSet.setVariable(variable)
                .setRowSet(innerRowSet)

        when:
        (rowSet as RowSetImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
            <RowSetType>Repeated</RowSetType>
            <SubRowId>$innerRowSetId</SubRowId>
            <VariableId>$variableId</VariableId>
            """)

    }

    def "export empty selectByCondition RowSet"() {
        given:
        GeneralRowSet rowSet = new RowSetImpl() as GeneralRowSet
        rowSet.setType(SELECT_BY_CONDITION)

        when:
        (rowSet as RowSetImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                        <RowSetType>Condition</RowSetType>
                        <RowSetCondition>
                            <SubRowId></SubRowId>
                            <VariableId/>
                        </RowSetCondition>
                        <TreatDefaultAsError>False</TreatDefaultAsError>
                        """)
    }

    def "export SelectByCondition RowSet with all values set"() {
        given:
        Variable variable = new VariableImpl()
        Variable secondVariable = new VariableImpl()
        GeneralRowSet innerRowSet = new RowSetImpl()
        GeneralRowSet secondInnerRowSet = new RowSetImpl()
        GeneralRowSet defRowSet = new RowSetImpl()

        String idInnerRowSet = exporter.idRegister.getOrCreateId(innerRowSet)
        String idVariable = exporter.idRegister.getOrCreateId(variable)
        String idDefRowSet = exporter.idRegister.getOrCreateId(defRowSet)
        String idSecondVariable = exporter.idRegister.getOrCreateId(secondVariable)
        String idSecondInnerRowSet = exporter.idRegister.getOrCreateId(secondInnerRowSet)

        GeneralRowSet rowSet = new RowSetImpl() as GeneralRowSet
        rowSet.setType(SELECT_BY_CONDITION)
                .addLineForSelectByCondition(variable, innerRowSet)
                .setDefaultRowSet(defRowSet)
                .addLineForSelectByCondition(secondVariable, secondInnerRowSet)
                .setDefaultError(true)

        when:
        (rowSet as RowSetImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                        <RowSetType>Condition</RowSetType>
                        <RowSetCondition>
                            <SubRowId>$idInnerRowSet</SubRowId>
                            <VariableId>$idVariable</VariableId>
                        </RowSetCondition>
                        <RowSetCondition>
                            <SubRowId>$idSecondInnerRowSet</SubRowId>
                            <VariableId>$idSecondVariable</VariableId>
                        </RowSetCondition>
                        <RowSetCondition>
                            <SubRowId>$idDefRowSet</SubRowId>
                            <VariableId/>
                        </RowSetCondition>
                        <TreatDefaultAsError>True</TreatDefaultAsError>
                        """)
    }

    def "export empty selectByInlineCondition RowSet"() {
        given:
        GeneralRowSet rowSet = new RowSetImpl() as GeneralRowSet
        rowSet.setType(SELECT_BY_INLINE_CONDITION)

        when:
        (rowSet as RowSetImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                        <RowSetType>InlCond</RowSetType>
                        <RowSetCondition>
                            <SubRowId></SubRowId>
                            <Condition/>
                        </RowSetCondition>
                        <VariableId/>  
                        <TreatDefaultAsError>False</TreatDefaultAsError>
                        """)
    }

    def "export SelectByInlineCondition RowSet with all values set"() {
        given:
        GeneralRowSet innerRowSet = new RowSetImpl()
        GeneralRowSet secondInnerRowSet = new RowSetImpl()
        GeneralRowSet defRowSet = new RowSetImpl()

        String idInnerRowSet = exporter.idRegister.getOrCreateId(innerRowSet)
        String idDefRowSet = exporter.idRegister.getOrCreateId(defRowSet)
        String idSecondInnerRowSet = exporter.idRegister.getOrCreateId(secondInnerRowSet)

        GeneralRowSet rowSet = new RowSetImpl() as GeneralRowSet
        rowSet.setType(SELECT_BY_INLINE_CONDITION)
                .addLineForSelectByInlineCondition("Test Script", innerRowSet)
                .addLineForSelectByInlineCondition("Test Script 2", secondInnerRowSet)
                .setDefaultRowSet(defRowSet)
                .setDefaultError(true)

        when:
        (rowSet as RowSetImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                        <RowSetType>InlCond</RowSetType>
                        <RowSetCondition>
                            <SubRowId>$idInnerRowSet</SubRowId>
                            <Condition>Test Script</Condition>
                        </RowSetCondition>
                        <RowSetCondition>
                            <SubRowId>$idSecondInnerRowSet</SubRowId>
                            <Condition>Test Script 2</Condition>
                        </RowSetCondition>
                        <RowSetCondition>
                            <SubRowId>$idDefRowSet</SubRowId>
                            <Condition/>
                        </RowSetCondition>
                        <VariableId/>
                        <TreatDefaultAsError>True</TreatDefaultAsError>
                        """)
    }
}