package com.quadient.migration.example.docx.style

import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleDefinitionBuilder
import com.quadient.migration.shared.LineSpacing
import org.junit.jupiter.api.Test

class DocxParagraphStylesTest {
    @Test
    void "automatic spacing is a line multiple and fixed spacing is in points"() {
        // given: an empty style definition
        def automatic = new ParagraphStyleDefinitionBuilder()
        // when: automatic spacing is supplied in 240ths of a line
        DocxParagraphStyles.applyLineSpacing(automatic, 'auto', 360)
        // then
        assert automatic.build().lineSpacing == new LineSpacing.MultipleOf(1.5d)
        // given: another empty style definition
        def exact = new ParagraphStyleDefinitionBuilder()
        // when: exact spacing is supplied in twips
        DocxParagraphStyles.applyLineSpacing(exact, 'exact', 360)
        // then
        assert exact.build().lineSpacing instanceof LineSpacing.Exact
        assert exact.build().lineSpacing.size.toPoints() == 18d
        // given: another empty style definition
        def minimum = new ParagraphStyleDefinitionBuilder()
        // when: minimum spacing is supplied in twips
        DocxParagraphStyles.applyLineSpacing(minimum, 'atLeast', 240)
        // then
        assert minimum.build().lineSpacing instanceof LineSpacing.AtLeast
        assert minimum.build().lineSpacing.size.toPoints() == 12d
    }
}
