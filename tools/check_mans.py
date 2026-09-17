import sqlite3
c = sqlite3.connect(r"C:\Users\technicien\Documents\grok\Cartulaire\app\src\main\assets\heritage.db")
print("dept 72 churches", c.execute("SELECT COUNT(*) FROM sites WHERE dept='72' AND kind='CHURCH'").fetchone())
print("cathedrale 72", list(c.execute("SELECT name FROM sites WHERE dept='72' AND name LIKE '%athédral%'")))
print("near", list(c.execute(
    "SELECT name, kind, lat, lon FROM sites WHERE lat BETWEEN 48.00 AND 48.02 AND lon BETWEEN 0.19 AND 0.21"
)))
print("kinds", list(c.execute("SELECT kind, COUNT(*) FROM sites GROUP BY kind")))
print("regions geo", ( __import__('pathlib').Path(r'C:\Users\technicien\Documents\grok\Cartulaire\app\src\main\assets\map\regions.geojson').stat().st_size))
