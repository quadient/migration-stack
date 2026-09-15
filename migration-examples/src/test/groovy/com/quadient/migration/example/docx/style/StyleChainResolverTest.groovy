package com.quadient.migration.example.docx.style


import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFStyle
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule

class StyleChainResolverTest {
    @Test
    void "run properties inherit individually and direct false overrides inherited true"() {
        // given: inherited bold, italic and font properties, with bold explicitly disabled on the run
        new XWPFDocument().withCloseable { doc ->
            doc.createStyles()
            def base = style(doc, 'Base', null)
            base.addNewRPr().addNewB()
            base.RPr.addNewRFonts().ascii = 'Arial'
            base.RPr.addNewSz().val = BigInteger.valueOf(23)
            def child = style(doc, 'Child', 'Base')
            child.addNewRPr().addNewI()
            def paragraph = doc.createParagraph()
            def run = paragraph.createRun()
            run.setBold(false)
            // when: properties are resolved through the fallback style and its parent
            def chain = StyleChainResolver.resolveRunPropertyChain(run, 'Child')
            // then: the direct override wins and other properties are inherited individually
            assert StyleChainResolver.resolveFirst(chain, StyleChainResolver.&resolveBold) == false
            assert StyleChainResolver.resolveFirst(chain, StyleChainResolver.&resolveItalic) == true
            assert StyleChainResolver.resolveFirst(chain, StyleChainResolver.&resolveFontName) == 'Arial'
            assert StyleChainResolver.resolveFirst(chain, StyleChainResolver.&resolveFontSize) == 11.5d
            // when: the run explicitly selects the base style
            run.setStyle('Base')
            // then: the fallback child style no longer contributes italic formatting
            assert StyleChainResolver.resolveFirst(StyleChainResolver.resolveRunPropertyChain(run, 'Child'), StyleChainResolver.&resolveItalic) == null
        }
    }

    @Test
    void "paragraph chain terminates cycles and preserves zero overrides"() {
        // given: cyclic parent styles and a direct zero-spacing override
        new XWPFDocument().withCloseable { doc ->
            doc.createStyles()
            style(doc, 'A', 'B').addNewPPr().addNewSpacing().before = BigInteger.valueOf(240)
            style(doc, 'B', 'A').addNewPPr().addNewInd().left = BigInteger.valueOf(720)
            def paragraph = doc.createParagraph()
            paragraph.style = 'A'
            paragraph.CTP.PPr.addNewSpacing().before = BigInteger.ZERO
            // when
            def chain = StyleChainResolver.resolveParagraphPropertyChain(paragraph)
            // then: traversal terminates, preserving zero and inheriting the remaining properties
            assert chain.size() == 4 // Direct properties, A, B, document defaults; no second visit to A.
            assert StyleChainResolver.resolveFirst(chain, StyleChainResolver.&resolveSpacingBefore) == 0
            assert StyleChainResolver.resolveFirst(chain, StyleChainResolver.&resolveIndentationLeft) == 720
            // when: the paragraph references a missing style
            paragraph.style = 'Missing'
            // then: only direct properties and document defaults remain
            assert StyleChainResolver.resolveParagraphPropertyChain(paragraph).size() == 2
        }
    }

    @ParameterizedTest
    @CsvSource(['true,true', 'false,false', '1,true', '0,false', 'on,true', 'off,false'])
    void "on off flags distinguish explicit false from absent properties"(String value, boolean expected) {
        // given: no bold or italic properties
        def properties = CTRPr.Factory.newInstance()
        // when / then: an absent bold property has no resolved value
        assert StyleChainResolver.resolveBold(properties) == null
        // when: explicit bold and italic flags are added
        properties.addNewB().val = value
        properties.addNewI().val = value
        // then: each XML representation resolves to the expected boolean
        assert StyleChainResolver.resolveBold(properties) == expected
        assert StyleChainResolver.resolveItalic(properties) == expected
    }

    @Test
    void "color conversion handles unsigned bytes and ignores automatic colors"() {
        // given: unsigned RGB bytes, a valid hex color and absent or unsupported color values
        // when / then: valid colors resolve, while unsupported values remain absent
        assert StyleChainResolver.colorValueToHex([0, 128, 255] as byte[]) == '0080FF'
        assert StyleChainResolver.toColor('abcdef') != null
        [null, 'auto', '', 'FFF', 'invalid'].each { assert StyleChainResolver.toColor(it) == null }
    }

    @Test
    void "line spacing defaults to auto only when a line value exists"() {
        // given: paragraph properties without spacing
        def properties = CTPPr.Factory.newInstance()
        // when / then: missing spacing remains absent
        assert StyleChainResolver.resolveLineSpacing(properties) == null
        // when: a line value is added without a spacing rule
        def spacing = properties.addNewSpacing()
        spacing.line = BigInteger.valueOf(360)
        // then: the rule defaults to automatic
        assert StyleChainResolver.resolveLineSpacing(properties) == [line: 360L, lineRule: 'auto']
        // when: an explicit rule is supplied
        spacing.lineRule = STLineSpacingRule.EXACT
        // then: the explicit rule is retained
        assert StyleChainResolver.resolveLineSpacing(properties) == [line: 360L, lineRule: 'exact']
    }

    private static CTStyle style(XWPFDocument doc, String id, String parent) {
        def properties = CTStyle.Factory.newInstance()
        properties.styleId = id
        if (parent) properties.addNewBasedOn().val = parent
        doc.styles.addStyle(new XWPFStyle(properties, doc.styles))
        return properties
    }
}
