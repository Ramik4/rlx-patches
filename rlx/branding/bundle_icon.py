#!/usr/bin/env python3
# RLX Patches
# https://github.com/Ramik4/rlx-patches
#
# The icon of RLX Patches (patches-bundle.png, the icon of the source in Morphe Manager and of the
# about page) and the banner of the README (images/social-preview.png), as SVG with every letter
# written as a path. render.js turns them into the PNG files.
#
# Icon: a white play on RLX red with "RLX" cut out of it, the R starting exactly at the left edge of
# the play, so its stem opens into the red. The letters are Teko SemiBold (SIL Open Font License
# 1.1), the subtitles of the banner Liberation Sans (SIL Open Font License 1.1). The fonts are only
# read here.
#
#   python3 rlx/branding/bundle_icon.py Teko-SemiBold.ttf LiberationSans-Regular.ttf OUT_DIR
#   node rlx/branding/render.js OUT_DIR/patches-bundle.svg patches-bundle.png 200 200
#   node rlx/branding/render.js OUT_DIR/social-preview.svg rlx/branding/images/social-preview.png 1280 640

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import generate as g  # noqa: E402

RED = g.RED
WHITE = g.WHITE
BACKGROUND = "#0F0F0F"
SUBTITLE = "#E6E6E6"
SUBTITLE_DIM = "#9A9A9A"

ICON = 200.0
PLAY_HEIGHT = 130.0  # Height of the play, of the 200 px icon.
LETTER_MARGIN = 14.4  # Least distance of the letters from the slanted edges of the play.


def text_path(font_path, text, size):
    """The text as one path at a font size in px, baseline at y = 0, starting at x = 0 (the origin
    of the first letter). Returns (path, advance width, cap height, left side bearing of the ink)."""
    import uharfbuzz as hb
    from fontTools.ttLib import TTFont
    from fontTools.pens.svgPathPen import SVGPathPen
    from fontTools.pens.transformPen import TransformPen
    from fontTools.pens.boundsPen import BoundsPen

    data = open(font_path, "rb").read()
    font = TTFont(font_path)
    glyphs = font.getGlyphSet()
    scale = size / font["head"].unitsPerEm
    face = hb.Font(hb.Face(hb.Blob(data)))
    buf = hb.Buffer()
    buf.add_str(text)
    buf.guess_segment_properties()
    hb.shape(face, buf, {"kern": True, "liga": True})
    order = font.getGlyphOrder()
    pen = SVGPathPen(glyphs, ntos=g.f)
    bounds = BoundsPen(glyphs)
    x = 0
    for info, pos in zip(buf.glyph_infos, buf.glyph_positions):
        name = order[info.codepoint]
        transform = (scale, 0, 0, -scale, (x + pos.x_offset) * scale, -pos.y_offset * scale)
        glyphs[name].draw(TransformPen(pen, transform))
        glyphs[name].draw(TransformPen(bounds, (1, 0, 0, 1, x + pos.x_offset, pos.y_offset)))
        x += pos.x_advance
    cap = getattr(font["OS/2"], "sCapHeight", 0) or bounds.bounds[3]
    return pen.getCommands(), x * scale, cap * scale, bounds.bounds[0] * scale


