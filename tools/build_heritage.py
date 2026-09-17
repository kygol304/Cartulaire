"""Build heritage.db: Christian churches only + castles, tagged by department."""
from __future__ import annotations

import csv
import json
import math
import sqlite3
from pathlib import Path

ROOT = Path(r"C:\Users\technicien\Documents\grok\Cartulaire")
DATA = ROOT / "tools" / "data"
ASSETS = ROOT / "app" / "src" / "main" / "assets"
OUT_DB = ASSETS / "heritage.db"
MAP_DIR = ASSETS / "map"

# dept code -> (region INSEE code, region name)
DEPT_REGION = {
    "01": ("84", "Auvergne-Rhône-Alpes"),
    "02": ("32", "Hauts-de-France"),
    "03": ("84", "Auvergne-Rhône-Alpes"),
    "04": ("93", "Provence-Alpes-Côte d'Azur"),
    "05": ("93", "Provence-Alpes-Côte d'Azur"),
    "06": ("93", "Provence-Alpes-Côte d'Azur"),
    "07": ("84", "Auvergne-Rhône-Alpes"),
    "08": ("44", "Grand Est"),
    "09": ("76", "Occitanie"),
    "10": ("44", "Grand Est"),
    "11": ("76", "Occitanie"),
    "12": ("76", "Occitanie"),
    "13": ("93", "Provence-Alpes-Côte d'Azur"),
    "14": ("28", "Normandie"),
    "15": ("84", "Auvergne-Rhône-Alpes"),
    "16": ("75", "Nouvelle-Aquitaine"),
    "17": ("75", "Nouvelle-Aquitaine"),
    "18": ("24", "Centre-Val de Loire"),
    "19": ("75", "Nouvelle-Aquitaine"),
    "21": ("27", "Bourgogne-Franche-Comté"),
    "22": ("53", "Bretagne"),
    "23": ("75", "Nouvelle-Aquitaine"),
    "24": ("75", "Nouvelle-Aquitaine"),
    "25": ("27", "Bourgogne-Franche-Comté"),
    "26": ("84", "Auvergne-Rhône-Alpes"),
    "27": ("28", "Normandie"),
    "28": ("24", "Centre-Val de Loire"),
    "29": ("53", "Bretagne"),
    "2A": ("94", "Corse"),
    "2B": ("94", "Corse"),
    "30": ("76", "Occitanie"),
    "31": ("76", "Occitanie"),
    "32": ("76", "Occitanie"),
    "33": ("75", "Nouvelle-Aquitaine"),
    "34": ("76", "Occitanie"),
    "35": ("53", "Bretagne"),
    "36": ("24", "Centre-Val de Loire"),
    "37": ("24", "Centre-Val de Loire"),
    "38": ("84", "Auvergne-Rhône-Alpes"),
    "39": ("27", "Bourgogne-Franche-Comté"),
    "40": ("75", "Nouvelle-Aquitaine"),
    "41": ("24", "Centre-Val de Loire"),
    "42": ("84", "Auvergne-Rhône-Alpes"),
    "43": ("84", "Auvergne-Rhône-Alpes"),
    "44": ("52", "Pays de la Loire"),
    "45": ("24", "Centre-Val de Loire"),
    "46": ("76", "Occitanie"),
    "47": ("75", "Nouvelle-Aquitaine"),
    "48": ("76", "Occitanie"),
    "49": ("52", "Pays de la Loire"),
    "50": ("28", "Normandie"),
    "51": ("44", "Grand Est"),
    "52": ("44", "Grand Est"),
    "53": ("52", "Pays de la Loire"),
    "54": ("44", "Grand Est"),
    "55": ("44", "Grand Est"),
    "56": ("53", "Bretagne"),
    "57": ("44", "Grand Est"),
    "58": ("27", "Bourgogne-Franche-Comté"),
    "59": ("32", "Hauts-de-France"),
    "60": ("32", "Hauts-de-France"),
    "61": ("28", "Normandie"),
    "62": ("32", "Hauts-de-France"),
    "63": ("84", "Auvergne-Rhône-Alpes"),
    "64": ("75", "Nouvelle-Aquitaine"),
    "65": ("76", "Occitanie"),
    "66": ("76", "Occitanie"),
    "67": ("44", "Grand Est"),
    "68": ("44", "Grand Est"),
    "69": ("84", "Auvergne-Rhône-Alpes"),
    "70": ("27", "Bourgogne-Franche-Comté"),
    "71": ("27", "Bourgogne-Franche-Comté"),
    "72": ("52", "Pays de la Loire"),
    "73": ("84", "Auvergne-Rhône-Alpes"),
    "74": ("84", "Auvergne-Rhône-Alpes"),
    "75": ("11", "Île-de-France"),
    "76": ("28", "Normandie"),
    "77": ("11", "Île-de-France"),
    "78": ("11", "Île-de-France"),
    "79": ("75", "Nouvelle-Aquitaine"),
    "80": ("32", "Hauts-de-France"),
    "81": ("76", "Occitanie"),
    "82": ("76", "Occitanie"),
    "83": ("93", "Provence-Alpes-Côte d'Azur"),
    "84": ("93", "Provence-Alpes-Côte d'Azur"),
    "85": ("52", "Pays de la Loire"),
    "86": ("75", "Nouvelle-Aquitaine"),
    "87": ("75", "Nouvelle-Aquitaine"),
    "88": ("44", "Grand Est"),
    "89": ("27", "Bourgogne-Franche-Comté"),
    "90": ("27", "Bourgogne-Franche-Comté"),
    "91": ("11", "Île-de-France"),
    "92": ("11", "Île-de-France"),
    "93": ("11", "Île-de-France"),
    "94": ("11", "Île-de-France"),
    "95": ("11", "Île-de-France"),
}

