#!/usr/bin/env python3
# RLX Patches
# https://github.com/Ramik4/rlx-patches
#
# Generates the RLX startup animation (Lottie JSON): the RLX red icon of the app, the square (YouTube)
# or circle (YouTube Music) popping in and the play symbol turning into place.
#
# Usage: animation.py <repository>
# Writes patches/src/main/resources/rlx/branding/<app>/raw/rlx_startup_animation.json, which the RLX
# core puts in the place of both Morphe startup animations (RlxBranding.kt). The splash screen style
# that plays it is Morphe's MORPHE_COLOR, named "RLX" in the settings.
#
# The proportions of the icon are those of generate.py. No dependencies.

import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from generate import BOX_RADIUS, PLAY_HEIGHT, PLAY_OFFSET, PLAY_RADIUS, PLAY_WIDTH, RED  # noqa: E402

CANVAS = 432  # Size of the animation, as the Morphe animations.
FPS = 60
FRAMES = 42  # 0.7 s, as the Morphe animations; YouTube closes it when the feed is loaded.
LOGO = 512.0  # Coordinates of the logo layer, scaled into the canvas.
CENTER = LOGO / 2
BOX = 360.0  # The square or circle, in logo coordinates.
LOGO_SCALE_START = 33.3  # Percent, as the Morphe animations (the logo grows slowly while it plays).
LOGO_SCALE_END = 37.0

WHITE = [1, 1, 1, 1]


def rgba(color):
    return [round(int(color[i:i + 2], 16) / 255, 6) for i in (1, 3, 5)] + [1]


def r(v):
    return round(v, 3)


def value(v):
    return {"a": 0, "k": v}


def ease(dimensions, out_x=0.2, in_x=0.1):
    """Ease in and out of a keyframe, as the Morphe animations (fast start, soft landing)."""
    def handle(x, y):
        return {"x": [x] * dimensions, "y": [y] * dimensions} if dimensions > 1 else {"x": x, "y": y}

    return handle(out_x, 0), handle(in_x, 1)


def animated(keyframes):
    """keyframes: (frame, value) pairs; every segment is eased."""
    out = []
    for i, (t, v) in enumerate(keyframes):
        frame = {"t": t, "s": v if isinstance(v, list) else [v]}
        if i < len(keyframes) - 1:
            o, i_ = ease(len(frame["s"]))
            frame["o"], frame["i"] = o, i_
        out.append(frame)
    return {"a": 1, "k": out}


def transform(position=None, anchor=None, scale=None, rotation=None, opacity=None):
    return {
        "o": opacity or value(100),
        "r": rotation or value(0),
        "p": position or value([0, 0, 0]),
        "a": anchor or value([0, 0, 0]),
        "s": scale or value([100, 100, 100]),
    }


def shape_transform():
    return {"ty": "tr", "p": value([0, 0]), "a": value([0, 0]), "s": value([100, 100]), "r": value(0),
            "o": value(100), "sk": value(0), "sa": value(0)}


def fill(color):
    return {"ty": "fl", "c": value(color), "o": value(100), "r": 1, "bm": 0, "nm": "fill"}


