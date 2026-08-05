#!/usr/bin/env python3
"""Builds the Play Store feature graphic at store/feature-graphic.png.

1024x500, no alpha channel, which is what the Play Console accepts. Generated rather
than drawn by hand so it can be rebuilt when the screenshots or the brand colour
change:

    python3 scripts/feature_graphic.py

Gradients are built small and scaled up with bicubic, which is smooth and far quicker
than a per pixel loop in Python.

Copy is measured and the artwork is fitted into what is left, then the script fails if
they would collide. Earlier hand placed versions had the title running underneath the
phones.
"""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

WIDTH, HEIGHT = 1024, 500
MARGIN = 64

# Deep violet through to the app's primary purple, with a lighter accent for the glow.
CORNERS = {
    "top_left": (46, 22, 92),
    "top_right": (108, 74, 190),
    "bottom_left": (24, 12, 48),
    "bottom_right": (72, 42, 140),
}
GLOW = (156, 122, 255)
TITLE_COLOUR = (255, 255, 255)
TAGLINE_COLOUR = (214, 200, 255)

FONT_BOLD = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
FONT_REGULAR = "/System/Library/Fonts/Supplemental/Arial.ttf"

ROOT = Path(__file__).resolve().parent.parent
SHOTS = ROOT / "store" / "screenshots"
OUT = ROOT / "store" / "feature-graphic.png"

TITLE = "GoPods"
TAGLINE = "Podcasts, simply played"

# Front phone last so it lands on top. Each has its own tilt for a bit of movement.
PHONES = [("04-now-playing.jpg", 7.0), ("01-library.jpg", -5.0)]
PHONE_MAX_HEIGHT = 372
PHONE_RADIUS = 26
PHONE_OVERLAP = 74
TEXT_GUTTER = 40

# Mirrors the equalizer bars the app shows beside a playing episode.
WAVEFORM = [0.30, 0.62, 0.42, 0.88, 0.55, 1.0, 0.70, 0.38, 0.80, 0.48, 0.26]


def background() -> Image.Image:
    """Four corner blend, built at 2x2 and scaled up."""
    small = Image.new("RGB", (2, 2))
    small.putpixel((0, 0), CORNERS["top_left"])
    small.putpixel((1, 0), CORNERS["top_right"])
    small.putpixel((0, 1), CORNERS["bottom_left"])
    small.putpixel((1, 1), CORNERS["bottom_right"])
    canvas = small.resize((WIDTH, HEIGHT), Image.BICUBIC)

    # A soft pool of light behind the artwork, so the phones sit in something rather
    # than floating on a flat panel.
    halo = Image.radial_gradient("L").resize((900, 900), Image.BICUBIC)
    halo = Image.eval(halo, lambda v: max(0, 190 - v))
    tint = Image.new("RGB", halo.size, GLOW)
    canvas.paste(tint, (int(WIDTH * 0.58), -210), halo)
    return canvas


def waveform(canvas: Image.Image, left: int, baseline: int) -> None:
    layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    bar_width, gap, tallest = 7, 11, 62
    x = left
    for value in WAVEFORM:
        height = int(tallest * value)
        draw.rounded_rectangle(
            [x, baseline - height, x + bar_width, baseline],
            radius=bar_width // 2,
            fill=(255, 255, 255, 64),
        )
        x += bar_width + gap
    canvas.paste(layer, (0, 0), layer)


def phone(name: str, height: int, tilt: float) -> Image.Image | None:
    path = SHOTS / name
    if not path.exists():
        print(f"skipping missing {path}")
        return None

    shot = Image.open(path).convert("RGB")
    width = int(shot.width * height / shot.height)
    shot = shot.resize((width, height), Image.LANCZOS)

    mask = Image.new("L", shot.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, width, height], PHONE_RADIUS, fill=255)
    framed = shot.convert("RGBA")
    framed.putalpha(mask)

    # A hairline edge keeps a dark screenshot from bleeding into a dark background.
    ImageDraw.Draw(framed).rounded_rectangle(
        [0, 0, width - 1, height - 1], PHONE_RADIUS, outline=(255, 255, 255, 70), width=2
    )
    return framed.rotate(tilt, expand=True, resample=Image.BICUBIC)


def drop_shadow(canvas: Image.Image, art: Image.Image, x: int, y: int) -> None:
    pad = 40
    layer = Image.new("RGBA", (art.width + pad * 2, art.height + pad * 2), (0, 0, 0, 0))
    layer.paste(Image.new("RGBA", art.size, (0, 0, 0, 130)), (pad, pad), art)
    layer = layer.filter(ImageFilter.GaussianBlur(18))
    canvas.paste(layer, (x - pad, y - pad + 12), layer)


def main() -> None:
    canvas = background()
    draw = ImageDraw.Draw(canvas)

    title_font = ImageFont.truetype(FONT_BOLD, 104)
    tagline_font = ImageFont.truetype(FONT_REGULAR, 33)

    text_right = MARGIN + max(
        draw.textlength(TITLE, font=title_font),
        draw.textlength(TAGLINE, font=tagline_font),
    )
    available = WIDTH - MARGIN - (text_right + TEXT_GUTTER)

    ratio = 1080 / 2400
    span = len(PHONES) - 1
    per_phone = (available + PHONE_OVERLAP * span) / len(PHONES)
    height = min(PHONE_MAX_HEIGHT, int(per_phone / ratio))

    shots = [(phone(name, height, tilt), tilt) for name, tilt in PHONES]
    shots = [(art, tilt) for art, tilt in shots if art is not None]

    if shots:
        total = sum(art.width for art, _ in shots) - PHONE_OVERLAP * (len(shots) - 1)
        start = WIDTH - MARGIN - total
        if start < text_right + TEXT_GUTTER:
            raise SystemExit(
                f"artwork would overlap the copy: starts at {start}, "
                f"copy needs up to {int(text_right + TEXT_GUTTER)}"
            )
        x = start
        for art, _ in shots:
            y = (HEIGHT - art.height) // 2
            drop_shadow(canvas, art, x, y)
            canvas.paste(art, (x, y), art)
            x += art.width - PHONE_OVERLAP
        print(f"artwork {height}px tall from x={start}, copy ends at {int(text_right)}")

    draw = ImageDraw.Draw(canvas)
    draw.text((MARGIN, 150), TITLE, font=title_font, fill=TITLE_COLOUR)
    draw.text((MARGIN + 5, 272), TAGLINE, font=tagline_font, fill=TAGLINE_COLOUR)
    waveform(canvas, left=MARGIN + 5, baseline=400)

    # No alpha: the Console rejects a graphic carrying a transparency channel.
    canvas.convert("RGB").save(OUT, "PNG")
    print(f"wrote {OUT.relative_to(ROOT)} {canvas.size[0]}x{canvas.size[1]}")


if __name__ == "__main__":
    main()