def icon_layers(font_path, x0, y0, size, uid):
    """The icon at x0, y0, size px: background, and the play with the letters cut out (mask)."""
    k = size / ICON
    height = PLAY_HEIGHT * k
    box = height / g.PLAY_HEIGHT
    width = height * g.PLAY_WIDTH
    cx, cy = x0 + size / 2, y0 + size / 2
    left = cx - width / 2 + box * g.PLAY_OFFSET

    # The largest letters that stay inside the play: at their right end, the play is still a
    # margin taller than them.
    _, advance1, cap1, lsb1 = text_path(font_path, "RLX", 1.0)
    lo, hi = 1.0, 400.0
    for _ in range(60):
        mid = (lo + hi) / 2
        ink = advance1 * mid  # Close enough: the right side bearing of X is tiny.
        half = height / 2 * (1 - ink / width) - LETTER_MARGIN * k
        lo, hi = (mid, hi) if cap1 * mid / 2 <= half else (lo, mid)
    size_px = lo
    path, _, cap, lsb = text_path(font_path, "RLX", size_px)
    tx, ty = left - lsb, cy + cap / 2
    return (
        f'<mask id="cut{uid}"><rect x="{g.f(x0)}" y="{g.f(y0)}" width="{g.f(size)}" height="{g.f(size)}" fill="white"/>'
        f'<path fill="black" transform="translate({g.f(tx)} {g.f(ty)})" d="{path}"/></mask>'
        f'<path fill="{WHITE}" mask="url(#cut{uid})" d="{g.play(cx, cy, box)}"/>'
    )


def icon_svg(font_path):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" width="200" height="200">'
            f'<rect width="200" height="200" fill="{RED}"/>{icon_layers(font_path, 0, 0, ICON, "i")}</svg>\n')


def banner_svg(font_path, sans_path):
    width, height = 1280.0, 640.0
    title_size = 200.0
    rlx, rlx_w, cap, rlx_lsb = text_path(font_path, "RLX", title_size)
    patches, patches_w, _, _ = text_path(font_path, "Patches", title_size)
    space = title_size * 0.22
    icon = cap * 1.05  # The icon as tall as the capitals.
    gap = icon * 0.32
    total = icon + gap + (rlx_w - rlx_lsb) + space + patches_w
    x = (width - total) / 2
    title_top = 186.0
    baseline = title_top + cap
    out = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {g.f(width)} {g.f(height)}" '
           f'width="{g.f(width)}" height="{g.f(height)}">',
           f'<rect width="{g.f(width)}" height="{g.f(height)}" fill="{BACKGROUND}"/>']
    icon_y = baseline - cap / 2 - icon / 2
    out.append(f'<clipPath id="round"><rect x="{g.f(x)}" y="{g.f(icon_y)}" width="{g.f(icon)}" '
               f'height="{g.f(icon)}" rx="{g.f(icon * 0.22)}"/></clipPath>')
    out.append(f'<g clip-path="url(#round)"><rect x="{g.f(x)}" y="{g.f(icon_y)}" width="{g.f(icon)}" '
               f'height="{g.f(icon)}" fill="{RED}"/>{icon_layers(font_path, x, icon_y, icon, "b")}</g>')
    x += icon + gap - rlx_lsb
    out.append(f'<path fill="{WHITE}" transform="translate({g.f(x)} {g.f(baseline)})" d="{rlx}"/>')
    x += rlx_w + space
    out.append(f'<path fill="{RED}" transform="translate({g.f(x)} {g.f(baseline)})" d="{patches}"/>')

    y = baseline + 82
    for text, size, color in (("YouTube  |  YT Music", 38.0, SUBTITLE),
                              ("Morphe patches + voice-over translation + quick downloads + Save to device",
                               28.0, SUBTITLE_DIM)):
        path, w, _, _ = text_path(sans_path, text, size)
        out.append(f'<path fill="{color}" transform="translate({g.f((width - w) / 2)} {g.f(y)})" d="{path}"/>')
        y += 54
    out.append("</svg>")
    return "\n".join(out) + "\n"


def main(font_path, sans_path, out_dir):
    os.makedirs(out_dir, exist_ok=True)
    with open(os.path.join(out_dir, "patches-bundle.svg"), "w", encoding="utf-8") as fh:
        fh.write(icon_svg(font_path))
    with open(os.path.join(out_dir, "social-preview.svg"), "w", encoding="utf-8") as fh:
        fh.write(banner_svg(font_path, sans_path))


if __name__ == "__main__":
    main(*sys.argv[1:4])
