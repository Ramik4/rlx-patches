#!/usr/bin/env python3
# RLX Patches
# https://github.com/Ramik4/rlx-patches
#
# Generates the RLX app icons and header logos as Android vector drawables and SVG previews.
#
# Usage: generate.py <Roboto Condensed Bold font (.woff/.ttf)> <output directory>
# The font (SIL Open Font License 1.1) is only read here; the letters are written as paths.
# It is in the npm package @fontsource/roboto-condensed (files/roboto-condensed-latin-700-normal.woff).
#
# Needs: fonttools, uharfbuzz.

import math
import os
import sys

RED = "#FF0040"
GREEN = "#00E686"
WHITE = "#FFFFFF"
TEXT_LIGHT = "#0F0F0F"  # On the light theme.
TEXT_DARK = "#F0F0F0"  # On the dark theme.

# Proportions, measured from the reference icons (432 px adaptive foreground).
ICON_CANVAS = 108.0  # Adaptive icon viewport (dp).
BOX = ICON_CANVAS * 194 / 432  # The red square or circle.
BOX_RADIUS = 0.263  # Corner radius of the square, of BOX.
PLAY_HEIGHT = 0.495  # Height of the play symbol, of BOX.
PLAY_WIDTH = 0.875  # Width of the play symbol, of its height.
PLAY_RADIUS = 0.208  # Corner radius of the play symbol, of its height.
PLAY_OFFSET = 0.0155  # Optical shift of the play symbol to the right, of BOX.

# Layered card: the front square and the green square behind it.
LAYERED_BOX = BOX * 0.92
LAYERED_SHIFT = BOX * 0.085

# Header logo: icon, then the word, centered on the same line.
HEADER_HEIGHT = 192.0
HEADER_ICON = 100.0
HEADER_TEXT_HEIGHT = 0.76  # Height of the word (top of the tallest letter to the baseline), of the icon.
HEADER_GAP = 0.15  # Space between the icon and the word, of the icon.
HEADER_PADDING = 44.0  # Left and right.


def f(v):
    s = "%.3f" % v
    s = s.rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


def rounded_rect(cx, cy, size, radius):
    h = size / 2
    r = radius
    x0, y0, x1, y1 = cx - h, cy - h, cx + h, cy + h
    return (
        f"M{f(x0 + r)},{f(y0)}H{f(x1 - r)}A{f(r)},{f(r)} 0 0 1 {f(x1)},{f(y0 + r)}"
        f"V{f(y1 - r)}A{f(r)},{f(r)} 0 0 1 {f(x1 - r)},{f(y1)}"
        f"H{f(x0 + r)}A{f(r)},{f(r)} 0 0 1 {f(x0)},{f(y1 - r)}"
        f"V{f(y0 + r)}A{f(r)},{f(r)} 0 0 1 {f(x0 + r)},{f(y0)}Z"
    )


def circle(cx, cy, d):
    r = d / 2
    return (
        f"M{f(cx - r)},{f(cy)}A{f(r)},{f(r)} 0 1 1 {f(cx + r)},{f(cy)}"
        f"A{f(r)},{f(r)} 0 1 1 {f(cx - r)},{f(cy)}Z"
    )


def play(cx, cy, box):
    """The play symbol: a triangle pointing right with round corners, centered in a box at cx, cy."""
    height = box * PLAY_HEIGHT
    width = height * PLAY_WIDTH
    r = height * PLAY_RADIUS
    left = cx - width / 2 + box * PLAY_OFFSET
    # Inner triangle; the symbol is the inner triangle grown by r.
    a = (left + r, cy - height / 2 + r)
    b = (left + width - r, cy)
    c = (left + r, cy + height / 2 - r)
    pts = [a, b, c]

    def normal(p, q):  # Outward normal of edge p->q (clockwise order on screen).
        dx, dy = q[0] - p[0], q[1] - p[1]
        length = math.hypot(dx, dy)
        return dy / length, -dx / length

    normals = [normal(pts[i], pts[(i + 1) % 3]) for i in range(3)]
    d = ""
    for i in range(3):
        p, q = pts[i], pts[(i + 1) % 3]
        n = normals[i]
        start = (p[0] + r * n[0], p[1] + r * n[1])
        end = (q[0] + r * n[0], q[1] + r * n[1])
        nn = normals[(i + 1) % 3]
        arc_end = (q[0] + r * nn[0], q[1] + r * nn[1])
        if i == 0:
            d += f"M{f(start[0])},{f(start[1])}"
        d += f"L{f(end[0])},{f(end[1])}A{f(r)},{f(r)} 0 0 1 {f(arc_end[0])},{f(arc_end[1])}"
    return d + "Z"


