"""Aero Client branding: original pixel-art logo ("A" badge + slanted AERO CLIENT),
animation parts, favicon and opaque loading pictures.

Usage: python3 tools/make_branding.py OUT_DIR   (needs Python 3 + Pillow)
The outputs are copied into desktopRuntime/resources/assets/minecraft/aeroclient/,
javascript/favicon.png, AeroLoadingImages.java and OfflineDownloadTemplate.txt.
Prints the atlas layout (positions/sizes) that the Java code needs.
"""
import sys, os, json
from PIL import Image, ImageDraw

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)

def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)

BG = hexc("0E1116")
OUTLINE = hexc("05080E")
ACCENT = hexc("38BDF8")
TEXT = hexc("E5E7EB")
FADED = hexc("8B949E")
CLIENT_C = hexc("CBD5E1")
HIGHLIGHT = hexc("F0F9FF")
INNER_SHADE = hexc("0369A1")
GRAD = [hexc(c) for c in ["E0F2FE", "BAE6FD", "BAE6FD", "7DD3FC", "7DD3FC", "38BDF8", "38BDF8", "38BDF8",
                          "0EA5E9", "0EA5E9", "0EA5E9", "0EA5E9", "0284C7", "0284C7", "0284C7", "0284C7"]]

# 12x16 squared "tech" letters with cut corners
BIG = {
 "A": ["..########..", ".##########.", "###......###", "###......###", "###......###", "###......###",
       "############", "############", "###......###", "###......###", "###......###", "###......###",
       "###......###", "###......###", "###......###", "###......###"],
 "E": [".###########", "############", "###.........", "###.........", "###.........", "###.........",
       "#########...", "#########...", "###.........", "###.........", "###.........", "###.........",
       "###.........", "###.........", "############", ".###########"],
 "R": ["###########.", "############", "###......###", "###......###", "###......###", "###......###",
       "############", "###########.", "###....###..", "###.....###.", "###......###", "###......###",
       "###......###", "###......###", "###......###", "###......###"],
 "O": [".##########.", "############", "###......###", "###......###", "###......###", "###......###",
       "###......###", "###......###", "###......###", "###......###", "###......###", "###......###",
       "###......###", "###......###", "############", ".##########."],
}
SMALL = {
    "A": [".#.", "#.#", "###", "#.#", "#.#"], "B": ["##.", "#.#", "##.", "#.#", "##."],
    "C": [".##", "#..", "#..", "#..", ".##"], "D": ["##.", "#.#", "#.#", "#.#", "##."],
    "E": ["###", "#..", "##.", "#..", "###"], "F": ["###", "#..", "##.", "#..", "#.."],
    "G": [".##", "#..", "#.#", "#.#", ".##"], "H": ["#.#", "#.#", "###", "#.#", "#.#"],
    "I": ["###", ".#.", ".#.", ".#.", "###"], "J": ["..#", "..#", "..#", "#.#", ".#."],
    "K": ["#.#", "#.#", "##.", "#.#", "#.#"], "L": ["#..", "#..", "#..", "#..", "###"],
    "M": ["#...#", "##.##", "#.#.#", "#...#", "#...#"], "N": ["#..#", "##.#", "#.##", "#..#", "#..#"],
    "O": [".#.", "#.#", "#.#", "#.#", ".#."], "P": ["##.", "#.#", "##.", "#..", "#.."],
    "Q": [".#.", "#.#", "#.#", "##.", ".##"], "R": ["##.", "#.#", "##.", "#.#", "#.#"],
    "S": [".##", "#..", ".#.", "..#", "##."], "T": ["###", ".#.", ".#.", ".#.", ".#."],
    "U": ["#.#", "#.#", "#.#", "#.#", "###"], "V": ["#.#", "#.#", "#.#", "#.#", ".#."],
    "W": ["#...#", "#...#", "#.#.#", "##.##", "#...#"], "X": ["#.#", "#.#", ".#.", "#.#", "#.#"],
    "Y": ["#.#", "#.#", ".#.", ".#.", ".#."], "Z": ["###", "..#", ".#.", "#..", "###"],
    "0": ["###", "#.#", "#.#", "#.#", "###"], "1": [".#.", "##.", ".#.", ".#.", "###"],
    "2": ["##.", "..#", ".#.", "#..", "###"], "3": ["##.", "..#", ".#.", "..#", "##."],
    "4": ["#.#", "#.#", "###", "..#", "..#"], "5": ["###", "#..", "##.", "..#", "##."],
    "6": [".##", "#..", "###", "#.#", "###"], "7": ["###", "..#", ".#.", ".#.", ".#."],
    "8": ["###", "#.#", "###", "#.#", "###"], "9": ["###", "#.#", "###", "..#", "##."],
    ".": [".", ".", ".", ".", "#"], "!": ["#", "#", "#", ".", "#"], ":": [".", "#", ".", "#", "."],
    "(": [".#", "#.", "#.", "#.", ".#"], ")": ["#.", ".#", ".#", ".#", "#."],
    "-": ["...", "...", "###", "...", "..."], " ": ["..", "..", "..", "..", ".."],
}

