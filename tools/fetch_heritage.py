"""Build a local SQLite of OSM heritage sites (France + nearby)."""
from __future__ import annotations

import json
import sqlite3
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

OUT = Path(r"C:\Users\technicien\Documents\grok\Cartulaire\app\src\main\assets\heritage.db")
OUT.parent.mkdir(parents=True, exist_ok=True)

ENDPOINTS = [
    "https://overpass.kumi.systems/api/interpreter",
    "https://overpass-api.de/api/interpreter",
    "https://overpass.openstreetmap.fr/api/interpreter",
]

# Tiles covering France, Belgium, Switzerland west, northern Spain, northern Italy
TILES = []
for lat0 in range(42, 52, 2):
    for lon0 in range(-6, 10, 2):
        TILES.append((lat0, lon0, lat0 + 2, lon0 + 2))


def overpass(query: str) -> dict:
    data = urllib.parse.urlencode({"data": query}).encode()
    last = None
    for url in ENDPOINTS:
        req = urllib.request.Request(
            url,
            data=data,
            headers={"User-Agent": "Cartulaire/1.0 heritage harvest", "Content-Type": "application/x-www-form-urlencoded"},
        )
        try:
            with urllib.request.urlopen(req, timeout=90) as resp:
                return json.loads(resp.read().decode("utf-8"))
        except Exception as e:
            last = e
            print("  fail", url, e)
            time.sleep(2)
    raise RuntimeError(last)


def coords(el: dict) -> tuple[float, float] | None:
    if "lat" in el and "lon" in el:
        return float(el["lat"]), float(el["lon"])
    c = el.get("center")
    if c and "lat" in c:
        return float(c["lat"]), float(c["lon"])
    return None


def classify(tags: dict) -> str | None:
    building = tags.get("building", "")
    historic = tags.get("historic", "")
    amenity = tags.get("amenity", "")
    religion = tags.get("religion", "")
    place = tags.get("place", "")
    ruins = tags.get("ruins", "")
    worship = tags.get("place_of_worship", "")
    name = (tags.get("name:fr") or tags.get("name") or "").lower()
    is_parish = building in {"church", "cathedral"} or historic == "church"

    if historic == "altar" or tags.get("man_made") == "altar" or worship == "altar":
        return "ALTAR"
    if (
        historic in {"shrine", "wayside_shrine"}
        or building == "shrine"
        or worship == "shrine"
        or "sanctuaire" in name
        or "oratoire" in name
        or (tags.get("pilgrimage") == "yes" and not is_parish)
    ):
        return "SANCTUARY"
    if building in {"church", "cathedral", "chapel"} or historic == "church" or (
        amenity == "place_of_worship" and religion == "christian"
    ):
        return "CHURCH"
    if historic in {"castle", "fort"} or ruins == "castle" or "castle_type" in tags:
        return "CASTLE"
    if place in {"village", "town", "hamlet"} and (historic or tags.get("heritage")):
        return "VILLAGE"
    if historic in {"citywalls", "city_gate"}:
        return "VILLAGE"
    return None


def default_name(kind: str, tags: dict) -> str:
    building = tags.get("building", "")
    if building == "cathedral":
        return "Cathédrale"
    if building == "chapel":
        return "Chapelle"
    if tags.get("historic") == "wayside_shrine":
        return "Oratoire"
    return {
        "CHURCH": "Église",
        "CASTLE": "Château",
        "VILLAGE": "Cité médiévale",
        "ALTAR": "Autel",
        "SANCTUARY": "Sanctuaire",
    }[kind]


def harvest_tile(s, w, n, e) -> list[dict]:
    bbox = f"{s},{w},{n},{e}"
    query = f"""
    [out:json][timeout:60];
    (
      nwr["historic"="castle"]({bbox});
      nwr["historic"="fort"]({bbox});
      nwr["building"="cathedral"]({bbox});
      nwr["building"="church"]["name"]({bbox});
      nwr["building"="chapel"]["name"]({bbox});
      nwr["amenity"="place_of_worship"]["religion"="christian"]["name"]({bbox});
      nwr["historic"="church"]({bbox});
      nwr["place"~"^(village|town|hamlet)$"]["historic"]({bbox});
      nwr["place"~"^(village|town|hamlet)$"]["heritage"]({bbox});
      nwr["historic"="citywalls"]({bbox});
      nwr["historic"="altar"]({bbox});
      nwr["man_made"="altar"]({bbox});
      nwr["historic"="shrine"]({bbox});
      nwr["historic"="wayside_shrine"]["name"]({bbox});
      nwr["pilgrimage"="yes"]({bbox});
      nwr["name"~"sanctuaire", i]({bbox});
    );
    out center tags;
    """
    data = overpass(query)
    rows = []
    for el in data.get("elements", []):
        tags = el.get("tags") or {}
        kind = classify(tags)
        xy = coords(el)
        if not kind or not xy:
            continue
        lat, lon = xy
        name = tags.get("name:fr") or tags.get("name") or default_name(kind, tags)
        rows.append(
            {
                "id": f"{el.get('type')}:{el.get('id')}",
                "name": name,
                "kind": kind,
                "lat": lat,
                "lon": lon,
                "subtitle": tags.get("start_date") or tags.get("castle_type") or tags.get("denomination"),
                "wikipedia": tags.get("wikipedia"),
                "wikidata": tags.get("wikidata"),
                "start_date": tags.get("start_date"),
            }
        )
    return rows


def main():
    if OUT.exists():
        OUT.unlink()
    conn = sqlite3.connect(OUT)
    cur = conn.cursor()
    cur.execute(
        """
        CREATE TABLE sites (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            kind TEXT NOT NULL,
            lat REAL NOT NULL,
            lon REAL NOT NULL,
            subtitle TEXT,
            wikipedia TEXT,
            wikidata TEXT,
            start_date TEXT
        )
        """
    )
    cur.execute("CREATE INDEX idx_lat_lon ON sites(lat, lon)")
    cur.execute("CREATE INDEX idx_kind ON sites(kind)")

    total = 0
    for i, (s, w, n, e) in enumerate(TILES, 1):
        print(f"[{i}/{len(TILES)}] {s},{w} -> {n},{e}")
        try:
            rows = harvest_tile(s, w, n, e)
        except Exception as ex:
            print("  SKIP", ex)
            time.sleep(3)
            continue
        cur.executemany(
            """
            INSERT OR REPLACE INTO sites (id, name, kind, lat, lon, subtitle, wikipedia, wikidata, start_date)
            VALUES (:id, :name, :kind, :lat, :lon, :subtitle, :wikipedia, :wikidata, :start_date)
            """,
            rows,
        )
        conn.commit()
        total += len(rows)
        print(f"  +{len(rows)}  total~{total}")
        time.sleep(1.2)

    counts = dict(cur.execute("SELECT kind, COUNT(*) FROM sites GROUP BY kind"))
    print("DONE", dict(cur.execute("SELECT COUNT(*) FROM sites")) , counts)
    conn.close()
    print("wrote", OUT, "bytes", OUT.stat().st_size)


if __name__ == "__main__":
    main()
