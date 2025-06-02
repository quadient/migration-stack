package com.quadient.wfdxml.internal.module.xmldatainput

import com.quadient.wfdxml.api.module.xmldatainput.LocationType
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.api.module.xmldatainput.LocationType.VCS_LOCATION
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEquals
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class XmlDataInputImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "Export empty XMLDataInput"() {
        given:
        XmlDataInputImpl xmlDataInput = new XmlDataInputImpl()
        when:
        xmlDataInput.export(exporter)
        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                        <XMLDataInput>
                            <Id>SR_1</Id>
                            <ModulePos X="0" Y="0"></ModulePos>
                            <AllowAnyOrderOfElements>False</AllowAnyOrderOfElements>                            
                            <CreateByScript>False</CreateByScript>
                        </XMLDataInput> """)
    }

    def "export XMLDataInput with all values"() {
        given:
        XmlDataInputImpl xmlDataInput = new XmlDataInputImpl()
                .setDiskLocation("C:/tmp/XmlExample.xml")
                .setCreateByScript(true)
                .setScript("Test Script")
                .allowAnyOrderOfElements(true)

        xmlDataInput.addRootXmlNode()

        when:
        xmlDataInput.export(exporter)
        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
            <XMLDataInput>
                <Id>SR_1</Id>
                <ModulePos X="0" Y="0"></ModulePos>
                <Location>DiskLocation,C:/tmp/XmlExample.xml</Location>
                <XMLNode Type="Element" Level="1" Optionality="One" Change="Flatten" Name="Root" XMLName="Root"></XMLNode>
                <AllowAnyOrderOfElements>True</AllowAnyOrderOfElements>                
                <CreateByScript>True</CreateByScript>
                <Script>Test Script</Script>
            </XMLDataInput> """)
    }

    def "It is not possible to add two xmlNodes into XmlDataInput"() {
        given:
        XmlDataInputImpl xmlDataInput = new XmlDataInputImpl()
        xmlDataInput.addRootXmlNode()
        when:
        xmlDataInput.addRootXmlNode()
        then:
        IllegalStateException ex = thrown()
        ex.message == "It is not possible to add two XmlNodes directly into XmlDataInput"
    }

    def "export XmlDataInput setFileLocation"() {
        given:
        XmlDataInputImpl xmlDataInput = new XmlDataInputImpl()
                .setFileLocation(VCS_LOCATION, "icm://tmp/XmlExample.xml") as XmlDataInputImpl

        when:
        xmlDataInput.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                         <XMLDataInput>
                            <Id>SR_1</Id>
                            <ModulePos X="0" Y="0"></ModulePos>
                            <Location>VCSLocation,icm://tmp/XmlExample.xml</Location >
                            <AllowAnyOrderOfElements>False</AllowAnyOrderOfElements>                            
                            <CreateByScript>False</CreateByScript>
                        </XMLDataInput>""")
    }


    def "convert LocationsType to xmlName"() {
        when:
        List<String> locationTypes = LocationType.values().collect {
            XmlDataInputImpl dataInput = new XmlDataInputImpl().setFileLocation(it, null) as XmlDataInputImpl
            //noinspection GroovyAccessibility
            dataInput.convertLocationTypeToXmlName()
        }

        then:
        assert locationTypes == ["DiskLocation",
                                 "UnknownLocation",
                                 "VCSLocation",]
    }

    def "export XmlDataInput with simple script"() {
        given:
        XmlDataInputImpl dataInput = new XmlDataInputImpl().setCreateByScript(true).setScript("SIMPLE SCRIPT")

        when:
        dataInput.export(exporter)

        then:
        assertXmlEquals(exporter.buildString(), """
           <XMLDataInput>
               <Id>SR_1</Id>
               <ModulePos X="0" Y="0"></ModulePos>
               <AllowAnyOrderOfElements>False</AllowAnyOrderOfElements>               
               <CreateByScript>True</CreateByScript>
               <Script>SIMPLE SCRIPT</Script>
           </XMLDataInput>
        """)
    }

    def "export XmlDataInput works even without setting setCreateByScript"() {
        given:
        XmlDataInputImpl dataInput = new XmlDataInputImpl().setScript("SIMPLE SCRIPT")

        when:
        dataInput.export(exporter)

        then:
        assertXmlEquals(exporter.buildString(), """
               <XMLDataInput>
                   <Id>SR_1</Id>
                   <ModulePos X="0" Y="0"></ModulePos>
                   <AllowAnyOrderOfElements>False</AllowAnyOrderOfElements>                   
                   <CreateByScript>True</CreateByScript>
                   <Script>SIMPLE SCRIPT</Script>
               </XMLDataInput>
            """)
    }
}