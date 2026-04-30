package com.quadient.migration.api.dto.migrationmodel.builder.documentcontent

import com.quadient.migration.api.dto.migrationmodel.Shape
import com.quadient.migration.api.dto.migrationmodel.builder.PositionBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasName
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasPosition
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.ShapePath
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size

class ShapeBuilder: HasName<ShapeBuilder>, HasPosition<ShapeBuilder> {
    override var name: String? = null
    private val paths: MutableList<ShapePath> = mutableListOf()
    override var position: Position? = null
    private var fill: Color? = null
    private var lineFill: Color? = null
    private var lineWidth: Size = Size.ofMillimeters(0.2)

    /**
     * Sets the fill color for the shape interior.
     * @param fill The fill color.
     * @return This builder instance for method chaining.
     */
    fun fill(fill: Color) = apply { this.fill = fill }

    /**
     * Sets the stroke (line) color.
     * @param lineFill The line color.
     * @return This builder instance for method chaining.
     */
    fun lineFill(lineFill: Color) = apply { this.lineFill = lineFill }

    /**
     * Sets the stroke (line) width.
     * @param width The line width value.
     * @return This builder instance for method chaining.
     */
    fun lineWidth(width: Size) = apply { this.lineWidth = width }

    /**
     * Appends a move command to the path sequence.
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @return This builder instance for method chaining.
     */
    fun moveTo(x: Size, y: Size) = apply { paths.add(ShapePath.MoveTo(x, y)) }

    /**
     * Appends a line command to the path sequence.
     * @param x The destination X coordinate.
     * @param y The destination Y coordinate.
     * @return This builder instance for method chaining.
     */
    fun lineTo(x: Size, y: Size) = apply { paths.add(ShapePath.LineTo(x, y)) }

    /**
     * Appends a cubic Bezier curve to the path sequence.
     * @param x0 The first control point X coordinate.
     * @param y0 The first control point Y coordinate.
     * @param x1 The second control point X coordinate.
     * @param y1 The second control point Y coordinate.
     * @param x2 The end point X coordinate.
     * @param y2 The end point Y coordinate.
     * @return This builder instance for method chaining.
     */
    fun bezierTo(x0: Size, y0: Size, x1: Size, y1: Size, x2: Size, y2: Size) = apply {
        paths.add(ShapePath.BezierTo(x0, y0, x1, y1, x2, y2))
    }

    /**
     * Appends a quadratic/conic curve to the path sequence.
     * @param x0 The control point X coordinate.
     * @param y0 The control point Y coordinate.
     * @param x1 The end point X coordinate.
     * @param y1 The end point Y coordinate.
     * @return This builder instance for method chaining.
     */
    fun conicTo(x0: Size, y0: Size, x1: Size, y1: Size) = apply {
        paths.add(ShapePath.ConicTo(x0, y0, x1, y1))
    }

    /**
     * Defines a square path using a position builder.
     * @param block A builder function to construct the square [Position].
     * @return This builder instance for method chaining.
     */
    fun square(block: PositionBuilder.() -> Unit) = apply {
        val position = PositionBuilder().apply(block).build()
        square(position)
    }

    /**
     * Defines a square path based on the provided [Position].
     * This method requires that no path commands have been added yet and no position is already set.
     * @param position The square bounds.
     * @return This builder instance for method chaining.
     */
    fun square(position: Position) = apply {
        require(paths.isEmpty()) { "Cannot set square path on a PathObject with existing paths." }
        require(this.position == null) { "Cannot set square path on a PathObject with non-null position." }

        moveTo(Size.ofMeters(0), Size.ofMeters(0))
        lineTo(position.width, Size.ofMeters(0))
        lineTo(position.width, position.height)
        lineTo(Size.ofMeters(0), position.height)
        lineTo(Size.ofMeters(0), Size.ofMeters(0))

        this.position = position
    }

    /**
     * Defines a triangle path using a position builder.
     * @param block A builder function to construct the triangle [Position].
     * @return This builder instance for method chaining.
     */
    fun triangle(block: PositionBuilder.() -> Unit) = apply {
        val position = PositionBuilder().apply(block).build()
        triangle(position)
    }

    /**
     * Defines a triangle path that fills the provided [Position].
     * @param position The triangle bounds.
     * @return This builder instance for method chaining.
     */
    fun triangle(position: Position) = apply {
        require(paths.isEmpty()) { "Cannot set square path on a PathObject with existing paths." }
        require(this.position == null) { "Cannot set square path on a PathObject with non-null position." }

        moveTo(position.width / 2.0, Size.ofMeters(0))
        lineTo(position.width, position.height)
        lineTo(Size.ofMeters(0), position.height)
        lineTo(position.width / 2.0, Size.ofMeters(0))

        this.position = position
    }

    /**
     * Defines an ellipse path using a position builder.
     * @param block A builder function to construct the ellipse [Position].
     * @return This builder instance for method chaining.
     */
    fun ellipse(block: PositionBuilder.() -> Unit) = apply {
        val position = PositionBuilder().apply(block).build()
        ellipse(position)
    }

    /**
     * Defines an ellipse path that fills the provided [Position].
     * @param position The ellipse bounds.
     * @return This builder instance for method chaining.
     */
    fun ellipse(position: Position) = apply {
        require(paths.isEmpty()) { "Cannot set ellipse path on a PathObject with existing paths." }
        require(this.position == null) { "Cannot set ellipse path on a PathObject with non-null position." }

        val kappa = 0.5522847498307936
        val zero = Size.ofMeters(0)
        val rx = position.width / 2.0
        val ry = position.height / 2.0

        moveTo(rx, zero)
        bezierTo(
            position.width, ry - (ry * kappa),
            rx + (rx * kappa), zero,
            position.width, ry,
        )
        bezierTo(
            rx + (rx * kappa), position.height,
            position.width, ry + (ry * kappa),
            rx, position.height,
        )
        bezierTo(
            zero, ry + (ry * kappa),
            rx - (rx * kappa), position.height,
            zero, ry,
        )
        bezierTo(
            rx - (rx * kappa), zero,
            zero, ry - (ry * kappa),
            rx, zero,
        )

        this.position = position
    }

    /**
     * Builds a [Shape] from the current builder state.
     * @return A fully configured [Shape].
     * @throws IllegalArgumentException if position is not set.
     */
    fun build(): Shape {
        return Shape(
            name = name,
            paths = paths,
            position = requireNotNull(position) { "Position can not be null." },
            fill = fill,
            lineFill = lineFill,
            lineWidth = lineWidth,
        )
    }
}
