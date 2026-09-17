"""Chroma-key generated illuminations and export Android drawables."""
from pathlib import Path
from PIL import Image, ImageFilter, ImageEnhance
import math

SESSION = Path(
    r"C:\Users\technicien\.grok\sessions"
    r"\C%3A%5CUsers%5Ctechnicien%5CDocuments%5Cgrok"
    r"\01a0aea0-55e9-7e02-80b6-cfcd4d5c4c3b\images"
)
ROOT = Path(r"C:\Users\technicien\Documents\grok\Cartulaire")
DRAW = ROOT / "app" / "src" / "main" / "res" / "drawable-xxhdpi"
DRAW_NODPI = ROOT / "app" / "src" / "main" / "res" / "drawable"
MIPMAPS = {
    "mdpi": (ROOT / "app" / "src" / "main" / "res" / "mipmap-mdpi", 48),
    "hdpi": (ROOT / "app" / "src" / "main" / "res" / "mipmap-hdpi", 72),
    "xhdpi": (ROOT / "app" / "src" / "main" / "res" / "mipmap-xhdpi", 96),
    "xxhdpi": (ROOT / "app" / "src" / "main" / "res" / "mipmap-xxhdpi", 144),
    "xxxhdpi": (ROOT / "app" / "src" / "main" / "res" / "mipmap-xxxhdpi", 192),
}


def chroma_key(im: Image.Image, threshold: int = 70) -> Image.Image:
    import colorsys
    im = im.convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            hls = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            hue, sat, val = hls
            greenish = (0.18 <= hue <= 0.47 and sat >= 0.28 and val >= 0.28)
            strong = g > 70 and g > r + 25 and g > b + 20
            if greenish or strong:
                px[x, y] = (r, g, b, 0)
    return im


def trim_alpha(im: Image.Image, pad: int = 8) -> Image.Image:
    bbox = im.getbbox()
    if not bbox:
        return im
    l, t, r, b = bbox
    l = max(0, l - pad)
    t = max(0, t - pad)
    r = min(im.width, r + pad)
    b = min(im.height, b + pad)
    return im.crop((l, t, r, b))


def save_png(im: Image.Image, path: Path, size: int | None = None):
    if size:
        im = im.resize((size, size), Image.Resampling.LANCZOS)
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path, "PNG")
    print("wrote", path, im.size)


def circular_alpha(im: Image.Image) -> Image.Image:
    """Keep a circular mask so leftover green corners disappear."""
    im = im.convert("RGBA")
    w, h = im.size
    cx, cy = w / 2, h / 2
    rad = min(w, h) / 2 - 2
    px = im.load()
    for y in range(h):
        for x in range(w):
            d = math.hypot(x - cx, y - cy)
            r, g, b, a = px[x, y]
            if d > rad:
                fade = max(0.0, 1.0 - (d - rad) / 3.0)
                px[x, y] = (r, g, b, int(a * fade))
    return im


def process_medallion(src: Path, dest_name: str, size: int = 256):
    im = chroma_key(Image.open(src))
    im = trim_alpha(im, pad=4)
    im = circular_alpha(im)
    save_png(im, DRAW / dest_name, size)


def process_freeform(src: Path, dest: Path, size: int | None = None):
    im = chroma_key(Image.open(src), threshold=55)
    im = trim_alpha(im, pad=2)
    if size:
        # keep aspect
        ratio = im.width / im.height
        if ratio >= 1:
            im = im.resize((size, int(size / ratio)), Image.Resampling.LANCZOS)
        else:
            im = im.resize((int(size * ratio), size), Image.Resampling.LANCZOS)
    dest.parent.mkdir(parents=True, exist_ok=True)
    im.save(dest, "PNG")
    print("wrote", dest, im.size)
    return im


def main():
    process_medallion(SESSION / "1.jpg", "marker_church.png", 256)
    process_medallion(SESSION / "8.jpg", "marker_castle.png", 256)
    process_medallion(SESSION / "10.jpg", "marker_village.png", 256)
    process_medallion(SESSION / "9.jpg", "marker_pilgrim.png", 256)
    process_freeform(SESSION / "3.jpg", DRAW / "compass_rose.png", 320)
    process_freeform(SESSION / "7.jpg", DRAW / "wax_seal.png", 256)
    corner = process_freeform(SESSION / "5.jpg", DRAW / "illum_corner_tl.png", 420)
    corner.transpose(Image.Transpose.FLIP_LEFT_RIGHT).save(DRAW / "illum_corner_tr.png", "PNG")
    corner.transpose(Image.Transpose.FLIP_TOP_BOTTOM).save(DRAW / "illum_corner_bl.png", "PNG")
    corner.transpose(Image.Transpose.FLIP_LEFT_RIGHT).transpose(
        Image.Transpose.FLIP_TOP_BOTTOM
    ).save(DRAW / "illum_corner_br.png", "PNG")
    print("wrote flipped corners")

    parchment = Image.open(SESSION / "4.jpg").convert("RGB")
    parchment = parchment.resize((1024, 1024), Image.Resampling.LANCZOS)
    parchment.save(DRAW_NODPI / "parchment_texture.jpg", "JPEG", quality=82)
    print("wrote parchment")

    frame = chroma_key(Image.open(SESSION / "2.jpg"), threshold=55)
    frame = trim_alpha(frame, pad=2)
    frame.resize((480, 640), Image.Resampling.LANCZOS).save(DRAW / "panel_frame.png", "PNG")
    print("wrote panel_frame")

    icon = Image.open(SESSION / "6.jpg").convert("RGBA")
    # crop to the oxblood square if present
    w, h = icon.size
    side = min(w, h)
    icon = icon.crop(((w - side) // 2, (h - side) // 2, (w + side) // 2, (h + side) // 2))
    for name, (folder, px) in MIPMAPS.items():
        resized = icon.resize((px, px), Image.Resampling.LANCZOS)
        resized.save(folder / "ic_launcher.png", "PNG")
        resized.save(folder / "ic_launcher_round.png", "PNG")
        print("launcher", name, px)

    # round adaptive-ish foreground: the medallion only
    fg = chroma_key(Image.open(SESSION / "6.jpg"), threshold=40)
    fg = trim_alpha(fg, 8)
    fg.resize((432, 432), Image.Resampling.LANCZOS).save(DRAW_NODPI / "ic_launcher_foreground.png", "PNG")


if __name__ == "__main__":
    main()
