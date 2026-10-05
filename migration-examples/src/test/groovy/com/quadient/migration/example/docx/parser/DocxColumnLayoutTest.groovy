package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.ColumnLayout
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.shared.DocumentObjectType
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STSectionMark

import java.nio.file.Files

import static com.quadient.migration.example.Utils.mockMigration
import static org.mockito.Mockito.atLeastOnce
import static org.mockito.Mockito.verify

class DocxColumnLayoutTest {

    @Test
    void "terms and conditions page flow begins with its two-column layout"() {
        // given: the supplied sample switches to two columns on its second page
        File sample = new File(getClass().getResource('/exampleResources/docx/02_terms_and_conditions.docx').toURI())

        // when
        def migration = mockMigration()
        def pages = DocxTemplateParser.parsePages(migration, sample,
                new DocumentObjectBuilder('terms', DocumentObjectType.Template), 'terms')

        // then: the marker begins the generated block that contains the second page's flow content
        Area secondPageFlow = pages[1].content.find { it instanceof Area } as Area
        assert !(secondPageFlow.content[0] instanceof ColumnLayout)
        def blockCaptor = ArgumentCaptor.forClass(DocumentObject)
        verify(migration.documentObjectRepository, atLeastOnce()).upsert(blockCaptor.capture())
        DocumentObject columnBlock = blockCaptor.allValues.find { it.content && it.content[0] instanceof ColumnLayout }
        ColumnLayout layout = columnBlock.content[0] as ColumnLayout
        assert layout.numberOfColumns == 2
        assert layout.gutterWidth.toPoints() == 17d
        assert layout.applyTo.name() == 'ThisBlockOnly'
    }

    @Test
    void "continuous two-column section is inserted at its position within the page flow"() {
        File sample = Files.createTempFile('continuous-columns', '.docx').toFile()
        try {
            new XWPFDocument().withCloseable { doc ->
                doc.createStyles()
                doc.createParagraph().createRun().setText('Single-column content')
                def boundary = doc.createParagraph()
                boundary.createRun().setText('End of first section')
                boundary.CTP.addNewPPr().addNewSectPr().addNewCols().space = BigInteger.valueOf(720)
                doc.createParagraph().createRun().setText('Two-column content')
                def finalSection = doc.document.body.addNewSectPr()
                finalSection.addNewType().val = STSectionMark.CONTINUOUS
                def columns = finalSection.addNewCols()
                columns.num = BigInteger.valueOf(2)
                columns.space = BigInteger.valueOf(720)
                Files.newOutputStream(sample.toPath()).withCloseable { doc.write(it) }
            }

            // when
            def pages = DocxTemplateParser.parsePages(mockMigration(), sample,
                    new DocumentObjectBuilder('continuous', DocumentObjectType.Template), 'continuous')

            // then: the final section shares the page but introduces its layout only after the preceding content
            assert pages.size() == 1
            Area flow = pages[0].content.find { it instanceof Area } as Area
            int layoutIndex = flow.content.findIndexOf { it instanceof ColumnLayout }
            assert layoutIndex > 0
            assert (flow.content[layoutIndex] as ColumnLayout).numberOfColumns == 2
        } finally {
            Files.deleteIfExists(sample.toPath())
        }
    }
}