def solid(px):
    return px[3] == 255

def text_width(text, font=SMALL, gap=1):
    return sum(len(font[c][0]) for c in text) + gap * (len(text) - 1)

def draw_small(img, text, x, y, color, gap=1):
    for c in text:
        for gy, row in enumerate(SMALL[c]):
            for gx, ch in enumerate(row):
                if ch == "#":
                    img.putpixel((x + gx, y + gy), color)
        x += len(SMALL[c][0]) + gap

def ring(img, color, src_test):
    """Pixels next to (8-way) pixels passing src_test get `color` if empty."""
    w, h = img.size
    src = img.copy()
    for y in range(h):
        for x in range(w):
            if src.getpixel((x, y))[3] != 0:
                continue
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h and src_test(src.getpixel((nx, ny))):
                        img.putpixel((x, y), color)
                        break
                else:
                    continue
                break

def decorate(art, glow=True):
    """Outline, then two soft glow rings in the accent color. Adds a 3px border."""
    w, h = art.size
    img = Image.new("RGBA", (w + 6, h + 6), (0, 0, 0, 0))
    img.paste(art, (3, 3))
    ring(img, OUTLINE, solid)
    if glow:
        ring(img, hexc("38BDF8", 70), solid)
        ring(img, hexc("38BDF8", 28), lambda p: p[3] == 70)
    return img

# ---------------- wordmark ----------------
SHEAR = 4   # top rows shift right by up to SHEAR-1 pixels: italic "speed" look
def shear_off(row):
    return (15 - row) // SHEAR

