"""Append one Wikidata query into heritage.db. Usage: python wd_one.py KIND"""
import json
import sqlite3
import sys
import urllib.parse
import urllib.request
from pathlib import Path

OUT = Path(r"C:\Users\technicien\Documents\grok\Cartulaire\app\src\main\assets\heritage.db")
KIND = sys.argv[1]
QUERIES = {
    "CASTLE": """
        SELECT ?item ?itemLabel ?coord WHERE {
          ?item wdt:P31 wd:Q23413; wdt:P17 wd:Q142; wdt:P625 ?coord.
          SERVICE wikibase:label { bd:serviceParam wikibase:language "fr,en". }
        } LIMIT 4000
    """,
    "VILLAGE": """
        SELECT ?item ?itemLabel ?coord WHERE {
          ?item wdt:P463 wd:Q3374524; wdt:P625 ?coord.
          SERVICE wikibase:label { bd:serviceParam wikibase:language "fr,en". }
        }
    """,
    "SANCTUARY": """
        SELECT ?item ?itemLabel ?coord WHERE {
          ?item wdt:P31 wd:Q1258086; wdt:P17 wd:Q142; wdt:P625 ?coord.
          SERVICE wikibase:label { bd:serviceParam wikibase:language "fr,en". }
        }
    """,
    "BASILICA": """
        SELECT ?item ?itemLabel ?coord WHERE {
          ?item wdt:P31 wd:Q44613; wdt:P17 wd:Q142; wdt:P625 ?coord.
          SERVICE wikibase:label { bd:serviceParam wikibase:language "fr,en". }
        }
    """,
    "ABBEY": """
        SELECT ?item ?itemLabel ?coord WHERE {
          ?item wdt:P31 wd:Q317557; wdt:P17 wd:Q142; wdt:P625 ?coord.
          SERVICE wikibase:label { bd:serviceParam wikibase:language "fr,en". }
        }
    """,
    "CHAPEL": """
        SELECT ?item ?itemLabel ?coord WHERE {
          ?item wdt:P31 wd:Q108325; wdt:P17 wd:Q142; wdt:P625 ?coord.
          SERVICE wikibase:label { bd:serviceParam wikibase:language "fr,en". }
        } LIMIT 3000
    """,
}
KIND_MAP = {"BASILICA": "CHURCH", "ABBEY": "CHURCH", "CHAPEL": "CHURCH"}


def parse_point(value: str):
    if not value or "Point(" not in value:
        return None
    inner = value[value.find("Point(") + 6 : value.rfind(")")]
    parts = inner.split()
    return float(parts[1]), float(parts[0])


def main():
    sparql = QUERIES[KIND]
    table_kind = KIND_MAP.get(KIND, KIND)
    url = "https://query.wikidata.org/sparql?" + urllib.parse.urlencode({"query": sparql, "format": "json"})
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "Cartulaire/1.0", "Accept": "application/sparql-results+json"},
    )
    with urllib.request.urlopen(req, timeout=90) as resp:
        rows = json.loads(resp.read().decode())["results"]["bindings"]
    conn = sqlite3.connect(OUT)
    cur = conn.cursor()
    batch = []
    for b in rows:
        qid = b.get("item", {}).get("value", "").rsplit("/", 1)[-1]
        name = b.get("itemLabel", {}).get("value") or qid
        coord = parse_point(b.get("coord", {}).get("value", ""))
        if not qid or not coord:
            continue
        lat, lon = coord
        batch.append((f"wd:{qid}", name, table_kind, lat, lon, None, None, qid, None))
    cur.executemany("INSERT OR REPLACE INTO sites VALUES (?,?,?,?,?,?,?,?,?)", batch)
    conn.commit()
    print(KIND, "+", len(batch), "counts", dict(cur.execute("SELECT kind, COUNT(*) FROM sites GROUP BY kind")))
    conn.close()


if __name__ == "__main__":
    main()