EXCLUDE_RELIGION = {
    "muslim", "islam", "islamic", "jewish", "judaism", "buddhist", "buddhism",
    "hindu", "hinduism", "sikh", "shinto", "taoist", "taoism", "pagan",
    "multifaith", "bahai", "jain", "scientologist",
}
EXCLUDE_NAME = (
    "mosquée", "mosquee", "mosque", "synagogue", "pagode", "gurdwara",
    "temple bouddhiste", "temple hindou", "salle du royaume",
)


def parse_point(value: str):
    if not value:
        return None
    value = value.strip().strip('"')
    if "," not in value:
        return None
    a, b = value.split(",", 1)
    try:
        x, y = float(a.strip()), float(b.strip())
    except ValueError:
        return None
    # churches csv is "lat, lon"; castles csv is "lat, lon" in meta_geo_point too
    if abs(x) <= 90 and abs(y) <= 180:
        return x, y  # lat, lon
    return y, x


def is_christian_church(religion: str, denomination: str, name: str, other_tags: str) -> bool:
    rel = (religion or "").strip().lower()
    den = (denomination or "").strip().lower()
    nm = (name or "").strip().lower()
    tags = (other_tags or "").lower()
    if rel in EXCLUDE_RELIGION:
        return False
    if any(bad in nm for bad in EXCLUDE_NAME):
        return False
    if "mosquée" in nm or nm.startswith("mosque"):
        return False
    if rel in {"christian", "christianity", "catholic", "protestant", "orthodox"}:
        return True
    if den in {"catholic", "roman_catholic", "protestant", "reformed", "orthodox",
               "anglican", "baptist", "evangelical", "lutheran", "methodist"}:
        return True
    if any(w in nm for w in ("église", "eglise", "chapelle", "cathédrale", "cathedrale",
                             "basilique", "abbatiale", "prieuré", "prieure", "collégiale",
                             "collegiale", "oratoire")):
        return True
    # French "Temple" almost always = Protestant church
    if nm.startswith("temple ") or "temple protestant" in nm:
        return True
    if '"building": "church"' in tags or '"building": "cathedral"' in tags or '"building": "chapel"' in tags:
        if rel in ("", "christian"):
            return True
    if rel in ("",) and den in ("",) and not nm:
        return False
    return rel in ("", "christian") and not any(bad in nm for bad in EXCLUDE_NAME)


def point_in_ring(lat: float, lon: float, ring: list) -> bool:
    # ray casting, ring is [[lon, lat], ...]
    inside = False
    n = len(ring)
    if n < 3:
        return False
    j = n - 1
    for i in range(n):
        xi, yi = ring[i][0], ring[i][1]
        xj, yj = ring[j][0], ring[j][1]
        if ((yi > lat) != (yj > lat)) and (lon < (xj - xi) * (lat - yi) / (yj - yi + 1e-15) + xi):
            inside = not inside
        j = i
    return inside


def point_in_polygon(lat: float, lon: float, geom: dict) -> bool:
    gtype = geom.get("type")
    coords = geom.get("coordinates") or []
    if gtype == "Polygon":
        if not coords:
            return False
        if not point_in_ring(lat, lon, coords[0]):
            return False
        for hole in coords[1:]:
            if point_in_ring(lat, lon, hole):
                return False
        return True
    if gtype == "MultiPolygon":
        for poly in coords:
            if point_in_polygon(lat, lon, {"type": "Polygon", "coordinates": poly}):
                return True
        return False
    return False


