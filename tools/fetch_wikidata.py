"""Harvest heritage coordinates from Wikidata into heritage.db."""
from __future__ import annotations

import json
import sqlite3
import time
import urllib.parse
import urllib.request
from pathlib import Path

OUT = Path(r"C:\Users\technicien\Documents\grok\Cartulaire\app\src\main\assets\heritage.db")
OUT.parent.mkdir(parents=True, exist_ok=True)
ENDPOINT = "https://query.wikidata.org/sparql"

QUERIES = {
    "CASTLE": """
    SELECT ?item ?itemLabel ?coord WHERE {
      ?item wdt:P31/wdt:P279* wd:Q23413;
            wdt:P625 ?coord.
      OPTIONAL { ?item wdt:P17 ?country. }
      FILTER(?country IN (wd:Q142, wd:Q31, wd:Q39, wd:Q183, wd:Q29, wd:Q38, wd:Q145, wd:Q55, wd:Q40, wd:Q213))
      SERVICE wikibase:label { bd:serviceParam wikibase:language "fr,en". }
    }
    """,
    "CHURCH": """
    SELECT ?item ?itemLabel ?coord WHERE {
      VALUES ?type { wd:Q2977 wd:Q16970 wd:Q44539 wd:Q317557 wd:Q108325 wd:Q44613 }
      ?item wdt:P31/wdt:P279* ?type;
            wdt:P17 wd:Q142;
            wdt:P625 ?coord.
      SERVICE wikibase:label { bd:serviceParam wikibase:language "fr,en". }
    }
    """,
    "SANCTUARY": """
    SELECT ?item ?itemLabel ?coord WHERE {
      VALUES ?type { wd:Q697295 wd:Q1258086 wd:Q3395121 wd:Q381885 wd:Q44613 }
      ?item wdt:P31/wdt:P279* ?type;
            wdt:P625 ?coord.
      OPTIONAL { ?item wdt:P17 ?country. }
      FILTER(?country IN (wd:Q142, wd:Q31, wd:Q39, wd:Q183, wd:Q29, wd:Q38, wd:Q145, wd:Q40) || BOUND(?country) = false)
      SERVICE wikibase:label { bd:serviceParam wikibase:language "fr,en". }
    }
    """,
    "ALTAR": """
    SELECT ?item ?itemLabel ?coord WHERE {
      ?item wdt:P31/wdt:P279* wd:Q101687;
            wdt:P625 ?coord.
      SERVICE wikibase:label { bd:serviceParam wikibase:language "fr,en". }
    }
    """,
    "VILLAGE": """
    SELECT ?item ?itemLabel ?coord WHERE {
      { ?item wdt:P463 wd:Q3374524. }
      UNION
      { ?item wdt:P31 wd:Q676050; wdt:P17 wd:Q142. }
      ?item wdt:P625 ?coord.
      SERVICE wikibase:label { bd:serviceParam wikibase:language "fr,en". }
    }
    """,
}


def parse_point(value: str) -> tuple[float, float] | None:
    # Point(lon lat)
    if not value or "Point(" not in value:
        return None
    inner = value[value.find("Point(") + 6 : value.find(")", value.find("Point("))]
    parts = inner.split()
    if len(parts) < 2:
        return None
    return float(parts[1]), float(parts[0])


def run_query(sparql: str) -> list[dict]:
    url = ENDPOINT + "?" + urllib.parse.urlencode({"query": sparql, "format": "json"})
    req = urllib.request.Request(
        url,
        headers={
            "User-Agent": "Cartulaire/1.0 (heritage map; educational)",
            "Accept": "application/sparql-results+json",
        },
    )
    with urllib.request.urlopen(req, timeout=120) as resp:
        payload = json.loads(resp.read().decode("utf-8"))
    return payload.get("results", {}).get("bindings", [])


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
    for kind, sparql in QUERIES.items():
        print("query", kind)
        try:
            rows = run_query(sparql)
        except Exception as e:
            print("  FAIL", e)
            time.sleep(2)
            continue
        batch = []
        for b in rows:
            qid = b.get("item", {}).get("value", "").rsplit("/", 1)[-1]
            name = b.get("itemLabel", {}).get("value") or qid
            coord = parse_point(b.get("coord", {}).get("value", ""))
            if not qid or not coord:
                continue
            lat, lon = coord
            if not (-90 <= lat <= 90 and -180 <= lon <= 180):
                continue
            batch.append((f"wd:{qid}", name, kind, lat, lon, None, None, qid, None))
        cur.executemany(
            "INSERT OR REPLACE INTO sites VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            batch,
        )
        conn.commit()
        total += len(batch)
        print(f"  +{len(batch)} total {total}")
        time.sleep(1)

    print("counts", dict(cur.execute("SELECT kind, COUNT(*) FROM sites GROUP BY kind")))
    print("total", cur.execute("SELECT COUNT(*) FROM sites").fetchone()[0])
    conn.close()
    print("wrote", OUT, OUT.stat().st_size)


if __name__ == "__main__":
    main()
