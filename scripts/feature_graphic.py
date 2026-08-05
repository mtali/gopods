#!/usr/bin/env python3
"""Builds the Play Store feature graphic at store/feature-graphic.png.

1024x500 with no alpha channel, which is what the Play Console accepts. Generated
rather than drawn by hand so it can be rebuilt when the screenshots or the brand
colour change:

    python3 scripts/feature_graphic.py

Text is kept well inside the edges because the Console overlays controls near them
and crops the graphic differently across surfaces.
"""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

WIDTH, HEIGHT = 1024, 500
MARGIN = 64

# Seeded from the app's fallback primary, the deep purple the app shipped with.
TOP_LEFT = (103, 80, 164)
BOTTOM_RIGHT = (36, 22, 66)

FONT_BOLD = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
FONT_REGULAR = "/System/Library/Fonts/Supplemental/Arial.ttf"

ROOT = Path(__file__).resolve().parent.parent
PHOTOS = ROOT / "store" / "photos"
OUT = ROOT / "store" / "feature-graphic.png"

PHONES = ["01-library.png", "04-now-playing.png"]
PHONE_MAX_HEIGHT = 384
TEXT_GUTTER = 48
PHONE_RADIUS = 22
PHONE_GAP = 20


def gradient() -> Image.Image:
    """Diagonal gradient, drawn per pixel row and column via a small blend."""
    base = Image.new("RGB", (WIDTH, HEIGHT), TOP_LEFT)
    draw = ImageDraw.Draw(base)
    for y in range(HEIGHT):
        for_x = y / max(HEIGHT - 1, 1)
        # Blend along the diagonal by mixing the row's progress with the column's.
        start = tuple(
            int(TOP_LEFT[c] + (BOTTOM_RIGHT[c] - TOP_LEFT[c]) * for_x * 0.55)
            for c in range(3)
        )
        end = tuple(
            int(TOP_LEFT[c] + (BOTTOM_RIGHT[c] - TOP_LEFT[c]) * min(for_x * 0.55 + 0.45, 1.0))
            for c in range(3)
        )
        for x in range(0, WIDTH, 8):
            t = x / max(WIDTH - 1, 1)
            colour = tuple(int(start[c] + (end[c] - start[c]) * t) for c in range(3))
            draw.rectangle([x, y, x + 8, y + 1], fill=colour)
    return base


def rounded(image: Image.Image, radius: int) -> Image.Image:
    mask = Image.new("L", image.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, image.size[0], image.size[1]], radius, fill=255)
    out = image.convert("RGBA")
    out.putalpha(mask)
    return out


def phone(name: str, height: int) -> Image.Image | None:
    path = PHOTOS / name
    if not path.exists():
        print(f"skipping missing {path}")
        return None
    shot = Image.open(path).convert("RGB")
    width = int(shot.width * height / shot.height)
    return rounded(shot.resize((width, height), Image.LANCZOS), PHONE_RADIUS)


def main() -> None:
    canvas = gradient()
    draw = ImageDraw.Draw(canvas)

    title_font = ImageFont.truetype(FONT_BOLD, 96)
    tagline_font = ImageFont.truetype(FONT_REGULAR, 34)
    title, tagline = "GoPods", "Podcasts, simply played"

    # Measure the copy and give the phones whatever is left, rather than guessing a
    # width and having the title run underneath them.
    text_right = MARGIN + max(
        draw.textlength(title, font=title_font),
        draw.textlength(tagline, font=tagline_font),
    )
    available = WIDTH - MARGIN - (text_right + TEXT_GUTTER)

    ratio = 1080 / 2400
    per_phone = (available - PHONE_GAP * (len(PHONES) - 1)) / len(PHONES)
    height = min(PHONE_MAX_HEIGHT, int(per_phone / ratio))

    shots = [s for s in (phone(n, height) for n in PHONES) if s is not None]
    if shots:
        y = (HEIGHT - height) // 2
        total = sum(s.width for s in shots) + PHONE_GAP * (len(shots) - 1)
        x = WIDTH - MARGIN - total
        if x < text_right + TEXT_GUTTER:
            raise SystemExit(f"phones would overlap the copy: {x} < {text_right + TEXT_GUTTER}")
        for shot in shots:
            shadow = Image.new("RGBA", (shot.width + 24, shot.height + 24), (0, 0, 0, 0))
            ImageDraw.Draw(shadow).rounded_rectangle(
                [12, 12, shot.width + 12, shot.height + 12], PHONE_RADIUS, fill=(0, 0, 0, 70)
            )
            canvas.paste(shadow, (x - 12, y - 12), shadow)
            canvas.paste(shot, (x, y), shot)
            x += shot.width + PHONE_GAP
        print(f"phones {height}px tall starting at x={WIDTH - MARGIN - total}, copy ends at {int(text_right)}")

    draw.text((MARGIN, 176), title, font=title_font, fill=(255, 255, 255))
    draw.text((MARGIN + 4, 292), tagline, font=tagline_font, fill=(226, 216, 255))

    # No alpha: the Console rejects a graphic with a transparency channel.
    canvas.convert("RGB").save(OUT, "PNG")
    print(f"wrote {OUT.relative_to(ROOT)} {canvas.size[0]}x{canvas.size[1]}")


if __name__ == "__main__":
    main()