def icon_shapes(kind, cx, cy, box):
    """Full color icon as (path, color) layers. kind: red, green, layered, red_round, green_round."""
    if kind == "layered":
        b = box * LAYERED_BOX / BOX
        s = box * LAYERED_SHIFT / BOX
        return [
            (rounded_rect(cx + s, cy - s, b, b * BOX_RADIUS), GREEN),
            (rounded_rect(cx - s, cy + s, b, b * BOX_RADIUS), RED),
            (play(cx - s, cy + s, b), WHITE),
        ]
    color = GREEN if kind.startswith("green") else RED
    shape = circle(cx, cy, box) if kind.endswith("round") else rounded_rect(cx, cy, box, box * BOX_RADIUS)
    return [(shape, color), (play(cx, cy, box), WHITE)]


def mono_path(kind, cx, cy, box):
    """Single color icon: the square or circle with the play symbol cut out (even-odd fill)."""
    shape = circle(cx, cy, box) if kind.endswith("round") else rounded_rect(cx, cy, box, box * BOX_RADIUS)
    return shape + play(cx, cy, box)


# Text

def word_path(font_path, text, height):
    """The word as one path, scaled so the top of the tallest letter is `height` above the
    baseline. Returns (path, width, x offset of the first letter) with the baseline at y = 0."""
    import uharfbuzz as hb
    from fontTools.ttLib import TTFont
    from fontTools.pens.svgPathPen import SVGPathPen
    from fontTools.pens.transformPen import TransformPen
    from fontTools.pens.boundsPen import BoundsPen

    data = open(font_path, "rb").read()
    font = TTFont(font_path)
    glyph_set = font.getGlyphSet()
    hb_font = hb.Font(hb.Face(hb.Blob(data)))
    buf = hb.Buffer()
    buf.add_str(text)
    buf.guess_segment_properties()
    hb.shape(hb_font, buf, {"kern": True, "liga": True})
    order = font.getGlyphOrder()

    placed = []
    x = 0
    for info, pos in zip(buf.glyph_infos, buf.glyph_positions):
        placed.append((order[info.codepoint], x + pos.x_offset, pos.y_offset))
        x += pos.x_advance

    bounds = BoundsPen(glyph_set)
    for name, gx, gy in placed:
        glyph_set[name].draw(TransformPen(bounds, (1, 0, 0, 1, gx, gy)))
    xmin, ymin, xmax, ymax = bounds.bounds
    scale = height / ymax

    pen = SVGPathPen(glyph_set, ntos=f)
    for name, gx, gy in placed:
        # Font units have y up; flip, scale and put the left edge of the ink at x = 0.
        glyph_set[name].draw(TransformPen(pen, (scale, 0, 0, -scale, (gx - xmin) * scale, -gy * scale)))
    return pen.getCommands(), (xmax - xmin) * scale


def header(font_path, kind, word, text_color, height=HEADER_HEIGHT, icon=HEADER_ICON,
           padding=HEADER_PADDING):
    cy = height / 2
    icon_cx = padding + icon / 2
    layers = icon_shapes(kind, icon_cx, cy, icon)
    text_height = icon * HEADER_TEXT_HEIGHT
    path, text_width = word_path(font_path, word, text_height)
    baseline = cy + text_height / 2
    text_x = padding + icon + icon * HEADER_GAP
    width = text_x + text_width + padding
    return width, height, layers, (path, text_color, text_x, baseline)


# Output

