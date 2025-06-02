package com.quadient.wfdxml.internal.layoutnodes.flow

import com.quadient.wfdxml.api.layoutnodes.Image
import com.quadient.wfdxml.api.layoutnodes.flow.Text
import com.quadient.wfdxml.api.layoutnodes.tables.Table
import com.quadient.wfdxml.internal.layoutnodes.FlowImpl
import com.quadient.wfdxml.internal.layoutnodes.ImageImpl
import com.quadient.wfdxml.internal.layoutnodes.TextStyleImpl
import com.quadient.wfdxml.internal.layoutnodes.data.VariableImpl
import com.quadient.wfdxml.internal.layoutnodes.tables.TableImpl
import com.quadient.wfdxml.internal.xml.export.XmlExporter
import spock.lang.Specification

import static com.quadient.wfdxml.utils.AssertXml.assertXmlEquals
import static com.quadient.wfdxml.utils.AssertXml.assertXmlEqualsWrapRoot

class TextImplTest extends Specification {
    XmlExporter exporter = new XmlExporter()

    def "setText serialization"() {
        given:
        TextImpl text = new TextImpl().appendText("Initial text") as TextImpl

        when:
        text.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                     <T xml:space="preserve">Initial text</T>""")
    }

    def "appendText works with null"() {
        given:
        TextImpl text = new TextImpl().appendText(null) as TextImpl

        when:
        text.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                     <T xml:space="preserve"></T>""")
    }

    def "appendText twice serialization"() {
        given:
        TextImpl text = new TextImpl().appendText("Initial text").appendText("Appended text") as TextImpl

        when:
        text.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                    <T xml:space="preserve">Initial textAppended text</T>""")

    }

    def "setTextStyle serialization"() {
        given:
        TextStyleImpl textStyle = new TextStyleImpl()
        TextImpl text = new TextImpl().setTextStyle(textStyle) as TextImpl

        when:
        text.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """ 
                   <T Id="SR_1" xml:space="preserve"/>""")
    }


    def "appendFlow"() {
        given:
        FlowImpl flow = new FlowImpl()
        TextImpl text = new TextImpl().appendFlow(flow) as TextImpl

        when:
        text.export(exporter)

        then:
        assertXmlEquals(exporter.buildString(), """
                    <T xml:space="preserve"><O Id="SR_1"/></T>""")

    }

    def "appendMultipleFlows"() {
        given:
        TextImpl text = new TextImpl()
                .appendFlow(new FlowImpl())
                .appendFlow(new FlowImpl())
                .appendFlow(new FlowImpl()) as TextImpl

        when:
        text.export(exporter)

        then:
        assertXmlEquals(exporter.buildString(), """
                    <T xml:space="preserve">
                    <O Id="SR_1"/>
                    <O Id="SR_2"/>
                    <O Id="SR_3"/>
                    </T>""")
    }

    def "appendFlow into text"() {
        given:
        TextImpl text = new TextImpl()
                .appendText("Text example")
                .appendFlow(new FlowImpl())
                .appendText("Text example") as TextImpl

        when:
        text.export(exporter)

        then:
        assertXmlEquals(exporter.buildString(), """
                    <T xml:space="preserve">Text example <O Id="SR_1"/>Text example 
                    </T>""")

    }

    def "appendVariable serialization"() {
        given:
        VariableImpl var = new VariableImpl()
        TextImpl text = new TextImpl()
                .appendVariable(var) as TextImpl

        when:
        text.export(exporter)

        then:

        assertXmlEqualsWrapRoot(exporter.buildString(), """
                    <T xml:space="preserve"><O Id="SR_1"/></T>""")
    }

    def "appendTable serialization"() {
        given:
        Table table = new TableImpl()
        Text text = new TextImpl()
                .appendTable(table)

        when:
        (text as TextImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                        <T xml:space="preserve"><O Id="SR_1"/></T>""")
    }

    def "appendVariable into text serialization"() {
        given:
        VariableImpl var = new VariableImpl()
        TextImpl text = new TextImpl()
                .appendText("Text example 1")
                .appendVariable(var)
                .appendText("Text example 2") as TextImpl

        when:
        text.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                    <T xml:space="preserve">Text example 1<O Id="SR_1"/>Text example 2</T>
                    """)

    }

    def "multiple appendVariable serialization"() {
        given:
        VariableImpl var = new VariableImpl()
        VariableImpl var2 = new VariableImpl()
        VariableImpl var3 = new VariableImpl()

        TextImpl text = new TextImpl()
                .appendVariable(var)
                .appendVariable(var2)
                .appendVariable(var3) as TextImpl

        when:
        text.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                     <T xml:space="preserve">
                        <O Id="SR_1"/>
                        <O Id="SR_2"/>
                        <O Id="SR_3"/>
                     </T>
                    """)
    }

    def "replacing tab in text serialization"() {
        given:
        TextImpl text = new TextImpl()
                .appendText("Text sample 1\tText sample 2") as TextImpl

        when:
        text.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                    <T xml:space="preserve">
                        Text sample 1<Tab></Tab>Text sample 2
                    </T>""")
    }

    def "replacing two tab in text serialization"() {
        given:
        TextImpl text = new TextImpl()
                .appendText("Text sample 1\t\tText sample 2") as TextImpl

        when:
        text.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                    <T xml:space="preserve">
                        Text sample 1<Tab></Tab><Tab></Tab>Text sample 2
                    </T>""")
    }

    def "replacing tab at the beginning text serialization"() {
        given:
        TextImpl text = new TextImpl()
                .appendText("\tText sample 1") as TextImpl

        when:
        text.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                    <T xml:space="preserve">
                        <Tab></Tab>Text sample 1
                    </T>""")
    }

    def "replacing tab at the end of text serialization"() {
        given:
        TextImpl text = new TextImpl()
                .appendText("Text sample 1\t") as TextImpl

        when:
        text.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                    <T xml:space="preserve">
                         Text sample 1<Tab></Tab>
                    </T>""")
    }

    def "replacing multi tabs serialization"() {
        given:
        TextImpl text = new TextImpl()
                .appendText("\t\t\tText sample 1\t\t\tText sample 2\t\t\t") as TextImpl

        when:
        text.export(exporter)

        then:
        def result = exporter.buildString()
        assertXmlEqualsWrapRoot(result, """
                  <T xml:space="preserve">
                      <Tab/>
                      <Tab/>
                      <Tab/>
                      Text sample 1
                      <Tab/>
                      <Tab/>
                      <Tab/>
                      Text sample 2
                      <Tab/>
                      <Tab/>
                      <Tab/>
                  </T>""")
    }

    def "replacing newLine serialization"() {
        given:
        TextImpl text = new TextImpl()
                .appendText("Text sample\nnew line") as TextImpl

        when:
        text.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                    <T xml:space="preserve">Text sample<BR/>new line</T>
                    """)
    }

    def "replacing newLine at the beginning text serialization"() {
        given:
        TextImpl text = new TextImpl()
                .appendText("\nText sample new line") as TextImpl

        when:
        text.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                    <T xml:space="preserve"><BR></BR>Text sample new line</T>
                    """)
    }

    def "replacing newLine at the end text serialization"() {
        given:
        TextImpl text = new TextImpl()
                .appendText("Text sample new line\n") as TextImpl

        when:
        text.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                    <T xml:space="preserve">Text sample new line<BR></BR></T>
                    """)
    }

    def "replacing multi newLine in text serialization"() {
        given:
        TextImpl text = new TextImpl()
                .appendText("Text sample\n\nnew line\n") as TextImpl

        when:
        text.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                    <T xml:space="preserve">Text sample<BR></BR><BR></BR>new line<BR></BR></T>
                    """)
    }

    def "replacing newLine and tab serialization"() {
        given:
        TextImpl text = new TextImpl()
                .appendText("Text sample\tnew line\n") as TextImpl

        when:
        text.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
                    <T xml:space="preserve">Text sample<Tab></Tab>new line<BR></BR></T>
                    """)
    }

    def "replacing tab remove tab char "() {
        given:
        TextImpl textImpl = new TextImpl().appendText("\tText sample") as TextImpl

        when:
        textImpl.export(exporter)
        String text = exporter.buildString()

        then:
        assert !text.contains("\t")
    }

    def "replacing line remove newLine char"() {
        given:
        TextImpl textImpl = new TextImpl().appendText("Text sample \n") as TextImpl

        when:
        textImpl.export(exporter)
        String text = exporter.buildString()

        then:
        assert !text.contains("\n")
    }

    def "export appendImage"() {
        given:
        Image image = new ImageImpl() as Image
        Text text = new TextImpl().appendImage(image)

        when:
        (text as TextImpl).export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(), """
               <T xml:space="preserve">
                   <O Id="SR_1"></O>
               </T>""")

    }

    def "export Text with ExistingTextStyle"() {
        given:
        TextImpl text = new TextImpl()

        text.setExistingTextStyle("TextStyles.Existing Text Style")

        when:
        text.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(),
                """<T xml:space="preserve" Id="TextStyles.Existing Text Style"></T>""")
    }

    def "exporting Text with both TextStyle and ExistingTextStyle prefers TextStyle"() {
        given:
        TextImpl text = new TextImpl()

        text.setTextStyle(new TextStyleImpl())
        text.setExistingTextStyle("TextStyles.Existing Text Style")

        when:
        text.export(exporter)

        then:
        assertXmlEqualsWrapRoot(exporter.buildString(),
                """<T xml:space="preserve" Id="SR_1"></T>""")
    }
}