def bbox_of(geom: dict):
    minx = miny = 1e9
    maxx = maxy = -1e9

    def walk(c):
        nonlocal minx, miny, maxx, maxy
        if isinstance(c, (int, float)):
            return
        if c and isinstance(c[0], (int, float)) and len(c) >= 2:
            minx, maxx = min(minx, c[0]), max(maxx, c[0])
            miny, maxy = min(miny, c[1]), max(maxy, c[1])
            return
        for x in c:
            walk(x)

    walk(geom.get("coordinates"))
    return miny, minx, maxy, maxx  # s,w,n,e


def load_depts():
    raw = json.loads((DATA / "departements.geojson").read_text(encoding="utf-8"))
    depts = []
    for feat in raw["features"]:
        code = str(feat["properties"]["code"])
        name = feat["properties"]["nom"]
        geom = feat["geometry"]
        s, w, n, e = bbox_of(geom)
        reg = DEPT_REGION.get(code, ("", ""))
        depts.append({
            "code": code, "name": name, "region": reg[0], "region_name": reg[1],
            "south": s, "west": w, "north": n, "east": e, "geom": geom,
        })
        feat["properties"]["region"] = reg[0]
        feat["properties"]["region_name"] = reg[1]
    MAP_DIR.mkdir(parents=True, exist_ok=True)
    (MAP_DIR / "departements.geojson").write_text(
        json.dumps(raw, ensure_ascii=False, separators=(",", ":")), encoding="utf-8"
    )
    return depts


def load_regions():
    raw = json.loads((DATA / "regions.geojson").read_text(encoding="utf-8"))
    regions = []
    for feat in raw["features"]:
        code = str(feat["properties"]["code"])
        name = feat["properties"]["nom"]
        s, w, n, e = bbox_of(feat["geometry"])
        regions.append({"code": code, "name": name, "south": s, "west": w, "north": n, "east": e})
    (MAP_DIR / "regions.geojson").write_text(
        json.dumps(raw, ensure_ascii=False, separators=(",", ":")), encoding="utf-8"
    )
    return regions


def locate(lat: float, lon: float, depts: list) -> tuple[str, str]:
    candidates = [
        d for d in depts
        if d["south"] - 0.05 <= lat <= d["north"] + 0.05
        and d["west"] - 0.05 <= lon <= d["east"] + 0.05
    ]
    for d in candidates:
        if point_in_polygon(lat, lon, d["geom"]):
            return d["code"], d["region"]
    # nearest bbox center
    best, best_d = "", 1e18
    for d in depts:
        cy = (d["south"] + d["north"]) / 2
        cx = (d["west"] + d["east"]) / 2
        dist = (lat - cy) ** 2 + (lon - cx) ** 2
        if dist < best_d:
            best_d, best = dist, d
    return best["code"], best["region"]


