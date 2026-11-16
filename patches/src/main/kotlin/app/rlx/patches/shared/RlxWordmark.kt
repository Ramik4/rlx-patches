/*
 * RLX Patches
 * https://github.com/Ramik4/rlx-patches
 *
 * Based on Morphe Patches. See the included NOTICE file for the GPLv3 Section 7 terms.
 */

package app.rlx.patches.shared

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

/**
 * The letters of an RLX header logo taken from the header logo of the app itself ("YouTube", "Premium",
 * "Music"), so they look like the app. Only pure JDK code, so it can be tested without the patcher.
 *
 * The header logo of the app must be a vector drawable of the logo and, to the right of it, the letters.
 * The paths are grouped by their horizontal extent: the leftmost group is the logo, the rest are the letters.
 */
internal object RlxWordmark {

    /**
     * Returns [rlxHeader] (an RLX header logo of rlx/branding/generate.py: the icon as paths, then the letters
     * in a group) with the letters of [appLogo] in place of its letters, at the same height and position.
     * Returns null if [appLogo] is not a vector drawable of a logo followed by letters.
     */
    fun compose(appLogo: String, rlxHeader: String): String? {
        val app = parse(appLogo)
        val rlx = parse(rlxHeader)
        val appRoot = app.documentElement
        val rlxRoot = rlx.documentElement
        if (appRoot.tagName != "vector" || rlxRoot.tagName != "vector") return null
        // A tint colors all paths, also the logo, so the letters could not keep their colors without it.
        if (appRoot.hasAttributeNS(ANDROID_NS, "tint")) return null

        // Where the paths are, in the coordinates of the viewport.
        val shapes = mutableListOf<Pair<Element, Bounds>>()
        if (!collect(appRoot, Transform.IDENTITY, shapes)) return null
        if (shapes.isEmpty()) return null

        val clusters = cluster(shapes)
        if (clusters.size < 2) return null
        val logo = clusters.first()
        val letters = clusters.drop(1)
        val logoBounds = logo.map { it.second }.reduce(Bounds::union)
        val letterBounds = letters.flatten().map { it.second }.reduce(Bounds::union)
        val logoRatio = logoBounds.width / logoBounds.height
        if (logoRatio !in 0.7..2.2) return null
        if (letterBounds.width < logoBounds.width || letterBounds.height <= 0.0) return null
        if (letterBounds.height > logoBounds.height * 1.5) return null

        // The RLX layout: the icon paths, and the group of the letters (translated to the baseline).
        val iconPaths = rlxRoot.childElements().filter { it.tagName == "path" }
        val rlxLetters = rlxRoot.childElements().firstOrNull { it.tagName == "group" } ?: return null
        if (iconPaths.isEmpty()) return null
        val icon = iconPaths.map { bounds(it.androidAttribute("pathData") ?: return null) ?: return null }
            .reduce(Bounds::union)
        val viewportHeight = rlxRoot.androidAttribute("viewportHeight")?.toDoubleOrNull() ?: return null
        val heightDp = rlxRoot.androidAttribute("height")?.removeSuffix("dp")?.toDoubleOrNull() ?: return null
        val textX = rlxLetters.androidAttribute("translateX")?.toDoubleOrNull() ?: return null
        val baseline = rlxLetters.androidAttribute("translateY")?.toDoubleOrNull() ?: return null
        val padding = icon.minX
        val textHeight = icon.height * TEXT_HEIGHT

        val scale = textHeight / letterBounds.height
        val width = textX + letterBounds.width * scale + padding

        // The letters: the app logo without the paths of its logo, with the groups of the paths kept.
        val logoPaths = logo.map { it.first }.toSet()
        val out = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().newDocument()
        out.appendChild(out.createComment(" RLX Patches: the RLX icon, with the letters of the header logo of the app. "))
        val vector = out.importNode(appRoot, false) as Element
        out.appendChild(vector)
        listOf("width", "height", "viewportWidth", "viewportHeight", "tint", "tintMode", "alpha", "autoMirrored")
            .forEach { vector.removeAttributeNS(ANDROID_NS, it) }
        vector.setAttributeNS(ANDROID_NS, "android:width", "${num(width / viewportHeight * heightDp)}dp")
        vector.setAttributeNS(ANDROID_NS, "android:height", "${num(heightDp)}dp")
        vector.setAttributeNS(ANDROID_NS, "android:viewportWidth", num(width))
        vector.setAttributeNS(ANDROID_NS, "android:viewportHeight", num(viewportHeight))
        iconPaths.forEach { vector.appendChild(out.importNode(it, true)) }

        val group = out.createElementNS(null, "group")
        group.setAttributeNS(ANDROID_NS, "android:translateX", num(textX - letterBounds.minX * scale))
        group.setAttributeNS(ANDROID_NS, "android:translateY", num(baseline - letterBounds.maxY * scale))
        group.setAttributeNS(ANDROID_NS, "android:scaleX", num(scale))
        group.setAttributeNS(ANDROID_NS, "android:scaleY", num(scale))
        vector.appendChild(group)
        appRoot.childElements().forEach { child ->
            copyWithout(child, logoPaths, out)?.let(group::appendChild)
        }

        return serialize(out)
    }

