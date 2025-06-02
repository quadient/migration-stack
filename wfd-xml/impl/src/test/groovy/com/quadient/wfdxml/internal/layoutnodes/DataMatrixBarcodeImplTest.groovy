package com.quadient.wfdxml.internal.layoutnodes

import com.quadient.wfdxml.api.layoutnodes.Barcode
import com.quadient.wfdxml.api.layoutnodes.DataMatrixBarcode
import com.quadient.wfdxml.api.layoutnodes.FillStyle
import com.quadient.wfdxml.api.layoutnodes.TextStyle
import com.quadient.wfdxml.api.layoutnodes.data.Variable
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.api.layoutnodes.Barcode.HorizontalAlign
import static com.quadient.wfdxml.api.layoutnodes.Barcode.PositionType
import static com.quadient.wfdxml.api.layoutnodes.Barcode.VerticalAlign
import static com.quadient.wfdxml.api.layoutnodes.DataMatrixBarcode.DataMatrixEncodingType
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class DataMatrixBarcodeImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "def Matrix Data barcode serialization"() {
        given:
        Barcode barcode = new DataMatrixBarcodeImpl()

        when:
        barcode.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
          <Pos X="0.0" Y="0.0"></Pos>
          <Size X="0.1" Y="0.1"></Size>
          <Rotation>0.0</Rotation>
          <Skew>0.0</Skew>
          <FlipX>False</FlipX>
          <Scale X="1.0" Y="1.0"></Scale>
          <RotationPointX>0.0</RotationPointX>
          <RotationPointY>0.0</RotationPointY>
          <RotationRound>0.0</RotationRound>
          <BarcodeName>Data Matrix</BarcodeName>
          <VariableId></VariableId>
          <FillStyleId></FillStyleId>
          <FillBackgroungStyleId></FillBackgroungStyleId>
          <UseCodec>False</UseCodec>
          <TextCodec>UTF-8</TextCodec>
          <HorizontalAlign>Left</HorizontalAlign>
          <VerticalAlign>Top</VerticalAlign>
          <DataTextPosition>None</DataTextPosition>
          <DataTextDeltaX>0.0</DataTextDeltaX>
          <DataTextDeltaY>0.0</DataTextDeltaY>
          <TextStyleId></TextStyleId>
          <ShowDataTextProcessed>False</ShowDataTextProcessed>
          <BarcodeGenerator>
                <Type>DataMatrixBarcodeGenerator</Type>
                <ModulWidth>0.0</ModulWidth>
                <WhiteSpace>0.0</WhiteSpace>
                <WiziwayMode>False</WiziwayMode>
                <UsePnet3Encoding>True</UsePnet3Encoding>
                <SymbolSizeIndex>0.0</SymbolSizeIndex>
                <CompressType>Autodetect</CompressType>
                <UseDriveCharacter>True</UseDriveCharacter>
           </BarcodeGenerator>""")
    }

    def "Matrix Data barcode allSet serialization"() {
        given:
        FillStyle barcodeFill = Mock()
        FillStyle backgroundFillStyle = Mock()
        Variable variable = Mock()
        TextStyle textStyle = Mock()

        String barcodeFillId = exporter.idRegister.getOrCreateId(barcodeFill)
        String backgroundFillStyleId = exporter.idRegister.getOrCreateId(backgroundFillStyle)
        String variableId = exporter.idRegister.getOrCreateId(variable)
        String textStyleId = exporter.idRegister.getOrCreateId(textStyle)

        DataMatrixBarcode dataMatrixBarcode = new DataMatrixBarcodeImpl()
                .setPositionAndSize(0.5d, 0.2d, 0.4d, 0.3d)
                .setData("Test data 1")
                .setVariable(variable)
                .setBarcodeFill(barcodeFill)
                .setBackgroundFill(backgroundFillStyle)
                .setUseEncoding(true)
                .setEncoding("UTF-32")
                .setSettingsFile("C://tmp/setting.xml")
                .setHorizontalAlign(HorizontalAlign.CENTER)
                .setVerticalAlign(VerticalAlign.BOTTOM)
                .setDataTextPosition(PositionType.BOTTOM_RIGHT)
                .setDataTextDeltaX(15d)
                .setDataTextDeltaY(20d)
                .setDataTextStyle(textStyle)
                .setShowDataTextProcessed(true)
                .setModuleWidth(0.18d)
                .setQuitZoneRatio(2)
                .setUsePNet3Encoding(false)
                .setWiziwayBarcode(true)
                .setDataMatrixEncoding(DataMatrixEncodingType.ASCII)
                .setUseTildaCharacter(false) as DataMatrixBarcode

        when:
        (dataMatrixBarcode as BarcodeImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
          <Pos X="0.5" Y="0.2"></Pos>
          <Size X="0.4" Y="0.3"></Size>
          <Rotation>0.0</Rotation>
          <Skew>0.0</Skew>
          <FlipX>False</FlipX>
          <Scale X="1.0" Y="1.0"></Scale>
          <RotationPointX>0.0</RotationPointX>
          <RotationPointY>0.0</RotationPointY>
          <RotationRound>0.0</RotationRound>
          <BarcodeName>Data Matrix</BarcodeName>
          <ConvertString>Test data 1</ConvertString>
          <VariableId>$variableId</VariableId>
          <FillStyleId>$barcodeFillId</FillStyleId>
          <FillBackgroungStyleId>$backgroundFillStyleId</FillBackgroungStyleId>
          <UseCodec>True</UseCodec>
          <TextCodec>UTF-32</TextCodec>
          <SettingsLocation>DiskLocation,C://tmp/setting.xml</SettingsLocation>
          <HorizontalAlign>Center</HorizontalAlign>
          <VerticalAlign>Bottom</VerticalAlign>
          <DataTextPosition>BottomRight</DataTextPosition>
          <DataTextDeltaX>15.0</DataTextDeltaX>
          <DataTextDeltaY>20.0</DataTextDeltaY>
          <TextStyleId>$textStyleId</TextStyleId>  
          <ShowDataTextProcessed>True</ShowDataTextProcessed>
          <BarcodeGenerator>
                <Type>DataMatrixBarcodeGenerator</Type>
                <ModulWidth>0.18</ModulWidth>
                <WhiteSpace>2.0</WhiteSpace>
                <WiziwayMode>True</WiziwayMode>
                <UsePnet3Encoding>False</UsePnet3Encoding>
                <SymbolSizeIndex>0.0</SymbolSizeIndex>
                <CompressType>Ascii</CompressType>
                <UseDriveCharacter>False</UseDriveCharacter>
           </BarcodeGenerator>""")
    }

    def "convert HorizontalAlign to xml name"() {
        expect:
        BarcodeImpl.convertHorizontalAlignToXmlName(alings) == expectedResult

        where:
        expectedResult | _
        "Left"         | _
        "Center"       | _
        "Right"        | _

        alings << HorizontalAlign.values().toList()
    }

    def "convert VerticalAlign to xml name"() {
        expect:
        BarcodeImpl.convertVerticalAlignToXmlName(alings) == expectedResult

        where:
        expectedResult | _
        "Top"          | _
        "Center"       | _
        "Bottom"       | _

        alings << VerticalAlign.values().toList()
    }

    def "convert Position to xml name"() {
        expect:
        BarcodeImpl.convertPositionTypeToXmlName(types) == expectedResult

        where:
        expectedResult | _
        "None"         | _
        "TopLeft"      | _
        "TopCenter"    | _
        "TopRight"     | _
        "BottomLeft"   | _
        "BottomCenter" | _
        "BottomRight"  | _

        types << PositionType.values().toList()
    }

    def "convert DataMatrixEncodingType to xml name"() {
        expect:
        DataMatrixBarcodeImpl.convertEncodingTypeToXmlName(types) == expectedResult

        where:
        expectedResult | _
        "Autodetect"   | _
        "Ascii"        | _
        "Base11"       | _
        "Base41"       | _
        "Base256"      | _

        types << DataMatrixEncodingType.values().toList()
    }
}