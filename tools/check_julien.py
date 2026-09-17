import sqlite3
c = sqlite3.connect(r"C:\Users\technicien\Documents\grok\Cartulaire\app\src\main\assets\heritage.db")
for q in ("%Julien%", "%Mans%", "%Nantes%", "%Laval%", "%Sées%", "%Seez%"):
    rows = list(c.execute("SELECT name FROM sites WHERE name LIKE ?", (q,)))
    print(q, rows[:10], "n=", len(rows))