def main():
    print("loading admin…")
    depts = load_depts()
    regions = load_regions()
    if OUT_DB.exists():
        OUT_DB.unlink()
    conn = sqlite3.connect(OUT_DB)
    cur = conn.cursor()
    cur.execute(
        """CREATE TABLE sites (
            id TEXT PRIMARY KEY, name TEXT NOT NULL, kind TEXT NOT NULL,
            lat REAL NOT NULL, lon REAL NOT NULL, subtitle TEXT,
            wikipedia TEXT, wikidata TEXT, start_date TEXT,
            dept TEXT, region TEXT)"""
    )
    cur.execute(
        """CREATE TABLE departments (
            code TEXT PRIMARY KEY, name TEXT, region TEXT, region_name TEXT,
            south REAL, west REAL, north REAL, east REAL)"""
    )
    cur.execute(
        """CREATE TABLE regions (
            code TEXT PRIMARY KEY, name TEXT,
            south REAL, west REAL, north REAL, east REAL)"""
    )
    cur.execute("CREATE INDEX idx_lat_lon ON sites(lat, lon)")
    cur.execute("CREATE INDEX idx_kind ON sites(kind)")
    cur.execute("CREATE INDEX idx_dept ON sites(dept)")
    cur.execute("CREATE INDEX idx_region ON sites(region)")

    cur.executemany(
        "INSERT INTO departments VALUES (?,?,?,?,?,?,?,?)",
        [(d["code"], d["name"], d["region"], d["region_name"], d["south"], d["west"], d["north"], d["east"]) for d in depts],
    )
    cur.executemany(
        "INSERT INTO regions VALUES (?,?,?,?,?,?)",
        [(r["code"], r["name"], r["south"], r["west"], r["north"], r["east"]) for r in regions],
    )

    churches = 0
    skipped = 0
    print("churches…")
    with (DATA / "churches.csv").open(encoding="utf-8-sig", errors="replace", newline="") as f:
        reader = csv.DictReader(f, delimiter=";")
        batch = []
        for row in reader:
            if not is_christian_church(
                row.get("religion") or "",
                row.get("denomination") or "",
                row.get("name") or "",
                row.get("other_tags") or "",
            ):
                skipped += 1
                continue
            pt = parse_point(row.get("geo_point_2d") or "")
            if not pt:
                continue
            lat, lon = pt
            dept, region = locate(lat, lon, depts)
            osm_id = (row.get("id") or "").rsplit("/", 1)[-1]
            name = (row.get("name") or "").strip() or "Église"
            tags = row.get("other_tags") or ""
            kind = "CHURCH"
            if "chapel" in tags.lower() or name.lower().startswith("chapelle"):
                kind = "CHURCH"
            batch.append((
                f"osm:{osm_id}", name, kind, lat, lon,
                row.get("denomination") or None,
                row.get("wikipedia") or None,
                (row.get("wikidata") or "").rsplit("/", 1)[-1] or None,
                None, dept, region,
            ))
            if len(batch) >= 2000:
                cur.executemany("INSERT OR REPLACE INTO sites VALUES (?,?,?,?,?,?,?,?,?,?,?)", batch)
                churches += len(batch)
                batch.clear()
                print("  churches", churches, "skipped", skipped)
        if batch:
            cur.executemany("INSERT OR REPLACE INTO sites VALUES (?,?,?,?,?,?,?,?,?,?,?)", batch)
            churches += len(batch)
    conn.commit()
    print("churches kept", churches, "skipped non-christian", skipped)

    print("castles…")
    castles = 0
    with (DATA / "castles.csv").open(encoding="utf-8-sig", errors="replace", newline="") as f:
        reader = csv.DictReader(f, delimiter=";")
        batch = []
        for row in reader:
            pt = parse_point(row.get("meta_geo_point") or "")
            if not pt:
                continue
            lat, lon = pt
            dept = (row.get("meta_code_dep") or "").zfill(2) if (row.get("meta_code_dep") or "").isdigit() else (row.get("meta_code_dep") or "")
            region = row.get("meta_code_reg") or DEPT_REGION.get(dept, ("", ""))[0]
            if not dept:
                dept, region = locate(lat, lon, depts)
            osm_id = row.get("meta_osm_id") or ""
            name = (row.get("name") or "").strip() or "Château"
            batch.append((
                f"osm:castle:{osm_id}", name, "CASTLE", lat, lon,
                None, row.get("wikipedia") or None, row.get("wikidata") or None,
                row.get("build_date") or None, dept, region,
            ))
            if len(batch) >= 2000:
                cur.executemany("INSERT OR REPLACE INTO sites VALUES (?,?,?,?,?,?,?,?,?,?,?)", batch)
                castles += len(batch)
                batch.clear()
        if batch:
            cur.executemany("INSERT OR REPLACE INTO sites VALUES (?,?,?,?,?,?,?,?,?,?,?)", batch)
            castles += len(batch)
    conn.commit()
    print("castles", castles)

    # keep curated villages / sanctuaries / altars from previous db if present as extra csv? skip — merge from old sqlite if exists copy
    old = DATA / "heritage_old.db"
    prev = ROOT / "app" / "src" / "main" / "assets" / "heritage.db"
    # already overwritten. Extra curated from seed_extra is gone unless we re-run.
    # Re-insert a few extra kinds from seed_extra.py by importing module later.

    try:
        import seed_extra
        extra = 0
        for kind, rows in (
            ("VILLAGE", seed_extra.VILLAGES),
            ("SANCTUARY", seed_extra.SANCTUARIES),
            ("ALTAR", seed_extra.ALTARS),
        ):
            for i, (name, lat, lon) in enumerate(rows):
                dept, region = locate(lat, lon, depts)
                cur.execute(
                    "INSERT OR IGNORE INTO sites VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    (f"cur:{kind}:{i}", name, kind, lat, lon, None, None, None, None, dept, region),
                )
                extra += 1
        conn.commit()
        print("curated extra", extra)
    except Exception as e:
        print("curated skip", e)

    print("counts", dict(cur.execute("SELECT kind, COUNT(*) FROM sites GROUP BY kind")))
    print("le mans", list(cur.execute(
        "SELECT name, dept, region FROM sites WHERE name LIKE '%Mans%' AND kind='CHURCH' LIMIT 8"
    )))
    conn.close()
    print("db bytes", OUT_DB.stat().st_size)


if __name__ == "__main__":
    main()