    /** Height of the letters (top of the tallest to the baseline), of the icon. As in generate.py. */
    private const val TEXT_HEIGHT = 0.75

    private fun parse(xml: String): Document =
        DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(xml.byteInputStream())

    private fun serialize(document: Document): String {
        val writer = StringWriter()
        TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
        }.transform(DOMSource(document), StreamResult(writer))
        return writer.toString()
    }

    private fun Element.childElements(): List<Element> =
        (0 until childNodes.length).map { childNodes.item(it) }.filterIsInstance<Element>()

    private fun Element.androidAttribute(name: String): String? =
        if (hasAttributeNS(ANDROID_NS, name)) getAttributeNS(ANDROID_NS, name) else null

    /**
     * Adds the paths below [element] with their bounds. False if the drawable has something that cannot be
     * placed: a clip path, a rotated group, path data or a group value that is a resource reference.
     */
    private fun collect(element: Element, transform: Transform, shapes: MutableList<Pair<Element, Bounds>>): Boolean {
        for (child in element.childElements()) {
            when (child.tagName) {
                "group" -> {
                    val rotation = child.androidAttribute("rotation")?.toDoubleOrNull() ?: 0.0
                    if (child.androidAttribute("rotation") != null && rotation != 0.0) return false
                    val values = listOf("translateX", "translateY", "scaleX", "scaleY", "pivotX", "pivotY")
                        .associateWith { name ->
                            val value = child.androidAttribute(name) ?: return@associateWith null
                            value.toDoubleOrNull() ?: return false
                        }
                    val groupTransform = Transform.group(
                        translateX = values["translateX"] ?: 0.0,
                        translateY = values["translateY"] ?: 0.0,
                        scaleX = values["scaleX"] ?: 1.0,
                        scaleY = values["scaleY"] ?: 1.0,
                        pivotX = values["pivotX"] ?: 0.0,
                        pivotY = values["pivotY"] ?: 0.0,
                    )
                    if (!collect(child, transform.then(groupTransform), shapes)) return false
                }

                "path" -> {
                    val data = child.androidAttribute("pathData") ?: continue
                    if (data.startsWith("@") || data.startsWith("?")) return false
                    if (child.androidAttribute("fillAlpha") == "0" && child.androidAttribute("strokeColor") == null) {
                        continue
                    }
                    val bounds = bounds(data) ?: return false
                    shapes += child to transform.apply(bounds)
                }

                "clip-path" -> return false
            }
        }
        return true
    }

    /** Groups the shapes whose horizontal extents overlap, from left to right. */
    private fun cluster(shapes: List<Pair<Element, Bounds>>): List<List<Pair<Element, Bounds>>> {
        val clusters = mutableListOf<MutableList<Pair<Element, Bounds>>>()
        var right = Double.NEGATIVE_INFINITY
        shapes.sortedBy { it.second.minX }.forEach { shape ->
            if (clusters.isEmpty() || shape.second.minX > right) {
                clusters += mutableListOf(shape)
            } else {
                clusters.last() += shape
            }
            right = max(right, shape.second.maxX)
        }
        return clusters
    }

    /** A copy of [element] without [removed] paths and without groups that are left empty, or null. */
    private fun copyWithout(element: Element, removed: Set<Element>, document: Document): Node? {
        if (element in removed) return null
        if (element.tagName != "group") return document.importNode(element, true)
        val copy = document.importNode(element, false) as Element
        element.childElements().forEach { child -> copyWithout(child, removed, document)?.let(copy::appendChild) }
        return if (copy.hasChildNodes()) copy else null
    }

    private fun num(value: Double): String {
        val rounded = Math.round(value * 1000.0) / 1000.0
        return if (rounded == Math.floor(rounded)) rounded.toLong().toString() else rounded.toString()
    }

    internal data class Bounds(val minX: Double, val minY: Double, val maxX: Double, val maxY: Double) {
        val width get() = maxX - minX
        val height get() = maxY - minY
        fun union(other: Bounds) =
            Bounds(min(minX, other.minX), min(minY, other.minY), max(maxX, other.maxX), max(maxY, other.maxY))
    }

    /** x' = scaleX * x + translateX (no rotation). */
    private data class Transform(val scaleX: Double, val scaleY: Double, val translateX: Double, val translateY: Double) {
        fun then(inner: Transform) = Transform(
            scaleX * inner.scaleX,
            scaleY * inner.scaleY,
            scaleX * inner.translateX + translateX,
            scaleY * inner.translateY + translateY,
        )

        fun apply(b: Bounds): Bounds {
            val x1 = scaleX * b.minX + translateX
            val x2 = scaleX * b.maxX + translateX
            val y1 = scaleY * b.minY + translateY
            val y2 = scaleY * b.maxY + translateY
            return Bounds(min(x1, x2), min(y1, y2), max(x1, x2), max(y1, y2))
        }

        companion object {
            val IDENTITY = Transform(1.0, 1.0, 0.0, 0.0)

            /** A group of a vector drawable: scaled around the pivot, then translated. */
            fun group(
                translateX: Double, translateY: Double, scaleX: Double, scaleY: Double, pivotX: Double, pivotY: Double,
            ) = Transform(
                scaleX, scaleY,
                pivotX - scaleX * pivotX + translateX,
                pivotY - scaleY * pivotY + translateY,
            )
        }
    }

    /**
     * The bounds of path data: of its points and control points, which contain the curves. Null if the data
     * cannot be read.
     */
    internal fun bounds(data: String): Bounds? {
        val tokens = tokenize(data) ?: return null
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        fun add(x: Double, y: Double) {
            minX = min(minX, x); minY = min(minY, y); maxX = max(maxX, x); maxY = max(maxY, y)
        }

        var x = 0.0
        var y = 0.0
        var startX = 0.0
        var startY = 0.0
        var index = 0
        var command = ' '
        fun next(): Double? = (tokens.getOrNull(index++) as? Double)

        while (index < tokens.size) {
            val token = tokens[index]
            if (token is Char) {
                command = token
                index++
                if (command == 'Z' || command == 'z') {
                    x = startX; y = startY
                    continue
                }
            } else if (command == ' ' || command == 'Z' || command == 'z') {
                return null
            }
            val relative = command.isLowerCase()
            val ox = if (relative) x else 0.0
            val oy = if (relative) y else 0.0
            when (command.uppercaseChar()) {
                'M', 'L', 'T' -> {
                    x = ox + (next() ?: return null); y = oy + (next() ?: return null)
                    if (command.uppercaseChar() == 'M') {
                        startX = x; startY = y
                        // Further pairs of a move are lines.
                        command = if (relative) 'l' else 'L'
                    }
                    add(x, y)
                }

                'H' -> { x = ox + (next() ?: return null); add(x, y) }
                'V' -> { y = oy + (next() ?: return null); add(x, y) }
                'C' -> {
                    repeat(2) { add(ox + (next() ?: return null), oy + (next() ?: return null)) }
                    x = ox + (next() ?: return null); y = oy + (next() ?: return null); add(x, y)
                }

                'S', 'Q' -> {
                    add(ox + (next() ?: return null), oy + (next() ?: return null))
                    x = ox + (next() ?: return null); y = oy + (next() ?: return null); add(x, y)
                }

                'A' -> {
                    val rx = next() ?: return null
                    val ry = next() ?: return null
                    val rotation = next() ?: return null
                    val largeArc = (next() ?: return null) != 0.0
                    val sweep = (next() ?: return null) != 0.0
                    val fromX = x
                    val fromY = y
                    x = ox + (next() ?: return null); y = oy + (next() ?: return null)
                    arcPoints(fromX, fromY, rx, ry, rotation, largeArc, sweep, x, y).forEach { (px, py) -> add(px, py) }
                }

                else -> return null
            }
        }
        return if (minX.isFinite()) Bounds(minX, minY, maxX, maxY) else null
    }

    /** Points along an elliptical arc of path data (SVG 1.1 F.6.5), its ends included. */
    private fun arcPoints(
        x1: Double, y1: Double, radiusX: Double, radiusY: Double, rotation: Double,
        largeArc: Boolean, sweep: Boolean, x2: Double, y2: Double,
    ): List<Pair<Double, Double>> {
        var rx = abs(radiusX)
        var ry = abs(radiusY)
        if (rx == 0.0 || ry == 0.0 || (x1 == x2 && y1 == y2)) return listOf(x1 to y1, x2 to y2)
        val phi = Math.toRadians(rotation)
        val cosPhi = cos(phi)
        val sinPhi = sin(phi)
        val dx = (x1 - x2) / 2
        val dy = (y1 - y2) / 2
        val x1p = cosPhi * dx + sinPhi * dy
        val y1p = -sinPhi * dx + cosPhi * dy
        val lambda = (x1p * x1p) / (rx * rx) + (y1p * y1p) / (ry * ry)
        if (lambda > 1) {
            rx *= sqrt(lambda); ry *= sqrt(lambda)
        }
        val numerator = rx * rx * ry * ry - rx * rx * y1p * y1p - ry * ry * x1p * x1p
        val denominator = rx * rx * y1p * y1p + ry * ry * x1p * x1p
        var factor = sqrt(max(0.0, numerator / denominator))
        if (largeArc == sweep) factor = -factor
        val cxp = factor * rx * y1p / ry
        val cyp = -factor * ry * x1p / rx
        val cx = cosPhi * cxp - sinPhi * cyp + (x1 + x2) / 2
        val cy = sinPhi * cxp + cosPhi * cyp + (y1 + y2) / 2
        val start = atan2((y1p - cyp) / ry, (x1p - cxp) / rx)
        var delta = atan2((-y1p - cyp) / ry, (-x1p - cxp) / rx) - start
        if (sweep && delta < 0) delta += 2 * PI
        if (!sweep && delta > 0) delta -= 2 * PI
        val steps = 32
        return (0..steps).map { step ->
            val angle = start + delta * step / steps
            val ex = rx * cos(angle)
            val ey = ry * sin(angle)
            (cosPhi * ex - sinPhi * ey + cx) to (sinPhi * ex + cosPhi * ey + cy)
        }
    }

    /** Commands as Char and numbers as Double; arc flags may be written without separators. */
    private fun tokenize(data: String): List<Any>? {
        val tokens = mutableListOf<Any>()
        var i = 0
        var lastCommand = ' '
        var argument = 0
        while (i < data.length) {
            val c = data[i]
            when {
                c.isWhitespace() || c == ',' -> i++
                c.isLetter() && c != 'e' && c != 'E' -> {
                    tokens += c; lastCommand = c; argument = 0; i++
                }

                else -> {
                    // Arc flags (arguments 4 and 5 of 7) are single digits.
                    if (lastCommand.uppercaseChar() == 'A' && (argument % 7 == 3 || argument % 7 == 4) &&
                        (c == '0' || c == '1')
                    ) {
                        tokens += (c - '0').toDouble(); argument++; i++
                        continue
                    }
                    val start = i
                    if (c == '-' || c == '+') i++
                    var dot = false
                    while (i < data.length) {
                        val d = data[i]
                        if (d.isDigit()) i++
                        else if (d == '.' && !dot) { dot = true; i++ }
                        else if (d == 'e' || d == 'E') {
                            i++
                            if (i < data.length && (data[i] == '-' || data[i] == '+')) i++
                        } else break
                    }
                    tokens += data.substring(start, i).toDoubleOrNull() ?: return null
                    argument++
                }
            }
        }
        return tokens
    }
}
