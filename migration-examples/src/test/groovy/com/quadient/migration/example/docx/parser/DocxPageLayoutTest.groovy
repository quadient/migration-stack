package com.quadient.migration.example.docx.parser

import com.quadient.migration.shared.Size
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr

class DocxPageLayoutTest {
    @Test
    void "page dimensions and asymmetric margins are converted from twips"() {
        // given: explicit page dimensions and different margins on each side
        def section = section(12240, 15840, 720, 1440, 1080, 360)
        // when / then: page size is converted to points
        assert DocxPageLayout.resolvePageSize(section)*.toPoints() == [612d, 792d]
        // when: content bounds are resolved
        def position = DocxPageLayout.resolvePageContentPosition(section)
        // then: margins determine both the content origin and remaining space
        assert [position.x, position.y, position.width, position.height]*.toPoints() == [18d, 36d, 522d, 702d]
    }

    @Test
    void "missing section geometry uses defaults"() {
        // given: absent or empty section properties
        [null, CTSectPr.Factory.newInstance()].each {
            // when / then: both page size and content bounds use defaults
            assert DocxPageLayout.resolvePageSize(it) == DocxPageLayout.DEFAULT_PAGE_SIZE
            assert DocxPageLayout.resolvePageContentPosition(it) == DocxPageLayout.DEFAULT_CONTENT_POSITION
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ['w', 'h'])
    void "supplied page dimension survives when the other dimension and margins are missing"(String dimension) {
        // given: just one page dimension, with no margin element
        def incomplete = CTSectPr.Factory.newInstance()
        incomplete.addNewPgSz()."${dimension}" = BigInteger.valueOf(14400)

        // when
        def size = DocxPageLayout.resolvePageSize(incomplete)
        def position = DocxPageLayout.resolvePageContentPosition(incomplete)

        // then: the supplied dimension is 720 points; only the missing values use defaults
        double width = dimension == 'w' ? 720d : Size.ofMillimeters(210).toPoints()
        double height = dimension == 'h' ? 720d : Size.ofMillimeters(297).toPoints()
        assert size*.toPoints() == [width, height]
        assert Math.abs(position.width.toPoints() - (width - Size.ofMillimeters(40).toPoints())) < 1e-9
        assert Math.abs(position.height.toPoints() - (height - Size.ofMillimeters(57).toPoints())) < 1e-9
        assert position.x == Size.ofMillimeters(20)
        assert position.y == Size.ofMillimeters(20)
    }

    @ParameterizedTest
    @CsvSource(['left,0', 'right,360', 'top,0', 'bottom,360'])
    void "each supplied margin survives when page size and other margins are missing"(String side, long twips) {
        // given: one explicit margin, including valid zero margins
        def incomplete = CTSectPr.Factory.newInstance()
        incomplete.addNewPgMar()."${side}" = BigInteger.valueOf(twips)

        // when
        def position = DocxPageLayout.resolvePageContentPosition(incomplete)

        // then: only the specified margin changes the default content bounds
        def margins = [left: 20d, right: 20d, top: 20d, bottom: 37d].collectEntries {
            key, mm -> [key, Size.ofMillimeters(mm).toPoints()]
        }
        margins[side] = twips / 20d
        def expected = [margins.left, margins.top,
                        Size.ofMillimeters(210).toPoints() - margins.left - margins.right,
                        Size.ofMillimeters(297).toPoints() - margins.top - margins.bottom]
        [position.x, position.y, position.width, position.height].eachWithIndex { value, i ->
            assert Math.abs(value.toPoints() - expected[i]) < 1e-9
        }
    }

    @ParameterizedTest
    @CsvSource(['100,100,0,50,0,50', '100,100,60,0,50,0', '100,100,0,110,0,0'])
    void "nonpositive content dimensions use default position"(long w, long h, long top, long right, long bottom, long left) {
        // given: parameterized margins that leave no positive content width or height
        // when / then: resolving the content bounds returns the default position
        assert DocxPageLayout.resolvePageContentPosition(section(w, h, top, right, bottom, left)) == DocxPageLayout.DEFAULT_CONTENT_POSITION
    }

    private static CTSectPr section(long w, long h, long top, long right, long bottom, long left) {
        def section = CTSectPr.Factory.newInstance()
        def size = section.addNewPgSz()
        size.w = BigInteger.valueOf(w)
        size.h = BigInteger.valueOf(h)
        def margins = section.addNewPgMar()
        margins.top = BigInteger.valueOf(top)
        margins.right = BigInteger.valueOf(right)
        margins.bottom = BigInteger.valueOf(bottom)
        margins.left = BigInteger.valueOf(left)
        return section
    }
}
