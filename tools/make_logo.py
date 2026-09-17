"""Build Cartulaire emblem, wordmark, and launcher mipmaps."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import importlib.util

ROOT = Path(r"C:\Users\technicien\Documents\grok\Cartulaire")
SESSION = Path(
    r"C:\Users\technicien\.grok\sessions"
    r"\C%3A%5CUsers%5Ctechnicien%5CDocuments%5Cgrok"
    r"\01a0aea0-55e9-7e02-80b6-cfcd4d5c4c3b\images"
)
RES = ROOT / "app" / "src" / "main" / "res"
DRAW = RES / "drawable"
FONT = RES / "font" / "cinzel_decorative_bold.ttf"
MEDAL = SESSION / "33.jpg"
SEAL = SESSION / "32.jpg"

MIPMAPS = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def chroma_key(im: Image.Image) -> Image.Image:
    import colorsys
    im = im.convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            hls = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            hue, sat, val = hls
            if (0.18 <= hue <= 0.47 and sat >= 0.28 and val >= 0.28) or (
                g > 70 and g > r + 25 and g > b + 20
            ):
                px[x, y] = (r, g, b, 0)
    return im


def crop_square(im: Image.Image) -> Image.Image:
    im = im.convert("RGBA")
    w, h = im.size
    side = min(w, h)
    l = (w - side) // 2
    t = (h - side) // 2
    return im.crop((l, t, l + side, t + side))


def main():
    emblem = crop_square(Image.open(MEDAL))
    DRAW.mkdir(parents=True, exist_ok=True)
    emblem.resize((1024, 1024), Image.Resampling.LANCZOS).save(DRAW / "logo_emblem.png", "PNG")

    for folder, px in MIPMAPS.items():
        out = RES / folder
        out.mkdir(parents=True, exist_ok=True)
        ico = emblem.resize((px, px), Image.Resampling.LANCZOS)
        ico.save(out / "ic_launcher.png", "PNG")
        ico.save(out / "ic_launcher_round.png", "PNG")
        print("launcher", folder, px)

    # round adaptive-ish foreground: the medallion
    emblem.resize((432, 432), Image.Resampling.LANCZOS).save(
        RES / "drawable" / "ic_launcher_foreground.png", "PNG"
    )

    seal = chroma_key(Image.open(SEAL))
    bbox = seal.getbbox()
    if bbox:
        seal = seal.crop(bbox)
    seal.resize((512, 512), Image.Resampling.LANCZOS).save(DRAW / "logo_seal.png", "PNG")

    # Horizontal wordmark: emblem + CARTULAIRE
    mark = emblem.resize((420, 420), Image.Resampling.LANCZOS)
    font = ImageFont.truetype(str(FONT), 92)
    small = ImageFont.truetype(str(FONT), 28)
    title = "CARTULAIRE"

    tmp = Image.new("RGBA", (10, 10), (0, 0, 0, 0))
    d = ImageDraw.Draw(tmp)
    tb = d.textbbox((0, 0), title, font=font)
    tw, th = tb[2] - tb[0], tb[3] - tb[1]

    pad = 48
    width = pad + mark.width + 40 + tw + pad
    height = pad + max(mark.height, th + 24) + pad
    canvas = Image.new("RGBA", (width, height), (232, 213, 168, 255))
    draw = ImageDraw.Draw(canvas)
    canvas.paste(mark, (pad, (height - mark.height) // 2), mark)

    tx = pad + mark.width + 36
    ty = (height - th) // 2 - 8
    draw.text((tx + 2, ty + 2), title, font=font, fill=(58, 36, 22, 255))
    draw.text((tx, ty), title, font=font, fill=(107, 45, 45, 255))

    canvas.save(DRAW / "logo_wordmark.png", "PNG")
    # also a copy for the user to open
    (ROOT / "logo_cartulaire.png").write_bytes((DRAW / "logo_wordmark.png").read_bytes())
    print("wordmark", canvas.size)
    print("letters:", title)


if __name__ == "__main__":
    main()
