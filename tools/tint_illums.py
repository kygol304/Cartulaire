"""Chroma-key + parchment tint for map illuminations."""
from pathlib import Path
import colorsys
from PIL import Image
import importlib.util

spec = importlib.util.spec_from_file_location(
    "pa", Path(__file__).with_name("process_assets.py")
)
pa = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pa)

S, D = pa.SESSION, pa.DRAW

NEW = {
    "27.jpg": "illum_boar.png",
    "28.jpg": "illum_mill.png",
    "29.jpg": "illum_ship.png",
    "30.jpg": "illum_timber.png",
    "31.jpg": "illum_port.png",
}


def parchment_tint(im: Image.Image) -> Image.Image:
    im = im.convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a < 10:
                px[x, y] = (0, 0, 0, 0)
                continue
            yv = 0.32 * r + 0.50 * g + 0.18 * b
            sr = min(255, int(yv * 1.05 + 28))
            sg = min(255, int(yv * 0.88 + 12))
            sb = min(255, int(yv * 0.55 + 4))
            nr = int(r * 0.48 + sr * 0.52)
            ng = int(g * 0.48 + sg * 0.52)
            nb = int(b * 0.48 + sb * 0.52)
            px[x, y] = (nr, ng, nb, a)
    return im


def process(src: Path, dest: Path, size: int = 300):
    im = pa.chroma_key(Image.open(src), threshold=40)
    im = pa.trim_alpha(im, pad=2)
    im = parchment_tint(im)
    ratio = im.width / max(1, im.height)
    if ratio >= 1:
        im = im.resize((size, max(1, int(size / ratio))), Image.Resampling.LANCZOS)
    else:
        im = im.resize((max(1, int(size * ratio)), size), Image.Resampling.LANCZOS)
    dest.parent.mkdir(parents=True, exist_ok=True)
    im.save(dest, "PNG")
    print("wrote", dest.name, im.size)


def main():
    for src, dst in NEW.items():
        process(S / src, D / dst)
    existing = [
        "illum_city_wall.png",
        "illum_city_river.png",
        "illum_village.png",
        "illum_abbey.png",
        "illum_peasants.png",
        "illum_death.png",
        "illum_skeleton.png",
        "illum_knight.png",
        "illum_margin.png",
    ]
    for name in existing:
        path = D / name
        if not path.exists():
            continue
        im = parchment_tint(Image.open(path))
        im.save(path, "PNG")
        print("tinted", name, im.size)


if __name__ == "__main__":
    main()
