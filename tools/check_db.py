import sqlite3
p = r"C:\Users\technicien\Documents\grok\Cartulaire\app\src\main\assets\heritage.db"
c = sqlite3.connect(p)
print("counts", list(c.execute("SELECT kind, COUNT(*) FROM sites GROUP BY kind")))
print("total", c.execute("SELECT COUNT(*) FROM sites").fetchone())
q = "%Mans%"
print("mans", list(c.execute("SELECT name, kind, lat, lon FROM sites WHERE name LIKE ?", (q,))))
q2 = "%athédral%"
print("cathedrals sample", list(c.execute("SELECT name FROM sites WHERE name LIKE ? LIMIT 30", (q2,))))
print("le mans coords nearby", list(c.execute(
    "SELECT name, kind, lat, lon FROM sites WHERE lat BETWEEN 47.9 AND 48.1 AND lon BETWEEN 0.0 AND 0.4"
)))
