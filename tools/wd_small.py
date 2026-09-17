import json
import sqlite3
import time
import urllib.parse
import urllib.request
from pathlib import Path

OUT = Path(r"C:\Users\technicien\Documents\grok\Cartulaire\app\src\main\assets\heritage.db")
ENDPOINT = "https://query.wikidata.org/sparql"

QUERIES = [
    (
        "CHURCH",
        """
        SELECT ?item ?itemLabel ?coord WHERE {
          ?item wdt:P31 wd:Q2977;
                wdt:P17 wd:Q142;
                wdt:P625 ?coord.
          SERVICE wikibase:label { bd:serviceParam wikibase:language "fr,en". }
        }
        """,
    ),
    (
        "CHURCH",
        """
        SELECT ?item ?itemLabel ?coord WHERE {
          ?item wdt:P31 wd:Q44613;
                wdt:P17 wd:Q142;
                wdt:P625 ?coord.
          SERVICE wikibase:label { bd:serviceParam wikibase:language "fr,en". }
        }
        """,
    ),
    (
        "CASTLE",
        """
        SELECT ?item ?itemLabel ?coord WHERE {
          ?item wdt:P31 wd:Q23413;
                wdt:P17 wd:Q142;
                wdt:P625 ?coord.
          SERVICE wikibase:label { bd:serviceParam wikibase:language "fr,en". }
        }
        LIMIT 4000
        """,
    ),
    (
        "VILLAGE",
        """
        SELECT ?item ?itemLabel ?coord WHERE {
          ?item wdt:P463 wd:Q3374524;
                wdt:P625 ?coord.
          SERVICE wikibase:label { bd:serviceParam wikibase:language "fr,en". }
        }
        """,
    ),
    (
        "SANCTUARY",
        """
        SELECT ?item ?itemLabel ?coord WHERE {
          ?item wdt:P31 wd:Q1258086;
                wdt:P17 wd:Q142;
                wdt:P625 ?coord.
          SERVICE wikibase:label { bd:serviceParam wikibase:language "fr,en". }
        }
        """,
    ),
    (
        "CHURCH",
        """
        SELECT ?item ?itemLabel ?coord WHERE {
          ?item wdt:P31 wd:Q317557;
                wdt:P17 wd:Q142;
                wdt:P625 ?coord.
          SERVICE wikibase:label { bd:serviceParam wikibase:language "fr,en". }
        }
        """,
    ),
]


def parse_point(value: str):
    if not value or "Point(" not in value:
        return None
    inner = value[value.find("Point(") + 6 : value.rfind(")")]
    parts = inner.split()
    if len(parts) < 2:
        return None
    return float(parts[1]), float(parts[0])


def run_query(sparql: str):
    url = ENDPOINT + "?" + urllib.parse.urlencode({"query": sparql, "format": "json"})
    req = urllib.request.Request(
        url,
        headers={
            "User-Agent": "Cartulaire/1.0 (heritage map)",
            "Accept": "application/sparql-results+json",
        },
    )
    with urllib.request.urlopen(req, timeout=90) as resp:
        return json.loads(resp.read().decode("utf-8"))["results"]["bindings"]


def main():
    if OUT.exists():
        OUT.unlink()
    conn = sqlite3.connect(OUT)
    cur = conn.cursor()
    cur.execute(
        """CREATE TABLE sites (
            id TEXT PRIMARY KEY, name TEXT NOT NULL, kind TEXT NOT NULL,
            lat REAL NOT NULL, lon REAL NOT NULL, subtitle TEXT,
            wikipedia TEXT, wikidata TEXT, start_date TEXT)"""
    )
    cur.execute("CREATE INDEX idx_lat_lon ON sites(lat, lon)")
    cur.execute("CREATE INDEX idx_kind ON sites(kind)")
    total = 0
    for kind, sparql in QUERIES:
        print("query", kind)
        try:
            rows = run_query(sparql)
        except Exception as e:
            print(" FAIL", type(e), e)
            time.sleep(8)
            continue
        batch = []
        for b in rows:
            qid = b.get("item", {}).get("value", "").rsplit("/", 1)[-1]
            name = b.get("itemLabel", {}).get("value") or qid
            coord = parse_point(b.get("coord", {}).get("value", ""))
            if not qid or not coord:
                continue
            lat, lon = coord
            batch.append((f"wd:{qid}", name, kind, lat, lon, None, None, qid, None))
        cur.executemany("INSERT OR REPLACE INTO sites VALUES (?,?,?,?,?,?,?,?,?)", batch)
        conn.commit()
        total += len(batch)
        print(" +", len(batch), "total", total)
        time.sleep(8)
    print("counts", dict(cur.execute("SELECT kind, COUNT(*) FROM sites GROUP BY kind")))
    conn.close()
    print("bytes", OUT.stat().st_size)


if __name__ == "__main__":
    main()