word_w = 4 * 12 + 3 * 2 + (15 // SHEAR)
word = Image.new("RGBA", (word_w, 16), (0, 0, 0, 0))
mask = Image.new("RGBA", (word_w, 16), (0, 0, 0, 0))
x = 0
for c in "AERO":
    for gy, row in enumerate(BIG[c]):
        for gx, ch in enumerate(row):
            if ch == "#":
                word.putpixel((x + gx + shear_off(gy), gy), GRAD[gy])
                mask.putpixel((x + gx + shear_off(gy), gy), (255, 255, 255, 255))
    x += 14
# bevel: light top edges, darker bottom edges
src = word.copy()
for y in range(16):
    for x in range(word_w):
        if src.getpixel((x, y))[3]:
            def at(xx, yy):
                return 0 <= xx < word_w and 0 <= yy < 16 and src.getpixel((xx, yy))[3]
            # the slant shifts upper rows right, so check diagonals too
            if not at(x, y - 1) and not at(x + 1, y - 1):
                word.putpixel((x, y), HIGHLIGHT)
            elif not at(x, y + 1) and not at(x - 1, y + 1) and y > 8:
                word.putpixel((x, y), INNER_SHADE)

# "CLIENT" row with accent lines on both sides
client_w = text_width("CLIENT", gap=3)
row_y = 16 + 3
base_art = Image.new("RGBA", (word_w, row_y + 5), (0, 0, 0, 0))
base_art.paste(word, (0, 0))
cx = (word_w - client_w) // 2
draw_small(base_art, "CLIENT", cx, row_y, CLIENT_C, gap=3)
for lx in range(1, cx - 3):
    base_art.putpixel((lx, row_y + 2), ACCENT)
for lx in range(cx + client_w + 3, word_w - 1):
    base_art.putpixel((lx, row_y + 2), ACCENT)
base = decorate(base_art)
shine = Image.new("RGBA", base.size, (0, 0, 0, 0))
shine.paste(mask, (3, 3))

# ---------------- "A" emblem ----------------
def badge_fill(size, radius):
    icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    top, bot = hexc("7DD3FC"), hexc("0284C7")
    for y in range(size):
        t = y / (size - 1)
        c = tuple(round(top[i] + (bot[i] - top[i]) * t) for i in range(4))
        if y == 0: c = hexc("BAE6FD")
        if y == size - 1: c = hexc("075985")
        for x in range(size):
            icon.putpixel((x, y), c)
    for cx, cy in ((0, 0), (size - 1, 0), (0, size - 1), (size - 1, size - 1)):
        for dx in range(radius):
            for dy in range(radius - dx):
                icon.putpixel((abs(cx - dx), abs(cy - dy)), (0, 0, 0, 0))
    return icon

def stamp_a(icon, glyph, x0, y0, slant_every):
    """White slanted 'A' with a 1px darker shadow under/right of it."""
    rows = len(glyph)
    def off(r):
        return (rows - 1 - r) // slant_every if slant_every else 0
    for color, dx, dy in ((hexc("0369A1"), 1, 1), (hexc("FFFFFF"), 0, 0)):
        for gy, row in enumerate(glyph):
            for gx, ch in enumerate(row):
                if ch == "#":
                    icon.putpixel((x0 + gx + off(gy) + dx, y0 + gy + dy), color)

E = 22
emblem_art = badge_fill(E, 3)
stamp_a(emblem_art, BIG["A"], (E - 15) // 2, (E - 16) // 2 - 1, SHEAR)
emblem = decorate(emblem_art)
emblem_mask = Image.new("RGBA", emblem.size, (0, 0, 0, 0))
emblem_mask.paste(Image.new("RGBA", (E, E), (255, 255, 255, 255)), (3, 3), badge_fill(E, 3))

# ---------------- full composition (for static pictures) ----------------
GAP = 2
full_w = emblem.width + GAP + base.width
full_h = max(base.height, emblem.height)
full = Image.new("RGBA", (full_w, full_h), (0, 0, 0, 0))
emblem_x, emblem_y = 0, (full_h - emblem.height) // 2
base_x, base_y = emblem.width + GAP, (full_h - base.height) // 2
full.alpha_composite(emblem, (emblem_x, emblem_y))
full.alpha_composite(base, (base_x, base_y))

# ---------------- atlas (128x128) for the game ----------------
atlas = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
regions = {}
def put(name, im, x, y):
    atlas.alpha_composite(im, (x, y))
    regions[name] = [x, y, im.width, im.height]
put("base", base, 0, 0)
put("shine", shine, 0, base.height + 1)
put("emblem", emblem, 0, 2 * base.height + 2)
put("emblemShine", emblem_mask, emblem.width + 2, 2 * base.height + 2)
layout = {"regions": regions, "full": [full_w, full_h],
          "offsets": {"emblem": [emblem_x, emblem_y], "base": [base_x, base_y]}}
atlas.save(os.path.join(OUT, "logo.png"), optimize=True)
full.save(os.path.join(OUT, "logo_full.png"), optimize=True)
json.dump(layout, open(os.path.join(OUT, "layout.json"), "w"))
print(json.dumps(layout))

# ---------------- favicon: small badge with a small A ----------------
SMALL_A = [".#######.", "#########", "###...###", "###...###", "#########", "#########",
           "###...###", "###...###", "###...###", "###...###"]
fav = badge_fill(16, 2)
stamp_a(fav, SMALL_A, 3, 3, 5)
fav.resize((32, 32), Image.NEAREST).save(os.path.join(OUT, "favicon.png"), optimize=True)

# ---------------- opaque loading pictures ----------------
def screen(size, mark, scale, lines, bar=None):
    img = Image.new("RGBA", (size, size), BG)
    m = mark.resize((mark.width * scale, mark.height * scale), Image.NEAREST)
    total = m.height + 10 + 7 * len(lines) + (8 if bar is not None else 0)
    top = (size - total) // 2
    img.alpha_composite(m, ((size - m.width) // 2, top))
    y = top + m.height + 10
    if bar is not None:
        bw = min(size - 40, m.width)
        bx = (size - bw) // 2
        ImageDraw.Draw(img).rectangle([bx, y, bx + bw - 1, y + 1], fill=hexc("1F2630"))
        ImageDraw.Draw(img).rectangle([bx, y, bx + int(bw * bar) - 1, y + 1], fill=ACCENT)
        y += 8
    for text, color in lines:
        draw_small(img, text, (size - text_width(text)) // 2, y, color)
        y += 7
    return img.convert("RGB")

screen(192, full, 1, [("LOADING ASSETS...", FADED)]).save(os.path.join(OUT, "load_screen.png"), optimize=True)
screen(256, full, 2, [("STARTING UP...", FADED)]).save(os.path.join(OUT, "final_screen.png"), optimize=True)
screen(128, emblem, 2, [("PRESS ANY KEY", TEXT), ("TO ENABLE SOUND", TEXT), ("(REQUIRED)", FADED)]).save(
    os.path.join(OUT, "enable_screen.png"), optimize=True)