def svg(width, height, layers, text=None, even_odd=False):
    out = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {f(width)} {f(height)}" '
           f'width="{f(width)}" height="{f(height)}">']
    for d, color in layers:
        rule = ' fill-rule="evenodd"' if even_odd else ""
        out.append(f'<path fill="{color}"{rule} d="{d}"/>')
    if text:
        d, color, tx, ty = text
        out.append(f'<path fill="{color}" transform="translate({f(tx)} {f(ty)})" d="{d}"/>')
    out.append("</svg>")
    return "\n".join(out) + "\n"


HEADER_COMMENT = """<!--
    RLX Patches. Generated by rlx/branding/generate.py, do not edit.
    The letters are Roboto Condensed (SIL Open Font License 1.1).
-->
"""

ICON_COMMENT = """<!--
    RLX Patches. Generated by rlx/branding/generate.py, do not edit.
-->
"""


def vector(width_dp, height_dp, vw, vh, layers, text=None, even_odd=False, comment=ICON_COMMENT):
    out = ['<?xml version="1.0" encoding="utf-8"?>', comment.rstrip(),
           '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
           f'    android:width="{f(width_dp)}dp"', f'    android:height="{f(height_dp)}dp"',
           f'    android:viewportWidth="{f(vw)}"', f'    android:viewportHeight="{f(vh)}">']
    for d, color in layers:
        rule = '\n        android:fillType="evenOdd"' if even_odd else ""
        out.append(f'    <path\n        android:fillColor="{color}"{rule}\n        android:pathData="{d}" />')
    if text:
        d, color, tx, ty = text
        out.append(f'    <group\n        android:translateX="{f(tx)}"\n        android:translateY="{f(ty)}">')
        out.append(f'        <path\n            android:fillColor="{color}"\n            android:pathData="{d}" />')
        out.append('    </group>')
    out.append("</vector>")
    return "\n".join(out) + "\n"


def write(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(content)


def main(font_path, out_dir):
    c = ICON_CANVAS / 2
    preview = os.path.join(out_dir, "preview")
    for kind in ("red", "green", "layered", "red_round", "green_round"):
        layers = icon_shapes(kind, c, c, BOX)
        write(f"{preview}/icon_{kind}.svg", svg(ICON_CANVAS, ICON_CANVAS, layers))
        write(f"{out_dir}/vector/foreground_{kind}.xml",
              vector(ICON_CANVAS, ICON_CANVAS, ICON_CANVAS, ICON_CANVAS, layers))
    for kind in ("square", "round"):
        mono = [(mono_path(kind, c, c, BOX), "#000000")]
        write(f"{preview}/mono_{kind}.svg", svg(ICON_CANVAS, ICON_CANVAS, mono, even_odd=True))
        write(f"{out_dir}/vector/monochrome_{kind}.xml",
              vector(ICON_CANVAS, ICON_CANVAS, ICON_CANVAS, ICON_CANVAS, mono, even_odd=True))
        # Notification icon: 24 dp, the glyph fills 20 dp.
        n = [(mono_path(kind, 12, 12, 20), "#FFFFFF")]
        write(f"{preview}/notification_{kind}.svg", svg(24, 24, n, even_odd=True))
        write(f"{out_dir}/vector/notification_{kind}.xml", vector(24, 24, 24, 24, n, even_odd=True))

    for kind in ("red", "green"):
        for word in ("YouTube", "Premium"):
            for theme, color in (("light", TEXT_LIGHT), ("dark", TEXT_DARK)):
                w, h, layers, text = header(font_path, kind, word, color)
                name = f"header_{kind}_{word.lower()}_{theme}"
                write(f"{preview}/{name}.svg", svg(w, h, layers, text))
                write(f"{out_dir}/vector/{name}.xml",
                      vector(w / 4, h / 4, w, h, layers, text, comment=HEADER_COMMENT))
        # YouTube Music: round icon, dark theme only.
        w, h, layers, text = header(font_path, kind + "_round", "Music", TEXT_DARK)
        name = f"header_music_{kind}_dark"
        write(f"{preview}/{name}.svg", svg(w, h, layers, text))
        write(f"{out_dir}/vector/{name}.xml", vector(w / 4, h / 4, w, h, layers, text, comment=HEADER_COMMENT))


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__ or "usage: generate.py <font> <output directory>")
    main(sys.argv[1], sys.argv[2])
