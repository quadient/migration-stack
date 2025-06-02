package com.quadient.wfdxml.internal.layoutnodes

import com.quadient.wfdxml.api.layoutnodes.Page
import com.quadient.wfdxml.internal.layoutnodes.data.VariableImpl
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.api.layoutnodes.Pages.PageConditionType.SELECT_BY_CONDITION
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class PageImplTest extends Specification {

    XmlExporter exporter = new XmlExporter()

    def "export empty page"() {
        given:
        PageImpl page = new PageImpl(null)

        when:
        page.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
            <ConditionType>Simple</ConditionType>
            <NextPageId></NextPageId>
            <ConditionType>Simple</ConditionType>
            <NextPageId></NextPageId>
            <ConditionType>Simple</ConditionType>
            <NextPageId></NextPageId>            
        """)
    }

    def "export page with SelectByCondition type overflow option"() {
        given:
        Page page = new PageImpl(null) as Page

        page.setWidth(0.25)
        page.setHeight(0.3)

        VariableImpl var1 = new VariableImpl()
        VariableImpl var2 = new VariableImpl()
        PageImpl condPage1 = new PageImpl(null)
        PageImpl condPage2 = new PageImpl(null)
        String var1Id = exporter.idRegister.getOrCreateId(var1)
        String var2Id = exporter.idRegister.getOrCreateId(var2)
        PageImpl defaultPage = new PageImpl(null)
        String condPage1Id = exporter.idRegister.getOrCreateId(condPage1)
        String condPage2Id = exporter.idRegister.getOrCreateId(condPage2)
        String defaultPageId = exporter.idRegister.getOrCreateId(defaultPage)

        page.setType(SELECT_BY_CONDITION)
                .addLineForSelectByCondition(var1, 1, condPage1)
                .addLineForSelectByCondition(var2, 2, condPage2)
                .setDefaultRollback(3)
                .setDefaultError(true)
                .setDefaultPage(defaultPage)

        when:
        (page as PageImpl).export(exporter)
        String exportedString = exporter.buildString()

        then:
        assertXmlEqualsWrapRoot(exportedString, """
            <ConditionType>Condition</ConditionType>
            <DefaultPageId>$defaultPageId</DefaultPageId>
            <DefaultRollback>3</DefaultRollback>
            <PageCondition>
              <ConditionId>$var1Id</ConditionId>
              <Rollback>1</Rollback>
              <PageId>$condPage1Id</PageId>
            </PageCondition>
            <PageCondition>
              <ConditionId>$var2Id</ConditionId>
              <Rollback>2</Rollback>
              <PageId>$condPage2Id</PageId>
            </PageCondition>
            <TreatDefaultAsError>True</TreatDefaultAsError>
            <Width>0.25</Width>
            <Height>0.3</Height>
            <ConditionType>Simple</ConditionType>
            <NextPageId/>
            <ConditionType>Simple</ConditionType>
            <NextPageId/>            
           """)
    }
}