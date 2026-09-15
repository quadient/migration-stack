package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphBuilder
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHAnchor
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STVAnchor

class DocxFloatingTablesTest {
    @ParameterizedTest
    @CsvSource(['page,10,20,720', 'margin,50,80,660'])
    void "floating table uses anchor offsets and grid width"(String origin, double x, double y, double height) {
        // given: a table that initially has no floating properties
        new XWPFDocument().withCloseable { doc ->
            def table = doc.createTable()
            // when / then: the initial table is inline
            assert !DocxFloatingTables.isFloating(table)
            // given: floating offsets and two explicit grid column widths
            def properties = table.CTTbl.tblPr.addNewTblpPr()
            properties.horzAnchor = STHAnchor.Enum.forString(origin)
            properties.vertAnchor = STVAnchor.Enum.forString(origin)
            properties.tblpX = BigInteger.valueOf(200)
            properties.tblpY = BigInteger.valueOf(400)
            def grid = table.CTTbl.addNewTblGrid()
            grid.addNewGridCol().w = BigInteger.valueOf(1000)
            grid.addNewGridCol().w = BigInteger.valueOf(2000)
            // when / then: adding positioning properties makes the table floating
            assert DocxFloatingTables.isFloating(table)
            // given: content for the floating area
            def content = [new ParagraphBuilder().build()]
            // when
            def area = DocxFloatingTables.buildFloatingTableArea(table, content, pagePosition())
            // then: content is preserved and geometry uses offsets and the total grid width
            assert area.content == content
            assert [area.position.x, area.position.y, area.position.width, area.position.height]*.toPoints() == [x, y, 150d, height]
        }
    }

    @Test
    void "table without grid uses content width and off-page table retains minimum height"() {
        // given: a floating table below the page content, with no column grid
        new XWPFDocument().withCloseable { doc ->
            def table = doc.createTable()
            def properties = table.CTTbl.tblPr.addNewTblpPr()
            properties.tblpY = BigInteger.valueOf(20000)
            // when / then: empty content does not create an area
            assert DocxFloatingTables.buildFloatingTableArea(table, [], pagePosition()) == null
            // when: nonempty content is supplied
            def area = DocxFloatingTables.buildFloatingTableArea(table, [new ParagraphBuilder().build()], pagePosition())
            // then: width falls back to the page content and height remains positive
            assert area.position.width.toPoints() == 500d
            assert area.position.height.toPoints() == 10d
        }
    }

    private static Position pagePosition() {
        return new Position(Size.ofPoints(40), Size.ofPoints(60), Size.ofPoints(500), Size.ofPoints(680))
    }
}
