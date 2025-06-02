package com.quadient.wfdxml.internal.layoutnodes

import com.quadient.wfdxml.api.layoutnodes.Image
import com.quadient.wfdxml.api.layoutnodes.LocationType
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class ImageImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "export empty Image"() {
        given:
        Image image = new ImageImpl() as Image

        when:
        (image as ImageImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                  <ImageType>Simple</ImageType>
                  <ImageDPIX>0.0</ImageDPIX>
                  <ImageDPIY>0.0</ImageDPIY>  
                  <UseResizeWidth>False</UseResizeWidth>
                  <ResizeImageWidth>0.0</ResizeImageWidth>
                  <UseResizeHeight>False</UseResizeHeight>
                  <ResizeImageHeight>0.0</ResizeImageHeight>
                  <MakeTransparent>False</MakeTransparent>
                  <TransparencyR X="255" Y="255"></TransparencyR>
                  <TransparencyG X="255" Y="255"></TransparencyG>
                  <TransparencyB X="255" Y="255"></TransparencyB>
                  <UseDifferentImageSizeForHtml>False</UseDifferentImageSizeForHtml>
                  """)

    }

    def "export allSet Image"() {
        given:
        Image image = new ImageImpl().setImageDiskLocation("C://tmp/image.jpg")
                .setResizeWidthAndHeight(0.08d, 0.4d)
                .setDpiXAndDpiY(70d, 80d)
                .setTransparentR(233, 255)
                .setTransparentG(240, 250)
                .setTransparentB(1, 10)
                .setHtmlWidthAndHeight("500px", "600px") as Image

        when:
        (image as ImageImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                  <ImageType>Simple</ImageType>
                  <ImageLocation>DiskLocation,C://tmp/image.jpg</ImageLocation>
                  <ImageDPIX>70.0</ImageDPIX>
                  <ImageDPIY>80.0</ImageDPIY>  
                  <UseResizeWidth>True</UseResizeWidth>
                  <ResizeImageWidth>0.08</ResizeImageWidth>
                  <UseResizeHeight>True</UseResizeHeight>
                  <ResizeImageHeight>0.4</ResizeImageHeight>
                  <MakeTransparent>True</MakeTransparent>
                  <TransparencyR X="233" Y="255"></TransparencyR>
                  <TransparencyG X="240" Y="250"></TransparencyG>
                  <TransparencyB X="1" Y="10"></TransparencyB>
                  <UseDifferentImageSizeForHtml>True</UseDifferentImageSizeForHtml>
                  <HtmlImageWidthValue>500px</HtmlImageWidthValue>
                  <HtmlImageHeightValue>600px</HtmlImageHeightValue>           
                  """)
    }

    def "export image with ICM location"() {
        given:
        ImageImpl image = new ImageImpl().setImageLocation("vcs://Interactive/StandardPackage/Resources/Images/frog.png", LocationType.ICM)

        when:
        image.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                  <ImageType>Simple</ImageType>
                  <ImageLocation>VCSLocation,vcs://Interactive/StandardPackage/Resources/Images/frog.png</ImageLocation>
                  <ImageDPIX>0.0</ImageDPIX>
                  <ImageDPIY>0.0</ImageDPIY>
                  <UseResizeWidth>False</UseResizeWidth>
                  <ResizeImageWidth>0.0</ResizeImageWidth>
                  <UseResizeHeight>False</UseResizeHeight>
                  <ResizeImageHeight>0.0</ResizeImageHeight>
                  <MakeTransparent>False</MakeTransparent>
                  <TransparencyR X="255" Y="255"></TransparencyR>
                  <TransparencyG X="255" Y="255"></TransparencyG>
                  <TransparencyB X="255" Y="255"></TransparencyB>
                  <UseDifferentImageSizeForHtml>False</UseDifferentImageSizeForHtml>   
                  """)
    }
}