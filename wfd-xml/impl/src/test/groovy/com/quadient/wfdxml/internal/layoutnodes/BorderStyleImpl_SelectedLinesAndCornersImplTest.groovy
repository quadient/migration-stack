package com.quadient.wfdxml.internal.layoutnodes

import com.quadient.wfdxml.internal.layoutnodes.BorderStyleImpl.SelectedLinesAndCornersImpl
import spock.lang.Specification

import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.ALL_BORDERS
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.ALL_CORNERS
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.ALL_LINES
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.BOTTOM_LINE
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.LEFT_LINE
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.LOWER_LEFT_CORNER
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.LOWER_RIGHT_CORNER
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.MAIN_DIAGONAL_LINE
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.RIGHT_LINE
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.SECONDARY_DIAGONAL_LINE
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.TOP_LINE
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.UPPER_LEFT_CORNER
import static com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle.LinesAndCorners.UPPER_RIGHT_CORNER

@SuppressWarnings("GroovyAssignabilityCheck")
class BorderStyleImpl_SelectedLinesAndCornersImplTest extends Specification {
    BorderStyleImpl bs = new BorderStyleImpl()

    def "this dies"() {
        when:
        new SelectedLinesAndCornersImpl(bs)

        then:
        thrown IllegalArgumentException
    }

    def "SimpleSelect select correct LinesAndCorners"() {
        when:
        def selectedLinesAndCorners = new SelectedLinesAndCornersImpl(bs, LEFT_LINE, MAIN_DIAGONAL_LINE)

        then:
        selectedLinesAndCorners.getLinesAndCorners() == [LEFT_LINE, MAIN_DIAGONAL_LINE] as Set
    }


    def "AllLines select correct LinesAndCorners"() {
        when:
        def selectedLinesAndCorners = new SelectedLinesAndCornersImpl(bs, ALL_LINES)

        then:
        selectedLinesAndCorners.getLinesAndCorners() == [TOP_LINE, RIGHT_LINE, BOTTOM_LINE, LEFT_LINE] as Set
    }

    def "AllCorners select correct LinesAndCorners"() {
        when:
        def selectedLinesAndCorners = new SelectedLinesAndCornersImpl(bs, ALL_CORNERS)

        then:
        selectedLinesAndCorners.getLinesAndCorners() == [UPPER_LEFT_CORNER, UPPER_RIGHT_CORNER, LOWER_RIGHT_CORNER, LOWER_LEFT_CORNER] as Set
    }

    def "AllBorders select correct LinesAndCorners"() {
        when:
        def selectedLinesAndCorners = new SelectedLinesAndCornersImpl(bs, ALL_BORDERS)

        then:
        selectedLinesAndCorners.getLinesAndCorners() == [TOP_LINE, RIGHT_LINE, BOTTOM_LINE, LEFT_LINE, UPPER_LEFT_CORNER, UPPER_RIGHT_CORNER, LOWER_RIGHT_CORNER, LOWER_LEFT_CORNER] as Set
    }

    def "AllLines and seconderyDiagonalLine select correct LinesAndCorners"() {
        when:
        def selectedLinesAndCorners = new SelectedLinesAndCornersImpl(bs, ALL_LINES, SECONDARY_DIAGONAL_LINE, RIGHT_LINE)

        then:
        selectedLinesAndCorners.getLinesAndCorners() == [TOP_LINE, RIGHT_LINE, BOTTOM_LINE, LEFT_LINE, SECONDARY_DIAGONAL_LINE] as Set
    }

    def "BorderStyle.select() with no corners throw exception"() {
        when:
        new BorderStyleImpl().select()

        then:
        IllegalArgumentException ex = thrown()
        ex.message == "At least one Line or Corner have to be selected."
    }
}