def play_path(cx, cy, box):
    """The play symbol of generate.py as a Lottie path: lines, and a cubic curve for each round corner."""
    height = box * PLAY_HEIGHT
    width = height * PLAY_WIDTH
    radius = height * PLAY_RADIUS
    left = cx - width / 2 + box * PLAY_OFFSET
    corners = [(left + radius, cy - height / 2 + radius), (left + width - radius, cy),
               (left + radius, cy + height / 2 - radius)]

    def direction(p, q):
        dx, dy = q[0] - p[0], q[1] - p[1]
        length = math.hypot(dx, dy)
        return dx / length, dy / length

    directions = [direction(corners[i], corners[(i + 1) % 3]) for i in range(3)]
    normals = [(d[1], -d[0]) for d in directions]  # Outward, clockwise on screen.
    # A corner turns by 120 degrees; one cubic curve per corner.
    handle = radius * 4 / 3 * math.tan(math.radians(120) / 4)

    vertices, in_tangents, out_tangents = [], [], []
    for i in range(3):
        p, q = corners[i], corners[(i + 1) % 3]
        d, n = directions[i], normals[i]
        start = (p[0] + radius * n[0], p[1] + radius * n[1])
        end = (q[0] + radius * n[0], q[1] + radius * n[1])
        # Start of the edge: end of the curve of the corner before it.
        vertices.append([r(start[0]), r(start[1])])
        in_tangents.append([r(-d[0] * handle), r(-d[1] * handle)])
        out_tangents.append([0, 0])
        # End of the edge: start of the curve of the next corner.
        vertices.append([r(end[0]), r(end[1])])
        in_tangents.append([0, 0])
        out_tangents.append([r(d[0] * handle), r(d[1] * handle)])
    return {"ty": "sh", "nm": "play", "ks": value({"i": in_tangents, "o": out_tangents, "v": vertices, "c": True})}


def layer(index, name, shapes, ks):
    return {"ddd": 0, "ind": index, "ty": 4, "nm": name, "sr": 1, "ks": ks, "ao": 0, "shapes": shapes,
            "ip": 0, "op": FRAMES, "st": 0, "bm": 0, "parent": 1}


def animation(round_icon):
    center = [CENTER, CENTER, 0]
    if round_icon:
        box = {"ty": "el", "nm": "circle", "d": 1, "p": value([CENTER, CENTER]), "s": value([BOX, BOX])}
    else:
        box = {"ty": "rc", "nm": "square", "d": 1, "p": value([CENTER, CENTER]), "s": value([BOX, BOX]),
               "r": value(BOX * BOX_RADIUS)}

    logo = {"ddd": 0, "ind": 1, "ty": 3, "nm": "logo", "sr": 1, "ao": 0, "ip": 0, "op": FRAMES, "st": 0, "bm": 0,
            "ks": transform(position=value([CANVAS / 2, CANVAS / 2, 0]), anchor=value(center),
                            scale=animated([(0, [LOGO_SCALE_START] * 2 + [100]),
                                            (28, [LOGO_SCALE_END] * 2 + [100])]))}

    # The square pops in with a small turn, then the play symbol turns into place.
    box_layer = layer(3, "box", [{"ty": "gr", "nm": "box", "it": [box, fill(rgba(RED)), shape_transform()]}],
                      transform(position=value(center), anchor=value(center),
                                scale=animated([(0, [0, 0, 100]), (14, [106, 106, 100]), (22, [100, 100, 100])]),
                                rotation=animated([(0, -20), (18, 0)]),
                                opacity=animated([(0, 0), (6, 100)])))
    play_layer = layer(2, "play", [{"ty": "gr", "nm": "play", "it": [play_path(CENTER, CENTER, BOX), fill(WHITE),
                                                                         shape_transform()]}],
                       transform(position=value(center), anchor=value(center),
                                 scale=animated([(9, [0, 0, 100]), (22, [112, 112, 100]), (30, [100, 100, 100])]),
                                 rotation=animated([(9, -90), (26, 0)]),
                                 opacity=animated([(9, 0), (15, 100)])))

    return {
        "meta": {"a": "RLX Patches", "d": "RLX Patches startup animation. Generated by rlx/branding/animation.py."},
        "v": "5.12.1", "fr": FPS, "ip": 0, "op": FRAMES, "w": CANVAS, "h": CANVAS,
        "nm": "rlx_startup_animation", "ddd": 0, "assets": [],
        "layers": [logo, play_layer, box_layer],  # The first layer is drawn on top.
    }


def main(repo):
    for app, round_icon in (("youtube", False), ("music", True)):
        path = os.path.join(repo, f"patches/src/main/resources/rlx/branding/{app}/raw/rlx_startup_animation.json")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as fh:
            json.dump(animation(round_icon), fh, separators=(",", ":"))
            fh.write("\n")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("usage: animation.py <repository>")
    main(sys.argv[1])
